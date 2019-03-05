package neo.network.p2p.payloads;

import neo.csharp.io.BinaryReader;
import neo.exception.FormatException;

/**
 * 最普通交易（不是发布智能合约）
 */
public class ContractTransaction extends Transaction {

    /**
     * 构造函数
     */
    public ContractTransaction() {
        super(TransactionType.ContractTransaction);
    }

    /**
     * 反序列数据，未读取任何数据。只验证交易版本号为0
     *
     * @param reader 二进制输入流
     * @throws FormatException 若交易版本号不是0，抛出该异常
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) throw new FormatException();
    }
}
