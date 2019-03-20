package neo.Wallets;

import neo.ledger.CoinState;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.TransactionOutput;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: Coin
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 13:47 2019/3/14
 */
public class Coin {

    public CoinReference reference;
    public TransactionOutput output;
    public CoinState state;

    private String _address = null;

    public String getAddress() {
        if (_address == null) {
            _address = output.scriptHash.toAddress();
        }
        return _address;
    }

    public boolean equals(Coin other) {
        if (this==other) return true;
        if (other==null)return false;
        return reference.equals(other.reference);
    }

    @Override
    public boolean equals(Object obj) {
        return equals((Coin)obj);
    }

    @Override
    public int hashCode() {
        return reference.hashCode();
    }
}