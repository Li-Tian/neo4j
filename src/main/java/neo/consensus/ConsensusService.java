package neo.consensus;

import java.util.Date;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import neo.Wallets.Wallet;
import neo.csharp.Uint;

/**
 * Consensus sevice, implemented the dBFT algorithm. for more details:
 * http://docs.neo.org/en-us/basic/consensus/whitepaper.html
 */
public class ConsensusService extends AbstractActor {

    /**
     * Start consensus activity message(custom AKKA message type)
     */
    public static class Start {
    }

    /**
     * Change the current view number message (AKKA customized message type)
     */
    public static class SetViewNumber {
        public byte viewNumber;
    }

    /**
     * Time out message (AKKA customized message type). It contains the `Height` and `ViewNumber` of
     * the timeout block.
     */
    private static class Timer {
        public Uint height;
        public byte viewNumber;
    }

    private ConsensusContext context;
    private ActorRef localNode;
    private ActorRef taskManager;
    private Cancellable timer_token;
    private Date block_received_time;


    /**
     * Construct a ConsensusService
     *
     * @param localNode   local node
     * @param taskManager task manager
     * @param wallet      wallet
     */
    public ConsensusService(ActorRef localNode, ActorRef taskManager, Wallet wallet) {
        this.localNode = localNode;
        this.taskManager = taskManager;
        this.context = new ConsensusContext(wallet);
    }

    /**
     * Construct a ConsensusService
     *
     * @param localNode   local node
     * @param taskManager task manager
     * @param context     consensus context
     */
    public ConsensusService(ActorRef localNode, ActorRef taskManager, ConsensusContext context) {
        this.localNode = localNode;
        this.taskManager = taskManager;
        this.context = context;
    }


    /**
     * Create ActorRef with mail box `consensus-service-mailbox`
     *
     * @param localNode   local node
     * @param taskManager task manager
     * @param wallet      wallet
     * @return Akka.Actor.Props
     */
    public static Props props(ActorRef localNode, ActorRef taskManager, Wallet wallet) {
        return Props.create(ConsensusService.class, localNode, taskManager, wallet)
                .withMailbox("consensus-service-mailbox");
    }


    @Override
    public Receive createReceive() {
        return null;
    }
}
