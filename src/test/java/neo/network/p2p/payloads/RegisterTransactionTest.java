package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

import neo.AbstractBlockchainTest;
import neo.Fixed8;
import neo.ProtocolSettings;
import neo.UInt160;
import neo.Utils;
import neo.cryptography.ecc.ECC;
import neo.smartcontract.Contract;

public class RegisterTransactionTest extends AbstractBlockchainTest {

    @Test
    public void getOwnerScriptHash() {
        RegisterTransaction transaction = new RegisterTransaction() {{
            owner = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");
        }};
        Assert.assertEquals("AZUsvyLTuUEJtjuhHbHQsoeHG7YCERAFkD", transaction.getOwnerScriptHash().toAddress());
    }

    @Test
    public void size() {
        RegisterTransaction transaction = new RegisterTransaction() {{
            assetType = AssetType.Share;
            name = "test";
            amount = Fixed8.fromDecimal(BigDecimal.valueOf(100000000));
            precision = 0;
            owner = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");
            admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        }};
        // base.Size + sizeof(AssetType) + Name.GetVarSize() + Amount.Size
        //        // + sizeof(byte) + Owner.Size + Admin.Size;
        // 6 + 1 + 5 + 8 + 1 + 33 + 20
        Assert.assertEquals(74, transaction.size());
    }

    @Test
    public void getSystemFee() {
        RegisterTransaction transaction = new RegisterTransaction();

        Fixed8 fee = ProtocolSettings.Default.systemFee.get(TransactionType.RegisterTransaction);
        fee = fee == null ? Fixed8.ZERO : fee;
        Assert.assertEquals(fee, transaction.getSystemFee());
    }

    @Test
    public void serializeExclusiveData() {
        RegisterTransaction transaction = new RegisterTransaction() {{
            assetType = AssetType.Share;
            name = "test";
            amount = Fixed8.fromDecimal(BigDecimal.valueOf(100000000));
            precision = 0;
            owner = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");
            admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        }};

        RegisterTransaction copy = Utils.copyFromSerialize(transaction, RegisterTransaction::new);
        Assert.assertEquals(transaction.assetType, copy.assetType);
        Assert.assertEquals(transaction.name, copy.name);
        Assert.assertEquals(transaction.amount, copy.amount);
        Assert.assertEquals(transaction.precision, copy.precision);
        Assert.assertEquals(transaction.owner, copy.owner);
        Assert.assertEquals(transaction.admin, copy.admin);
    }

    @Test
    public void toJson() {
        RegisterTransaction transaction = new RegisterTransaction() {{
            assetType = AssetType.Share;
            name = "test";
            amount = Fixed8.fromDecimal(BigDecimal.valueOf(100000000));
            precision = 0;
            owner = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");
            admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        }};

        JsonObject jsonObject = transaction.toJson();
        Assert.assertEquals(transaction.hash().toString(), jsonObject.get("txid").getAsString());
        Assert.assertEquals(74, jsonObject.get("size").getAsInt());
        JsonObject assetJson = jsonObject.getAsJsonObject("asset");
        Assert.assertEquals("test", assetJson.get("name").getAsString());
        Assert.assertEquals("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007", assetJson.get("owner").getAsString());
        Assert.assertEquals(transaction.admin.toAddress(), assetJson.get("admin").getAsString());
    }

    @Test
    public void verify() {
        RegisterTransaction transaction = new RegisterTransaction();
        Assert.assertFalse(transaction.verify(null));
    }

    @Test
    public void getScriptHashesForVerifying() {
        RegisterTransaction transaction = new RegisterTransaction() {{
            assetType = AssetType.Share;
            name = "test";
            amount = Fixed8.fromDecimal(BigDecimal.valueOf(100000000));
            precision = 0;
            owner = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");
            admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        }};

        UInt160 ownerHash = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(transaction.owner));

        UInt160[] hashes = transaction.getScriptHashesForVerifying(store.getSnapshot());
        Assert.assertEquals(1, hashes.length);
        Assert.assertEquals(ownerHash, hashes[0]);
    }
}