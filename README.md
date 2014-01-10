# MQTT Broker

An mqtt broker on top of netty, netty.io. This is a very early stage project and should be considered experimental.

The intention is to build a fully async non blocking mqtt server.

## Motivation

Writing network services used to be hard or expensive. Now it is
neither, and this is what I want to demonstrate by publishing the code
of a working broker for the [MQTT](http://mqtt.org) protocol.

Of course, this is not yet a complete implementation of MQTT. It doesn't
include QoS, Last Will and Testament and other feature of the MQTT
protocol. It does, however, show how to write a modern scaleable
service, using Clojure on the JVM.

## Ingredients

* git
* Java Virtual Machine
* [Leiningen](http://leiningen.org/)
* [GraphViz](http://www.graphviz.org/)
* [mosquitto](http://mosquitto.org/)

## Method

```
$ lein repl
$ (go)

```

Open a new terminal and run the following

```
mosquitto_sub -t /test
```

Now open another terminal to publish messages to the broker, seeing them
appear in the other terminal.

```
mosquitto_pub -t /test -m "Hello"
```

## How it works

[config.clj](config.clj) shows you which components make up this system,
including the codec provided by
[clj-mqtt](https://github.com/xively/clj-mqtt).

Jig reads the [config.clj](config.clj) file and instantiates the
components therein. If you have [graphviz]() installed you can type
`(graph)` at the console to view a diagram showing how the components
fit together.

We provide the code to just one of these called `MqttHandler` which injects a
function into the _system map_. (Jig is a bit like an application
container and the 'system map' is an emerging Clojure pattern to collate
application state into a single data structure).

Another component called `jig.netty/Server` is specified here, along
with its port number (1883). This provides a generic adapter for Netty
5 - as such the code for this is in this [Jig extension](). On start up,
this component looks across its dependencies to find the other
components to include in the pipeline that it creates when a client
connects.

`jig.netty.mqtt/MqttEncoder` and `jig.netty.mqtt/MqttDecoder`, are
adapters for the codec code kindly provided by clj-mqtt. This codec
further reduces the Clojure code we must write, since they convert
between low-level Netty byte buffers and convenient Clojure maps.

Each request is handled by  the `case` form, dispatching on the :type value of the map decoded by clj-mqtt :-

```clojure
(case (:type msg)
          :connect (.writeAndFlush ctx {:type :connack})
          :pingreq (.writeAndFlush ctx {:type :pingresp})
          :publish (doseq [ctx (get @subs (:topic msg))]
                     (.writeAndFlush ctx msg))
          :subscribe (do (.writeAndFlush ctx {:type :suback})
                         (swap! subs (fn [subs]
                                       (reduce #(update-in %1 [%2] conj ctx)
                                               subs (map first (:topics msg))))))
          :disconnect (.close ctx))
```

The `:subscribe` handler acknowledges the request and places the context
in the `subs` map, using the topic as the key and a list of subsribers
as the value. You can't mutate maps in Clojure without using its STM and
I've chosen to use an atom. The `:publish` handler looks up the
subscribers based on the topic key and writes out the incoming message
to each of them.

## Caveats

Obviously this implementation is extremely threadbare, but I'm
publishing it now to show the core essence of a broker and as a
foundation on which I and others can build the rest of the MQTT broker
implementation.

## License

Copyright © 2014 opensensors.io

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

The use and distribution terms for this software are covered by the [Eclipse Public License 1.0](http://opensource.org/licenses/eclipse-1.0.php) which can be found in the file epl-v10.html at the root of this distribution. By using this software in any fashion, you are agreeing to be bound by the terms of this license. You must not remove this notice, or any other, from this software.
