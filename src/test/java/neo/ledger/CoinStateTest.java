package neo.ledger;

import org.junit.Assert;
import org.junit.Test;

public class CoinStateTest {

    @Test
    public void OR() {
        CoinState state = new CoinState((byte) 0x01);
        CoinState newState = state.or(CoinState.Confirmed).or(CoinState.Spent);

        Assert.assertTrue(newState.hasFlag(CoinState.Confirmed));
        Assert.assertTrue(newState.hasFlag(CoinState.Spent));
    }
}