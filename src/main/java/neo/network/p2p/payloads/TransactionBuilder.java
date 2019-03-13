package neo.network.p2p.payloads;

import java.util.HashMap;
import java.util.function.Supplier;

import neo.exception.FormatException;

/**
 * Transaction builder, create tx by tx's type
 */
public class TransactionBuilder {

    private static final HashMap<TransactionType, Supplier<? extends Transaction>> map = new HashMap<>(10);

    /**
     * register transaction generator
     *
     * @param type      transaction type
     * @param generator transaction generator
     * @param <T>       specific transaction
     */
    protected static <T extends Transaction> void register(TransactionType type, Supplier<T> generator) {
        map.put(type, generator);
    }

    /**
     * Create a transaction by the given type .
     *
     * @param type transaction type
     * @return Transaction
     * @throws FormatException if the transaction's generator is not registered.
     */
    public static Transaction build(TransactionType type) {
        Supplier<? extends Transaction> gen = map.get(type);
        if (gen == null) {
            throw new FormatException(String.format("TransactionType %d has not add generator method in this function!", type));
        }
        return gen.get();
    }

}
