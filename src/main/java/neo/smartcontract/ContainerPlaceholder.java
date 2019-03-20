package neo.smartcontract;

import neo.log.tr.TR;
import neo.vm.StackItem;

class ContainerPlaceholder extends StackItem {
    public StackItemType Type;
    public int ElementCount;

    @Override
    public boolean equals(StackItem other) {
        TR.enter();
        TR.exit();
        throw new RuntimeException("Not Supported");
    }

    @Override
    public byte[] getByteArray() {
        TR.enter();
        TR.exit();
        throw new RuntimeException("Not Supported");
    }
}