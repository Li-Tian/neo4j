package neo.consensus;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import neo.Utils;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;

import static org.junit.Assert.*;

public class ChangeViewTest {

    @Test
    public void size() {
        ChangeView changeView = new ChangeView();
        Assert.assertEquals(3, changeView.size());
    }

    @Test
    public void serialize() {
        ChangeView changeView = new ChangeView() {{
            viewNumber = (byte) 0x0a;
            newViewNumber = (byte) 0x0b;
        }};

        ChangeView copy = Utils.copyFromSerialize(changeView, ChangeView::new);
        Assert.assertEquals(changeView.viewNumber, copy.viewNumber);
        Assert.assertEquals(changeView.newViewNumber, copy.newViewNumber);
    }

    @Test
    public void deserializeFrom() {
        ChangeView changeView = new ChangeView() {{
            viewNumber = (byte) 0x0a;
            newViewNumber = (byte) 0x0b;
        }};

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(output);
        changeView.serialize(writer);

        ConsensusMessage message = ConsensusMessage.deserializeFrom(output.toByteArray());
        Assert.assertTrue(message instanceof ChangeView);
        ChangeView copy = (ChangeView) message;
        Assert.assertEquals(changeView.viewNumber, copy.viewNumber);
        Assert.assertEquals(changeView.newViewNumber, copy.newViewNumber);
    }
}