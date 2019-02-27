package neo.function;

@FunctionalInterface
public interface FuncA2T<A, T> {
    T get(A a);
}
