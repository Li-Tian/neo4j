package neo.persistence;

/**
 * Abstract persistent storage
 */
public abstract class Store extends AbstractPersistence {

    /**
     * Get snapshot
     */
    public abstract Snapshot getSnapshot();
}
