package neo.ledger;


import neo.common.ByteFlag;

/**
 * 智能合约属性状态
 */
public class ContractPropertyState extends ByteFlag {

    /**
     * 合约不包含属性
     */
    public final static ContractPropertyState NoProperty = new ContractPropertyState((byte) 0);

    /**
     * 包含存储区
     */
    public final static ContractPropertyState HasStorage = new ContractPropertyState((byte) (1 << 0));

    /**
     * 动态调用
     */
    public final static ContractPropertyState HasDynamicInvoke = new ContractPropertyState((byte) (1 << 1));

    /**
     * 可支付(保留功能)
     */
    public final static ContractPropertyState Payable = new ContractPropertyState((byte) (1 << 2));


    /**
     * 构造方法
     *
     * @param value 属性值
     */
    public ContractPropertyState(byte value) {
        super(value);
    }


    /**
     * 逻辑或操作
     */
    public ContractPropertyState or(ContractPropertyState state) {
        return new ContractPropertyState((byte) (this.value | state.value));
    }

}
