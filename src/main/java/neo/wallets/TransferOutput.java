package neo.wallets;

import java.math.BigDecimal;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.UIntBase;
import neo.network.p2p.payloads.TransactionOutput;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: TransferOutput
 * @Package neo.wallets
 * @Description: 交易输出
 * @date Created in 11:42 2019/3/14
 */
public class TransferOutput {
    //资产ID
    public UIntBase assetId;
    //数值
    public BigDecimal value;
    //脚本哈希
    public UInt160 scriptHash;

    /**
      * @Author:doubi.liu
      * @description:是否是全局资产
      * @date:2019/4/3
    */
    public boolean isGlobalAsset(){return assetId.size() == 32;}

    /**
      * @Author:doubi.liu
      * @description:转成交易输出
      * @date:2019/4/3
    */
    public TransactionOutput toTxOutput()
    {
        if (assetId instanceof UInt256){//asset_id
            TransactionOutput result= new TransactionOutput();
            result.assetId= (UInt256) assetId;
            result.value= Fixed8.fromDecimal(value);
            result.scriptHash=scriptHash;
            return result;
        }

        throw new UnsupportedOperationException();
    }

    /**
      * @Author:doubi.liu
      * @description:构造函数
      * @date:2019/4/3
    */
    public TransferOutput(UIntBase assetId, BigDecimal value, UInt160 scriptHash) {
        this.assetId = assetId;
        this.value = value;
        this.scriptHash = scriptHash;
    }

    /**
      * @Author:doubi.liu
      * @description:获取资产id
      * @date:2019/4/3
    */
    public UIntBase getAssetId() {
        return assetId;
    }

    /**
      * @Author:doubi.liu
      * @description:获取值
      * @date:2019/4/3
    */
    public BigDecimal getValue() {
        return value;
    }

    /**
      * @Author:doubi.liu
      * @description:获取脚本哈希
      * @date:2019/4/3
    */
    public UInt160 getScriptHash() {
        return scriptHash;
    }
}