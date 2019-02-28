package neo.io.caching;

import java.util.function.Supplier;

import neo.io.ICloneable;
import neo.csharp.io.ISerializable;

public class TestMetaDataCache <T extends ICloneable<T> & ISerializable> extends MetaDataCache<T>  {

    public TestMetaDataCache() {
        super(null);
    }

    public TestMetaDataCache(Supplier factory){
        super(factory);
    }

    @Override
    protected void addInternal(T item) {

    }

    @Override
    protected T tryGetInternal() {
        return null;
    }

    @Override
    protected void updateInternal(T item) {

    }
}
