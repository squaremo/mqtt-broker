(ns mqtt-broker.core
  (:require jig)
  (:import
   (io.netty.util ReferenceCountUtil)
   (io.netty.channel EventLoopGroup ChannelInitializer ChannelHandlerAdapter ChannelOption)
   (io.netty.channel.nio NioEventLoopGroup)
   (io.netty.bootstrap ServerBootstrap)
   (io.netty.channel.socket.nio NioServerSocketChannel)
   (jig Lifecycle)))

(deftype Handler [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system] system
    )
  (stop [_ system] system
    system
    ))

;; From https://github.com/netty/netty/wiki/User-guide-for-5.x
(deftype Server [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (let [boss-group (NioEventLoopGroup.)
          worker-group (NioEventLoopGroup.)]
      (let [b (ServerBootstrap.)]
        (-> b
            (.group boss-group worker-group)
            (.channel NioServerSocketChannel)
            (.childHandler
             (proxy [ChannelInitializer] []
               (initChannel [ch]
                 (-> ch (.pipeline)
                     (.addLast (proxy [ChannelHandlerAdapter] []
                                 (channelRead [ctx msg]
                                   (.release msg))
                                 (exceptionCaught [ctx cause]
                                   (.printStackTrace cause)
                                   (.close ctx))))))))
            (.option ChannelOption/SO_BACKLOG (int 128))
            (.childOption ChannelOption/SO_KEEPALIVE true))

        (-> system
            (assoc-in [(:jig/id config) :channel] (-> b (.bind (int 1234))))
            (assoc-in [(:jig/id config) :event-loop-groups :boss-group] (NioEventLoopGroup.))
            (assoc-in [(:jig/id config) :event-loop-groups :worker-group] (NioEventLoopGroup.)))))
    )
  (stop [_ system]
    (let [fut (get-in system [(:jig/id config) :channel])]
      (.awaitUninterruptibly fut) ; await for it to be bound
      (-> fut (.channel) (.close) (.sync))
      ) ; TODO Use protocols to deref channel futures
    (.shutdownGracefully (get-in system [(:jig/id config) :event-loop-groups :worker-group]))
    (.shutdownGracefully (get-in system [(:jig/id config) :event-loop-groups :boss-group]))
    system
    ))
