package io.advantageous.reakt.netty;

import io.advantageous.reakt.Expected;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.function.Consumer;

public class Server {

    private final EventLoopGroup parentGroup;
    private final EventLoopGroup childGroup;
    private final ServerBootstrap serverBootstrap;
    private final int port;
    private final Class<? extends ServerChannel> channelClassType;
    private final ChannelInboundHandlerAdapter channelInboundHandlerAdapter;
    private final ChannelHandler handler;
    private final LogLevel logLevel;
    private final Expected<Runnable> onClose;

    public Server(final EventLoopGroup parentGroup, final EventLoopGroup childGroup,
                  final ServerBootstrap serverBootstrap,
                  final int port, Class<? extends ServerChannel> channelClassType,
                  final ChannelInboundHandlerAdapter channelInboundHandlerAdapter, ChannelHandler handler,
                  final LogLevel logLevel,
                  final Runnable onClose) {
        this.parentGroup = parentGroup;
        this.childGroup = childGroup;
        this.serverBootstrap = serverBootstrap;
        this.port = port;
        this.channelClassType = channelClassType;
        this.channelInboundHandlerAdapter = channelInboundHandlerAdapter;
        this.handler = handler;
        this.logLevel = logLevel;
        this.onClose = Expected.ofNullable(onClose);
    }

    public void start() {
        try {

            serverBootstrap.group(parentGroup, childGroup)
                    .channel(channelClassType);

            /** Add logging if requested. */
            if (logLevel != null) {
                serverBootstrap.handler(new LoggingHandler(logLevel));
                serverBootstrap.childHandler(handler);
            } else {
                serverBootstrap.handler(handler);
            }

            final Channel serverChannel = serverBootstrap.bind(port).sync().channel();
            onClose.ifPresent(runnable -> serverChannel.closeFuture().addListener(future -> runnable.run()));
            serverChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new IllegalStateException(e);
        } finally {
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
        }
    }
}
