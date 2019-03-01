package neo.network.p2p.payloads;

@Deprecated
public class PublishTransaction extends Transaction {

    public PublishTransaction() {
        super(TransactionType.PublishTransaction);
    }

//    public byte[] Script;
//    public ContractParameterType[] ParameterList;
//    public ContractParameterType ReturnType;
//    public bool NeedStorage;
//    public string Name;
}
