package io.advantageous.reakt.netty;

import io.advantageous.reakt.Expected;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
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

    public Promise<HttpResponse> sendRequest(final URI requestURI, final String contentType, final String body) {

        return Promises.invokablePromise(returnPromise -> {
            final HttpRequest httpRequest = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, requestURI.getRawPath(),
                    Unpooled.wrappedBuffer(body.getBytes(StandardCharsets.UTF_8)));
            httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            httpRequests.offer(new Request(requestURI, httpRequest, returnPromise));
        });

    }

    public void start() {

        new Thread(() -> {

            try {
                bootstrap.group(group)
                        .channel(channelClassType).handler(handler);


                while (true) {
                    Request request = httpRequests.poll(50, TimeUnit.MILLISECONDS);
                    while (request != null) {

                        final Request theRequest = request;

                        /** Handle the connection async. */
                        bootstrap.connect(request.requestURI.getHost(), request.requestURI.getPort())
                                .addListener((final ChannelFuture channelFuture) -> {
                                    if (!channelFuture.isSuccess()) {
                                        theRequest.returnPromise.reject(channelFuture.cause());
                                    } else {
                                        final Channel clientChannel = channelFuture.channel();
                                        clientChannel.writeAndFlush(theRequest.httpRequest)
                                                .addListener(new ReaktChannelFutureListener(theRequest.returnPromise));
                                    }
                                });

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

    private class ReaktChannelFutureListener implements ChannelFutureListener {
        private final Promise<HttpResponse> promise;

        ReaktChannelFutureListener(final Promise<HttpResponse> promise) {
            this.promise = promise;
        }

        @Override
        public final void operationComplete(final ChannelFuture channelFuture) throws Exception {
            if (!channelFuture.isSuccess()) {
                promise.reject(channelFuture.cause());
            } else {
                channelFuture.channel().pipeline().addLast(new SimpleChannelInboundHandler<HttpResponse>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, HttpResponse msg) throws Exception {
                        promise.resolve(msg);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        promise.reject(cause);
                    }
                });
                promise.resolve();
            }
        }


    }

    class Request {
        private final URI requestURI;
        private final HttpRequest httpRequest;
        private final Promise<HttpResponse> returnPromise;

        Request(URI requestURI, HttpRequest httpRequest, Promise<HttpResponse> returnPromise) {
            this.requestURI = requestURI;
            this.httpRequest = httpRequest;
            this.returnPromise = returnPromise;
        }
    }
}
