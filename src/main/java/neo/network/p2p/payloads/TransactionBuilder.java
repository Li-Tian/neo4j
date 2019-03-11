package neo.network.p2p.payloads;

import neo.exception.FormatException;

public class TransactionBuilder {

    public static Transaction build(TransactionType type) {
        if (type == TransactionType.MinerTransaction) return new MinerTransaction();
        if (type == TransactionType.ClaimTransaction) return new ClaimTransaction();
        if (type == TransactionType.ContractTransaction) return new ContractTransaction();
        if (type == TransactionType.InvocationTransaction) return new InvocationTransaction();
        if (type == TransactionType.StateTransaction) return new StateTransaction();
        if (type == TransactionType.IssueTransaction) return new IssueTransaction();
        if (type == TransactionType.EnrollmentTransaction) return new EnrollmentTransaction();
        if (type == TransactionType.PublishTransaction) return new PublishTransaction();
        if (type == TransactionType.RegisterTransaction) return new RegisterTransaction();
        throw new FormatException(String.format("TransactionType %d is not exist!", type));
    }

}
