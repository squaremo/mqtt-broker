{:jig/components

 {:pub-channel
  {:jig/component jig.async/Channel
   :jig/project "../mqtt-broker/project.clj"
   :size 100}

  :mqtt-decoder
  {:jig/component jig.netty.mqtt/MqttDecoder
   :jig/project "../mqtt-broker/project.clj"}

  :mqtt-encoder
  {:jig/component jig.netty.mqtt/MqttEncoder
   :jig/project "../mqtt-broker/project.clj"}

  :mqtt-handler
  {:jig/component mqtt-broker.core/MqttHandler
   :jig/project "../mqtt-broker/project.clj"
   :jig/dependencies [:pub-channel]}

  :mqtt-server
  {:jig/component jig.netty/Server
   :jig/dependencies [:mqtt-decoder :mqtt-encoder :mqtt-handler]
   :jig/project "../mqtt-broker/project.clj"
   :port 1883}}}
