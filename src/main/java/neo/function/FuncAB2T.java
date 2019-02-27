package neo.function;

@FunctionalInterface
public interface FuncAB2T<A, B, T> {
    T get(A a, B b);
}
