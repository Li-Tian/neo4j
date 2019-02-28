package neo.network.p2p.payloads;

import neo.UInt256;

public interface IInventory extends IVerifiable {

    UInt256 hash();

    InventoryType inventoryType();
}
