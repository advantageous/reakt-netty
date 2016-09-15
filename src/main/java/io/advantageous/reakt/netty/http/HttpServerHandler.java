package io.advantageous.reakt.netty.http;


import io.advantageous.reakt.Expected;
import io.advantageous.reakt.Stream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AsciiString;

import java.util.function.Consumer;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {

    private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
    private static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
    private static final AsciiString CONNECTION = new AsciiString("Connection");
    private static final AsciiString KEEP_ALIVE = new AsciiString("keep-alive");
    private final static Consumer<Long> wantsMoreNoOp = amount -> {
    };
    private final static Runnable cancelNoOp = () -> {
    };
    private final Stream<HttpServerRequestContext> requestStream;
    private final Expected<Runnable> cancelHandler;


    public HttpServerHandler(final Stream<HttpServerRequestContext> requestStream,
                             final Runnable cancelHandler) {
        this.requestStream = requestStream;
        this.cancelHandler = Expected.ofNullable(cancelHandler);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        if (message instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) message;
            cancelHandler
                    .ifPresent(onCancel -> {
                        final Runnable cancelOp = () -> {
                            ctx.executor().submit(() -> {
                                onCancel.run();
                                ctx.channel().close();
                                ctx.channel().parent().close();
                            });
                        };
                        requestStream.reply(new HttpServerRequestContext(ctx, req), false, cancelOp);
                    })
                    .ifAbsent(() -> requestStream.reply(
                            new HttpServerRequestContext(ctx, req), false, cancelNoOp));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        requestStream.fail(cause);
        ctx.close();
    }

}
