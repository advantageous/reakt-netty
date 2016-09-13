package io.advantageous.reakt.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.List;
import java.util.function.Function;

public class Initializer extends ChannelInitializer<SocketChannel> {

    private final List<Function<SocketChannel, ChannelHandler>> channelHandlerSupplierList;

    public Initializer(final List<Function<SocketChannel, ChannelHandler>> channelHandlerSupplierList) {
        this.channelHandlerSupplierList = channelHandlerSupplierList;
    }

    public void initChannel(final SocketChannel socketChannel) {
        final ChannelPipeline channelPipeline = socketChannel.pipeline();
        for (Function<SocketChannel, ChannelHandler> function : channelHandlerSupplierList) {
            channelPipeline.addLast(function.apply(socketChannel));
        }
    }
}
