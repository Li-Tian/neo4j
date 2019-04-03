package neo.ledger;

import org.junit.Assert;
import org.junit.Test;

import neo.network.p2p.payloads.ClaimTransaction;

public class PoolItemTest {
    @Test
    public void compareToTest () {
        PoolItem poolItem1 = new PoolItem(Blockchain.GoverningToken);
        Assert.assertEquals(0, poolItem1.compareTo(poolItem1));
        Assert.assertEquals(1, poolItem1.compareTo(null));
        Assert.assertEquals(-1, poolItem1.compareTo(new PoolItem(new ClaimTransaction())));
        Assert.assertEquals(true, poolItem1.compareTo(new PoolItem(Blockchain.UtilityToken)) < 0);
    }
}