package neo.wallets.SQLite;

import org.junit.Assert;
import org.junit.Test;

import neo.Utils;
import neo.smartcontract.ContractParameterType;

public class VerificationContractTest {

    @Test
    public void size() {
        VerificationContract verificationContract = new VerificationContract() {{
            script = "this is test script".getBytes();
            parameterList = new ContractParameterType[]{
                    ContractParameterType.Signature
            };
        }};
        Assert.assertEquals(42, verificationContract.size());
    }

    @Test
    public void deserialize() {
        VerificationContract verificationContract = new VerificationContract() {{
            script = "this is test script".getBytes();
            parameterList = new ContractParameterType[]{
                    ContractParameterType.Signature
            };
        }};

        VerificationContract other = Utils.copyFromSerialize(verificationContract, VerificationContract::new);

        Assert.assertEquals(verificationContract, other);
        Assert.assertEquals(verificationContract.hashCode(), other.hashCode());
        Assert.assertTrue(verificationContract.equals(other));
    }

}