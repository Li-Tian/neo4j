package neo.network.p2p;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.network.p2p.payloads.VersionPayload;

import static org.junit.Assert.*;

public class TaskSessionTest {

    private TaskSession taskSession;

    @Before
    public void before() {
        VersionPayload payload = new VersionPayload() {{
            version = Uint.ZERO;
            services = Ulong.ZERO;
            timestamp = new Uint(128484428);
            port = new Ushort(8080);
            nonce = Uint.ZERO;
            userAgent = "test";
            startHeight = new Uint(10);
            relay = true;
        }};

        taskSession = new TaskSession(null, payload);
    }

    @Test
    public void hasTask() {
        UInt256 hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        Assert.assertFalse(taskSession.hasTask());
        taskSession.tasks.put(hash, new Date());
        Assert.assertTrue(taskSession.hasTask());
    }

    @Test
    public void headerTask() {
        Assert.assertFalse(taskSession.headerTask());
        taskSession.tasks.put(UInt256.Zero, new Date());
        Assert.assertTrue(taskSession.hasTask());
    }
}