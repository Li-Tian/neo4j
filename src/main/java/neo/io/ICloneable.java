package neo.io;

public interface ICloneable<T>{
    /**
     * @note 克隆方法，这里使用 copy, 不使用 clone方法，主要目的在于规避 Java对泛型安全检查时，对 Object.clone方法不通过缺陷。
     */
    T copy();

    void fromReplica(T replica);
}
