package neo.Wallets;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

import neo.UInt160;
import neo.UInt256;
import neo.network.p2p.payloads.TransactionOutput;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: TransferOutputTest
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 18:16 2019/4/3
 */
public class TransferOutputTest {
    @Test
    public void isGlobalAsset() throws Exception {
        TransferOutput t=new TransferOutput(UInt256.Zero,new BigDecimal(12),null);
        Assert.assertEquals(true,t.isGlobalAsset());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toTxOutput() throws Exception {
        TransferOutput t=new TransferOutput(UInt160.Zero,new BigDecimal(12),null);
        TransactionOutput tx=t.toTxOutput();
    }

    @Test
    public void getAssetId() throws Exception {
        TransferOutput t=new TransferOutput(UInt256.Zero,new BigDecimal(12),null);
        Assert.assertEquals(UInt256.Zero,t.getAssetId());
    }

    @Test
    public void getValue() throws Exception {
        TransferOutput t=new TransferOutput(UInt256.Zero,new BigDecimal(12),null);
        Assert.assertEquals(new BigDecimal(12),t.getValue());
    }

    @Test
    public void getScriptHash() throws Exception {
        TransferOutput t=new TransferOutput(UInt256.Zero,new BigDecimal(12),null);
        Assert.assertEquals(null,t.getScriptHash());
    }

}