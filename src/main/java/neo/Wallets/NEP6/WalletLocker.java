package neo.Wallets.NEP6;

import neo.csharp.common.IDisposable;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletLocker
 * @Package neo.Wallets.NEP6
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 15:04 2019/3/14
 */
public class WalletLocker implements IDisposable{
    private NEP6Wallet wallet;

    public WalletLocker(NEP6Wallet wallet)
    {
        this.wallet = wallet;
    }

    @Override
    public void dispose()
    {
        wallet.Lock();
    }
}