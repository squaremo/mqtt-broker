(def jig-version "2.0.0-RC5")

(defproject opensensors/mqtt-broker "0.1.0"
  :description "The OpenSensors.io MQTT broker"
  :url "https://github.com/OpenSensorsIO/mqtt-broker"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [jig/netty ~jig-version]
                 [jig/netty-mqtt ~jig-version]])
