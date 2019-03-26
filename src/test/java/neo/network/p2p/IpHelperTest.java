package neo.network.p2p;

import org.junit.Assert;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import neo.cryptography.Helper;

import static org.junit.Assert.*;

public class IpHelperTest {

    @Test
    public void toIPv4() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        InetAddress convert = IpHelper.toIPv4(address);
        Assert.assertEquals(address, convert);

        InetAddress ipv6 = InetAddress.getByName("0:0:0:0:0:FFFF:7F00:0001"); // 127.0.0.1
        convert = IpHelper.toIPv4(ipv6);
        Assert.assertTrue(convert instanceof Inet4Address);
        Assert.assertNotEquals(ipv6.getAddress(), convert.getAddress());
        Assert.assertEquals(ipv6.getHostName(), convert.getHostName());

        ipv6 = InetAddress.getByName("2002:7f00:0001:0:0:0:0:0"); // 6to4 address 127.0.0.1
        convert = IpHelper.toIPv4(ipv6);
        Assert.assertFalse(convert instanceof Inet4Address); // 不兼容的ipv6

        ipv6 = InetAddress.getByName("::ffff:1.2.3.4"); // 6to4 address 127.0.0.1
        convert = IpHelper.toIPv4(ipv6);
        Assert.assertTrue(convert instanceof Inet4Address); // 兼容的ipv6
    }

    @Test
    public void toIPv6Bytes() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        byte[] data = IpHelper.toIPv6Bytes(address);
        Assert.assertEquals(16, data.length);

        address = InetAddress.getByName("2002:7f00:0001:0:0:0:0:0");
        data = IpHelper.toIPv6Bytes(address);
        Assert.assertEquals(16, data.length);

        address = InetAddress.getByName("::ffff:1.2.3.4");
        data = IpHelper.toIPv6Bytes(address);
        Assert.assertEquals(16, data.length);
    }

    @Test
    public void toIPv41() throws UnknownHostException {
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8080);
        InetSocketAddress convert = IpHelper.toIPv4(socketAddress);
        Assert.assertEquals(socketAddress, convert);

        socketAddress = new InetSocketAddress(InetAddress.getByName("0:0:0:0:0:FFFF:7F00:0001"), 8080);
        convert = IpHelper.toIPv4(socketAddress);
        Assert.assertEquals(socketAddress, convert);

        socketAddress = new InetSocketAddress(InetAddress.getByName("2002:7f00:0001:0:0:0:0:0"), 8080);
        convert = IpHelper.toIPv4(socketAddress);
        Assert.assertEquals(socketAddress, convert);

        socketAddress = new InetSocketAddress(InetAddress.getByName("::ffff:1.2.3.4"), 8080);
        convert = IpHelper.toIPv4(socketAddress);
        Assert.assertEquals(socketAddress, convert);
    }


}