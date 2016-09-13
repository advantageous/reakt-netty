package io.advantageous.reakt.netty;

import io.netty.channel.ChannelHandlerContext;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class ServerMessageContext <M> {

    private final ChannelHandlerContext channelHandlerContext;
    private final M message;

    public ServerMessageContext(ChannelHandlerContext channelHandlerContext, M message) {
        this.channelHandlerContext = channelHandlerContext;
        this.message = message;
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    protected M getMessage() {
        return message;
    }


    public void schedule(final Duration duration, final Runnable runnable) {
        channelHandlerContext.executor().schedule(runnable, duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    public void submit(final Runnable runnable) {
        channelHandlerContext.executor().submit(runnable);
    }
}
