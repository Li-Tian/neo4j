package neo.ledger;

import org.junit.Assert;
import org.junit.Test;

import neo.Utils;

public class UnspentCoinStateTest {

    @Test
    public void size() {
        UnspentCoinState coinState = new UnspentCoinState();
        coinState.items = new CoinState[]{CoinState.Claimed, CoinState.Confirmed, CoinState.Spent, CoinState.Unconfirmed};
        Assert.assertEquals(1 + 1 + 4 * 1, coinState.size());
    }

    @Test
    public void copy() {
        UnspentCoinState coinState = new UnspentCoinState();
        coinState.items = new CoinState[]{CoinState.Claimed, CoinState.Confirmed, CoinState.Spent, CoinState.Unconfirmed};

        UnspentCoinState copy = coinState.copy();
        Assert.assertEquals(CoinState.Claimed, copy.items[0]);
        Assert.assertEquals(CoinState.Confirmed, copy.items[1]);
        Assert.assertEquals(CoinState.Spent, copy.items[2]);
        Assert.assertEquals(CoinState.Unconfirmed, copy.items[3]);
    }

    @Test
    public void fromReplica() {
        UnspentCoinState coinState = new UnspentCoinState();
        coinState.items = new CoinState[]{CoinState.Claimed, CoinState.Confirmed, CoinState.Spent, CoinState.Unconfirmed};

        UnspentCoinState copy = new UnspentCoinState();
        copy.fromReplica(coinState);
        Assert.assertEquals(CoinState.Claimed, copy.items[0]);
        Assert.assertEquals(CoinState.Confirmed, copy.items[1]);
        Assert.assertEquals(CoinState.Spent, copy.items[2]);
        Assert.assertEquals(CoinState.Unconfirmed, copy.items[3]);
    }

    @Test
    public void serialize() {
        UnspentCoinState coinState = new UnspentCoinState();
        coinState.items = new CoinState[]{CoinState.Claimed, CoinState.Confirmed, CoinState.Spent, CoinState.Unconfirmed};

        UnspentCoinState tmp = Utils.copyFromSerialize(coinState, UnspentCoinState::new);

        Assert.assertEquals(CoinState.Claimed, tmp.items[0]);
        Assert.assertEquals(CoinState.Confirmed, tmp.items[1]);
        Assert.assertEquals(CoinState.Spent, tmp.items[2]);
        Assert.assertEquals(CoinState.Unconfirmed, tmp.items[3]);
    }
}