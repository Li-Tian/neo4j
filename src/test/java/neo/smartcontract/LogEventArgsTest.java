package neo.smartcontract;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt160;
import neo.vm.IScriptContainer;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: LogEventArgsTest
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 16:23 2019/3/29
 */
public class LogEventArgsTest {
    @Test
    public void getScriptContainer() throws Exception {
        IScriptContainer scriptContainer=new IScriptContainer() {
            @Override
            public byte[] getMessage() {
                return new byte[0];
            }
        };
        UInt160 scriptHash=UInt160.Zero;
        String message="0";
        LogEventArgs logEventArgs=new LogEventArgs(scriptContainer,scriptHash,message);
        Assert.assertEquals(scriptContainer,logEventArgs.getScriptContainer());
    }

    @Test
    public void getScriptHash() throws Exception {
        IScriptContainer scriptContainer=new IScriptContainer() {
            @Override
            public byte[] getMessage() {
                return new byte[0];
            }
        };
        UInt160 scriptHash=UInt160.Zero;
        String message="0";
        LogEventArgs logEventArgs=new LogEventArgs(scriptContainer,scriptHash,message);
        Assert.assertEquals(scriptHash,logEventArgs.getScriptHash());
    }

    @Test
    public void getMessage() throws Exception {
        IScriptContainer scriptContainer=new IScriptContainer() {
            @Override
            public byte[] getMessage() {
                return new byte[0];
            }
        };
        UInt160 scriptHash=UInt160.Zero;
        String message="0";
        LogEventArgs logEventArgs=new LogEventArgs(scriptContainer,scriptHash,message);
        Assert.assertEquals(message,logEventArgs.getMessage());
    }

}