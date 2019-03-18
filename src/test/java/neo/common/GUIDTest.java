package neo.common;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class GUIDTest {

    @Test
    public void newGuid() {
        String guid = GUID.newGuid();
        Assert.assertEquals(36, guid.length());
    }
}