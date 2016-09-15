package io.advantageous.reakt.netty.impl;

import io.advantageous.reakt.promise.Promise;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Adapts a Netty Channel Future to a Reakt Promise.
 * This is so you can do a request / reply type scenario like an HTTP request that expects a reply.
 * This would not work with a truly streaming request / reply protocol.
 * It would have to be Http like.
 *
 * @param <T>
 */
public class ReaktChannelRequestReplyFutureListener<T> implements ChannelFutureListener {
    private final Promise<T> promise;

    public ReaktChannelRequestReplyFutureListener(final Promise<T> promise) {
        this.promise = promise;
    }

    @Override
    public final void operationComplete(final ChannelFuture channelFuture) throws Exception {
        if (!channelFuture.isSuccess()) {
            promise.reject(channelFuture.cause());
        } else {
            channelFuture.channel().pipeline().addLast(new SimpleChannelInboundHandler<Object>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                    promise.resolve((T) msg);
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    promise.reject(cause);
                }
            });
        }
    }
}
