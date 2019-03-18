package neo.consensus;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import neo.Utils;

import static org.junit.Assert.*;

public class PrepareResponseTest {

    @Test
    public void size() {
        PrepareResponse response = new PrepareResponse() {{
            viewNumber = 0x01;
            signature = new byte[64];
        }};
        Arrays.fill(response.signature, (byte) 0x01);
        Assert.assertEquals(66, response.size());
    }

    @Test
    public void serialize() {
        PrepareResponse response = new PrepareResponse() {{
            viewNumber = 0x01;
            signature = new byte[64];
        }};
        Arrays.fill(response.signature, (byte) 0x01);

        PrepareResponse copy = Utils.copyFromSerialize(response, PrepareResponse::new);
        Assert.assertEquals(response.viewNumber, copy.viewNumber);
        Assert.assertArrayEquals(response.signature, copy.signature);
    }
}