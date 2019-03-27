package neo.network.p2p;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import neo.NeoSystem;

public class MyTaskManager extends TaskManager {

    private ActorRef testRootRef;

    public MyTaskManager(NeoSystem system, ActorRef actorRef) {
        super(system);
        this.testRootRef = actorRef;
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
