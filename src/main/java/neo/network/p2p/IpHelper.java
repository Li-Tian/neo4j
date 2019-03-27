package neo.network.p2p;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv6.IPv6Address;
import neo.log.notr.TR;

/**
 * IP helper, provide converting method ipv4 with ipv6.
 */
public class IpHelper {

    /**
     * convert to ipv4 address
     *
     * @param address source address
     * @return if address is ipv4 or address compat with ipv4, it will return ipv4, otherwise it
     * will return the source address.
     */
    public static InetAddress toIPv4(InetAddress address) {
        TR.enter();
        if (address instanceof Inet4Address) {
            return TR.exit(address);
        }
        Inet6Address ipv6 = (Inet6Address) address;
        IPv6Address iPv6Address = new IPv6Address(ipv6);
        if (iPv6Address.isIPv4Compatible() || iPv6Address.isIPv4Mapped()) {
            IPv4Address iPv4Address = iPv6Address.getEmbeddedIPv4Address();
            address = iPv4Address.toInetAddress();
        }
        return TR.exit(address);
    }

    /**
     * force to convert address to ipv6 data bytes
     *
     * @param address source address
     * @return 16-byte array
     */
    public static byte[] toIPv6Bytes(InetAddress address) {
        TR.enter();
        byte[] data = address.getAddress();
        if (data.length == 16) {// ipv6
            return TR.exit(data);
        } else { // ipv4
            byte[] ipv6Data = new byte[16];
            Arrays.fill(ipv6Data, (byte) 0x00);
            System.arraycopy(data, 0, ipv6Data, 12, 4);
            return TR.exit(ipv6Data);
        }
    }


    /**
     * convert to ipv4 socket address
     *
     * @param socketAddress source socket address
     * @return ipv4 socket address. if the source address is already the ipv4 socket address or
     * convert failed, it will return the source address
     */
    public static InetSocketAddress toIPv4(InetSocketAddress socketAddress) {
        TR.enter();
        InetAddress address = socketAddress.getAddress();

        InetAddress newAddress = toIPv4(address);
        if (newAddress == address) {
            return socketAddress;
        }
        return TR.exit(new InetSocketAddress(newAddress, socketAddress.getPort()));
    }

}
