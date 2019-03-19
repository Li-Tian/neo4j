package neo.Wallets;

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
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 11:42 2019/3/14
 */
public class TransferOutput {
    public UIntBase assetId;
    public BigDecimal value;
    public UInt160 scriptHash;

    public boolean isGlobalAsset(){return assetId.size() == 32;}

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

    public TransferOutput(UIntBase assetId, BigDecimal value, UInt160 scriptHash) {
        this.assetId = assetId;
        this.value = value;
        this.scriptHash = scriptHash;
    }

    public UIntBase getAssetId() {
        return assetId;
    }

    public BigDecimal getValue() {
        return value;
    }

    public UInt160 getScriptHash() {
        return scriptHash;
    }
}