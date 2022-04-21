package sun.net.www.protocol.ipns;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static java.lang.System.getenv;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;

public class IpnsURLConnection extends URLConnection {

    public static final String IPNS_DEFAULT_GATEWAY = "https://wf.semantalytics.com/ipns/";

    @Override
    public void connect() throws IOException {

    }
    
    protected IpnsURLConnection(URL url) throws MalformedURLException {
        super(toIpfsGatewayUrl(url));
    }

    private static URL toIpfsGatewayUrl(final URL url) throws MalformedURLException {
        return new URL(appendIfMissing(System.getenv().getOrDefault("STARDOG_IPNS_GATEWAY", IPNS_DEFAULT_GATEWAY), "/") + url.getAuthority() + url.getPath());
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }

}
