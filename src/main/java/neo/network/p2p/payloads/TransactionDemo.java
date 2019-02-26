package neo.network.p2p.payloads;

import neo.UInt256;

public  class TransactionDemo implements IInventory {

    private UInt256 value;

    public TransactionDemo(UInt256 value) {
        this.value = value;
    }

    @Override
    public UInt256 hash() {
        return value;
    }

    @Override
    public InventoryType inventoryType() {
        return InventoryType.TR;
    }
}
