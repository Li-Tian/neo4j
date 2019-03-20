package neo.Wallets;

import java.util.Arrays;

import neo.ProtocolSettings;
import neo.UInt160;
import neo.cryptography.Crypto;
import neo.exception.FormatException;
import neo.network.p2p.payloads.IVerifiable;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: Helper
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 10:58 2019/3/14
 */
public class Helper {
    public static byte[] sign(IVerifiable verifiable, KeyPair key)
    {
        byte[] tempByteArray=new byte[20];
        System.arraycopy(key.publicKey.getEncoded(false),1,tempByteArray,0,20);
        return Crypto.Default.sign(IVerifiable.getHashData(verifiable), key.privateKey, tempByteArray);
    }

    public static String toAddress(UInt160 scriptHash)
    {
        byte[] data = new byte[21];
        data[0] = ProtocolSettings.Default.addressVersion;
        System.arraycopy(scriptHash.toArray(),0,data,1,20);
        return neo.cryptography.Helper.base58CheckEncode(data);
    }

    public static UInt160 toScriptHash(String address)
    {
        byte[] data = neo.cryptography.Helper.base58CheckDecode(address);
        if (data.length != 21)
            throw new FormatException();
        if (data[0] != ProtocolSettings.Default.addressVersion)
            throw new FormatException();
        byte[] tempByteArray=new byte[20];
        System.arraycopy(data,1,tempByteArray,0,20);
        return new UInt160(tempByteArray);
    }
}