package neo.ledger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;

import neo.Fixed8;
import neo.UInt160;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;


public class AccountStateTest {

    @Test
    public void size() {
        AccountState accountState = new AccountState();
        accountState.scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        accountState.isFrozen = false;
        accountState.votes = new ECPoint[]{new ECPoint(ECPoint.secp256r1.getCurve().getInfinity())};
        accountState.balances = new ConcurrentHashMap<>();
        accountState.balances.put(Blockchain.GoverningToken.hash(), new Fixed8(100));
        accountState.balances.put(Blockchain.UtilityToken.hash(), new Fixed8(200));

        Assert.assertEquals(105, accountState.size());
    }

    @Test
    public void copy() {
        AccountState accountState = new AccountState(UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
        accountState.isFrozen = false;
        accountState.votes = new ECPoint[]{new ECPoint(ECPoint.secp256r1.getCurve().getInfinity())};
        accountState.balances = new ConcurrentHashMap<>();
        accountState.balances.put(Blockchain.GoverningToken.hash(), new Fixed8(100));
        accountState.balances.put(Blockchain.UtilityToken.hash(), new Fixed8(200));

        AccountState copy = accountState.copy();
        Assert.assertEquals(false, copy.isFrozen);
        Assert.assertEquals(new ECPoint(ECPoint.secp256r1.getCurve().getInfinity()), copy.votes[0]);
        Assert.assertEquals(new Fixed8(100), copy.balances.get(Blockchain.GoverningToken.hash()));
        Assert.assertEquals(new Fixed8(200), copy.balances.get(Blockchain.UtilityToken.hash()));
    }

    @Test
    public void fromReplica() {
        AccountState accountState = new AccountState();
        accountState.scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        accountState.isFrozen = false;
        accountState.votes = new ECPoint[]{new ECPoint(ECPoint.secp256r1.getCurve().getInfinity())};
        accountState.balances = new ConcurrentHashMap<>();
        accountState.balances.put(Blockchain.GoverningToken.hash(), new Fixed8(100));
        accountState.balances.put(Blockchain.UtilityToken.hash(), new Fixed8(200));

        AccountState copy = accountState.copy();
        Assert.assertEquals(false, copy.isFrozen);
        Assert.assertEquals(new ECPoint(ECPoint.secp256r1.getCurve().getInfinity()), copy.votes[0]);
        Assert.assertEquals(new Fixed8(100), copy.balances.get(Blockchain.GoverningToken.hash()));
        Assert.assertEquals(new Fixed8(200), copy.balances.get(Blockchain.UtilityToken.hash()));
    }

    @Test
    public void getBalance() {
        AccountState accountState = new AccountState();
        accountState.scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        accountState.isFrozen = false;
        accountState.votes = new ECPoint[]{new ECPoint(ECPoint.secp256r1.getCurve().getInfinity())};
        accountState.balances = new ConcurrentHashMap<>();
        accountState.balances.put(Blockchain.GoverningToken.hash(), new Fixed8(100));
        accountState.balances.put(Blockchain.UtilityToken.hash(), new Fixed8(200));

        Assert.assertEquals(new Fixed8(100), accountState.getBalance(Blockchain.GoverningToken.hash()));
        Assert.assertEquals(new Fixed8(200), accountState.getBalance(Blockchain.UtilityToken.hash()));
    }

    @Test
    public void serialize() {
        AccountState accountState = new AccountState();
        accountState.scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        accountState.isFrozen = false;
        accountState.votes = new ECPoint[]{new ECPoint(ECPoint.secp256r1.getCurve().getInfinity())};
        accountState.balances = new ConcurrentHashMap<>();
        accountState.balances.put(Blockchain.GoverningToken.hash(), new Fixed8(100));
        accountState.balances.put(Blockchain.UtilityToken.hash(), new Fixed8(200));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        accountState.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        AccountState copy = new AccountState();
        copy.deserialize(new BinaryReader(inputStream));

        Assert.assertEquals(false, copy.isFrozen);
        Assert.assertEquals(new ECPoint(ECPoint.secp256r1.getCurve().getInfinity()), copy.votes[0]);
        Assert.assertEquals(new Fixed8(100), copy.balances.get(Blockchain.GoverningToken.hash()));
        Assert.assertEquals(new Fixed8(200), copy.balances.get(Blockchain.UtilityToken.hash()));
    }

    @Test
    public void toJson() {
        AccountState accountState = new AccountState();
        accountState.scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        accountState.isFrozen = false;
        accountState.votes = new ECPoint[]{new ECPoint(ECPoint.secp256r1.getCurve().getInfinity())};
        accountState.balances = new ConcurrentHashMap<>();
        accountState.balances.put(Blockchain.GoverningToken.hash(), new Fixed8(100));
        accountState.balances.put(Blockchain.UtilityToken.hash(), new Fixed8(200));

        JsonObject jsonObject = accountState.toJson();
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", jsonObject.get("script_hash").getAsString());

        JsonArray array = jsonObject.getAsJsonArray("balances");
        JsonObject asset1 = array.get(0).getAsJsonObject();
        String assetId = asset1.get("asset").getAsString();
        if (assetId.equals(Blockchain.UtilityToken.hash())) {
            Assert.assertEquals(new Fixed8(200), Fixed8.fromDecimal(asset1.get("value").getAsBigDecimal()));
        } else {
            Assert.assertEquals(new Fixed8(100), Fixed8.fromDecimal(asset1.get("value").getAsBigDecimal()));
        }
    }
}