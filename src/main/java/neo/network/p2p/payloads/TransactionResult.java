package neo.network.p2p.payloads;

import neo.Fixed8;
import neo.UInt256;

/**
 * A class descript transaction amount change
 */
public class TransactionResult {

    /**
     * The asset id
     */
    public UInt256 assetId;

    /**
     * amount change = inputs.Asset - outputs.Asset
     */
    public Fixed8 amount;
}
