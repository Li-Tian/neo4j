package neo.ledger;

public class CoinState {
    public static final byte Unconfirmed = 0;
    public static final byte Confirmed = 1 << 0;
    public static final byte Spent = 1 << 1;
    //Vote = 1 << 2,
    public static final byte Claimed = 1 << 3;
    //Locked = 1 << 4,
    public static final byte Frozen = 1 << 5;
}
