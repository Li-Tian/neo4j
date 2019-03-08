package neo.persistence.leveldb;

import java.util.ArrayList;

import akka.actor.Props;
import neo.UInt256;
import neo.csharp.Uint;
import neo.ledger.Blockchain;

public class BlockchainDemo extends Blockchain {

    public ArrayList<UInt256> headerIndex = new ArrayList<>();

    public BlockchainDemo() {
        super(null, null);
    }

    @Override
    public UInt256 getBlockHash(Uint index) {
        if (headerIndex.size() <= index.intValue()) {
            return null;
        }
        return headerIndex.get(index.intValue());
    }

    public static Props props() {
        return Props.create(BlockchainDemo.class).withMailbox("blockchain-mailbox");
    }

}
