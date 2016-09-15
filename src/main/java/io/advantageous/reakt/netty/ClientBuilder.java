package io.advantageous.reakt.netty;

import io.advantageous.reakt.Stream;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import static io.advantageous.reakt.netty.InitializerBuilder.initializerBuilder;

public class ClientBuilder {
    private EventLoopGroup group;
    private Bootstrap bootstrap;
    private Class<? extends SocketChannel> channelClassType = NioSocketChannel.class;
    private ChannelInboundHandlerAdapter channelInboundHandlerAdapter;
    private ChannelHandler initHandler;
    private Runnable onClose;

    public static ClientBuilder clientBuilder() {
        return new ClientBuilder();
    }

    public Runnable getOnClose() {
        return onClose;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public ClientBuilder withOnClose(final Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    public ClientBuilder withOnClose(final Stream<?> onCloseStream) {
        this.onClose = () -> onCloseStream.complete(null);
        return this;
    }

    public EventLoopGroup getGroup() {
        if (group == null) {
            group = new NioEventLoopGroup(1);
        }
        return group;
    }

    public void setGroup(EventLoopGroup group) {
        this.group = group;
    }

    public ClientBuilder withParentGroup(EventLoopGroup parentGroup) {
        this.group = parentGroup;
        return this;
    }

    public Bootstrap getBootstrap() {
        if (bootstrap == null) {
            bootstrap = new Bootstrap();
        }
        return bootstrap;
    }

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public ClientBuilder withBootstrap(final Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
        return this;
    }

    public Class<? extends SocketChannel> getChannelClassType() {
        return channelClassType;
    }

    public void setChannelClassType(Class<? extends SocketChannel> channelClassType) {
        this.channelClassType = channelClassType;
    }

    public ClientBuilder withChannelClassType(Class<? extends SocketChannel> channelClassType) {
        this.channelClassType = channelClassType;
        return this;
    }

    public ChannelInboundHandlerAdapter getChannelInboundHandlerAdapter() {
        return channelInboundHandlerAdapter;
    }

    public void setChannelInboundHandlerAdapter(ChannelInboundHandlerAdapter channelInboundHandlerAdapter) {
        this.channelInboundHandlerAdapter = channelInboundHandlerAdapter;
    }

    public ClientBuilder withChannelInboundHandlerAdapter(ChannelInboundHandlerAdapter channelInboundHandlerAdapter) {
        this.channelInboundHandlerAdapter = channelInboundHandlerAdapter;
        return this;
    }

    public ChannelHandler getInitHandler() {
        return initHandler;
    }

    public void setInitHandler(ChannelHandler initHandler) {
        this.initHandler = initHandler;
    }

    public ClientBuilder withInitHandler(ChannelHandler handler) {
        this.initHandler = handler;
        return this;
    }

    public SimpleHttpClient buildSimpleHttpClient() {
        return new SimpleHttpClient(getGroup(), getBootstrap(), getChannelClassType(), getChannelInboundHandlerAdapter(),
                getInitHandler(), getOnClose());
    }

    public ClientBuilder useHttp(final boolean ssl) {

        final InitializerBuilder initializerBuilder = initializerBuilder()
                .useClientHttp(ssl);

        withInitHandler(initializerBuilder.build());

        return this;
    }


}
