package neo.function;

@FunctionalInterface
public interface FuncAB2T<A, B, T> {
    T gen(A a, B b);
}
