package neo.persistence.leveldb;

import java.util.ArrayList;

import akka.actor.Props;
import neo.UInt256;
import neo.csharp.Uint;
import neo.ledger.Blockchain;

public class BlockchainDemo extends Blockchain {

    public static final ArrayList<UInt256> myheaderIndex = new ArrayList<>();

    public BlockchainDemo() {
        super(null, null);
    }

    @Override
    public UInt256 getBlockHash(Uint index) {
        if (myheaderIndex.size() <= index.intValue()) {
            return null;
        }
        return myheaderIndex.get(index.intValue());
    }

    public static Props props() {
        return Props.create(BlockchainDemo.class).withMailbox("blockchain-mailbox");
    }

}
