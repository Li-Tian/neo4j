package neo.ledger;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;

public class StateBaseTest {
    private class MyStateBase extends StateBase {
    }

    @Test
    public void serializeTest() {
        try {
            MyStateBase statebase = new MyStateBase();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
            statebase.serialize(writer);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            MyStateBase copy = new MyStateBase();
            copy.deserialize(new BinaryReader(inputStream));
            Assert.assertEquals(statebase.stateVersion, copy.stateVersion);
        } catch (NumberFormatException e) {
            Assert.fail();
        }
    }
}