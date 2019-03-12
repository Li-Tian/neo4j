package neo.ledger;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import neo.UInt256;
import neo.Utils;
import neo.csharp.Uint;
import neo.csharp.Ushort;

public class SpentCoinStateTest {

    @Test
    public void size() {
        SpentCoinState coinState = new SpentCoinState();
        coinState.transactionHeight = new Uint(10);
        coinState.transactionHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        coinState.items = new HashMap<>();
        coinState.items.put(new Ushort(1), new Uint(100));
        coinState.items.put(new Ushort(2), new Uint(200));

        Assert.assertEquals(50, coinState.size());
    }

    @Test
    public void copy() {
        SpentCoinState coinState = new SpentCoinState();
        coinState.transactionHeight = new Uint(10);
        coinState.transactionHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        coinState.items = new HashMap<>();
        coinState.items.put(new Ushort(1), new Uint(100));
        coinState.items.put(new Ushort(2), new Uint(200));

        SpentCoinState copy = coinState.copy();

        Assert.assertEquals(coinState.transactionHeight, copy.transactionHeight);
        Assert.assertEquals(coinState.transactionHash, copy.transactionHash);
        Assert.assertEquals(coinState.items, copy.items);
    }

    @Test
    public void fromReplica() {
        SpentCoinState coinState = new SpentCoinState();
        coinState.transactionHeight = new Uint(10);
        coinState.transactionHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        coinState.items = new HashMap<>();
        coinState.items.put(new Ushort(1), new Uint(100));
        coinState.items.put(new Ushort(2), new Uint(200));

        SpentCoinState copy = new SpentCoinState();
        copy.fromReplica(coinState);

        Assert.assertEquals(coinState.transactionHeight, copy.transactionHeight);
        Assert.assertEquals(coinState.transactionHash, copy.transactionHash);
        Assert.assertEquals(coinState.items, copy.items);
    }

    @Test
    public void serialize() {
        SpentCoinState coinState = new SpentCoinState();
        coinState.transactionHeight = new Uint(10);
        coinState.transactionHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        coinState.items = new HashMap<>();
        coinState.items.put(new Ushort(1), new Uint(100));
        coinState.items.put(new Ushort(2), new Uint(200));

        SpentCoinState tmp = Utils.copyFromSerialize(coinState, SpentCoinState::new);

        Assert.assertEquals(coinState.transactionHeight, tmp.transactionHeight);
        Assert.assertEquals(coinState.transactionHash, tmp.transactionHash);
        Assert.assertEquals(new Uint(100), tmp.items.get(new Ushort(1)));
        Assert.assertEquals(new Uint(200), tmp.items.get(new Ushort(2)));
    }
}