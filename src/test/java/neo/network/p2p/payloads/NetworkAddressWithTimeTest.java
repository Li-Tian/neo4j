package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import neo.Utils;
import neo.csharp.Uint;
import neo.csharp.Ulong;

import static org.junit.Assert.*;

public class NetworkAddressWithTimeTest {

    @Test
    public void size() {
        NetworkAddressWithTime address = new NetworkAddressWithTime();
        Assert.assertEquals(30, address.size());
    }

    @Test
    public void serialize() {
        NetworkAddressWithTime address = new NetworkAddressWithTime() {{
            timestamp = new Uint(1000);
            services = new Ulong(10000);
            endPoint = new InetSocketAddress("localhost", 8080);
        }};

        NetworkAddressWithTime copy = Utils.copyFromSerialize(address, NetworkAddressWithTime::new);

        Assert.assertEquals(address.timestamp, copy.timestamp);
        Assert.assertEquals(address.services, copy.services);
        Assert.assertEquals(address.endPoint.getPort(), copy.endPoint.getPort());
    }

    @Test
    public void create() throws UnknownHostException {
        NetworkAddressWithTime address = new NetworkAddressWithTime() {{
            endPoint = new InetSocketAddress(InetAddress.getByName("localhost"), 8080);
            timestamp = new Uint(1000);
            services = new Ulong(10000);
            endPoint = new InetSocketAddress("localhost", 8080);
        }};

        NetworkAddressWithTime copy = NetworkAddressWithTime.create(address.endPoint, address.services, address.timestamp);
        Assert.assertEquals(address.timestamp, copy.timestamp);
        Assert.assertEquals(address.services, copy.services);
        Assert.assertEquals(address.endPoint, copy.endPoint);
    }

}