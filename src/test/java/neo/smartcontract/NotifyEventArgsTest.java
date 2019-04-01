package neo.smartcontract;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt160;
import neo.vm.IScriptContainer;
import neo.vm.StackItem;
import neo.vm.Types.Integer;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NotifyEventArgsTest
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 16:42 2019/3/29
 */
public class NotifyEventArgsTest {
    @Test
    public void getScriptContainer() throws Exception {
        IScriptContainer scriptContainer=new IScriptContainer() {
            @Override
            public byte[] getMessage() {
                return new byte[0];
            }
        };
        UInt160 scriptHash=UInt160.Zero;
        StackItem stackItem=new Integer(0);
        NotifyEventArgs notifyEventArgs=new NotifyEventArgs(scriptContainer,scriptHash,stackItem);
        Assert.assertEquals(scriptContainer,notifyEventArgs.getScriptContainer());
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
        StackItem stackItem=new Integer(0);
        NotifyEventArgs notifyEventArgs=new NotifyEventArgs(scriptContainer,scriptHash,stackItem);
        Assert.assertEquals(scriptHash,notifyEventArgs.getScriptHash());
    }

    @Test
    public void getState() throws Exception {
        IScriptContainer scriptContainer=new IScriptContainer() {
            @Override
            public byte[] getMessage() {
                return new byte[0];
            }
        };
        UInt160 scriptHash=UInt160.Zero;
        StackItem stackItem=new Integer(0);
        NotifyEventArgs notifyEventArgs=new NotifyEventArgs(scriptContainer,scriptHash,stackItem);
        Assert.assertEquals(stackItem,notifyEventArgs.getState());
    }

}