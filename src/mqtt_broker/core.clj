(ns mqtt-broker.core
  (:require
   jig
   [jig.util :refer (get-dependencies satisfying-dependency)]
   [mqtt.decoder :refer (make-decoder)]
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
    (infof "Adding netty handler to system at %s" [(:jig/id config) ::handler])
    (assoc-in system [(:jig/id config) ::handler]
              (make-decoder)))
  (stop [_ system] system))

(deftype MqttHandler [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (infof "Adding netty handler to system at %s" [(:jig/id config) ::handler])
    (assoc-in system [(:jig/id config) ::handler]
              (proxy [ChannelHandlerAdapter] []
                (channelRead [ctx msg]
                  (infof "Got request!! msg is %s" msg)
                  (.write ctx msg)
                  (.flush ctx)
                  ;;(.release msg)
                  )
                (exceptionCaught [ctx cause]
                  (.printStackTrace cause)
                  (.close ctx)))))
  (stop [_ system] system))

(deftype GenericHandler [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (infof "Adding netty handler to system at %s" [(:jig/id config) ::handler])
    (assoc-in system [(:jig/id config) ::handler]
           (proxy [ChannelHandlerAdapter] []
             (channelRead [ctx msg]
               (infof "Got request!! msg is %s" (type msg))
               (.write ctx msg)
               (.flush ctx)
               ;;(.release msg)
               )
             (exceptionCaught [ctx cause]
               (.printStackTrace cause)
               (.close ctx)))))
  (stop [_ system] system))

;; From https://github.com/netty/netty/wiki/User-guide-for-5.x
(deftype Server [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]

    (infof "Dependencies are %s" (vec (get-dependencies system config)))

    (let [handlers (keep #(get-in system [(:jig/id %) ::handler]) (get-dependencies system config))]

      (when (empty? handlers)
        (throw (ex-info (format "No dependencies of %s register entries of %s"
                                (:jig/id config) ::handler) {:jig/id (:jig/id config)})))

      (let [boss-group (NioEventLoopGroup.)
            worker-group (NioEventLoopGroup.)]
        (let [b (ServerBootstrap.)]
          (-> b
              (.group boss-group worker-group)
              (.channel NioServerSocketChannel)
              (.childHandler
               (proxy [ChannelInitializer] []
                 (initChannel [ch]
                   (infof "Initializing channel with handlers: %s" (vec handlers))
                   (-> ch (.pipeline) (.addLast (into-array ChannelHandler handlers))))))
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
