package neo.network.p2p;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import neo.NeoSystem;

public class MyLocalNode extends LocalNode {

    private ActorRef testRootRef;

    public static MyLocalNode instance;

    public MyLocalNode(NeoSystem system, ActorRef actorRef) {
        super(system);
        this.testRootRef = actorRef;
        instance = this;
    }

    @Override
    protected void init() {
        singleton = this;
    }

    public static Props props(NeoSystem system, ActorRef actorRef) {
        return Props.create(MyLocalNode.class, system, actorRef);
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .matchAny(obj -> testRootRef.tell(obj, self()))
                .build();
    }
}
