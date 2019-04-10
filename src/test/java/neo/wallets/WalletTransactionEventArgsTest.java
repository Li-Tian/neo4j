package neo.wallets;

import org.junit.Test;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletTransactionEventArgsTest
 * @Package neo.wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:33 2019/4/9
 */
public class WalletTransactionEventArgsTest {
    @Test
    public void constructor() throws Exception {
        WalletTransactionEventArgs args1=new WalletTransactionEventArgs();
        WalletTransactionEventArgs args2=new WalletTransactionEventArgs(null,null,null,null);
    }

}