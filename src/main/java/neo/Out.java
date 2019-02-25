package neo;

/**
 *  注意!! 此类是模拟 C# 的 out参数， 可以在调用函数时，将返回参数放在形参列表中。
 * @param <T>
 */
public class Out<T> {
    private T t;

    public void set(T t) {
        this.t = t;
    }

    public T get() {
        return t;
    }

}
