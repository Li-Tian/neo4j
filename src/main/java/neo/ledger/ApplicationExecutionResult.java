package neo.ledger;

import neo.Fixed8;
import neo.smartcontract.NotifyEventArgs;
import neo.smartcontract.TriggerType;
import neo.UInt160;
import neo.vm.StackItem;
import neo.vm.VMState;

public class ApplicationExecutionResult {
    public TriggerType trigger;
    public UInt160 scriptHash;
    public VMState vmState;
    public Fixed8 gasConsumed;
    public StackItem[] stack;
    public NotifyEventArgs[] notifications;
}
