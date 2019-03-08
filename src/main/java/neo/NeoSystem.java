package neo;

import neo.ledger.Blockchain;
import neo.persistence.Store;

/**
 * NEO core system class for controlling and running NEO functions
 */
public class NeoSystem {

    public Blockchain blockchain;

    public NeoSystem(Store store) {
        // TODO
        blockchain = new Blockchain(this, store);
    }

}
