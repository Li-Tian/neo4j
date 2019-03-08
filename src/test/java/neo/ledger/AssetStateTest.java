package neo.ledger;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.network.p2p.payloads.AssetType;


public class AssetStateTest {

    @Test
    public void size() {
        AssetState assetState = new AssetState();
        assetState.assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.assetType = AssetType.Share;
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = new ECPoint(ECPoint.secp256r1.getCurve().getInfinity());
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        Assert.assertEquals(131, assetState.size());
    }

    @Test
    public void copy() {
        AssetState assetState = new AssetState();
        assetState.assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.assetType = AssetType.Share;
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = new ECPoint(ECPoint.secp256r1.getCurve().getInfinity());
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        AssetState copy = assetState.copy();
        Assert.assertEquals(assetState.assetId, copy.assetId);
        Assert.assertEquals(assetState.name, copy.name);
        Assert.assertEquals(assetState.available, copy.available);
        Assert.assertEquals(assetState.expiration, copy.expiration);
    }

    @Test
    public void fromReplica() {
        AssetState assetState = new AssetState();
        assetState.assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.assetType = AssetType.Share;
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = new ECPoint(ECPoint.secp256r1.getCurve().getInfinity());
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        AssetState copy = new AssetState();
        copy.fromReplica(assetState);
        Assert.assertEquals(assetState.assetId, copy.assetId);
        Assert.assertEquals(assetState.name, copy.name);
        Assert.assertEquals(assetState.available, copy.available);
        Assert.assertEquals(assetState.expiration, copy.expiration);
    }

    @Test
    public void getName() {
        AssetState assetState = new AssetState();
        assetState.assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.assetType = AssetType.Share;
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = new ECPoint(ECPoint.secp256r1.getCurve().getInfinity());
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        Assert.assertEquals("Test", assetState.getName());
        Assert.assertEquals("Test", assetState.getName(Locale.CHINA));
        Assert.assertEquals("Test", assetState.getName(Locale.ENGLISH));

        assetState = new AssetState();
        assetState.assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.assetType = AssetType.Share;
        assetState.name = "[{ \"lang\":\"en\", \"name\":\"Test\" }, { \"lang\":\"zh-CN\", \"name\":\"测试\" }]";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = new ECPoint(ECPoint.secp256r1.getCurve().getInfinity());
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;
        Assert.assertEquals("Test", assetState.getName(Locale.ENGLISH));
        Assert.assertEquals("测试", assetState.getName(Locale.CHINA));
        Assert.assertEquals(assetState.getName(), assetState.toString());
    }

    @Test
    public void serialize() {
        AssetState assetState = new AssetState();
        assetState.assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.assetType = AssetType.Share;
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = new ECPoint(ECPoint.secp256r1.getCurve().getInfinity());
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        assetState.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        AssetState tmp = new AssetState();
        tmp.deserialize(new BinaryReader(inputStream));

        Assert.assertEquals(assetState.assetId, tmp.assetId);
        Assert.assertEquals(assetState.name, tmp.name);
        Assert.assertEquals(AssetType.Share, tmp.assetType);
        Assert.assertEquals(assetState.available, tmp.available);
        Assert.assertEquals(assetState.expiration, tmp.expiration);
    }

    @Test
    public void toJson() {
        AssetState assetState = new AssetState();
        assetState.assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.assetType = AssetType.Share;
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = new ECPoint(ECPoint.secp256r1.getCurve().getInfinity());
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        JsonObject jsonObject = assetState.toJson();
        Assert.assertEquals("Test", jsonObject.get("name").getAsString());
        Assert.assertEquals("AG3iUs1EeeXDsvMvtsyFmdatySEvKQBcBK", jsonObject.get("admin").getAsString());
        Assert.assertEquals("AG91A48Z4pkvQTxAxjgZjvqEQj7xQ1URgK", jsonObject.get("issuer").getAsString());
        Assert.assertEquals(false, jsonObject.get("frozen").getAsBoolean());
    }


}