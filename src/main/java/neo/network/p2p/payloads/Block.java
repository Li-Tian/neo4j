package neo.network.p2p.payloads;

public class Block extends BlockBase implements IInventory {

    @Override
    public InventoryType inventoryType() {
        return InventoryType.Block;
    }

}
