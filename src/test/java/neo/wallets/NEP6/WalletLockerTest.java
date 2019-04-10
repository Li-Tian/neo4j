package neo.wallets.NEP6;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.net.URL;
import java.util.HashSet;

import neo.UInt160;
import neo.UInt256;
import neo.wallets.Coin;
import neo.wallets.WalletIndexer;
import neo.csharp.Uint;
import neo.log.notr.TR;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletLockerTest
 * @Package neo.wallets.NEP6
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 15:52 2019/4/4
 */
public class WalletLockerTest {
    class UserWalletIndexer extends WalletIndexer {

        public UserWalletIndexer() {
            super();
        }

        @Override
        public Uint getIndexHeight() {
            return Uint.ONE;
        }

        @Override
        public void registerAccounts(Iterable<UInt160> accounts) {
            //super.registerAccounts(accounts);
        }

        @Override
        public void registerAccounts(Iterable<UInt160> accounts, Uint height) {
            //super.registerAccounts(accounts, height);
        }

        @Override
        public void unregisterAccounts(Iterable<UInt160> accounts) {
            //super.unregisterAccounts(accounts);
        }


        @Override
        public Iterable<UInt256> getTransactions(Iterable<UInt160> accounts) {
            return new HashSet<UInt256>();
        }


        @Override
        public Iterable<Coin> getCoins(Iterable<UInt160> accounts) {
            return new HashSet<Coin>();
        }
    }

    @Test
    public void dispose() throws Exception {
        String json="{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract=NEP6Contract.fromJson(object);
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        WalletLocker locker=new WalletLocker(wallet);
        locker.dispose();
    }

}