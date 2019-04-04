package neo.ledger;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class CoinStateTest {

    @Test
    public void OR() {
        CoinState state = new CoinState((byte) 0x01);
        CoinState newState = state.OR(CoinState.Confirmed).OR(CoinState.Spent);

        Assert.assertTrue(newState.hasFlag(CoinState.Confirmed));
        Assert.assertTrue(newState.hasFlag(CoinState.Spent));
    }
}