package io.advantageous.reakt.netty.http;


import io.advantageous.reakt.Expected;
import io.advantageous.reakt.Stream;
import io.advantageous.reakt.netty.http.HttpServerRequestContext;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@ChannelHandler.Sharable
public class HttpServerWithLimitHandler extends ChannelInboundHandlerAdapter {

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
    private final Duration checkLimitDuration;
    private AtomicLong limitCount;
    private BlockingQueue requestAmountQueue = new ArrayBlockingQueue(100);
    private AtomicBoolean counting = new AtomicBoolean();


    public HttpServerWithLimitHandler(final Stream<HttpServerRequestContext> requestStream,
                                      final Runnable cancelHandler,
                                      final long initialRequestCount,
                                      final Duration checkLimitDuration) {
        this.requestStream = requestStream;
        this.cancelHandler = Expected.ofNullable(cancelHandler);
        this.limitCount = new AtomicLong(initialRequestCount);
        this.checkLimitDuration = checkLimitDuration;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        if (message instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) message;


            final Consumer<Long> wantsMore = amountRequested -> {
                ctx.executor().submit(() -> {
                    requestAmountQueue.add(amountRequested);
                });
            };

            if (limitCount.get() > 0) {
                requestAmountQueue.add(-1L);
                handleRequest(ctx, req, wantsMore);

                if (counting.compareAndSet(false, true)) {
                    ctx.executor().schedule(() -> {
                        countOutstandingRequestsNow();
                    }, checkLimitDuration.toNanos(), TimeUnit.NANOSECONDS);
                }
            } else {
                if (counting.compareAndSet(false, true)) {
                    ctx.executor().submit(() -> {
                        countOutstandingRequestsNow();
                    });
                }
                new HttpServerRequestContext(ctx, req).sendResponse("text/plain", HttpResponseStatus.TOO_MANY_REQUESTS,
                        "too many requests");
            }
        }
    }

    private void handleRequest(ChannelHandlerContext ctx, HttpRequest req, Consumer<Long> wantsMore) {
        cancelHandler
                .ifPresent(onCancel -> {
                    final Runnable cancelOp = createCancelOp(ctx, onCancel);
                    requestStream.reply(new HttpServerRequestContext(ctx, req), false, cancelOp, wantsMore);
                })
                .ifAbsent(() -> requestStream.reply(
                        new HttpServerRequestContext(ctx, req), false, cancelNoOp, wantsMore));
    }

    private long countOutstandingRequestsNow() {

        final List<Long> countList = new ArrayList<>();
        requestAmountQueue.drainTo(countList);
        long requestCount = limitCount.get();
        for (int i = 0; i < countList.size(); i++) {
            requestCount += countList.get(i);
        }
        limitCount.set(requestCount);
        counting.set(false);
        return requestCount;
    }

    private Runnable createCancelOp(ChannelHandlerContext ctx, Runnable onCancel) {
        return () -> {
            ctx.executor().submit(() -> {
                onCancel.run();
                ctx.channel().close();
                ctx.channel().parent().close();
            });
        };
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        requestStream.fail(cause);
        ctx.close();
    }

}
