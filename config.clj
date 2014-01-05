{:jig/components


 {:decoder
  {:jig/component mqtt-broker.core/MqttDecoder
   :jig/project "../mqtt-broker/project.clj"}

  :handler
  {:jig/component mqtt-broker.core/MqttHandler
   :jig/project "../mqtt-broker/project.clj"}

  :server
  {:jig/component mqtt-broker.core/Server
   :jig/dependencies [:decoder :handler]
   :jig/project "../mqtt-broker/project.clj"}}}
