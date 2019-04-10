package neo.cryptography.ecc;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;

public class ECPointTest {
    @Test
    public void size() {
        Assert.assertEquals(1, new neo.cryptography.ecc.ECPoint().size());
        Assert.assertEquals(33, new neo.cryptography.ecc.ECPoint(ECC.Secp256r1.getCurve().getA(), ECC.Secp256r1.getCurve().getB(), ECC.Secp256r1.getCurve(), true).size());
    }

    @Test
    public void serialize() {
        ECPoint point = new neo.cryptography.ecc.ECPoint(ECC.Secp256r1.getCurve().getA(), ECC.Secp256r1.getCurve().getB(), ECC.Secp256r1.getCurve(), true);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        point.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        ECPoint copy = new ECPoint();
        copy.deserialize(new BinaryReader(inputStream));
        Assert.assertEquals(point.toString(), copy.toString());
    }

    @Test
    public void compareTo() {
        ECPoint pointA = new neo.cryptography.ecc.ECPoint(ECC.Secp256r1.getCurve().getA(), ECC.Secp256r1.getCurve().getB(), ECC.Secp256r1.getCurve(), true);
        ECPoint pointB = new neo.cryptography.ecc.ECPoint(pointA.getXCoord().addOne(), pointA.getYCoord(), ECC.Secp256r1.getCurve(), true);
        ECPoint pointC = new neo.cryptography.ecc.ECPoint(pointA.getXCoord().negate(), pointA.getYCoord(), ECC.Secp256r1.getCurve(), true);
        ECPoint pointD = new neo.cryptography.ecc.ECPoint(pointA.getXCoord(), pointA.getYCoord().addOne(), ECC.Secp256r1.getCurve(), true);
        Assert.assertEquals(0, pointA.compareTo(pointA));
        Assert.assertEquals(-1, pointA.compareTo(pointB));
        Assert.assertEquals(1, pointA.compareTo(pointC));
        Assert.assertEquals(-1, pointA.compareTo(pointD));
    }

    @Test
    public void fromBytes() {
        String pubKey = "03b209fd4f53a7170ea4444e0cb0a6bb6a53c2bd016926989cf85f9b0fba17a70c";
        ECPoint point = ECPoint.fromBytes(BitConverter.hexToBytes(pubKey), ECC.Secp256r1.getCurve());
        Assert.assertEquals(pubKey, point.toString());
    }

    @Test
    public void getHashCode() {
        String pubKey = "03b209fd4f53a7170ea4444e0cb0a6bb6a53c2bd016926989cf85f9b0fba17a70c";
        ECPoint point = ECPoint.fromBytes(BitConverter.hexToBytes(pubKey), ECC.Secp256r1.getCurve());
        Assert.assertEquals(745416063, point.getHashCode());
    }

    @Test
    public void toStringTest() {
        String pubKey = "03b209fd4f53a7170ea4444e0cb0a6bb6a53c2bd016926989cf85f9b0fba17a70c";
        ECPoint point = ECPoint.fromBytes(BitConverter.hexToBytes(pubKey), ECC.Secp256r1.getCurve());
        Assert.assertEquals(pubKey, point.toString());
    }

    @Test
    public void multiply() {
        String prikeyString = "dcc816fd420d9023b46f7ecba9713886e22722272d79867d15324d22ee716502";
        byte[] prikey = BitConverter.hexToBytes(prikeyString);
        ECPoint pubkey = new ECPoint(ECC.Secp256r1.getG()).multiply(prikey);
        Assert.assertEquals(true, pubkey.toString().equals("03943930a90fcf8616da21eca2a127151efdcbcb47a41a73a9fd3c474a90ba46a6"));
    }
}