package neo.ledger;

import neo.log.notr.TR;
import neo.network.p2p.payloads.ConsensusPayload;
import neo.persistence.Snapshot;

public class MyConsensusPayload extends ConsensusPayload {
    @Override
    public boolean verify(Snapshot snapshot) {
        TR.enter();
        return TR.exit(true);
    }
}
