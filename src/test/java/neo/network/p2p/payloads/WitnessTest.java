package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt160;
import neo.Utils;
import neo.csharp.BitConverter;
import neo.vm.OpCode;

import static org.junit.Assert.*;

public class WitnessTest {

    private Witness witness = new Witness() {{
        verificationScript = new byte[]{OpCode.PUSHT.getCode()};
        invocationScript = new byte[]{OpCode.PUSHT.getCode()};
    }};

    @Test
    public void scriptHash() {
        Assert.assertEquals(UInt160.parseToScriptHash(witness.verificationScript), witness.scriptHash());
    }

    @Test
    public void size() {
        Assert.assertEquals(4, witness.size());
    }

    @Test
    public void serialize() {
        Witness copy = Utils.copyFromSerialize(witness, Witness::new);
        Assert.assertArrayEquals(witness.verificationScript, copy.verificationScript);
        Assert.assertArrayEquals(witness.invocationScript, copy.invocationScript);
    }

    @Test
    public void toJson() {
        JsonObject jsonObject = witness.toJson();

        Assert.assertEquals(BitConverter.toHexString(witness.verificationScript), jsonObject.get("verification").getAsString());
        Assert.assertEquals(BitConverter.toHexString(witness.invocationScript), jsonObject.get("invocation").getAsString());
    }
}