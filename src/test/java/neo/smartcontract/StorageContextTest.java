package neo.smartcontract;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt160;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: StorageContextTest
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:03 2019/3/29
 */
public class StorageContextTest {
    @Test
    public void toArray() throws Exception {
        StorageContext context=new StorageContext();
        context.scriptHash= UInt160.Zero;
        byte[] temp=new byte[20];
        Assert.assertArrayEquals(temp,context.toArray());
    }

}