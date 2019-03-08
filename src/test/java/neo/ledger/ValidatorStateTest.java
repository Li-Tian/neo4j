package neo.ledger;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import neo.Fixed8;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;


public class ValidatorStateTest {

    @Test
    public void copy() {
        ValidatorState validatorState = new ValidatorState();
        validatorState.publicKey = new ECPoint(ECPoint.secp256r1.getCurve().getInfinity());
        validatorState.registered = false;
        validatorState.votes = new Fixed8(2);

        ValidatorState copy = validatorState.copy();
        Assert.assertEquals(validatorState.publicKey, copy.publicKey);
        Assert.assertEquals(validatorState.registered, copy.registered);
        Assert.assertEquals(validatorState.votes, copy.votes);
        Assert.assertEquals(validatorState.publicKey, validatorState.getPublicKey());
        Assert.assertEquals(validatorState.votes, validatorState.getVotes());
    }

    @Test
    public void fromReplica() {
        ValidatorState validatorState = new ValidatorState();
        validatorState.publicKey = new ECPoint(ECPoint.secp256r1.getCurve().getInfinity());
        validatorState.registered = false;
        validatorState.votes = new Fixed8(2);

        ValidatorState replica = new ValidatorState();
        replica.fromReplica(validatorState);
        Assert.assertEquals(validatorState.publicKey, replica.publicKey);
        Assert.assertEquals(validatorState.registered, replica.registered);
        Assert.assertEquals(validatorState.votes, replica.votes);
    }

    @Test
    public void size() {
        ValidatorState validatorState = new ValidatorState();
        validatorState.publicKey = new ECPoint(ECPoint.secp256r1.getCurve().getInfinity());
        validatorState.registered = false;
        validatorState.votes = new Fixed8(2);

        Assert.assertEquals(11, validatorState.size());
    }

    @Test
    public void serialize() {
        ValidatorState validatorState = new ValidatorState(new ECPoint(ECPoint.secp256r1.getCurve().getInfinity()));
        validatorState.registered = false;
        validatorState.votes = new Fixed8(2);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        validatorState.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        ValidatorState tmp = new ValidatorState();
        tmp.deserialize(new BinaryReader(inputStream));

        Assert.assertEquals(validatorState.publicKey, tmp.publicKey);
        Assert.assertEquals(validatorState.registered, tmp.registered);
        Assert.assertEquals(validatorState.votes, tmp.votes);
    }

    @Test
    public void toJson() {
        ValidatorState validatorState = new ValidatorState();
        validatorState.publicKey = new ECPoint(ECPoint.secp256r1.getCurve().getInfinity());
        validatorState.registered = false;
        validatorState.votes = new Fixed8(2);

        Assert.assertEquals("{\"version\":0}", validatorState.toJson().toString());
    }
}