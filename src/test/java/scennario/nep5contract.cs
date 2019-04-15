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
     * 步骤:
     * 1. 部署该智能合约
     * 2. 调用该合约的`deploy`方法，进行代币生成到Owner账户上
     * 3. 调用合约的`transfer`方法，进行代币分发
     *
     * @link 合约部署，可以使用社区的浏览器工具 https://neoray.nel.group/#/login
     *   需要配合本地钱包，将账户地址，拿到账户合约的ScriptHash值
     *
     * @Note
     *  1. 大家在填写地址 byte[] 时候，需要对从GUI上提取的合约脚本散列按照字节反转，
     *    例如 `0xce24e60088712968392d0fe31a39879b71b0af30`, 反转为 `30afb0719b87391ae30f2d396829718800e624cd`（比较蛋疼）
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

        // TODO 记得改成自己的钱包地址
        public static readonly byte[] Owner = "AUejN4mLdGA8Yyhmj1NwqURwE3FrMjZ4e9".ToScriptHash();

        [DisplayName("transfer")]
        public static event Action<byte[], byte[], BigInteger> Transferred;//交易事件

        public static Object Main(string operation, params object[] args)
        {
            if (Runtime.Trigger == TriggerType.Application)
            {
                if (operation == "deploy") return Deploy();             // 部署
                if (operation == "totalSupply") return TotalSupply();   // 总量
                if (operation == "name") return Name();                 // 名称
                if (operation == "symbol") return Symbol();             // 标示符
                if (operation == "decimals") return Decimals();         // 精度
                if (operation == "owner") return Owner;                 // 所有者
                if (operation == "transfer")                            // 转账
                {
                    if (args.Length != 3) return false;
                    byte[] from = (byte[])args[0];
                    byte[] to = (byte[])args[1];
                    BigInteger value = (BigInteger)args[2];
                    return Transfer(from, to, value);
                }
                if (operation == "balanceOf")//余额
                {
                    if (args.Length != 1) return 0;
                    byte[] account = (byte[])args[0];
                    return BalanceOf(account);
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
    }
}
