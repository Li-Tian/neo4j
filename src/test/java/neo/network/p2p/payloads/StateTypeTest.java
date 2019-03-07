package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import neo.exception.FormatException;

import static org.junit.Assert.*;

public class StateTypeTest {

    @Test
    public void value() {
        Assert.assertEquals(0x40, StateType.Account.value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse() {
        StateType type = StateType.parse((byte) 0x40);
        Assert.assertEquals(StateType.Account, type);
        StateType.parse((byte) 0x00);
    }
}