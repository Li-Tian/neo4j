package neo.network.p2p.payloads;

import neo.UInt256;
import neo.persistence.Snapshot;

/**
 * The interface of inventory
 */
public interface IInventory extends IVerifiable {

    /**
     *  The hash value of inventory
     */
    UInt256 hash();

    /**
     * The inventory type
     */
    InventoryType inventoryType();

    /**
     * The verify function, which verify according to the snapshop
     *
     * @param snapshot The snapshot of blockchain
     * @return If verify successfully return true otherwise return false
     */
    boolean verify(Snapshot snapshot);
}
