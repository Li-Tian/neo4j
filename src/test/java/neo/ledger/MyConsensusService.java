package neo.ledger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import neo.NeoSystem;
import neo.consensus.ConsensusService;
import neo.consensus.MyWallet;
import neo.network.p2p.MyTaskManager;
import neo.wallets.Wallet;

public class MyConsensusService extends ConsensusService {

    private ActorRef testRootRef;

    public MyConsensusService(ActorRef localNode, ActorRef taskManager, ActorRef testRootRef) {
        super(localNode, taskManager, new MyWallet());
        this.testRootRef = testRootRef;
    }

    public static Props props(ActorRef localNode, ActorRef taskManager, ActorRef testRootRef) {
        return Props.create(MyConsensusService.class, localNode, taskManager, testRootRef);
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .matchAny(obj -> testRootRef.tell(obj, self()))
                .build();
    }

}
