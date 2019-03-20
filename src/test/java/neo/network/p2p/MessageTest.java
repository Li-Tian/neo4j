package neo.network.p2p;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt160;
import neo.Utils;

import static org.junit.Assert.*;

public class MessageTest {

    @Test
    public void size() {
        Message message = Message.create("hello", new byte[]{0x00, 0x01, 0x02, 0x03});
        // 24 + 4 = 28
        Assert.assertEquals(28, message.size());
    }

    @Test
    public void serialize() {
        Message message = Message.create("hello", new byte[]{0x00, 0x01, 0x02, 0x03});

        Message copy = Utils.copyFromSerialize(message, Message::new);

        Assert.assertEquals(message.size(), copy.size());
        Assert.assertEquals(message.command, copy.command);
        Assert.assertEquals(message.checksum, copy.checksum);
        Assert.assertArrayEquals(message.payload, copy.payload);

        // case 2: create by ISerializable
        message = Message.create("hello", UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
        copy = Utils.copyFromSerialize(message, Message::new);

        Assert.assertEquals(message.size(), copy.size());
        Assert.assertEquals(message.command, copy.command);
        Assert.assertEquals(message.checksum, copy.checksum);
        Assert.assertArrayEquals(message.payload, copy.payload);

        // case 3:
        message = Message.create("hello");
        copy = Utils.copyFromSerialize(message, Message::new);

        Assert.assertEquals(message.size(), copy.size());
        Assert.assertEquals(message.command, copy.command);
        Assert.assertEquals(message.checksum, copy.checksum);
        Assert.assertArrayEquals(message.payload, copy.payload);
    }

}