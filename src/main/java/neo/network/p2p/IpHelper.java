package neo.network.p2p;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import neo.log.tr.TR;

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
        if (address instanceof Inet4Address) {
            return address;
        }

        Inet6Address ipv6 = (Inet6Address) address;
        if (ipv6.isIPv4CompatibleAddress()) {
            byte[] data = ipv6.getAddress();
            byte[] ipv4Data = new byte[4];
            System.arraycopy(data, 12, ipv4Data, 0, 4);
            try {
                return Inet4Address.getByAddress(data);
            } catch (UnknownHostException e) {
                // log the error, and return the origin address
                TR.error(e);
                return address;
            }
        }
        return address;
    }

    /**
     * convert to ipv6 address
     *
     * @param address source address
     * @return ipv6 address. if convert failed, it will return the source address
     */
    public static InetAddress toIPv6(InetAddress address) {
        if (address instanceof Inet6Address) {
            return address;
        }

        byte[] ipv4Data = address.getAddress();
        byte[] ipv6Data = new byte[16];
        Arrays.fill(ipv6Data, (byte) 0x00);
        System.arraycopy(ipv4Data, 0, ipv6Data, 12, 8);
        try {
            return Inet6Address.getByAddress(ipv6Data);
        } catch (UnknownHostException e) {
            // log the error, and return the origin address
            TR.error(e);
            return address;
        }
    }

    /**
     * force to convert address to ipv6 data bytes
     *
     * @param address source address
     * @return 16-byte array
     */
    public static byte[] toIPv6Bytes(InetAddress address) {
        if (address instanceof Inet6Address) {
            return address.getAddress();
        }
        byte[] ipv4Data = address.getAddress();
        byte[] ipv6Data = new byte[16];
        Arrays.fill(ipv6Data, (byte) 0x00);
        System.arraycopy(ipv4Data, 0, ipv6Data, 12, 4);
        return ipv6Data;
    }


    /**
     * convert to ipv6 socket address
     *
     * @param socketAddress source socket address
     * @return ipv6 socket address. if the source address is already the ipv6 socket address or
     * convert failed, it will return the source address
     */
    public static InetSocketAddress toIPv6(InetSocketAddress socketAddress) {
        return convert(socketAddress, true);
    }


    /**
     * convert to ipv4 socket address
     *
     * @param socketAddress source socket address
     * @return ipv4 socket address. if the source address is already the ipv4 socket address or
     * convert failed, it will return the source address
     */
    public static InetSocketAddress toIPv4(InetSocketAddress socketAddress) {
        return convert(socketAddress, false);
    }


    private static InetSocketAddress convert(InetSocketAddress socketAddress, boolean ipv4Tov6) {
        InetAddress address = socketAddress.getAddress();

        InetAddress newAddress = ipv4Tov6 ? toIPv6(address) : toIPv4(address);
        if (newAddress == address) {
            return socketAddress;
        }
        return new InetSocketAddress(newAddress, socketAddress.getPort());
    }



}
