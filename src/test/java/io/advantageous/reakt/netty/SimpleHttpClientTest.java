package io.advantageous.reakt.netty;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import org.junit.Test;

import java.net.URI;
import java.time.Duration;

import static io.advantageous.reakt.netty.ClientBuilder.clientBuilder;
import static org.junit.Assert.assertNotNull;

public class SimpleHttpClientTest {


    @Test
    public void test() throws Exception {

        final SimpleHttpClient client = clientBuilder().useHttp(false)
                .buildSimpleHttpClient();

        final FullHttpResponse fullHttpResponse1 = client.get(URI.create("http://www.google.com:80"))
                .blockingGet(Duration.ofSeconds(30));

        assertNotNull(fullHttpResponse1);
        final ByteBuf content = fullHttpResponse1.content();
        assertNotNull(content);

    }

}