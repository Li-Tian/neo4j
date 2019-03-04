package neo.network.p2p.payloads;

public class TransactionBuilder {

    public static Transaction build(TransactionType type) {
        // TODO 根据不同交易类型，创建不同对象
        if (type == TransactionType.MinerTransaction) return new MinerTransaction();
        return null;
    }

}
