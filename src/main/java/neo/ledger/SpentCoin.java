package neo.ledger;

import neo.Fixed8;
import neo.csharp.Uint;
import neo.network.p2p.payloads.TransactionOutput;

public class SpentCoin {
    public TransactionOutput output;
    public Uint startHeight;
    public Uint endHeight;

    public Fixed8 value() {
        return output.value;
    }
}
