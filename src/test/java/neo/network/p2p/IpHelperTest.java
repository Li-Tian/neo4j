package neo.network.p2p;

import org.junit.Assert;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

public class IpHelperTest {

    @Test
    public void toIPv4() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        InetAddress convert = IpHelper.toIPv4(address);

        Assert.assertEquals(address, convert);

        InetAddress ipv6 = InetAddress.getByName("0:0:0:0:0:FFFF:7F00:0001"); // 127.0.0.1
        convert = IpHelper.toIPv4(ipv6);

        Assert.assertNotEquals(ipv6.getAddress(), convert.getAddress());
        Assert.assertEquals(ipv6.getHostName(), convert.getHostName());

        ipv6 = InetAddress.getByName("2002:7f00:0001:0:0:0:0:0"); // 6to4 address 127.0.0.1
        convert = IpHelper.toIPv4(ipv6);

        Assert.assertTrue(convert instanceof Inet4Address);
        Assert.assertNotEquals(ipv6, convert);

    }

    @Test
    public void toIPv6() {

    }

    @Test
    public void toIPv6Bytes() {

    }

    @Test
    public void toIPv61() {

    }


    @Test
    public void toIPv41() {

    }
}