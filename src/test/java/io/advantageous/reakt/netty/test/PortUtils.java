package io.advantageous.reakt.netty.test;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * created by rhightower on 3/4/15.
 */
public class PortUtils {

    public static int useOneOfThesePorts(int... ports) {
        for (int port : ports) {
            ServerSocket serverSocket = null;

            if (tryPort(port, serverSocket)) return port;

        }
        // if the program gets here, no port in the range was found
        throw new IllegalStateException("no free port found");
    }

    private static boolean tryPort(int port, ServerSocket serverSocket) {
        try {

            serverSocket = new ServerSocket(port);
            serverSocket.close();
            return true;
        } catch (IOException ex) {
            //
        } finally {
            if (serverSocket != null) {
                if (!serverSocket.isClosed()) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }


    public static int useOneOfThePortsInThisRange(int start, int stop) {
        for (int index = start; index < stop; index++) {
            ServerSocket serverSocket = null;

            if (tryPort(index, serverSocket)) return index;
        }
        // if the program gets here, no port in the range was found
        throw new IllegalStateException("no free port found");
    }


    public static int findOpenPort() {
        return useOneOfThePortsInThisRange(6000, 30_000);
    }


    public static int findOpenPortStartAt(int start) {
        return useOneOfThePortsInThisRange(start, 30_000);
    }
}
