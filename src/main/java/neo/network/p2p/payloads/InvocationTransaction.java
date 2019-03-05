package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import java.awt.*;
import java.math.BigDecimal;
import java.util.Collection;

import neo.Fixed8;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.persistence.Snapshot;

/**
 * 执行交易，执行脚本或智能合约。包括部署和执行智能合约
 */
public class InvocationTransaction extends Transaction {

    /**
     * 脚本
     */
    public byte[] script;

    /**
     * GAS消耗
     */
    public Fixed8 gas;

    /**
     * 构造函数：创建执行交易
     */
    public InvocationTransaction() {
        super(TransactionType.InvocationTransaction);
    }

    /**
     * 存储大小
     */
    @Override
    public int size() {
        return super.size() + BitConverter.getVarSize(script);
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>Script: 待执行脚本</li>
     * <li>Gas: 若交易版本号大于等于1，则序列化Gas字段</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        writer.writeVarBytes(script);
        if (version >= 1) {
            writer.writeSerializable(gas);
        }
    }

    /**
     * 反序列化数据，除了data数据外。
     *
     * @param reader 二进制读取流
     * @return Version为0时，不指定GAS。默认为0<br/> Version为1时，需要指定GAS<br/>
     * @throws FormatException <ul>
     *                         <li>1. 若交易版本大于1，则抛出该异常</li>
     *                         <li>2. 反序列化的脚本数组长度为0</li>
     *                         <li>3. 指定的执行智能合约的GAS额度小于0.</li>
     *                         </ul>
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version > 1) throw new FormatException();
        script = reader.readVarBytes(65536);
        if (script.length == 0) {
            throw new FormatException();
        }
        if (version >= 1) {
            gas = reader.readSerializable(Fixed8::new);
            if (gas.compareTo(Fixed8.ZERO) < 0) {
                throw new FormatException();
            }
        } else {
            gas = Fixed8.ZERO;
        }
    }

    /**
     * 获取GAS消耗
     *
     * @param consumed 实际消耗的gas
     * @return <ul>
     * <li>GAS消息等于实际消耗的GAS减去免费的10GAS；</li>
     * <li>若gas消耗小于等于0，则返回0；；</li>
     * <li>最后对gas消耗取上整数；</li>
     * </ul>
     */
    public static Fixed8 getGas(Fixed8 consumed) {
        // c3 code Fixed8 gas = consumed - Fixed8.FromDecimal(10);
        Fixed8 gas = Fixed8.subtract(consumed, Fixed8.fromDecimal(BigDecimal.valueOf(10)));
        if (gas.compareTo(Fixed8.ZERO) <= 0) return Fixed8.ZERO;
        return gas.ceiling();
    }


    /**
     * 校验交易
     *
     * @param snapshot 数据库快照
     * @param mempool  内存池交易
     * @return <ul>
     * <li>1. 若消耗的GAS不能整除10^8, 则返回false.（即，GAS必须是整数单位形式的Fixed8，即不能包含有小数的GAS） </li>
     * <li>2. 进行交易的基本验证，若验证失败，则返回false  </li>
     * </ul>
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        if (gas.getData() % 100000000 != 0) {
            return false;
        }
        return super.verify(snapshot, mempool);
    }

    /**
     * 转成json对象
     *
     * @return json对象
     */
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = super.toJson();
        jsonObject.addProperty("script", BitConverter.toHexString(script));
        jsonObject.addProperty("gas", gas.toString());
        return jsonObject;
    }
}
