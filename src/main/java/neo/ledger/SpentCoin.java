package neo.ledger;

import neo.Fixed8;
import neo.csharp.Uint;
import neo.network.p2p.payloads.TransactionOutput;

/**
 * 已花费的output状态
 */
public class SpentCoin {

    /**
     * 已经花费的output
     */
    public TransactionOutput output;

    /**
     * output所在区块高度
     */
    public Uint startHeight;

    /**
     * output被花费的block高度
     */
    public Uint endHeight;


    /**
     * 花费量
     */
    public Fixed8 value() {
        return output.value;
    }
}
