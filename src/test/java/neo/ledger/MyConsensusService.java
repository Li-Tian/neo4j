package neo.ledger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import neo.NeoSystem;
import neo.consensus.ConsensusService;
import neo.consensus.MyWallet;
import neo.network.p2p.MyTaskManager;

public class MyConsensusService extends ConsensusService {

    private ActorRef testRootRef;

    public MyConsensusService(ActorRef localNode, ActorRef taskManager) {
        super(localNode, taskManager, new MyWallet());
        this.testRootRef = taskManager;
    }

    public static Props props(NeoSystem system, ActorRef actorRef) {
        return Props.create(MyTaskManager.class, system, actorRef);
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .matchAny(obj -> testRootRef.tell(obj, self()))
                .build();
    }

}
