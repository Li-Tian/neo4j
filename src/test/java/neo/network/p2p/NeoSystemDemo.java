package neo.network.p2p;

import neo.NeoSystem;
import neo.persistence.Store;

public class NeoSystemDemo extends NeoSystem {

    public NeoSystemDemo() {
        super(null);
    }

    public NeoSystemDemo(Store store) {
        super(store);
    }

}
