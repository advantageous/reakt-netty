package io.advantageous.reakt.netty;

import io.advantageous.reakt.Expected;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;

import java.net.URI;

public class Client {

    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final int port;
    private final Class<? extends SocketChannel> channelClassType;
    private final ChannelInboundHandlerAdapter channelInboundHandlerAdapter;
    private final ChannelHandler handler;
    private final Expected<Runnable> onClose;
    private final URI connectionUri;


    public Client(EventLoopGroup group,
                  Bootstrap bootstrap,
                  int port,
                  Class<? extends SocketChannel> channelClassType,
                  ChannelInboundHandlerAdapter channelInboundHandlerAdapter,
                  ChannelHandler handler, Expected<Runnable> onClose,
                  URI connectionUri) {
        this.group = group;
        this.bootstrap = bootstrap;
        this.port = port;
        this.channelClassType = channelClassType;
        this.channelInboundHandlerAdapter = channelInboundHandlerAdapter;
        this.handler = handler;
        this.onClose = onClose;
        this.connectionUri = connectionUri;
    }


    public void sendRequest(final URI requestURI, final String contentType, final String body) {


        final HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, requestURI.getRawPath());
        request.headers().set(HttpHeaderNames.HOST, requestURI.getHost());
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        try {
            final Channel clientChannel = bootstrap.connect(connectionUri.getHost(), connectionUri.getPort()).sync().channel();
            clientChannel.writeAndFlush(request).addListener(ChannelFutureListener.CLOSE);

        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new IllegalStateException(e);
        }
    }

    public void start() {
        //try {
        bootstrap.group(group)
                .channel(channelClassType).handler(handler);
//        } catch (InterruptedException e) {
//            Thread.interrupted();
//            throw new IllegalStateException(e);
//        } finally {
//            group.shutdownGracefully();
//        }
    }

    public void stop() {

        group.shutdownGracefully();
    }



    static final String URI = System.getProperty("url", "http://127.0.0.1:8080/hello");
    public static void main(final String... args) throws Exception {


        EventLoopGroup group = new NioEventLoopGroup();

    }
}
