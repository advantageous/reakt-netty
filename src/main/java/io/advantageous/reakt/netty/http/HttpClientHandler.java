package io.advantageous.reakt.netty.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

public class HttpClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    @Override
    public void channelRead0(final ChannelHandlerContext channelHandlerContext, final HttpObject httpObject) {
        if (httpObject instanceof HttpResponse) {
            final HttpResponse response = (HttpResponse) httpObject;

            if (!response.headers().isEmpty()) {
                for (CharSequence name: response.headers().names()) {
                    for (CharSequence value: response.headers().getAll(name)) {
                        System.err.println("HEADER: " + name + " = " + value);
                    }
                }
                System.err.println();
            }

            if (HttpUtil.isTransferEncodingChunked(response)) {
                System.err.println("CHUNKED CONTENT {");
            } else {
                System.err.println("CONTENT {");
            }
        } else if (httpObject instanceof HttpContent) {
            final HttpContent content = (HttpContent) httpObject;

            if (content instanceof LastHttpContent) {
                System.err.println("} END OF CONTENT");
                channelHandlerContext.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
