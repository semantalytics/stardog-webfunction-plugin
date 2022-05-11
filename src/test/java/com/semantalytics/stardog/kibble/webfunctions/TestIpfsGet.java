package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class TestIpfsGet extends AbstractStardogTest {

    @Test
    public void testCacheLoadFromLiteral() throws IOException {
        URL url = new URL("ipfs://QmPXME1oRtoT627YKaDPDQ3PwA8tdP9rWuAAweLzqSwAWT/readme");
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        System.out.println(IOUtils.toString(in, "UTF-8"));
    }
}
