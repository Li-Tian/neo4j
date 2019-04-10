package neo.Wallets;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.Wallets.NEP6.NEP6Wallet;
import neo.Wallets.NEP6.NEP6WalletTest;
import neo.ledger.MyBlockchain2;
import neo.log.notr.TR;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.TaskManager;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletTransactionEventArgsTest
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:33 2019/4/9
 */
public class WalletTransactionEventArgsTest {
    @Test
    public void constructor() throws Exception {
        WalletTransactionEventArgs args1=new WalletTransactionEventArgs();
        WalletTransactionEventArgs args2=new WalletTransactionEventArgs(null,null,null,null);
    }

}