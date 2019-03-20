package neo.ledger;

import org.junit.Assert;
import org.junit.Test;

import neo.Fixed8;
import neo.Utils;
import neo.cryptography.ecc.ECC;


public class ValidatorStateTest {

    @Test
    public void copy() {
        ValidatorState validatorState = new ValidatorState();
        validatorState.publicKey = ECC.getInfinityPoint();
        validatorState.registered = false;
        validatorState.votes = new Fixed8(2);

        ValidatorState copy = validatorState.copy();
        Assert.assertEquals(validatorState.publicKey, copy.publicKey);
        Assert.assertEquals(validatorState.registered, copy.registered);
        Assert.assertEquals(validatorState.votes, copy.votes);
        Assert.assertEquals(validatorState.publicKey, validatorState.publicKey);
        Assert.assertEquals(validatorState.votes, validatorState.getVotes());
    }

    @Test
    public void fromReplica() {
        ValidatorState validatorState = new ValidatorState();
        validatorState.publicKey = ECC.getInfinityPoint();
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
        validatorState.publicKey = ECC.getInfinityPoint();
        validatorState.registered = false;
        validatorState.votes = new Fixed8(2);

        Assert.assertEquals(11, validatorState.size());
    }

    @Test
    public void serialize() {
        ValidatorState validatorState = new ValidatorState(ECC.getInfinityPoint());
        validatorState.registered = false;
        validatorState.votes = new Fixed8(2);

        ValidatorState tmp = Utils.copyFromSerialize(validatorState, ValidatorState::new);

        Assert.assertEquals(validatorState.publicKey, tmp.publicKey);
        Assert.assertEquals(validatorState.registered, tmp.registered);
        Assert.assertEquals(validatorState.votes, tmp.votes);
    }

    @Test
    public void toJson() {
        ValidatorState validatorState = new ValidatorState();
        validatorState.publicKey = ECC.getInfinityPoint();
        validatorState.registered = false;
        validatorState.votes = new Fixed8(2);

        Assert.assertEquals("{\"VERSION\":0}", validatorState.toJson().toString());
    }
}