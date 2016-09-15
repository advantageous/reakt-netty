package io.advantageous.reakt.netty;

import io.advantageous.reakt.netty.impl.ReaktChannelRequestReplyFutureListener;
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

public class SimpleHttpClient extends Client {


    private BlockingQueue<Request> httpRequests = new LinkedTransferQueue<>();

    public SimpleHttpClient(EventLoopGroup group, Bootstrap bootstrap, Class<? extends SocketChannel> channelClassType, ChannelInboundHandlerAdapter channelInboundHandlerAdapter, ChannelHandler handler, Runnable onClose) {
        super(group, bootstrap, channelClassType, channelInboundHandlerAdapter, handler, onClose);
    }

    public Promise<FullHttpResponse> sendRequest(final URI requestURI, final String contentType, final String body) {
        return Promises.invokablePromise(returnPromise -> {
            final HttpRequest httpRequest = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, requestURI.getRawPath(),
                    Unpooled.wrappedBuffer(body.getBytes(StandardCharsets.UTF_8)));
            httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            httpRequests.offer(new Request(requestURI, httpRequest, returnPromise));
        });
    }

    @Override
    protected void process() throws Exception {
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
                                        .addListener(new ReaktChannelRequestReplyFutureListener<>(theRequest.returnPromise));
                            }
                        });
                request = httpRequests.poll();
            }
        }
    }



    private class Request {
        private final URI requestURI;
        private final HttpRequest httpRequest;
        private final Promise<FullHttpResponse> returnPromise;
        Request(final URI requestURI, final HttpRequest httpRequest,
                final Promise<FullHttpResponse> returnPromise) {
            this.requestURI = requestURI;
            this.httpRequest = httpRequest;
            this.returnPromise = returnPromise;
        }
    }
}
