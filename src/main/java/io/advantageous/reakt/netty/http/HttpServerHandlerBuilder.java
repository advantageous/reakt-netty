package io.advantageous.reakt.netty.http;

import io.advantageous.reakt.Stream;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.time.Duration;

public class HttpServerHandlerBuilder {

    private Stream<HttpServerRequestContext> requestStream;
    private Runnable onCancel;
    private boolean throttleRequests;
    private Duration checkRate = Duration.ofMillis(10);

    private long initialRequestCount = 100;


    public long getInitialRequestCount() {
        return initialRequestCount;
    }

    public HttpServerHandlerBuilder withInitialRequestCount(long initialRequestCount) {
        this.initialRequestCount = initialRequestCount;
        return this;
    }

    public void setInitialRequestCount(long initialRequestCount) {
        this.initialRequestCount = initialRequestCount;
    }

    public Duration getCheckRate() {
        return checkRate;
    }

    public void setCheckRate(Duration checkRate) {
        this.checkRate = checkRate;
    }

    public HttpServerHandlerBuilder withCheckRate(Duration checkRate) {
        this.checkRate = checkRate;
        return this;
    }



    public boolean isThrottleRequests() {
        return throttleRequests;
    }

    public HttpServerHandlerBuilder withThrottleRequests(boolean throttleRequests) {
        this.throttleRequests = throttleRequests;
        return this;
    }

    public void setThrottleRequests(boolean throttleRequests) {
        this.throttleRequests = throttleRequests;
    }

    public static HttpServerHandlerBuilder httpServerHandlerBuilder() {
        return new HttpServerHandlerBuilder();
    }

    public Runnable getOnCancel() {
        return onCancel;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public HttpServerHandlerBuilder withOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    public Stream<HttpServerRequestContext> getRequestStream() {
        return requestStream;
    }

    public void setRequestStream(Stream<HttpServerRequestContext> requestStream) {
        this.requestStream = requestStream;
    }

    public HttpServerHandlerBuilder withRequestStream(Stream<HttpServerRequestContext> requestStream) {
        this.requestStream = requestStream;
        return this;
    }

    public ChannelInboundHandlerAdapter build() {
        final Stream<HttpServerRequestContext> requestStream = getRequestStream();
        final Runnable onCancel = getOnCancel();
        final boolean throttleRequests = isThrottleRequests();

        if (throttleRequests) {
            final Duration checkRate = getCheckRate();
            final long initialCount = getInitialRequestCount();
            return new HttpServerWithLimitHandler(requestStream, onCancel, initialCount, checkRate);
        } else {
            return new HttpServerHandler(requestStream, onCancel);
        }
    }
}
