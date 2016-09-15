package io.netty.example.http.helloworld;

import io.advantageous.reakt.netty.Client;
import io.advantageous.reakt.netty.SimpleHttpClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static io.advantageous.reakt.netty.ClientBuilder.clientBuilder;

public class HttpHelloWorldClient {

    static final boolean SSL = System.getProperty("ssl") != null;
    public static void main(String[] args) throws Exception {

        final SimpleHttpClient client = clientBuilder()
                .useHttp(SSL)
                .buildSimpleHttpClient();

        client.start();

        for (int index = 0; index < 100; index++) {
            client.sendRequest(URI.create("http://localhost:8080/hello"), "text/play", "HELLO WORLD! " + index)
                    .then(httpResponse -> {
                        final String body = httpResponse.content().toString(StandardCharsets.UTF_8);
                        System.out.println("GOT The Body...........!!!! \n" + body);
                    })
                    .catchError(error -> error.printStackTrace())
                    .invoke();
        }

        Thread.sleep(1_000_000_000);
    }
}
