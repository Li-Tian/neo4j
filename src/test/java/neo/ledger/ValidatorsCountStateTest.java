package neo.ledger;

import org.junit.Assert;
import org.junit.Test;

import neo.Fixed8;
import neo.Utils;


public class ValidatorsCountStateTest {

    @Test
    public void size() {
        ValidatorsCountState countState = new ValidatorsCountState();
        countState.votes[0] = new Fixed8(1);
        countState.votes[1] = new Fixed8(2);
        countState.votes[2] = new Fixed8(3);
        countState.votes[3] = new Fixed8(0);

        Assert.assertEquals(1024 * 8 + 1 + 3, countState.size());
    }

    @Test
    public void copy() {
        ValidatorsCountState countState = new ValidatorsCountState();
        countState.votes[0] = new Fixed8(1);
        countState.votes[1] = new Fixed8(2);
        countState.votes[2] = new Fixed8(3);
        countState.votes[3] = new Fixed8(0);

        ValidatorsCountState copy = countState.copy();
        Assert.assertEquals(new Fixed8(1), copy.votes[0]);
        Assert.assertEquals(new Fixed8(2), copy.votes[1]);
        Assert.assertEquals(new Fixed8(3), copy.votes[2]);
        Assert.assertEquals(new Fixed8(0), copy.votes[3]);
    }

    @Test
    public void fromReplica() {
        ValidatorsCountState countState = new ValidatorsCountState();
        countState.votes[0] = new Fixed8(1);
        countState.votes[1] = new Fixed8(2);
        countState.votes[2] = new Fixed8(3);
        countState.votes[3] = new Fixed8(0);

        ValidatorsCountState replica = new ValidatorsCountState();
        replica.fromReplica(countState);
        Assert.assertEquals(new Fixed8(1), replica.votes[0]);
        Assert.assertEquals(new Fixed8(2), replica.votes[1]);
        Assert.assertEquals(new Fixed8(3), replica.votes[2]);
        Assert.assertEquals(new Fixed8(0), replica.votes[3]);
    }

    @Test
    public void serialize() {
        ValidatorsCountState countState = new ValidatorsCountState();
        countState.votes[0] = new Fixed8(1);
        countState.votes[1] = new Fixed8(2);
        countState.votes[2] = new Fixed8(3);
        countState.votes[3] = new Fixed8(0);

        ValidatorsCountState tmp = Utils.copyFromSerialize(countState, ValidatorsCountState::new);

        Assert.assertEquals(new Fixed8(1), tmp.votes[0]);
        Assert.assertEquals(new Fixed8(2), tmp.votes[1]);
        Assert.assertEquals(new Fixed8(3), tmp.votes[2]);
        Assert.assertEquals(new Fixed8(0), tmp.votes[3]);
    }

    @Test
    public void toJson() {
        ValidatorsCountState countState = new ValidatorsCountState();
        countState.votes[0] = new Fixed8(1);
        countState.votes[1] = new Fixed8(2);
        countState.votes[2] = new Fixed8(3);
        countState.votes[3] = new Fixed8(0);

        Assert.assertEquals("{\"version\":0}", countState.toJson().toString());
    }
}