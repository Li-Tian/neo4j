package neo.ledger;

public class RelayResultReason {
    public static final byte Succeed = 0;
    public static final byte AlreadyExists = 1;
    public static final byte OutOfMemory = 2;
    public static final byte Invalid = 4;
    public static final byte PolicyFail = 5;
    public static final byte Unknown = 6;
}
