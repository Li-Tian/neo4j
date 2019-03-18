package neo;

import akka.actor.ActorRef;
import neo.ledger.Blockchain;
import neo.network.p2p.LocalNode;
import neo.persistence.Store;

/**
 * NEO core system class for controlling and running NEO functions
 */
public class NeoSystem {

    public Blockchain blockchain;

    public ActorRef localNode;

    public  ActorRef taskManager;

    public NeoSystem(Store store) {
        // TODO
        blockchain = new Blockchain(this, store);
    }

}
