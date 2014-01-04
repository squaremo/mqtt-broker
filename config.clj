{:jig/components


 {:handler
  {:jig/component mqtt-broker.core/Handler
   :jig/project "../mqtt-broker/project.clj"}

  :server
  {:jig/component mqtt-broker.core/Server
   :jig/dependencies [:handler]
   :jig/project "../mqtt-broker/project.clj"}}}
