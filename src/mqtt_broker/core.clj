(ns mqtt-broker.core
  (:require
   jig
   [jig.util :refer (get-dependencies satisfying-dependency)]
   [clojure.core.async :refer (>!!)]
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

(defn reply [ctx msg]
  (infof "Replying to MQTT message with response: %s" msg)
  (doto ctx
    (.write msg)
    (.flush)))

(deftype MqttHandler [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (let [ch (some (comp :channel system) (:jig/dependencies config))
          subs (atom {})]
      (assert ch)
      (infof "Channel %s" ch)
      (debugf "Adding netty handler to system at %s" [(:jig/id config) :jig.netty/handler-factory])
      (-> system
          (assoc-in
           [(:jig/id config) :jig.netty/handler-factory]
           (fn []
             (proxy [ChannelHandlerAdapter] []
               (channelActive [ctx]
                 (infof "Channel active!"))
               (channelRead [ctx msg]
                 (case (:type msg)
                   :connect (reply ctx {:type :connack})
                   :pingreq (reply ctx {:type :pingresp})

                   :publish (do
                              (infof "Message received on topic '%s', publish to %d subscribers" (:topic msg) (count (get @subs (:topic msg))))
                              (doseq [ctx (get @subs (:topic msg))]
                                (.writeAndFlush ctx msg)))

                   :subscribe (do
                                (infof "Subscribe to %s" (:topics msg))
                                ;; TODO QoS
                                (reply ctx {:type :suback})
                                (swap! subs (fn [subs]
                                              (reduce #(update-in %1 [%2] conj ctx) subs (map first (:topics msg)))))
                                )
                   :disconnect (.close ctx)
                   (throw (ex-info (format "TODO: handle type: %s" (:type msg)) msg))))

               (exceptionCaught [ctx cause]
                 (try
                   (throw cause)
                   (finally (.close ctx)))

                 ))))
          (assoc-in [(:jig/id config) :subscriptions] subs))))
  (stop [_ system] system))
