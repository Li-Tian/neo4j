package neo.smartcontract;

import java.util.HashMap;
import java.util.Map;

import neo.log.notr.TR;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: TriggerType
 * @Package neo.smartcontract
 * @Description: 触发器类型
 * @date Created in 9:57 2019/3/12
 */
public enum TriggerType {
    /// <summary>
    /// 验证触发器的目的在于将该合约作为验证函数（verification function）进行调用，
    /// 验证函数可以接受多个参数（parameters），并且应返回有效的布尔值，标志着交易或区块的有效性。
    /// 如果智能合约被验证触发器触发了，则调用智能合约入口点:
    ///     main(...);
    /// 智能合约的入口点必须能够处理这种类型的调用。
    /// </summary>
    Verification(0x00),
    // <summary>
    // the verificationr trigger indicates that the contract is being invoked as a verification function because it is specified as a target of an output of the transaction.
    // the verification function accepts no parameter, and should return a boolean value that indicates the validity of the transaction.
    // the entry point of the contract will be invoked if the contract is triggered by verificationr:
    //     main("receiving", new object[0]);
    // the receiving function should have the following signature:
    //     public bool receiving()
    // the receiving function will be invoked automatically when a contract is receiving assets from a transfer.
    // </summary>
    /// <summary>
    /// 验证触发器R的目的在于将该合约作为验证函数进行调用，因为它被指定为交易输出的目标。
    /// 验证函数不接受参数，并且应返回有效的布尔值，标志着交易的有效性。
    /// 如果智能合约被验证触发器R触发了，则调用智能合约入口点:
    ///     main("receiving", new object[0]);
    /// receiving函数应具有以下编程接口:
    ///     public bool receiving()
    /// 当智能合约从转账中收到一笔资产时，receiving函数将会自动被调用。
    /// </summary>
    VerificationR(0x01),
    // <summary>
    // The application trigger indicates that the contract is being invoked as an application function.
    // The application function can accept multiple parameters, change the states of the blockchain, and return any type of value.
    // The contract can have any form of entry point, but we recommend that all contracts should have the following entry point:
    //     public byte[] main(string operation, params object[] args)
    // The functions can be invoked by creating an InvocationTransaction.
    // </summary>
    /// <summary>
    /// 应用触发器的目的在于将该合约作为应用函数（verification function）进行调用，
    /// 应用函数可以接受多个参数（parameters），对区块链的状态进行更改，并返回任意类型的返回值。
    /// 理论上智能合约可以有任意的入口点，但我们推荐智能合约使用 main 函数作为入口点以方便调用:
    ///    public byte[] main(string operation, params object[] args)
    /// 当创建一个InvocationTransaction时这个函数可以被调用。
    /// </summary>
    Application(0x10),
    // <summary>
    // The ApplicationR trigger indicates that the default function received of the contract is being invoked because it is specified as a target of an output of the transaction.
    // The received function accepts no parameter, changes the states of the blockchain, and returns any type of value.
    // The entry point of the contract will be invoked if the contract is triggered by ApplicationR:
    //     main("received", new object[0]);
    // The received function should have the following signature:
    //     public byte[] received()
    // The received function will be invoked automatically when a contract is receiving assets from a transfer.
    // </summary>
    /// <summary>
    /// 应用触发器R指明了当智能合约被调用时的默认函数received，因为它被指定为交易输出的目标。
    /// received函数不接受参数，对区块链的状态进行更改，并返回任意类型的返回值。
    /// 如果智能合约被应用触发器R触发了，则调用智能合约入口点:
    ///     main("received", new object[0]);
    /// received函数应具有以下编程接口:
    ///     public byte[] received()
    /// 当智能合约从转账中收到一笔资产时，receiving函数将会自动被调用。
    /// </summary>
    ApplicationR(0x11);


    //字节数据和OpCode的映射关系
    private static final Map<Byte, TriggerType> byteToTypeMap = new HashMap<Byte, TriggerType>();

    static {
        TR.info("TriggerType枚举器初始化");
        for (TriggerType type : TriggerType.values()) {
            byteToTypeMap.put(type.getTriggerType(), type);
        }
    }
    private byte triggerType;

    /**
     * @Author:doubi.liu
     * @description:构造函数
     * @param value 字节数据
     * @date:2019/3/11
     */
    TriggerType(int value) {
        TR.enter();
        triggerType = (byte) value;
        TR.exit();
    }

    /**
     * @Author:doubi.liu
     * @description:构造函数
     * @param value opcode
     * @date:2019/3/11
     */
    TriggerType(TriggerType value) {
        TR.enter();
        this.triggerType = value.getTriggerType();
        TR.exit();
    }

    /**
     * @Author:doubi.liu
     * @description:获取OpCode的字节数据
     * @param
     * @date:2019/3/11
     */
    public byte getTriggerType() {
        TR.enter();
        return TR.exit(triggerType);
    }

    /**
     * @Author:doubi.liu
     * @description:通过字节数据获取TriggerType
     * @param i 字节数据
     * @date:2019/3/11
     */
    public static TriggerType fromByte(byte i) {
        TR.enter();
        TriggerType type = byteToTypeMap.get(i);
        if (type == null){
            throw TR.exit(new UnsupportedOperationException());
        }
        return TR.exit(type);
    }
}