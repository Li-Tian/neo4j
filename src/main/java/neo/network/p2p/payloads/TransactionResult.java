package neo.network.p2p.payloads;

import neo.Fixed8;
import neo.UInt256;

/**
 * 封装的交易金额变化类
 */
public class TransactionResult {

    /**
     * 资产Id
     */
    public UInt256 assetId;

    /**
     * 金额变化 = inputs.Asset - outputs.Asset
     */
    public Fixed8 amount;
}
