# Reakt Netty

The idea behind this is to create the thinnest possible wrapper around Netty so that ***Reakt*** projects that
need IO can use Reakt-Netty and not include all of Vert.x.

(This is early days for this project.)

This does not in anyway shape or form try to abstract the low level details of Netty. 
If you are looking for a high-level lib that uses Netty, then look into ***Vert.x***.

This project is just a thin wrapper of Netty to adapt Reakt streams and promises.

This project is nascent so advice and code reviews would be helpful. 


#### Start up a Netty HttpServer
```java
import static io.advantageous.reakt.netty.ServerBuilder.serverBuilder;
...
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);

        serverBuilder()
                .withServerBootstrap(serverBootstrap)
                .withPort(PORT)
                .withThrottle(true)
                .withInitialRequestCount(OUTSTANDING_REQUEST_COUNT)
                .useHttp(SSL, result -> {
                    handleRequest(result); // <----------- stream of requests
                })
                .build().start();
```



#### Handle stream results
```java
    private static void handleRequest(final StreamResult<HttpServerRequestContext> result) {
        /** See if stream stopped in this case, HttpServer stream of httpRequests. */
        if (result.complete()) { System.out.println("Server stopped"); return; }
        /** Handle requests. */
        result.then(httpServerRequestContext -> {  // <--- stream processing then(
            // If request path ends with "stop" 
            // Cancel more requests coming from the stream, which shuts down the HttpServer. 
            if (httpServerRequestContext.getHttpRequest().uri().contains("stop")) {
                httpServerRequestContext.sendOkResponse("DONE\n");
                result.cancel();
                return;
            }
            // If request path ends with "pause"
            // Stop processing requests for 10 seconds. Using stream request more method.
            if (httpServerRequestContext.getHttpRequest().uri().contains("pause")) {
                result.request(OUTSTANDING_REQUEST_COUNT * -1);   // <-- uses stream result request
                // Disable requests for 10 seconds 
                httpServerRequestContext.schedule(Duration.ofSeconds(10),
                        ()-> result.request(OUTSTANDING_REQUEST_COUNT));
            } else {
                // Ask for another request.
                result.request(1);
            }
            // Send an ok message. "HelloWorld!\n"
            httpServerRequestContext.sendOkResponse("Hello World!\n");
        }).catchError(error -> { // <-- stream processing catch Error
            error.printStackTrace();
        });
    }
```


Once the example is running you can use the following.

```sh
# To get a hello world.
$ curl http://localhost:8080/hello
 
# To test streamResult.request(numRequests) works
$ curl http://localhost:8080/pause
  
# You won't be able to get hello world until ten seconds pass.  
 
# To test streamResult.cancel works.
$ curl http://localhost:8080/stop

# Server will be closed after this. (This is a test to show how streaming works)

```

#### Status 9/13/2015
(Compare the status of this to the number of commits after this date.)
Fairly early days for this project.
We want to provide a thin layer around ***Netty*** and then make it easy to 
create an ***HttpServer*** (this is the furthest along), an ***HttpClient***,
***DnsClient***, ***TcpServer*** and a ***TcpClient***. We don't plan on abstracting any of the
lower level Netty classes. You will work with Netty directly.
Server will supports streams via Reakt and possibly reactive streaming specification. 
Clients will support Promises and Streams via Reakt. 
There will be (and is) builders to get rid of the boiler code, but you can
always pass a builder Netty handlers, and such (or replace a boiler point creating 
of a Netty object with something better).

Goals
* HttpServer that uses full Reakt stream support (DONE)
* HttpClient that uses Reakt promises (in progress)
* HttpServer/HttpClient that can stream http responses into messages. 
* HttpServer/HttpClient that can stream via WebSocket messages with Reakt stream support to control the stream
* DnsClient
* TcpServer that can have a server side stream handler via Reakt
* TcpClient that can consume a serverside stream
* TcpClient that can stream calls and responses  (using Promises)
* The classes and builders to make this happen so building similar things with Reakt/Netty is easy and Java lambda friendly
 
