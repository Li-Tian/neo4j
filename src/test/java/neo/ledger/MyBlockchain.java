package neo.ledger;

import java.util.ArrayList;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import neo.NeoSystem;
import neo.UInt256;
import neo.csharp.Uint;
import neo.log.tr.TR;
import neo.persistence.Store;

public class MyBlockchain extends Blockchain {

    public ArrayList<UInt256> myheaderIndex;

    public ActorRef testRootRef;

    public MyBlockchain(NeoSystem system, Store store, ActorRef actorRef) {
        super(system, store);
        this.testRootRef = actorRef;
        myheaderIndex = this.headerIndex;
    }

    @Override
    protected void init(NeoSystem system, Store store) {
        this.system = system;
        this.store = store;
        this.memPool = new MemoryPool(system, MemoryPoolMaxTransactions);
        // 测试环境下，由于akka的创建，可以同时存在多个
        singleton = this;

        initData();
    }

    @Override
    public UInt256 getBlockHash(Uint index) {
        if (myheaderIndex.size() <= index.intValue()) {
            return null;
        }
        return myheaderIndex.get(index.intValue());
    }

    public static Props props(NeoSystem system, Store store, ActorRef actorRef) {
        TR.enter();
        return TR.exit(Props.create(MyBlockchain.class, system, store, actorRef).withMailbox("blockchain-mailbox"));
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .matchAny(obj -> testRootRef.tell(obj, self()))
                .build();
    }
}
