package io.advantageous.reakt.netty;

import io.advantageous.reakt.Expected;
import io.advantageous.reakt.reactor.Reactor;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

public class Client {

    static final String URI = System.getProperty("url", "http://127.0.0.1:8080/hello");
    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final Class<? extends SocketChannel> channelClassType;
    private final ChannelInboundHandlerAdapter channelInboundHandlerAdapter;
    private final ChannelHandler handler;
    private final Expected<Runnable> onClose;
    private BlockingQueue<Request> httpRequests = new LinkedTransferQueue<>();


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

    class Request {
        private final URI requestURI;
        private final HttpRequest httpRequest;

        Request(URI requestURI, HttpRequest httpRequest) {
            this.requestURI = requestURI;
            this.httpRequest = httpRequest;
        }
    }

    public void sendRequest(final URI requestURI, final String contentType, final String body) {


        final HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, requestURI.getRawPath(),
                Unpooled.wrappedBuffer(body.getBytes(StandardCharsets.UTF_8)));
        request.headers().set(HttpHeaderNames.HOST, requestURI.getHost());
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        request.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);


        httpRequests.offer(new Request(requestURI, request));


    }

    public void start() {

        new Thread(() -> {

            try {
                bootstrap.group(group)
                        .channel(channelClassType).handler(handler);


                while (true) {
                    Request request = httpRequests.poll(50, TimeUnit.MILLISECONDS);

                    while (request != null) {
                        final Channel clientChannel = bootstrap.connect(request.requestURI.getHost(), request.requestURI.getPort()).sync().channel();
                        clientChannel.writeAndFlush(request).addListener(ChannelFutureListener.CLOSE);
                        clientChannel.closeFuture().sync();
                        request = httpRequests.poll();
                    }
                }

            } catch (InterruptedException e) {
                Thread.interrupted();
                throw new IllegalStateException(e);
            } finally {
                group.shutdownGracefully();
            }
        }).start();


    }

    public void stop() {

        group.shutdownGracefully();
    }
}
