package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import neo.Utils;
import neo.smartcontract.ContractParameterType;
import neo.vm.OpCode;

import static org.junit.Assert.*;

public class PublishTransactionTest {

    @Test
    public void getScriptHash() {
        PublishTransaction transaction = new PublishTransaction() {{
            script = new byte[OpCode.PUSHT.getCode()];
        }};
        Assert.assertEquals("0x2abb3c286870d1298f24edd7cfc4d81c8d9008d8", transaction.getScriptHash().toString());
    }

    @Test
    public void size() {
        PublishTransaction transaction = new PublishTransaction() {{
            script = new byte[]{OpCode.PUSHT.getCode()};
            parameterList = new ContractParameterType[0];
            returnType = ContractParameterType.Void;
            needStorage = true;
            name = "test";
            codeVersion = "1.0";
            author = "dev";
            email = "dev@neo.org";
            description = "publish a contract";
        }};
        // 6 + script(1+) + param(1+) + 1 + name(1+) + code(1+) + author(1+) + emai(1+) + desc(1+)
        // 6 + 2 + 1 + 1 + 5 + 4 + 4 + 12 + 19
        Assert.assertEquals(54, transaction.size());
    }

    @Test
    public void serializeExclusiveData() {
        PublishTransaction transaction = new PublishTransaction() {{
            version = 1;
            script = new byte[]{OpCode.PUSHT.getCode()};
            parameterList = new ContractParameterType[0];
            returnType = ContractParameterType.Void;
            needStorage = true;
            name = "test";
            codeVersion = "1.0";
            author = "dev";
            email = "dev@neo.org";
            description = "publish a contract";
        }};

        PublishTransaction copy = Utils.copyFromSerialize(transaction, () -> {
            PublishTransaction tmp = new PublishTransaction();
            tmp.version = 1;
            return tmp;
        });

        Assert.assertArrayEquals(transaction.script, copy.script);
        Assert.assertArrayEquals(transaction.parameterList, copy.parameterList);
        Assert.assertEquals(transaction.returnType, copy.returnType);
        Assert.assertEquals(transaction.needStorage, copy.needStorage);
        Assert.assertEquals(transaction.name, copy.name);
        Assert.assertEquals(transaction.codeVersion, copy.codeVersion);
        Assert.assertEquals(transaction.description, copy.description);
    }

    @Test
    public void toJson() {
        PublishTransaction transaction = new PublishTransaction() {{
            version = 1;
            script = new byte[]{OpCode.PUSHT.getCode()};
            parameterList = new ContractParameterType[0];
            returnType = ContractParameterType.Void;
            needStorage = true;
            name = "test";
            codeVersion = "1.0";
            author = "dev";
            email = "dev@neo.org";
            description = "publish a contract";
        }};

        JsonObject jsonObject = transaction.toJson();
        Assert.assertEquals(transaction.hash().toString(), jsonObject.get("txid").getAsString());
        Assert.assertEquals(transaction.size(), jsonObject.get("size").getAsInt());
        Assert.assertEquals(transaction.type.value(), jsonObject.get("type").getAsInt());

        JsonObject contract = jsonObject.getAsJsonObject("contract");
        Assert.assertEquals(transaction.getScriptHash().toString(), contract.getAsJsonObject("code").get("hash").getAsString());
        Assert.assertEquals(ContractParameterType.Void.value(), contract.getAsJsonObject("code").get("returntype").getAsInt());
    }

    @Test
    public void verify() {
        PublishTransaction transaction = new PublishTransaction();
        Assert.assertFalse(transaction.verify(null));
    }
}