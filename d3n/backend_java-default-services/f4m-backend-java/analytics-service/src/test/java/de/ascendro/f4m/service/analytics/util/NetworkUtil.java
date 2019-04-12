package de.ascendro.f4m.service.analytics.util;

import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUtil.class);

    public static boolean portAvailable(int port) {
        LOGGER.info("--------------Testing port " + port);
        Socket s = null;
        try {
            s = new Socket("localhost", port);
            // If the code makes it this far without an exception it means
            // something is using the port and has responded.
            LOGGER.info("--------------Port " + port + " is not available");
            return false;
        } catch (IOException e) {
            LOGGER.info("--------------Port " + port + " is available");
            return true;
        } finally {
            if( s != null){
                try {
                    s.close();
                } catch (IOException e) {
                    throw new RuntimeException("You should handle this error." , e);
                }
            }
        }
    }
}
