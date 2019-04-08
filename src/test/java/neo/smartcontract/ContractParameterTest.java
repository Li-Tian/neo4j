package neo.smartcontract;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

import neo.UInt160;
import neo.UInt256;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;

public class ContractParameterTest {
    @Test
    public void test() {
        ContractParameter parameter = new ContractParameter();
        Assert.assertEquals(true, parameter.toString().equals("(null)"));

        parameter = new ContractParameter(ContractParameterType.Signature);
        Assert.assertEquals(byte[].class, parameter.value.getClass());
        Assert.assertEquals(64, ((byte[]) parameter.value).length);
        String value = "19d64c43948ce0be5ce0752ca543539123695e358e3d30b060424e11f277245a82bef978e94f4381e712d2039c7ce561445a718ed6937cc610f3791cddcff407";
        parameter.setValue(value);
        Assert.assertEquals(true, parameter.toString().equals(value));
        JsonObject jsonObject = parameter.toJson();
        ContractParameter replica = ContractParameter.fromJson(jsonObject);
        Assert.assertEquals(true, parameter.type.equals(replica.type));
        Assert.assertArrayEquals(((byte[]) parameter.value), ((byte[]) replica.value));

        parameter = new ContractParameter(ContractParameterType.Boolean);
        Assert.assertEquals(false, parameter.value);
        parameter.setValue("true");
        Assert.assertEquals(true, parameter.value);
        Assert.assertEquals(true, parameter.toString().equals("true"));
        jsonObject = parameter.toJson();
        replica = ContractParameter.fromJson(jsonObject);
        Assert.assertEquals(true, parameter.type.equals(replica.type));
        Assert.assertEquals(true, parameter.value.equals(replica.value));

        parameter = new ContractParameter(ContractParameterType.Integer);
        Assert.assertEquals(0, parameter.value);
        value = "123456789";
        parameter.setValue(value);
        Assert.assertEquals(true, parameter.value.equals(new BigInteger(value)));
        Assert.assertEquals(true, parameter.toString().equals(value));
        jsonObject = parameter.toJson();
        replica = ContractParameter.fromJson(jsonObject);
        Assert.assertEquals(true, parameter.type.equals(replica.type));
        Assert.assertEquals(true, parameter.value.equals(replica.value));

        parameter = new ContractParameter(ContractParameterType.Hash160);
        Assert.assertEquals(UInt160.class, parameter.value.getClass());
        value = "0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01";
        parameter.setValue(value);
        Assert.assertEquals(true, parameter.value.equals(UInt160.parse(value)));
        Assert.assertEquals(true, parameter.toString().equals(value));

        parameter = new ContractParameter(ContractParameterType.Hash256);
        Assert.assertEquals(UInt256.class, parameter.value.getClass());
        value = "0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01";
        parameter.setValue(value);
        Assert.assertEquals(true, parameter.value.equals(UInt256.parse(value)));
        Assert.assertEquals(true, parameter.toString().equals(value));
        jsonObject = parameter.toJson();
        replica = ContractParameter.fromJson(jsonObject);
        Assert.assertEquals(true, parameter.type.equals(replica.type));
        Assert.assertEquals(true, parameter.value.equals(replica.value));

        parameter = new ContractParameter(ContractParameterType.ByteArray);
        Assert.assertEquals(byte[].class, parameter.value.getClass());
        Assert.assertEquals(0, ((byte[]) parameter.value).length);
        value = "a400ff00ff00ff00ff00ff00ff00ff00ff00ff01";
        parameter.setValue(value);
        Assert.assertArrayEquals(BitConverter.hexToBytes(value), ((byte[]) parameter.value));
        Assert.assertEquals(true, parameter.toString().equals(value));
        jsonObject = parameter.toJson();
        replica = ContractParameter.fromJson(jsonObject);
        Assert.assertEquals(true, parameter.type.equals(replica.type));
        Assert.assertArrayEquals(((byte[]) parameter.value), ((byte[]) replica.value));

        parameter = new ContractParameter(ContractParameterType.PublicKey);
        Assert.assertEquals(ECPoint.class, parameter.value.getClass());
        value = "03b209fd4f53a7170ea4444e0cb0a6bb6a53c2bd016926989cf85f9b0fba17a70c";
        parameter.setValue(value);
        Assert.assertEquals(true, ((ECPoint) parameter.value).toString().equals(value));
        Assert.assertEquals(true, parameter.toString().equals(value));
        jsonObject = parameter.toJson();
        replica = ContractParameter.fromJson(jsonObject);
        Assert.assertEquals(true, parameter.type.equals(replica.type));
        Assert.assertEquals(true, parameter.value.equals(replica.value));

        parameter = new ContractParameter(ContractParameterType.String);
        Assert.assertEquals(String.class, parameter.value.getClass());
        Assert.assertEquals(true, ((String) parameter.value).equals(""));
        value = "Hello";
        parameter.setValue(value);
        Assert.assertEquals(true, parameter.value.equals(value));
        Assert.assertEquals(true, parameter.toString().equals(value));
        jsonObject = parameter.toJson();
        replica = ContractParameter.fromJson(jsonObject);
        Assert.assertEquals(true, parameter.type.equals(replica.type));
        Assert.assertEquals(true, parameter.value.equals(replica.value));

        parameter = new ContractParameter(ContractParameterType.Array);
        Assert.assertEquals(ContractParameter.ContractParameterList.class, parameter.value.getClass());
        boolean exceptionOccured = false;
        try {
            parameter.setValue("123");
        } catch (IllegalArgumentException e) {
            exceptionOccured = true;
        }
        Assert.assertEquals(true, exceptionOccured);
        jsonObject = parameter.toJson();
        replica = ContractParameter.fromJson(jsonObject);
        Assert.assertEquals(true, parameter.type.equals(replica.type));
        Assert.assertEquals(true, parameter.value.equals(replica.value));

        parameter = new ContractParameter(ContractParameterType.Map);
        Assert.assertEquals(ContractParameter.MapList.class, parameter.value.getClass());
        exceptionOccured = false;
        try {
            parameter.setValue("123");
        } catch (IllegalArgumentException e) {
            exceptionOccured = true;
        }
        Assert.assertEquals(true, exceptionOccured);
        jsonObject = parameter.toJson();
        replica = ContractParameter.fromJson(jsonObject);
        Assert.assertEquals(true, parameter.type.equals(replica.type));
        Assert.assertEquals(true, parameter.value.equals(replica.value));
    }
}