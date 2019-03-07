package neo.persistence;

public abstract class Store extends AbstractPersistence {

    /**
     * 获取快照
     */
    public abstract Snapshot getSnapshot();
}
