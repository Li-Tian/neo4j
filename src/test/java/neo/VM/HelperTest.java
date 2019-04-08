package neo.VM;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt160;
import neo.Wallets.NEP6.ScryptParameters;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.smartcontract.ContractParameter;
import neo.smartcontract.ContractParameterType;
import neo.vm.OpCode;
import neo.vm.ScriptBuilder;
import neo.vm.Types.Boolean;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: HelperTest
 * @Package neo.VM
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 10:24 2019/4/4
 */
public class HelperTest {
    @Test
    public void emit() throws Exception {
        ScriptBuilder builder=new ScriptBuilder();
        Helper.emit(builder, new OpCode[]{OpCode.PUSH0,OpCode.PUSH0});
        Assert.assertArrayEquals(new byte[]{0x00,0x00},builder.toArray());
    }

    @Test
    public void emitAppCall() throws Exception {
        ScriptBuilder builder=new ScriptBuilder();
        Helper.emitAppCall(builder, UInt160.Zero);
        Assert.assertArrayEquals(new byte[]{0x67,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00},builder.toArray());
    }

    @Test
    public void emitAppCall1() throws Exception {
        ScriptBuilder builder=new ScriptBuilder();
        Helper.emitAppCall(builder, UInt160.Zero,true);
        Assert.assertArrayEquals(new byte[]{105,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00},builder.toArray());
    }

    @Test
    public void emitAppCall2() throws Exception {
        ScriptBuilder builder=new ScriptBuilder();
        ContractParameter parameter=new ContractParameter(ContractParameterType.Boolean);
        Helper.emitAppCall(builder, UInt160.Zero,new ContractParameter[]{parameter});
        Assert.assertArrayEquals(new byte[]{0x00,103,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00},builder.toArray());
    }

    @Test
    public void emitAppCall3() throws Exception {
        ScriptBuilder builder=new ScriptBuilder();
        Helper.emitAppCall(builder, UInt160.Zero,"Hello");
        Assert.assertArrayEquals(new
                byte[]{0x00,0x05,0x48,0x65,0x6c,0x6c,
                0x6f,0x67,0x00,0x00,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,0x00,0x00},builder
                .toArray());
    }

    @Test
    public void emitAppCall4() throws Exception {
        ScriptBuilder builder=new ScriptBuilder();
        ContractParameter parameter=new ContractParameter(ContractParameterType.Boolean);
        Helper.emitAppCall(builder, UInt160.Zero,"Hello",new ContractParameter[]{parameter});
        Assert.assertArrayEquals(new
                byte[]{0x00,0x51, (byte) 0xc1,0x05,0x48,
                0x65,0x6c,0x6c,0x6f,0x67,0x00,0x00,
                0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00},builder
                .toArray());
    }

    @Test
    public void emitAppCall5() throws Exception {
        ScriptBuilder builder=new ScriptBuilder();
        Helper.emitAppCall(builder, UInt160.Zero,"Hello",false);
        Assert.assertArrayEquals(new
                byte[]{0x00,0x51, (byte) 0xc1,0x05,0x48,
                0x65,0x6c,0x6c,0x6f,0x67,0x00,0x00,
                0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00},builder
                .toArray());
    }

    @Test
    public void emitPush() throws Exception {
        ScriptBuilder builder=new ScriptBuilder();
        Helper.emitPush(builder, new ISerializable() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public void serialize(BinaryWriter binaryWriter) {
                binaryWriter.writeByte((byte) 0x22);
            }

            @Override
            public void deserialize(BinaryReader binaryReader) {

            }
        });

        Assert.assertArrayEquals(new
                byte[]{0x01,0x22},builder
                .toArray());
    }

    @Test
    public void emitPush1() throws Exception {
        ScriptBuilder builder=new ScriptBuilder();
        ContractParameter parameter=new ContractParameter(ContractParameterType.Boolean);
        Helper.emitPush(builder,parameter);

        Assert.assertArrayEquals(new byte[]{0x00},builder.toArray());
    }

    @Test
    public void emitPush2() throws Exception {
        ScriptBuilder builder=new ScriptBuilder();
        Helper.emitPush(builder,false);

        Assert.assertArrayEquals(new byte[]{0x00},builder.toArray());
    }

    @Test
    public void emitSysCall() throws Exception {
        ScriptBuilder builder=new ScriptBuilder();
        Helper.emitSysCall(builder,"Hello",new Object[]{false});

        Assert.assertArrayEquals(new byte[]{0x00,0x68,0x05,0x48,0x65,0x6c,0x6c,0x6f},builder
                .toArray());
    }

    @Test
    public void toParameter() throws Exception {
        ContractParameter parameter=Helper.toParameter(new Boolean(true));
        Assert.assertEquals(ContractParameterType.Boolean,parameter.type);
        Assert.assertEquals(true,parameter.value);
    }

}