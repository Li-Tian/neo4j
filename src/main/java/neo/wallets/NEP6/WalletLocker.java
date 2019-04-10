package neo.wallets.NEP6;

import neo.csharp.common.IDisposable;
import neo.log.notr.TR;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletLocker
 * @Package neo.wallets.NEP6
 * @Description: 钱包锁
 * @date Created in 15:04 2019/3/14
 */
public class WalletLocker implements IDisposable{
    //NEP6钱包对象
    private NEP6Wallet wallet;

    /**
      * @Author:doubi.liu
      * @description:构造函数
      * @param wallet NEP6钱包对象
      * @date:2019/4/4
    */
    public WalletLocker(NEP6Wallet wallet)
    {
        TR.enter();
        this.wallet = wallet;
        TR.exit();
    }

    /**
      * @Author:doubi.liu
      * @description:资源释放方法
      * @date:2019/4/4
    */
    @Override
    public void dispose()
    {
        TR.enter();
        wallet.lock();
        TR.exit();
    }
}