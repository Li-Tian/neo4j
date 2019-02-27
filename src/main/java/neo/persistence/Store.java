package neo.persistence;

public abstract class Store implements IPersistence {

    public abstract Snapshot GetSnapshot();
}
