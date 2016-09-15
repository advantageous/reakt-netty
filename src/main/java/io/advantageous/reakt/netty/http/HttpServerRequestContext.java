package io.advantageous.reakt.netty.http;

import io.advantageous.reakt.netty.ServerMessageContext;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpServerRequestContext extends ServerMessageContext<HttpRequest> {

    public static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
    public static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
    public static final AsciiString CONNECTION = new AsciiString("Connection");
    public static final AsciiString KEEP_ALIVE = new AsciiString("keep-alive");
    public static final AsciiString CONTENT_TYPE_TEXT_PLAIN = new AsciiString("text/plain; charset=utf-8");
    public static final AsciiString CONTENT_TYPE_JSON = new AsciiString("application/json; charset=utf-8");


    public HttpServerRequestContext(ChannelHandlerContext channelHandlerContext, HttpRequest message) {
        super(channelHandlerContext, message);
    }

    public void sendOkResponse(final CharSequence contentType, final CharSequence body) {
        sendResponse(contentType, OK, body);
    }

    public void sendOkResponse(final CharSequence body) {
        sendResponse(CONTENT_TYPE_TEXT_PLAIN, OK, body);
    }

    public void sendOkJsonResponse(final CharSequence body) {
        sendResponse(CONTENT_TYPE_JSON, OK, body);
    }

    public void sendErrorResponse(final CharSequence body) {
        sendResponse(CONTENT_TYPE_TEXT_PLAIN, INTERNAL_SERVER_ERROR, body);
    }

    public void sendErrorJsonResponse(final CharSequence body) {
        sendResponse(CONTENT_TYPE_JSON, INTERNAL_SERVER_ERROR, body);
    }


    public void sendResponse(final CharSequence contentType, final HttpResponseStatus status, final CharSequence body) {
        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
                Unpooled.wrappedBuffer(body.toString().getBytes(StandardCharsets.UTF_8)));
        response.headers().set(CONTENT_TYPE, contentType);
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
        getChannelHandlerContext().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    public HttpRequest getHttpRequest() {
        return getMessage();
    }
}
