package io.advantageous.reakt.netty;

import io.advantageous.reakt.netty.impl.ReaktChannelRequestReplyFutureListener;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;


/**
 * Design decision: Use Netty classes whenever possible. Do not write wrappers.
 * Design decision: Do not have 100 versions of sending a request. If needed create a builder for DefaultFullHttpRequest and
 * use the sendRequest that takes a FullHttpRequest.
 * This class should have no more than 20 public methods ever.
 */
public class SimpleHttpClient extends Client {


    public SimpleHttpClient(EventLoopGroup group, Bootstrap bootstrap, Class<? extends SocketChannel> channelClassType, ChannelInboundHandlerAdapter channelInboundHandlerAdapter, ChannelHandler handler, Runnable onClose) {
        super(group, bootstrap, channelClassType, channelInboundHandlerAdapter, handler, onClose);
    }


    public Promise<FullHttpResponse> get(final URI requestURI) {
        return sendRequest(HttpMethod.GET, requestURI);
    }

    public Promise<FullHttpResponse> delete(final URI requestURI) {
        return sendRequest(HttpMethod.DELETE, requestURI);
    }

    public Promise<FullHttpResponse> post(final URI requestURI, final String contentType, final CharSequence body) {
        return sendRequest(HttpMethod.POST, requestURI, contentType, body);
    }

    public Promise<FullHttpResponse> put(final URI requestURI, final String contentType, final CharSequence body) {
        return sendRequest(HttpMethod.PUT, requestURI, contentType, body);
    }


    public Promise<FullHttpResponse> post(final URI requestURI, final String contentType, final ByteBuf body) {
        return sendRequest(HttpMethod.POST, requestURI, contentType, body);
    }

    public Promise<FullHttpResponse> put(final URI requestURI, final String contentType, final ByteBuf body) {
        return sendRequest(HttpMethod.PUT, requestURI, contentType, body);
    }

    public Promise<FullHttpResponse> sendRequest(final HttpMethod httpMethod, final URI requestURI,
                                                 final String contentType, final CharSequence body) {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, httpMethod, requestURI.getRawPath(),
                Unpooled.wrappedBuffer(body.toString().getBytes(StandardCharsets.UTF_8)));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        return sendRequest(requestURI, httpRequest);
    }

    public Promise<FullHttpResponse> sendRequest(final HttpMethod httpMethod, final URI requestURI,
                                                 final String contentType, final ByteBuf body) {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, httpMethod, requestURI.getRawPath(), body);
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        return sendRequest(requestURI, httpRequest);
    }


    public Promise<FullHttpResponse> sendRequest(final HttpMethod httpMethod, final URI requestURI) {

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, httpMethod, requestURI.getRawPath());
        return sendRequest(requestURI, httpRequest);
    }

    public Promise<FullHttpResponse> sendRequest(final URI requestURI, final FullHttpRequest httpRequest) {
        return Promises.invokablePromise(returnPromise -> {
            /** Handle the connection async. */
            bootstrap.connect(requestURI.getHost(), requestURI.getPort())
                    .addListener((final ChannelFuture channelFuture) -> {
                        if (!channelFuture.isSuccess()) {
                            returnPromise.reject(channelFuture.cause());
                        } else {
                            final Channel clientChannel = channelFuture.channel();
                            clientChannel.writeAndFlush(httpRequest)
                                    .addListener(new ReaktChannelRequestReplyFutureListener<>(returnPromise));
                        }
                    });
        });
    }

}
