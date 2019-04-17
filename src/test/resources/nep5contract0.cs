using Neo.SmartContract.Framework;
using Neo.SmartContract.Framework.Services.Neo;
using System;
using System.ComponentModel;
using System.Numerics;

namespace Neo.SmartContract
{
    /**
     * NEP5 Token Demo
     *
     *
     * @Note
     *  1. don't forget to reverse the byte array, when assgin the parameters.
     *
     * @author Minge
     * @email luchuan@neo.org
     * @date 2019/1/3
     */
    public class MyNep5Token : Framework.SmartContract
    {
        public static string Name() => "my nep5 token";
        public static string Symbol() => "MGR";
        public static byte Decimals() => 8;
        public static BigInteger TotalSupply() => (ulong)100000000 * 100000000;

        // TODO set the address to yours
        public static readonly byte[] Owner = "AUejN4mLdGA8Yyhmj1NwqURwE3FrMjZ4e9".ToScriptHash();

        [DisplayName("transfer")]
        public static event Action<byte[], byte[], BigInteger> Transferred;

        public static Object Main(string operation, params object[] args)
        {
            if (Runtime.Trigger == TriggerType.Application)
            {
                if (operation == "deploy") return Deploy();
                if (operation == "totalSupply") return TotalSupply();
                if (operation == "name") return Name();
                if (operation == "symbol") return Symbol();
                if (operation == "decimals") return Decimals();
                if (operation == "owner") return Owner;
                if (operation == "transfer")
                {
                    if (args.Length != 3) return false;
                    byte[] from = (byte[])args[0];
                    byte[] to = (byte[])args[1];
                    BigInteger value = (BigInteger)args[2];
                    return Transfer(from, to, value);
                }
                if (operation == "balanceOf")
                {
                    if (args.Length != 1) return 0;
                    byte[] account = (byte[])args[0];
                    return BalanceOf(account);
                }
                if (operation == "migrate")
                {
                    if (args.Length != 9) return false;

                    byte[] script = (byte[]) args[0];
                    byte[] parameter_list = (byte[])args[1];
                    byte return_type = (byte)args[2];
                    ContractPropertyState contract_properties = (ContractPropertyState)args[3];
                    string name = (string)args[4];
                    string version = (string)args[5];
                    string author = (string)args[6];
                    string email = (string)args[7];
                    string description = (string)args[8];
                    return Migrate(script, parameter_list, return_type, contract_properties, name, version, author, email, description);
                }
                if(operation == "delete")
                {
                    return Delete();
                }

            }
            return true;
        }

        public static BigInteger BalanceOf(byte[] account)
        {
            return Storage.Get(Storage.CurrentContext, account).AsBigInteger();
        }

        public static bool Transfer(byte[] from, byte[] to, BigInteger amount)
        {
            if (amount <= 0) return false;
            if (!Runtime.CheckWitness(from)) return false;
            if (to.Length != 20) return false;

            BigInteger from_value = Storage.Get(Storage.CurrentContext, from).AsBigInteger();
            if (from_value < amount) return false;
            if (from == to) return true;
            if (from_value == amount)
                Storage.Delete(Storage.CurrentContext, from);
            else
                Storage.Put(Storage.CurrentContext, from, from_value - amount);
            BigInteger to_value = Storage.Get(Storage.CurrentContext, to).AsBigInteger();
            Storage.Put(Storage.CurrentContext, to, to_value + amount);

            Transferred(from, to, amount);
            return true;
        }

        public static bool Deploy()
        {
            byte[] total_supply = Storage.Get(Storage.CurrentContext, "totalSupply");
            if (total_supply.Length != 0) return false;

            Storage.Put(Storage.CurrentContext, Owner, TotalSupply());
            Storage.Put(Storage.CurrentContext, "totalSupply", TotalSupply());

            Transferred(null, Owner, TotalSupply());

            return true;
        }

        public static bool Migrate(byte[] script, byte[] parameter_list, byte return_type, ContractPropertyState contract_properties, string name, string version, string author, string email, string description)
        {
            Contract.Migrate(script, parameter_list, return_type, contract_properties, name, version, author, email, description);
            return true;
        }

        public static bool Delete()
        {
            Contract.Destroy();
            return true;
        }
    }
}
