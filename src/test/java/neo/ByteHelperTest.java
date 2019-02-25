package neo;

import org.junit.Assert;
import org.junit.Test;


public class ByteHelperTest {

    @Test
    public void toHexString() {
        String hexString = ByteHelper.toHexString(new byte[]{0x01, 0x02, 0x03, 0x04});
        Assert.assertEquals("01020304", hexString);
    }

    @Test
    public void reverse() {
        byte[] old = new byte[]{0x01, 0x02, 0x03, 0x04};
        byte[] reverse = ByteHelper.reverse(old);
        Assert.assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, reverse);
    }

    @Test
    public void hexToBytes() {
        byte[] bytes = ByteHelper.hexToBytes("0x01020304");
        Assert.assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, bytes);
    }

    @Test
    public void addBytes() {
        byte[] bytes1 = new byte[]{0x01, 0x02};
        byte[] bytes2 = new byte[]{0x03, 0x04};

        byte[] bytes3 = ByteHelper.addBytes(bytes1, bytes2);
        Assert.assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, bytes3);
    }
}