package neo.network.p2p.payloads;

import neo.Fixed8;
import neo.UInt256;

public class TransactionResult {
    public UInt256 assetId;
    public Fixed8 amount;

    public TransactionResult() {
    }

    public TransactionResult(UInt256 assetId, Fixed8 amount) {
        this.assetId = assetId;
        this.amount = amount;
    }
}
