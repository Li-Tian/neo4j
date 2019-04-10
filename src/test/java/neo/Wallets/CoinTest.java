package neo.Wallets;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt160;
import neo.UInt256;
import neo.csharp.Ushort;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.TransactionOutput;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: CoinTest
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 15:16 2019/4/9
 */
public class CoinTest {
    @Test
    public void getAddress() throws Exception {
         Coin coin=new Coin();
        TransactionOutput output=new TransactionOutput();
        output.scriptHash= UInt160.Zero;
        coin.output=output;
        Assert.assertEquals("AFmseVrdL9f9oyCzZefL9tG6UbvhPbdYzM",coin.getAddress());

    }

    @Test
    public void equals() throws Exception {
        Coin coin=new Coin();
        Assert.assertEquals(true,coin.equals(coin));
    }

    @Test
    public void equals1() throws Exception {
        Coin coin=new Coin();
        Object result=coin;
        Assert.assertEquals(true,coin.equals(result));
    }

    @Test
    public void hashCode2() throws Exception {
        Coin coin=new Coin();
        coin.reference=new CoinReference();
        coin.reference.prevIndex= Ushort.ZERO;
        coin.reference.prevHash= UInt256.Zero;
        Assert.assertEquals(0,coin.hashCode());

    }

}