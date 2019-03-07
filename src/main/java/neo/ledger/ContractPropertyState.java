package neo.ledger;


import java.util.Objects;

/**
 * 智能合约属性状态
 */
public class ContractPropertyState {

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


    private byte value;

    /**
     * 构造方法
     *
     * @param value 属性值
     */
    public ContractPropertyState(byte value) {
        this.value = value;
    }

    /**
     * 属性值
     */
    public byte value() {
        return value;
    }

    /**
     * 是否包含某属性
     *
     * @param flag 属性
     */
    public boolean hasFlag(ContractPropertyState flag) {
        return (this.value & flag.value) != (byte) 0x00;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractPropertyState that = (ContractPropertyState) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
