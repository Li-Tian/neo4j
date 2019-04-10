package neo.wallets;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt160;
import neo.wallets.NEP6.NEP6Account;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletAccountTest
 * @Package neo.wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 18:00 2019/4/3
 */
public class WalletAccountTest {
    @Test
    public void getAddress() throws Exception {
        WalletAccount account=new NEP6Account(null,UInt160.Zero);
        account.scriptHash=UInt160.Zero;
        Assert.assertEquals("AFmseVrdL9f9oyCzZefL9tG6UbvhPbdYzM",account.getAddress());
    }

    @Test
    public void watchOnly() throws Exception {
        WalletAccount account=new NEP6Account(null,UInt160.Zero);
        Assert.assertEquals(true,account.watchOnly());
    }


}