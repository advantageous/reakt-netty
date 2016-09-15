package io.advantageous.reakt.netty;

import io.advantageous.reakt.Expected;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;

public abstract class Client {

    static final String URI = System.getProperty("url", "http://127.0.0.1:8080/hello");
    protected final EventLoopGroup group;
    protected final Bootstrap bootstrap;
    private final Class<? extends SocketChannel> channelClassType;
    private final ChannelInboundHandlerAdapter channelInboundHandlerAdapter;
    private final ChannelHandler handler;
    private final Expected<Runnable> onClose;


    public Client(EventLoopGroup group,
                  Bootstrap bootstrap,
                  Class<? extends SocketChannel> channelClassType,
                  ChannelInboundHandlerAdapter channelInboundHandlerAdapter,
                  ChannelHandler handler,
                  Runnable onClose) {
        this.group = group;
        this.bootstrap = bootstrap;
        this.channelClassType = channelClassType;
        this.channelInboundHandlerAdapter = channelInboundHandlerAdapter;
        this.handler = handler;
        this.onClose = Expected.ofNullable(onClose);
    }

    public void start() {

        new Thread(() -> {

            try {
                bootstrap.group(group)
                        .channel(channelClassType)
                        .handler(handler);


                process();

            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                group.shutdownGracefully();
            }
        }).start();


    }

    protected abstract void process() throws Exception;

    public void stop() {
        group.shutdownGracefully();
    }

}
