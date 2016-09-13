package io.advantageous.reakt.netty;

import io.advantageous.reakt.Stream;
import io.advantageous.reakt.netty.http.HttpServerHandlerBuilder;
import io.advantageous.reakt.netty.http.HttpServerRequestContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;

import static io.advantageous.reakt.netty.http.HttpServerHandlerBuilder.httpServerHandlerBuilder;
import static io.advantageous.reakt.netty.ServerInitializerBuilder.serverInitializerBuilder;

public class ServerBuilder {
    private EventLoopGroup parentGroup;
    private EventLoopGroup childGroup;
    private ServerBootstrap serverBootstrap;
    private int port = Integer.MIN_VALUE;
    private Class<? extends ServerChannel> channelClassType = NioServerSocketChannel.class;
    private ChannelInboundHandlerAdapter channelInboundHandlerAdapter;
    private ChannelHandler initHandler;
    private LogLevel logLevel = LogLevel.DEBUG;

    private Runnable onClose;
    private boolean throttle;
    private long initialRequestCount;

    public static ServerBuilder serverBuilder() {
        return new ServerBuilder();
    }

    public Runnable getOnClose() {
        return onClose;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public ServerBuilder withOnClose(final Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    public ServerBuilder withOnClose(final Stream<?> onCloseStream) {
        this.onClose = () -> onCloseStream.complete(null);
        return this;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public ServerBuilder withLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public EventLoopGroup getParentGroup() {
        if (parentGroup == null) {
            parentGroup = new NioEventLoopGroup(1);
        }
        return parentGroup;
    }

    public void setParentGroup(EventLoopGroup parentGroup) {
        this.parentGroup = parentGroup;
    }

    public ServerBuilder withParentGroup(EventLoopGroup parentGroup) {
        this.parentGroup = parentGroup;
        return this;
    }

    public EventLoopGroup getChildGroup() {
        if (childGroup == null) {
            childGroup = new NioEventLoopGroup();
        }
        return childGroup;
    }

    public void setChildGroup(EventLoopGroup childGroup) {
        this.childGroup = childGroup;
    }

    public ServerBuilder withChildGroup(EventLoopGroup childGroup) {
        this.childGroup = childGroup;
        return this;
    }

    public ServerBootstrap getServerBootstrap() {
        return serverBootstrap;
    }

    public void setServerBootstrap(ServerBootstrap serverBootstrap) {
        this.serverBootstrap = serverBootstrap;
    }

    public ServerBuilder withServerBootstrap(ServerBootstrap serverBootstrap) {
        this.serverBootstrap = serverBootstrap;
        return this;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ServerBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    public Class<? extends ServerChannel> getChannelClassType() {
        return channelClassType;
    }

    public void setChannelClassType(Class<? extends ServerChannel> channelClassType) {
        this.channelClassType = channelClassType;
    }

    public ServerBuilder withChannelClassType(Class<? extends ServerChannel> channelClassType) {
        this.channelClassType = channelClassType;
        return this;
    }

    public ChannelInboundHandlerAdapter getChannelInboundHandlerAdapter() {
        return channelInboundHandlerAdapter;
    }

    public void setChannelInboundHandlerAdapter(ChannelInboundHandlerAdapter channelInboundHandlerAdapter) {
        this.channelInboundHandlerAdapter = channelInboundHandlerAdapter;
    }

    public ServerBuilder withChannelInboundHandlerAdapter(ChannelInboundHandlerAdapter channelInboundHandlerAdapter) {
        this.channelInboundHandlerAdapter = channelInboundHandlerAdapter;
        return this;
    }

    public ChannelHandler getInitHandler() {
        return initHandler;
    }

    public void setInitHandler(ChannelHandler initHandler) {
        this.initHandler = initHandler;
    }

    public ServerBuilder withInitHandler(ChannelHandler handler) {
        this.initHandler = handler;
        return this;
    }

    public Server build() {
        if (port == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("port must be set");
        }
        return new Server(getParentGroup(), getChildGroup(), getServerBootstrap(),
                getPort(), getChannelClassType(),
                getChannelInboundHandlerAdapter(), getInitHandler(), getLogLevel(), getOnClose());
    }

    public ServerBuilder useHttp(final boolean ssl, final Stream<HttpServerRequestContext> requestStream) {

        final HttpServerHandlerBuilder httpServerHandlerBuilder = httpServerHandlerBuilder()
                .withOnCancel(() -> {})
                .withThrottleRequests(isThrottle())
                .withInitialRequestCount(getInitialRequestCount())
                .withRequestStream(requestStream);

        final ServerInitializerBuilder serverInitializerBuilder = serverInitializerBuilder()
                .useHttp(ssl);

        if (isThrottle()) {
            serverInitializerBuilder.addChannelHandler(
                    httpServerHandlerBuilder.build()
            );
        } else {
            serverInitializerBuilder.addChannelHandlerSupplier(() -> httpServerHandlerBuilder.build());
        }

        withOnClose(requestStream)
                .withInitHandler(serverInitializerBuilder.build());

        return this;
    }

    public ServerBuilder withThrottle(boolean throttle) {
        this.throttle = throttle;
        return this;
    }

    public boolean isThrottle() {
        return throttle;
    }

    public void setThrottle(boolean throttle) {
        this.throttle = throttle;
    }

    public ServerBuilder withInitialRequestCount(long outstandingRequestCount) {
        this.initialRequestCount = outstandingRequestCount;
        return this;
    }

    public long getInitialRequestCount() {
        return initialRequestCount;
    }

    public void setInitialRequestCount(long initialRequestCount) {
        this.initialRequestCount = initialRequestCount;
    }
}
