package neo.io.wrappers;

import neo.csharp.io.ISerializable;
import neo.log.notr.TR;

/**
 * A encapsulized class for serializable objects, implementing extended functions such as Equal.
 * This is a abstract class
 */
public abstract class SerializableWrapper<T> implements ISerializable {

    /**
     * encapsulized object
     */
    protected T value;

    /**
     * Whether the value is equal to another object
     *
     * @param obj another object
     * @return Compare results, equality returns true, otherwise returns false
     */
    @Override
    public boolean equals(Object obj) {
        TR.enter();

        if (this == obj) {
            return TR.exit(true);
        }
        if (value == null || obj == null) {
            return TR.exit(false);
        }

        if (obj instanceof SerializableWrapper) {
            final SerializableWrapper other = (SerializableWrapper) obj;
            return TR.exit(value.equals(other.value));
        } else {
            return TR.exit(value.equals(obj));
        }
    }
}
