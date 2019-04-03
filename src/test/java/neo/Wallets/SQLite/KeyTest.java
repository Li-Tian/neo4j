package neo.Wallets.SQLite;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class KeyTest {

    @Test
    public void testGetAndSet() {
        Key key = new Key();
        key.setName("name");
        key.setValue("value".getBytes());

        Assert.assertEquals("name", key.getName());
        Assert.assertArrayEquals("value".getBytes(), key.getValue());
    }
}