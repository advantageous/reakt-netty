package io.advantageous.reakt.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
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

public class ServerInitializerBuilder {

    private final List<Function<SocketChannel, ChannelHandler>> channelHandlerSupplierList = new ArrayList<>();


    public static ServerInitializerBuilder serverInitializerBuilder() {
        return new ServerInitializerBuilder();
    }

    public ServerInitializerBuilder addChannelHandler(final ChannelHandler channelHandler) {
        channelHandlerSupplierList.add(socketChannel -> channelHandler);
        return this;
    }

    public ServerInitializerBuilder addChannelHandlerSupplier(final Supplier<? extends ChannelHandler> supplier) {
        channelHandlerSupplierList.add(socketChannel -> supplier.get());
        return this;
    }
    public ServerInitializerBuilder addChannelHandlerFunction(final Function<SocketChannel, ChannelHandler> function) {
        channelHandlerSupplierList.add(function);
        return this;
    }

    public ServerInitializerBuilder useSslSelfSigned() {
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

    public ServerInitializerBuilder useHttp() {
        useHttp(false);
        return this;
    }

    public ServerInitializerBuilder useHttp(boolean ssl) {
        if (ssl) {
            useSslSelfSigned();
        }
        addChannelHandlerSupplier(HttpServerCodec::new);
        return this;
    }


    public ServerInitializer build() {
        return new ServerInitializer(channelHandlerSupplierList);
    }

}
