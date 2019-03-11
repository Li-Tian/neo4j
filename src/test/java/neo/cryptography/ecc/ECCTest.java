package neo.cryptography.ecc;

import org.junit.Assert;
import org.junit.Test;

public class ECCTest {
    @Test
    public void parseFromHexString () {
        String pubKey = "03b209fd4f53a7170ea4444e0cb0a6bb6a53c2bd016926989cf85f9b0fba17a70c";
        ECPoint point = ECC.parseFromHexString(pubKey);
        Assert.assertEquals(pubKey, point.toString());
    }

    @Test
    public void getInfinityPoint () {
        Assert.assertEquals(true, ECC.getInfinityPoint().isInfinity());
    }
}