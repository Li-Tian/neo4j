package neo.network.p2p.payloads;

import neo.UInt256;
import neo.persistence.Snapshot;

/**
 * Inventory接口
 */
public interface IInventory extends IVerifiable {

    /**
     * Inventory哈希值
     */
    UInt256 hash();

    /**
     * Inventory类型
     */
    InventoryType inventoryType();

    /**
     * 校验函数，根据快照进行校验
     *
     * @param snapshot 快照
     * @return 校验成功返回true，否则返回false
     */
    boolean verify(Snapshot snapshot);
}
