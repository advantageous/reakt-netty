package io.netty.example.http.helloworld;


import io.advantageous.reakt.StreamResult;
import io.advantageous.reakt.netty.http.HttpServerRequestContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.advantageous.reakt.netty.ServerBuilder.serverBuilder;

/**
 * An HTTP server that sends back the content of the received HTTP request
 * in a pretty plaintext form.
 */
public final class HttpHelloWorldServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));
    static final long OUTSTANDING_REQUEST_COUNT = 100;

    public static void main(String[] args) throws Exception {

        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);

        serverBuilder()
                .withServerBootstrap(serverBootstrap)
                .withPort(PORT)
                .withThrottle(true)
                .withInitialRequestCount(OUTSTANDING_REQUEST_COUNT)
                .useHttp(SSL, result -> {
                    handleRequest(result);
                })
                .build().start();
    }

    private static void handleRequest(final StreamResult<HttpServerRequestContext> result) {
        if (result.complete()) {
            System.out.println("Server stopped");
            return;
        }
        result.then(httpServerRequestContext -> {

            /* Cancel more requests coming from the stream, which shuts down the HttpServer. */
            if (httpServerRequestContext.getHttpRequest().uri().contains("stop")) {
                httpServerRequestContext.sendOkResponse("DONE\n");
                result.cancel();
                return;
            }

            /*
             * Stop processing requests for 30 seconds. Using stream request more method.
             */
            if (httpServerRequestContext.getHttpRequest().uri().contains("pause")) {
                result.request(OUTSTANDING_REQUEST_COUNT * -1);
                httpServerRequestContext.schedule(Duration.ofSeconds(10),
                        ()-> result.request(OUTSTANDING_REQUEST_COUNT));
            } else {
                // Ask for another request.
                result.request(1);
            }
            /*
             * Send an ok message.
             */
            httpServerRequestContext.sendOkResponse("Hello World!\n");
        }).catchError(error -> {
            error.printStackTrace();
        });
    }
}

