package neo.ledger;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;

import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.network.p2p.payloads.BlockBase;
import neo.network.p2p.payloads.Header;

public class TrimmedBlock extends BlockBase {

    public UInt160 hashes[];

    private Header header;

    public boolean isBlock() {
        return hashes.length > 0;
    }

//    public Block GetBlock(DataCache<UInt256, TransactionState> cache) {
//        return new Block() {{
//            version = version;
//                    prevHash = prevHash;
//                    merkleRoot = merkleRoot;
//            timestamp = timestamp,
//                    index = index,
//                    consensusData = consensusData,
//                    nextConsensus = nextConsensus,
//                    witness = witness,
//                    transactions = Arrays.stream(hashes).map(p -> cache::get).Select(p = > cache[p].Transaction).ToArray()
//        }};
//    }

    public Header getHeader() {
        if (header == null) {
            header = new Header() {{
                version = TrimmedBlock.this.version;
                prevHash = TrimmedBlock.this.prevHash;
                merkleRoot = TrimmedBlock.this.merkleRoot;
                timestamp = TrimmedBlock.this.timestamp;
                index = TrimmedBlock.this.index;
                consensusData = TrimmedBlock.this.consensusData;
                nextConsensus = TrimmedBlock.this.nextConsensus;
                witness = TrimmedBlock.this.witness;
            }};
        }
        return header;
    }

    @Override
    public int size() {
        return super.size();
        // TODO getvarsize
//        return super.size() + Hashes.GetVarSize();
    }

    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        hashes = reader.readArray(UInt160[]::new, UInt160::new);
    }

    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeArray(hashes);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        JsonArray array = new JsonArray(hashes.length);
        Arrays.stream(hashes).forEach(p -> array.add(p.toString()));
        json.add("hashes", array);
        return json;
    }

}
