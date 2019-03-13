package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransactionBuilderTest {

    @Test
    public void build() {
        Transaction tx = TransactionBuilder.build(TransactionType.RegisterTransaction);
        Assert.assertTrue(tx instanceof RegisterTransaction);
    }
}