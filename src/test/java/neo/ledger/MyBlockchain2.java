package neo.ledger;

import java.util.ArrayList;

import akka.actor.ActorRef;
import akka.actor.Props;
import neo.NeoSystem;
import neo.UInt256;
import neo.log.tr.TR;
import neo.persistence.Store;

public class MyBlockchain2 extends Blockchain {

    public ArrayList<UInt256> myheaderIndex;

    public MyBlockchain2(NeoSystem system, Store store) {
        super(system, store);
    }

    @Override
    protected void init(NeoSystem system, Store store) {
        this.system = system;
        this.store = store;
        this.memPool = new MemoryPool(system, MemoryPoolMaxTransactions);
        // 测试环境下，由于akka的创建，可以同时存在多个
        singleton = this;
        myheaderIndex = this.headerIndex;

        initData();
    }

    public static Props props(NeoSystem system, Store store, ActorRef actorRef) {
        TR.enter();
        return TR.exit(Props.create(MyBlockchain2.class, system, store).withMailbox("blockchain-mailbox"));
    }
}
