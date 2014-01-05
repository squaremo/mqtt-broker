(ns mqtt-broker.core
  (:require
   jig
   [jig.util :refer (get-dependencies satisfying-dependency)]
   [mqtt.decoder :refer (make-decoder)]
   [mqtt.encoder :refer (make-encoder)]
   [clojure.tools.logging :refer :all])
  (:import
   (io.netty.util ReferenceCountUtil)
   (io.netty.channel EventLoopGroup ChannelInitializer ChannelHandlerAdapter ChannelOption ChannelHandler)
   (io.netty.channel.nio NioEventLoopGroup)
   (io.netty.bootstrap ServerBootstrap)
   (io.netty.channel.socket.nio NioServerSocketChannel)
   (jig Lifecycle)))

(deftype MqttDecoder [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (debugf "Adding netty handler to system at %s" [(:jig/id config) ::handler-factory])
    (assoc-in system [(:jig/id config) ::handler-factory] #(make-decoder)))
  (stop [_ system] system))

(deftype MqttEncoder [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (debugf "Adding netty handler to system at %s" [(:jig/id config) ::handler-factory])
    (assoc-in system [(:jig/id config) ::handler-factory] #(make-encoder)))
  (stop [_ system] system))

(defn reply [ctx msg]
  (doto ctx
    (.write msg)
    (.flush)))

(deftype MqttHandler [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (debugf "Adding netty handler to system at %s" [(:jig/id config) ::handler-factory])
    (assoc-in
     system
     [(:jig/id config) ::handler-factory]
     #(proxy [ChannelHandlerAdapter] []
        (channelRead [ctx msg]
          (case (:type msg)
            :connect (reply ctx {:type :connack})
            :pingreq (reply ctx {:type :pingresp})
            :publish (infof "PUBLISH MESSAGE: topic is %s, payload is '%s'" (:topic msg) (String. (:payload msg)))
            :disconnect (.close ctx)
            (throw (ex-info (format "TODO: handle type: %s" (:type msg)) msg))))

        (exceptionCaught [ctx cause]
          (.printStackTrace cause)
          (.close ctx)))))
  (stop [_ system] system))

(deftype EchoHandler [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (assoc-in system [(:jig/id config) ::handler-factory]
              #(proxy [ChannelHandlerAdapter] []
                 (channelRead [ctx msg]
                   (.write ctx msg)
                   (.flush ctx))
                 (exceptionCaught [ctx cause]
                   (.printStackTrace cause)
                   (.close ctx)))))
  (stop [_ system] system))

;; From https://github.com/netty/netty/wiki/User-guide-for-5.x
(deftype Server [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]

    (debugf "Dependencies are %s" (vec (get-dependencies system config)))

    (let [handlers (keep #(get-in system [(:jig/id %) ::handler-factory]) (get-dependencies system config))]

      (when (empty? handlers)
        (throw (ex-info (format "No dependencies of %s register entries of %s"
                                (:jig/id config) ::handler-factory) {:jig/id (:jig/id config)})))

      (let [boss-group (NioEventLoopGroup.)
            worker-group (NioEventLoopGroup.)]
        (let [b (ServerBootstrap.)]
          (-> b
              (.group boss-group worker-group)
              (.channel NioServerSocketChannel)
              (.childHandler
               (proxy [ChannelInitializer] []
                 (initChannel [ch]
                   (debugf "Initializing channel with handlers: %s" (vec handlers))
                   (-> ch (.pipeline) (.addLast (into-array ChannelHandler (map (fn [f] (f)) handlers)))))))
              (.option ChannelOption/SO_BACKLOG (int 128))
              (.childOption ChannelOption/SO_KEEPALIVE true))

          (-> system
              (assoc-in [(:jig/id config) :channel] (-> b (.bind (int 1234))))
              (assoc-in [(:jig/id config) :event-loop-groups :boss-group] (NioEventLoopGroup.))
              (assoc-in [(:jig/id config) :event-loop-groups :worker-group] (NioEventLoopGroup.)))))))
  (stop [_ system]
    (let [fut (get-in system [(:jig/id config) :channel])]
      (.awaitUninterruptibly fut)       ; await for it to be bound
      (-> fut (.channel) (.close) (.sync)))
    (.shutdownGracefully (get-in system [(:jig/id config) :event-loop-groups :worker-group]))
    (.shutdownGracefully (get-in system [(:jig/id config) :event-loop-groups :boss-group]))
    system))
