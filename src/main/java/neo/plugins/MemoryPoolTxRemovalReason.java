package neo.plugins;

public enum MemoryPoolTxRemovalReason {
    /**
     * The transaction was ejected since it was the lowest priority transaction and the MemoryPool capacity was exceeded.
     */
    CapacityExceeded,
    /**
     * The transaction was ejected due to failing re-validation after a block was persisted.
     */
    NoLongerValid,
}