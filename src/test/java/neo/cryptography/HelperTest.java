package neo.cryptography;

import org.junit.Assert;
import org.junit.Test;

import neo.csharp.BitConverter;

public class HelperTest {
    @Test
    public void aes256Encrypt() throws Exception {
        String string = new String("abcdefghabcdefghabcdefghabcdefgh");
        byte[] correctResult = new byte[]{-73, 95, 127, -74, 22, 31, 84, 34, 119, -4, 32, 0, 84, 66, 70, -68, -73, 95, 127, -74, 22, 31, 84, 34, 119, -4, 32, 0, 84, 66, 70, -68};
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i++) key[i] = 0;
        byte[] result = Helper.aes256Encrypt(string.getBytes(), key);
        Assert.assertEquals(correctResult, result);
    }

    @Test
    public void aes256Decrypt() throws Exception {
        byte[] input = new byte[]{-73, 95, 127, -74, 22, 31, 84, 34, 119, -4, 32, 0, 84, 66, 70, -68, -73, 95, 127, -74, 22, 31, 84, 34, 119, -4, 32, 0, 84, 66, 70, -68};
        byte[] correctResult = new byte[]{97, 98, 99, 100, 101, 102, 103, 104, 97, 98, 99, 100, 101, 102, 103, 104, 97, 98, 99, 100, 101, 102, 103, 104, 97, 98, 99, 100, 101, 102, 103, 104};
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i++) key[i] = 0;
        byte[] result = Helper.aes256Decrypt(input, key);
        Assert.assertEquals(correctResult, result);
    }

    @Test
    public void aesEncrypt() throws Exception {
        byte[] data = new byte[]{97, 98, 99, 100, 101, 102, 103, 104, 97, 98, 99, 100, 101, 102, 103, 104, 97, 98, 99, 100, 101, 102, 103, 104, 97, 98, 99, 100, 101, 102, 103, 104};
        byte[] correctResult = new byte[]{52, 80, -119, -110, 42, -37, -9, -101, -82, 6, 39, -5, 9, 51, -25, 90, -39, -83, 118, 73, 100, 55, -93, -109, -103, 50, 23, -101, -98, -39, 51, -37};
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = 0;
        }
        byte[] iv = new byte[16];
        for (int i = 0; i < iv.length; i++) {
            iv[i] = 0;
        }
        byte[] result = Helper.aesEncrypt(data, key, iv);
        Assert.assertEquals(correctResult, result);
    }

    @Test
    public void aesDecrypt() throws Exception {
        byte[] correctResult = new byte[]{97, 98, 99, 100, 101, 102, 103, 104, 97, 98, 99, 100, 101, 102, 103, 104, 97, 98, 99, 100, 101, 102, 103, 104, 97, 98, 99, 100, 101, 102, 103, 104};
        byte[] data = new byte[]{52, 80, -119, -110, 42, -37, -9, -101, -82, 6, 39, -5, 9, 51, -25, 90, -39, -83, 118, 73, 100, 55, -93, -109, -103, 50, 23, -101, -98, -39, 51, -37};
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = 0;
        }
        byte[] iv = new byte[16];
        for (int i = 0; i < iv.length; i++) {
            iv[i] = 0;
        }
        byte[] result = Helper.aesDecrypt(data, key, iv);
        Assert.assertEquals(correctResult, result);
    }

    @Test
    public void base58CheckEncode() {
        String saltdata = "17ad5cac596a1ef6c18ac1746dfd304f93964354b5";
        String correctResult = new String("AXaXZjZGA3qhQRTCsyG5uFKr9HeShgVhTF");
        String result = Helper.base58CheckEncode(hexStringToByte(saltdata));
        Assert.assertEquals(correctResult, result);
    }

    @Test
    public void base58CheckDecode() {
        String data = new String("AXaXZjZGA3qhQRTCsyG5uFKr9HeShgVhTF");
        byte[] correctResult = hexStringToByte("17ad5cac596a1ef6c18ac1746dfd304f93964354b5");
        byte[] result = Helper.base58CheckDecode(data);
        Assert.assertEquals(correctResult, result);
    }

    private static byte[] hexStringToByte(String hex) {
        if (hex == null || hex.trim().equals("")) {
            return new byte[0];
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length() / 2; i++) {
            String subStr = hex.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }

    @Test
    public void ripeMD160() throws Exception {
        // http://tools.jb51.net/password/hash_md5_sha
        String input = "hello world";
        byte[] output = Helper.ripeMD160(input.getBytes());
        Assert.assertEquals("98c615784ccb5fe5936fbc0cbe9dfdb408d92f0f", BitConverter.toHexString(output));
    }

    @Test
    public void sha256() throws Exception {
        // http://encode.chahuo.com/
        String input = "hello world";
        byte[] output = Helper.sha256(input.getBytes());
        Assert.assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", BitConverter.toHexString(output));
    }

    @Test
    public void toAesKey() {
        String data = "abcdrfg";
        byte[] correctResult = new byte[]{-38, -42, -71, 101, -83, -122, -72, -128, -50, -74, -103, 63, -104, -21, -18, -78, 66, -34, 57, -10, -72, 122, 69, -116, 101, 16, -75, -95, 95, -9, -69, -15};
        byte[] result = Helper.toAesKey(data);
        Assert.assertEquals(correctResult, result);
    }
}