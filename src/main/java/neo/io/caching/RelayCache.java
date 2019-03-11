package neo.io.caching;

import neo.UInt256;
import neo.log.notr.TR;
import neo.network.p2p.payloads.IInventory;

public class RelayCache extends FIFOCache<UInt256, IInventory> {

    public RelayCache(int maxCapacity) {
        super(maxCapacity);
    }

    @Override
    protected UInt256 getKeyForItem(IInventory item) {
        TR.enter();
        return TR.exit(item.hash());
    }
}
