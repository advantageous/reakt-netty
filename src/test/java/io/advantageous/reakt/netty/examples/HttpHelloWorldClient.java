package io.advantageous.reakt.netty.examples;

import io.advantageous.reakt.netty.SimpleHttpClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static io.advantageous.reakt.netty.ClientBuilder.clientBuilder;

public class HttpHelloWorldClient {

    static final boolean SSL = System.getProperty("ssl") != null;

    public static void main(String[] args) throws Exception {

        final SimpleHttpClient client = clientBuilder()
                .useHttp(SSL)
                .buildSimpleHttpClient();


        for (int index = 0; index < 100; index++) {
            client.get(URI.create("http://localhost:8080/hello"))
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
