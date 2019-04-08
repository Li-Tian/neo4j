package neo.Wallets.SQLite;

import org.junit.Assert;
import org.junit.Test;

import neo.csharp.BitConverter;

import static org.junit.Assert.*;

public class VersionTest {

    @Test
    public void getValue() {
        Version version = new Version(2, 9, 4, 0);
        byte[] values = new byte[16];
        System.arraycopy(BitConverter.getBytes(2), 0, values, 0, 4);
        System.arraycopy(BitConverter.getBytes(9), 0, values, 4, 4);
        System.arraycopy(BitConverter.getBytes(4), 0, values, 8, 4);
        System.arraycopy(BitConverter.getBytes(0), 0, values, 12, 4);

        Assert.assertArrayEquals(values, version.getValue());
    }

    @Test
    public void testToString() {
        Version version = new Version(2, 9, 4, 0);
        Assert.assertEquals("2.9.4.0", version.toString());
    }

    @Test
    public void parse() {
        Version version = Version.parse("2.9.4.0");
        Version a = new Version(2, 9, 4, 0);

        Assert.assertEquals(0, version.compareTo(a));

        byte[] values = BitConverter.hexToBytes("02000000090000000200000000000000");
        version = new Version(values);

        Assert.assertEquals("2.9.2.0", version.toString());
    }

    @Test
    public void tryParse() {
        Version defaultVersion = new Version(0, 0, 0, 0);
        Version version = Version.tryParse("2.9.4.0", defaultVersion);
        Version a = new Version(2, 9, 4, 0);

        Assert.assertEquals(0, version.compareTo(a));

        version = Version.tryParse("2", defaultVersion);
        Assert.assertEquals(0, version.compareTo(defaultVersion));
    }

}