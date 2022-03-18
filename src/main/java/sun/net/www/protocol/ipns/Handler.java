package sun.net.www.protocol.ipns;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        return new IpnsURLConnection(url);
    }
}
