package neo.ledger;

import neo.log.notr.TR;
import neo.network.p2p.payloads.Block;
import neo.persistence.Snapshot;

public class MyBlock extends Block {
    @Override
    public boolean verify(Snapshot snapshot) {
        TR.enter();
        return TR.exit(true);
    }
}
