package neo.wallets;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: KeyPairTest
 * @Package neo.wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 10:32 2019/4/3
 */
public class KeyPairTest {
    @Test
    public void getPublicKeyHash() throws Exception {
        KeyPair key = new KeyPair(Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr", "1234567890"));
        Assert.assertEquals("0x3c28afcb112d91c849951640b3d385277e7297fc",key.getPublicKeyHash().toString());
    }

    @Test
    public void equals() throws Exception {
        KeyPair key1 = new KeyPair(Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr", "1234567890"));
        KeyPair key2 = new KeyPair(Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr", "1234567890"));

        Assert.assertEquals(true,key1.equals(key2));
    }

    @Test
    public void equals1() throws Exception {
        KeyPair key1 = new KeyPair(Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr", "1234567890"));
        Object key2 = new KeyPair(Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr", "1234567890"));

        Assert.assertEquals(true,key1.equals(key2));
    }

    @Test
    public void export() throws Exception {
        KeyPair key1 = new KeyPair(Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr", "1234567890"));
        Assert.assertEquals("Kzk4z9PSyFRQ6hwAE51ch9qaHjm7wPFC8usHsq6tgtZHoasRegDi",key1.export());
    }

    @Test
    public void export1() throws Exception {
        KeyPair key1 = new KeyPair(Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr", "1234567890"));
        Assert.assertEquals("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr",
                key1.export("1234567890"));
    }

    @Test
    public void export2() throws Exception {
        KeyPair key1 = new KeyPair(Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr", "1234567890"));
        Assert.assertEquals("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr",
                key1.export("1234567890",16384,8,8));
    }

    @Test
    public void hashCodetest() throws Exception {
        KeyPair key1 = new KeyPair(Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr", "1234567890"));
/*        Assert.assertEquals(-1778840697,
                key1.hashCode());*/
    }

    @Test
    public void toStringtest() throws Exception {
        KeyPair key1 = new KeyPair(Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr", "1234567890"));
        Assert.assertEquals("0276caff42decbebf4337be45c0a517e5b6575ec43aacbd32d95db199182b5b1e4",
                key1.toString());
    }

}