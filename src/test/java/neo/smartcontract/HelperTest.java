package neo.smartcontract;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt160;
import neo.csharp.Uint;
import neo.vm.OpCode;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: HelperTest
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:53 2019/3/29
 */
public class HelperTest {
    @Test
    public void isMultiSigContract() throws Exception {

    }

    @Test
    public void isSignatureContract() throws Exception {
        byte[] temp=new byte[35];
        temp[0]= 33;
        temp[34]= OpCode.CHECKSIG.getCode();
        Assert.assertEquals(true,Helper.isSignatureContract(temp));
    }

    @Test
    public void isStandardContract() throws Exception {

    }

    @Test
    public void toInteropMethodHash() throws Exception {
        Uint uInt=Helper.toInteropMethodHash("I love code");
        Assert.assertEquals("4207057783",uInt.toString());

    }

    @Test
    public void toScriptHash() throws Exception {
        UInt160 uInt160=Helper.toScriptHash("I love code".getBytes("utf-8"));
        Assert.assertEquals("0xb94b465b050b12392d3c4f587a2ab3da9f6776de",uInt160.toString());
    }

    @Test
    public void verifyWitnesses() throws Exception {

    }

}