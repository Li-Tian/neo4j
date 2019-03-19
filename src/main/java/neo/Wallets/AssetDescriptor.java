package neo.Wallets;

import neo.UInt160;
import neo.UInt256;
import neo.UIntBase;
import neo.VM.Helper;
import neo.ledger.AssetState;
import neo.ledger.Blockchain;
import neo.smartcontract.ApplicationEngine;
import neo.vm.ScriptBuilder;
import neo.vm.VMState;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: AssetDescriptor
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 10:44 2019/3/14
 */
public class AssetDescriptor {
    public UIntBase assetId;
    public String assetName;
    public byte decimals;

    public AssetDescriptor(UIntBase asset_id)
    {
        if (asset_id instanceof UInt160)
        {
            byte[] script;
            ScriptBuilder sb = new ScriptBuilder();//asset_id_160
            Helper.emitAppCall(sb,(UInt160)asset_id,"decimals") ;
            Helper.emitAppCall(sb,(UInt160)asset_id,"name") ;
            script = sb.toArray();
            ApplicationEngine engine = ApplicationEngine.run(script);
            if (engine.state.hasFlag(VMState.FAULT)) throw new IllegalArgumentException();
            this.assetId = asset_id;
            this.assetName = engine.resultStack.pop().getString();
            this.decimals = engine.resultStack.pop().getBigInteger().byteValue();
        }
            else
        {
            AssetState state = Blockchain.singleton().getStore().getAssets().get((UInt256)asset_id);
            this.assetId = state.assetId;
            this.assetName = state.getName();
            this.decimals = state.precision;
        }
    }

    @Override
    public String toString()
    {
        return assetName;
    }
}