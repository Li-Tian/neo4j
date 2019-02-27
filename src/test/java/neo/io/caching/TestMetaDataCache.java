package neo.io.caching;

import neo.function.FuncVoid2T;
import neo.io.ICloneable;
import neo.io.ISerializable;

public class TestMetaDataCache <T extends ICloneable<T> & ISerializable> extends MetaDataCache<T>  {

    public TestMetaDataCache() {
        super(null);
    }

    public TestMetaDataCache(FuncVoid2T factory){
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
