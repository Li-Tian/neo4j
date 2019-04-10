package neo.wallets;

import neo.UInt160;
import neo.csharp.Uint;
import neo.log.notr.TR;
import neo.network.p2p.payloads.Transaction;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletTransactionEventArgs
 * @Package neo.wallets
 * @Description: 钱包交易委托事件消息类
 * @date Created in 11:16 2019/3/14
 */
public class WalletTransactionEventArgs {
    //交易
    public Transaction transaction;
    //关联账户
    public UInt160[] relatedAccounts;
    //高度
    public Uint height;
    //时间
    public Uint time;

    /**
      * @Author:doubi.liu
      * @description:构造函数
      * @date:2019/4/3
    */
    public WalletTransactionEventArgs(Transaction transaction, UInt160[] relatedAccounts, Uint height, Uint time) {
        TR.enter();
        this.transaction = transaction;
        this.relatedAccounts = relatedAccounts;
        this.height = height;
        this.time = time;
        TR.exit();
    }

    /**
     * @Author:doubi.liu
     * @description:构造函数
     * @date:2019/4/3
     */
    public WalletTransactionEventArgs() {
        TR.enter();
        TR.exit();
    }
}