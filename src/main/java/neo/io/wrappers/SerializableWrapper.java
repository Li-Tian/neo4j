package neo.io.wrappers;

import neo.io.ISerializable;

public abstract class SerializableWrapper<T> implements ISerializable {

    protected T value;

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }
        if (value == null || obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            final SerializableWrapper other = (SerializableWrapper) obj;
            return value.equals(other.value);
        } else {
            return value.equals(obj);
        }
    }
}
