package neo.ledger;

import neo.log.notr.TR;
import neo.network.p2p.payloads.Header;
import neo.persistence.Snapshot;

public class MyHeader extends Header {
    @Override
    public boolean verify(Snapshot snapshot) {
        TR.enter();
        return TR.exit(true);
    }
}
