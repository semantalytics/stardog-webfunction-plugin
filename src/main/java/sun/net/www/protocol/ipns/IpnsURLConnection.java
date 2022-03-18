package sun.net.www.protocol.ipns;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static java.lang.System.getenv;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;

public class IpnsURLConnection extends URLConnection {

    @Override
    public void connect() throws IOException {

    }
    
    protected IpnsURLConnection(URL url) throws MalformedURLException {
        super(toIpfsGatewayUrl(url));
    }

    private static URL toIpfsGatewayUrl(final URL url) throws MalformedURLException {
        return new URL(appendIfMissing(getenv("IPFS_GATEWAY"), "/") + "/ipns/" + url.getAuthority() + "/" + url.getPath());
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }

}
