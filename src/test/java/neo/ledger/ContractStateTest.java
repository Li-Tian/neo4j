package neo.ledger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.smartcontract.ContractParameterType;

import static org.junit.Assert.*;

public class ContractStateTest {

    @Test
    public void hasStorage() {
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        Assert.assertEquals(Objects.hash(ContractPropertyState.HasStorage.value()), ContractPropertyState.HasStorage.hashCode());
        Assert.assertTrue(contractState.hasStorage());
        Assert.assertTrue(contractState.hasDynamicInvoke());
        Assert.assertTrue(contractState.payable());
    }

    @Test
    public void size() {
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        Assert.assertEquals(44, contractState.size());
    }

    @Test
    public void copy() {
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        ContractState copy = contractState.copy();
        Assert.assertEquals(contractState.author, copy.author);
        Assert.assertEquals(contractState.codeVersion, copy.codeVersion);
        Assert.assertArrayEquals(contractState.parameterList, copy.parameterList);
        Assert.assertEquals(contractState.contractProperties, copy.contractProperties);
        Assert.assertEquals(contractState.name, copy.name);
        Assert.assertEquals(contractState.email, copy.email);
        Assert.assertEquals(contractState.description, copy.description);
        Assert.assertEquals(contractState.returnType, copy.returnType);
        Assert.assertArrayEquals(contractState.script, copy.script);
    }

    @Test
    public void fromReplica() {
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        ContractState copy = new ContractState();
        copy.fromReplica(contractState);
        Assert.assertEquals(contractState.author, copy.author);
        Assert.assertEquals(contractState.codeVersion, copy.codeVersion);
        Assert.assertArrayEquals(contractState.parameterList, copy.parameterList);
        Assert.assertEquals(contractState.contractProperties, copy.contractProperties);
        Assert.assertEquals(contractState.name, copy.name);
        Assert.assertEquals(contractState.email, copy.email);
        Assert.assertEquals(contractState.description, copy.description);
        Assert.assertEquals(contractState.returnType, copy.returnType);
        Assert.assertArrayEquals(contractState.script, copy.script);
    }

    @Test
    public void getScriptHash() {
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        // TODO
        Assert.assertEquals("0x706ea1768da7f0c489bf931b362c2d26d8cbd2ec", contractState.getScriptHash().toString());
    }

    @Test
    public void serialize() {
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};


        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        contractState.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        ContractState copy = new ContractState();
        copy.deserialize(new BinaryReader(inputStream));

        Assert.assertEquals(contractState.author, copy.author);
        Assert.assertEquals(contractState.codeVersion, copy.codeVersion);
        Assert.assertArrayEquals(contractState.parameterList, copy.parameterList);
        Assert.assertEquals(contractState.contractProperties, copy.contractProperties);
        Assert.assertEquals(contractState.name, copy.name);
        Assert.assertEquals(contractState.email, copy.email);
        Assert.assertEquals(contractState.description, copy.description);
        Assert.assertEquals(contractState.returnType, copy.returnType);
        Assert.assertArrayEquals(contractState.script, copy.script);
    }

    @Test
    public void toJson() {
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        JsonObject jsonObject = contractState.toJson();

        Assert.assertEquals("01020304", jsonObject.get("script").getAsString());

        JsonArray array = jsonObject.getAsJsonArray("parameters");
        Assert.assertEquals(ContractParameterType.Signature.value(), array.get(0).getAsInt());
        Assert.assertEquals(ContractParameterType.String.value(), array.get(1).getAsInt());
        Assert.assertEquals(ContractParameterType.Hash160.value(), array.get(2).getAsInt());

        JsonObject propertyObj = jsonObject.get("properties").getAsJsonObject();
        Assert.assertTrue(propertyObj.get("storage").getAsBoolean());
        Assert.assertTrue(propertyObj.get("dynamic_invoke").getAsBoolean());
    }
}