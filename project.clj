(def jig-version "2.0.0-RC6-SNAPSHOT")

(defproject opensensors/mqtt-broker "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [jig/netty ~jig-version]
                 [jig/netty-mqtt ~jig-version]
                 [jig/async ~jig-version]])
