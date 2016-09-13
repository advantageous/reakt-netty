package io.netty.example.http.helloworld;


import io.advantageous.reakt.StreamResult;
import io.advantageous.reakt.netty.http.HttpServerRequestContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;

import java.time.Duration;

import static io.advantageous.reakt.netty.ServerBuilder.serverBuilder;

/**
 * An HTTP server that sends back the content of the received HTTP request
 * in a pretty plaintext form.
 * <p>
 * To get a hello world.
 * curl http://localhost:8080/hello
 * <p>
 * To test streamResult.request(numRequests) works
 * <p>
 * To test streamResult.cancel works.
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
        /** See if stream stopped in this case, HttpServer stream of httpRequests. */
        if (result.complete()) {
            System.out.println("Server stopped");
            return;
        }

        /** Handle requests. */
        result.then(httpServerRequestContext -> {  // <--- stream processing then(


            /* If request path ends with "stop"
             * Cancel more requests coming from the stream, which shuts down the HttpServer.
             */
            if (httpServerRequestContext.getHttpRequest().uri().contains("stop")) {
                httpServerRequestContext.sendOkResponse("DONE\n");
                result.cancel();
                return;
            }

            /*
             * If request path ends with "pause"
             * Stop processing requests for 10 seconds. Using stream request more method.
             */
            if (httpServerRequestContext.getHttpRequest().uri().contains("pause")) {
                result.request(OUTSTANDING_REQUEST_COUNT * -1);   // <-- uses stream result request
                // Disable requests for 10 seconds
                httpServerRequestContext.schedule(Duration.ofSeconds(10),
                        () -> result.request(OUTSTANDING_REQUEST_COUNT));
            } else {
                // Ask for another request.
                result.request(1);
            }
            /*
             * Send an ok message. "HelloWorld!\n"
             */
            httpServerRequestContext.sendOkResponse("Hello World!\n");
        }).catchError(error -> { // <-- stream processing catch Error
            error.printStackTrace();
        });
    }
}

