package neo.io;

public interface ICloneable<T> {
    T clone();

    void fromReplica(T replica);
}
