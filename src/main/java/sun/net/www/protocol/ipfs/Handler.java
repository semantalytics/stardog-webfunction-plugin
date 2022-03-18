package sun.net.www.protocol.ipfs;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        return new IpfsURLConnection(url);
    }
}
