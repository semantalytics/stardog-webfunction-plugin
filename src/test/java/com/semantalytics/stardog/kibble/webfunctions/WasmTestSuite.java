package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.common.protocols.server.Server;
import com.complexible.common.protocols.server.ServerException;
import com.complexible.stardog.Stardog;
import com.complexible.stardog.StardogConfiguration;
import com.complexible.stardog.StardogException;
import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.admin.AdminConnection;
import com.complexible.stardog.api.admin.AdminConnectionConfiguration;
import com.google.common.io.Files;
import com.semantalytics.stardog.kibble.webfunctions.*;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

@RunWith(Suite.class)
@SuiteClasses({
    TestAggregate.class,
    TestArrayOf.class,
    TestCache.class,
    TestCall.class,
    TestCompose.class,
    TestDoc.class,
    TestFilter.class,
    TestInCallOutPropertyFunction.class,
    TestIpfsGet.class,
    TestMap.class,
    TestMappingDictionaryGet.class,
    TestMappingDictionaryAdd.class,
    TestMemoize.class,
    TestOutCallInPropertyFunction.class,
    TestPartial.class,
    TestPluginVersion.class,
    TestReduce.class,
    TestServiceQuery.class,
    TestToUpper.class,
    TestWebFunctions.class,
})

public class WasmTestSuite extends TestCase {

	private static Stardog STARDOG;
	private static Server SERVER;
	public static final String DB = "test";
	public static final int TEST_PORT = 5888;
	protected Connection connection;
    private static final String STARDOG_LICENSE_PATH = System.getenv("STARDOG_LICENSE_PATH");

    @BeforeClass
    public static void beforeClass() throws IOException, ServerException {

        Process process = new ProcessBuilder("cargo", "make", "build").directory(new File("./src/test/rust")).start();
        System.out.println(IOUtils.toString(process.getErrorStream(), Charset.defaultCharset()));
        System.out.println(IOUtils.toString(process.getErrorStream(), Charset.defaultCharset()));

        try{
            AdminConnectionConfiguration.toEmbeddedServer()
                    .credentials("admin", "admin")
                    .connect();
        } catch(StardogException e) {


            final File TEST_HOME;

            TEST_HOME = Files.createTempDir();
            TEST_HOME.deleteOnExit();

            STARDOG = Stardog.builder()
                    .set(StardogConfiguration.LICENSE_LOCATION, STARDOG_LICENSE_PATH)
                    .home(TEST_HOME).create();

            SERVER = STARDOG.newServer()
                    .bind(new InetSocketAddress("localhost", TEST_PORT))
                    .start();

            final AdminConnection adminConnection = AdminConnectionConfiguration.toEmbeddedServer()
                    .credentials("admin", "admin")
                    .connect();

            if (adminConnection.list().contains(DB)) {
                adminConnection.drop(DB);
            }

            adminConnection.newDatabase(DB).create();
        }
    }

    @AfterClass
    public static void afterClass() {
        if (SERVER != null) {
            SERVER.stop();
        }
        if (STARDOG != null) {
            STARDOG.shutdown();
        }
    }
}
