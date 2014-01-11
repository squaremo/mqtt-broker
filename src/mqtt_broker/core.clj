;; Copyright © 2014, OpenSensors.IO. All Rights Reserved.
(ns mqtt-broker.core
  (:require jig)
  (:import
   (io.netty.channel ChannelHandlerAdapter)
   (jig Lifecycle)))

(defn make-channel-handler [subs]
  (proxy [ChannelHandlerAdapter] []
    (channelRead [ctx msg]
      (case (:type msg)
        :connect (.writeAndFlush ctx {:type :connack})
        :subscribe (do (.writeAndFlush ctx
                                       {:type :suback
                                        :message-id (:message-id msg)})
                       (swap! subs (fn [subs]
                                     (reduce #(update-in %1 [%2] conj ctx)
                                             subs (map first (:topics msg))))))
        :publish (doseq [ctx (get @subs (:topic msg))]
                   (.writeAndFlush ctx msg))
        :pingreq (.writeAndFlush ctx {:type :pingresp})
        :disconnect (.close ctx)))
    (exceptionCaught [ctx cause]
      (try (throw cause)
           (finally (.close ctx))))))

(deftype MqttHandler [config]
  Lifecycle
  (init [_ system]
    system)
  (start [_ system]
    (let [submap (atom {})]
      (assoc-in system
                [(:jig/id config) :jig.netty/handler-factory]
                #(make-channel-handler submap))))
  (stop [_ system] system))
