package neo;

import java.util.function.Consumer;

import neo.network.p2p.TaskManagerTest;
import neo.persistence.Store;

/**
 * 使用方法请参考 {@link TaskManagerTest#setUp()}
 */
public class MyNeoSystem extends NeoSystem {

    public MyNeoSystem(Store store, Consumer<MyNeoSystem> generator) {
        super(store);
        reinit(generator);
    }

    @Override
    public void init(Store store) {
    }

    public void reinit(Consumer<MyNeoSystem> generator) {
        generator.accept(this);
    }

}
