package io.netty.example.http.helloworld;

import io.advantageous.reakt.netty.Client;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpResponse;

import java.net.URI;
import java.util.function.Consumer;

import static io.advantageous.reakt.netty.ClientBuilder.clientBuilder;

public class HttpHelloWorldClient {

    static final boolean SSL = System.getProperty("ssl") != null;
    public static void main(String[] args) throws Exception {

        final Client client = clientBuilder()
                .useHttp(SSL)
                .build();

        client.start();

        Thread.sleep(1000);

        for (int index = 0; index < 100; index++) {
            client.sendRequest(URI.create("http://localhost:8080/hello"), "text/play", "HELLO WORLD! " + index)
                    .then(httpResponse -> {
                        System.out.println("GOT THIS FAR...........");
                    })
                    .invoke();
        }
        Thread.sleep(1_000_000_000);
    }
}
