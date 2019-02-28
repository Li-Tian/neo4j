package neo.network.p2p.payloads;

public class Block extends BlockBase implements IInventory {

    public Transaction[] transactions;

    private Header header = null;

    @Override
    public InventoryType inventoryType() {
        return InventoryType.Block;
    }

    public Header getHeader() {
        if (header == null) {
            header = new Header();
            header.prevHash = this.prevHash;
            header.merkleRoot = this.prevHash;
            header.timestamp = this.timestamp;
            header.index = this.index;
            header.consensusData = this.consensusData;
            header.nextConsensus = this.nextConsensus;
            header.witness = this.witness;
        }
        return header;
    }

}
