package neo.network.p2p;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import akka.testkit.TestKit;
import neo.NeoSystem;
import neo.persistence.AbstractLeveldbTest;

public class ProtocolHandlerTest extends AbstractLeveldbTest {

    private static NeoSystem neoSystem;
    private static TestKit testKit;

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractLeveldbTest.setUp();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown();
    }

    @Test
    public void createReceive() {

    }


}