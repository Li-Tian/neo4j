package neo.wallets.SQLite;

import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.smartcontract.ContractParameterType;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: VerificationContract
 * @Package neo.wallets.SQLite
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:46 2019/3/14
 */
public class VerificationContract extends neo.smartcontract.Contract implements ISerializable {

    @Override
    public int size(){
       return 20 + BitConverter.getVarSize(parameterList) + BitConverter.getVarSize(script);
    }

    @Override
    public void deserialize(BinaryReader reader)
    {
        reader.readSerializable(UInt160::new);
        //LINQ START
        //parameterList = reader.readVarBytes().Select(p => (ContractParameterType)p).ToArray();
        byte[] temparray=reader.readVarBytes();
        ContractParameterType[] tempparameterList=new ContractParameterType[temparray.length];
        for (int i=0;i<temparray.length;i++){
            tempparameterList[i]=ContractParameterType.parse(temparray[i]);
        }
        parameterList = tempparameterList;
        //LINQ END
        script = reader.readVarBytes();
    }

    public boolean equals(VerificationContract other)
    {
        if (this==other) return true;
        if (other==null) return false;
        return scriptHash().equals(other.scriptHash());
    }

    @Override
    public boolean equals(Object obj)
    {
        return equals((VerificationContract)obj);
    }

    @Override
    public int hashCode()
    {
        return scriptHash().hashCode();
    }

    @Override
    public void serialize(BinaryWriter writer)
    {
        writer.write(new UInt160().toArray());
        //LINQ START
        //writer.writeVarBytes(parameterList.Select(p => (byte)p).ToArray());
        byte[] temparay=new byte[parameterList.length];
        for (int i=0;i<parameterList.length;i++){
            temparay[i]=parameterList[i].value();
        }
        writer.writeVarBytes(temparay);
        //LINQ END
        writer.writeVarBytes(script);
    }
}