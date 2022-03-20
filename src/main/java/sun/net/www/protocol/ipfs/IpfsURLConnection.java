package sun.net.www.protocol.ipfs;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static org.apache.commons.lang3.StringUtils.*;

public class IpfsURLConnection extends URLConnection {

    @Override
    public void connect() throws IOException {

    }

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     */
    protected IpfsURLConnection(URL url) throws MalformedURLException {
        super(toIpfsGatewayUrl(url));
    }

    private static URL toIpfsGatewayUrl(final URL url) throws MalformedURLException {
        return new URL(appendIfMissing(System.getenv().getOrDefault("STARDOG_IPFS_GATEWAY", "https://gateway.ipfs.io/ipfs/"), "/") + url.getAuthority() + url.getPath());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }
}
