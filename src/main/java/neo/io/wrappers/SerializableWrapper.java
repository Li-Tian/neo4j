package neo.io.wrappers;

import neo.io.ISerializable;
import neo.log.tr.TR;

public abstract class SerializableWrapper<T> implements ISerializable {

    protected T value;

    @Override
    public boolean equals(Object obj) {
        TR.enter();

        if (this == obj) {
            return TR.exit(true);
        }
        if (value == null || obj == null) {
            return TR.exit(false);
        }

        if (getClass() != obj.getClass()) {
            final SerializableWrapper other = (SerializableWrapper) obj;
            return TR.exit(value.equals(other.value));
        } else {
            return TR.exit(value.equals(obj));
        }
    }
}
