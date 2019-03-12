package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

import neo.Fixed8;
import neo.UInt160;
import neo.Utils;
import neo.ledger.Blockchain;

import static org.junit.Assert.*;

public class MinerTransactionTest {

    @Test
    public void size() {
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};

        Assert.assertEquals(70, minerTransaction.size());
    }

    @Test
    public void getNetworkFee() {
        MinerTransaction minerTransaction = new MinerTransaction();
        Assert.assertEquals(Fixed8.ZERO, minerTransaction.getNetworkFee());
    }

    @Test
    public void serializeExclusiveData() {
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};

        MinerTransaction copy = Utils.copyFromSerialize(minerTransaction, MinerTransaction::new);

        Assert.assertEquals(minerTransaction.outputs.length, copy.outputs.length);
        Assert.assertEquals(minerTransaction.outputs[0].assetId, copy.outputs[0].assetId);
        Assert.assertEquals(minerTransaction.outputs[0].value, copy.outputs[0].value);
        Assert.assertEquals(minerTransaction.outputs[0].scriptHash, copy.outputs[0].scriptHash);
    }

    @Test
    public void toJson() {
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};

        JsonObject jsonObject = minerTransaction.toJson();
        Assert.assertEquals(minerTransaction.hash().toString(), jsonObject.get("txid").getAsString());
        Assert.assertEquals(minerTransaction.outputs[0].scriptHash.toAddress(), jsonObject.getAsJsonArray("vout")
                .get(0).getAsJsonObject()
                .get("address").getAsString());
    }
}