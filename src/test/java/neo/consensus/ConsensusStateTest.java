package neo.consensus;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConsensusStateTest {

    @Test
    public void and() {
        ConsensusState a = new ConsensusState((byte) 0x01);
        ConsensusState b = new ConsensusState((byte) 0x03);

        ConsensusState c = a.and(b);
        Assert.assertEquals(0x01, c.value());
    }

    @Test
    public void or() {
        ConsensusState a = new ConsensusState((byte) 0x01);
        ConsensusState b = new ConsensusState((byte) 0x03);

        ConsensusState c = a.or(b);
        Assert.assertEquals(0x03, c.value());
    }
}