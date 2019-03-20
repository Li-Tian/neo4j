package neo.Wallets;

import neo.UInt160;
import neo.csharp.Uint;
import neo.network.p2p.payloads.Transaction;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletTransactionEventArgs
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 11:16 2019/3/14
 */
public class WalletTransactionEventArgs {
    public Transaction transaction;
    public UInt160[] relatedAccounts;
    public Uint height;
    public Uint time;

    public WalletTransactionEventArgs(Transaction transaction, UInt160[] relatedAccounts, Uint height, Uint time) {
        this.transaction = transaction;
        this.relatedAccounts = relatedAccounts;
        this.height = height;
        this.time = time;
    }
}