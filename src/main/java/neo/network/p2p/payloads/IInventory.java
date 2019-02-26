package neo.network.p2p.payloads;

import neo.UInt256;

public interface IInventory {

    UInt256 hash();

    InventoryType inventoryType();
}
