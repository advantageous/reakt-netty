package io.advantageous.reakt.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class InitializerBuilder {

    private final List<Function<SocketChannel, ChannelHandler>> channelHandlerSupplierList = new ArrayList<>();


    public static InitializerBuilder initializerBuilder() {
        return new InitializerBuilder();
    }

    public InitializerBuilder addChannelHandler(final ChannelHandler channelHandler) {
        channelHandlerSupplierList.add(socketChannel -> channelHandler);
        return this;
    }

    public InitializerBuilder addChannelHandlerSupplier(final Supplier<? extends ChannelHandler> supplier) {
        channelHandlerSupplierList.add(socketChannel -> supplier.get());
        return this;
    }
    public InitializerBuilder addChannelHandlerFunction(final Function<SocketChannel, ChannelHandler> function) {
        channelHandlerSupplierList.add(function);
        return this;
    }

    public InitializerBuilder useSslSelfSigned() {
        channelHandlerSupplierList.set(0, socketChannel -> {

            try {
                final SelfSignedCertificate ssc = new SelfSignedCertificate();
                final SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
                return sslCtx.newHandler(socketChannel.alloc());
            } catch (CertificateException e) {
                throw new IllegalStateException(e);
            } catch (SSLException e) {
                throw new IllegalStateException(e);
            }

        });
        return this;
    }

    public InitializerBuilder useServerHttp() {
        useServerHttp(false);
        return this;
    }

    public InitializerBuilder useServerHttp(final boolean ssl) {
        if (ssl) {
            useSslSelfSigned();
        }
        addChannelHandlerSupplier(HttpServerCodec::new);
        return this;
    }

    public InitializerBuilder useClientHttp() {
        useClientHttp(false);
        return this;
    }

    public InitializerBuilder useClientHttp(final boolean ssl) {
        if (ssl) {
            useSslSelfSigned();
        }
        addChannelHandlerSupplier(HttpClientCodec::new);
        addChannelHandlerSupplier(HttpContentDecompressor::new);
        addChannelHandlerSupplier(() -> new HttpObjectAggregator(512*1024));
        return this;
    }

    public Initializer build() {
        return new Initializer(channelHandlerSupplierList);
    }

}
