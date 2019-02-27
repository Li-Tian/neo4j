package neo;

import java.io.IOException;
import java.math.BigDecimal;

import neo.csharp.Ulong;
import neo.io.BinaryReader;
import neo.io.BinaryWriter;
import neo.io.ISerializable;

import java.io.OutputStream;
import java.io.InputStream;

import neo.log.tr.TR;

public class Fixed8 implements Comparable<Fixed8>, ISerializable {
    /// <summary>
    /// Accurate to 10^-8 64-bit fixed-point numbers minimize rounding errors.
    /// By controlling the accuracy of the multiplier, rounding errors can be completely eliminated.
    /// </summary>

    private static final long D = 100000000L;
    private long value;

    public static final Fixed8 MAX_VALUE = new Fixed8(Long.MAX_VALUE);

    public static final Fixed8 MIN_VALUE = new Fixed8(Long.MIN_VALUE);

    public static final Fixed8 ONE = new Fixed8(D);

    public static final Fixed8 SATOSHI = new Fixed8(1);

    public static final Fixed8 ZERO = new Fixed8(0);

    static final String EMPTY = "";
    static final String POINT = ".";
    static final String COMMA = ",";
    static final String POINT_AS_STRING = ".";
    static final String COMMA_AS_STRING = ",";

    public Fixed8(long data) {
        TR.enter();
        this.value = data;
        TR.exit();
    }

    public Fixed8 clone() {
        TR.enter();
        return TR.exit(new Fixed8(value));
    }

    public Fixed8 abs() {
        TR.enter();
        if (value >= 0) {
            return TR.exit(this);
        }
        return TR.exit(new Fixed8(-value));
    }

    @Override
    public int size() {
        TR.enter();
        return TR.exit(Long.BYTES);
    }

    public Fixed8 ceiling() {
        TR.enter();
        long remainder = value % D;
        if (remainder == 0) return this;
        if (remainder > 0) {
            return TR.exit(new Fixed8(value - remainder + D));
        } else {
            return TR.exit(new Fixed8(value - remainder));
        }
    }

    @Override
    public int compareTo(Fixed8 other) {
        TR.enter();
        return TR.exit(Long.compare(this.value, other.value));
    }

    @Override
    public void deserialize(BinaryReader reader) throws IOException {
        TR.enter();
        value = reader.readLong();
        TR.exit();
    }

    public boolean equals(Fixed8 other) {
        TR.enter();
        return TR.exit(Long.compare(this.value, other.value) == 0);
    }

    public boolean equals(Object obj) {
        TR.enter();
        if (!(obj instanceof Fixed8)) {
            return TR.exit(false);
        }
        return TR.exit(equals((Fixed8) obj));
    }

    public static Fixed8 fromDecimal(BigDecimal value) {
        TR.enter();
        return TR.exit(new Fixed8(value.multiply(new BigDecimal(D)).longValueExact()));
    }

    public long getData() {
        TR.enter();
        return TR.exit(value);
    }

    @Override
    public int hashCode() {
        TR.enter();
        return TR.exit(Long.hashCode(value));
    }

    public static Fixed8 max(Fixed8 first, Fixed8[] others) {
        TR.enter();
        for (Fixed8 other : others) {
            if (first.compareTo(other) < 0) {
                first = other;
            }
        }
        return TR.exit(first);
    }

    public static Fixed8 min(Fixed8 first, Fixed8[] others) {
        TR.enter();
        for (Fixed8 other : others) {
            if (first.compareTo(other) > 0) {
                first = other;
            }
        }
        return TR.exit(first);
    }

    public static Fixed8 parse(String s) {
        TR.enter();
        return TR.exit(fromDecimal(new BigDecimal(s)));
    }

    @Override
    public void serialize(BinaryWriter writer) throws IOException {
        TR.enter();
        writer.writeLong(value);
        TR.exit();
    }

    @Override
    public String toString() {
        TR.enter();
        BigDecimal v = new BigDecimal(value);
        v = v.divide(new BigDecimal(D), 8, BigDecimal.ROUND_UNNECESSARY);
        return TR.exit(v.toPlainString());
    }

    public static boolean tryParse(String s, Fixed8 result) {
        TR.enter();
        try {
            BigDecimal val = new BigDecimal(s);
            Fixed8 fixed8Val = fromDecimal(val);
            result.value = fixed8Val.value;
            return TR.exit(true);
        } catch (NumberFormatException | ArithmeticException ex) {
            result.value = 0;
            return TR.exit(false);
        }
    }

    public static BigDecimal toBigDecimal(Fixed8 value) {
        TR.enter();
        return TR.exit(new BigDecimal(value.value).divide(new BigDecimal(D), 8, BigDecimal.ROUND_HALF_UP));
    }

    public static long toLong(Fixed8 value) {
        TR.enter();
        return TR.exit(value.value / D);
    }

    public static boolean equal(Fixed8 x, Fixed8 y) {
        TR.enter();
        return TR.exit(x.equals(y));
    }

    public static boolean notEqual(Fixed8 x, Fixed8 y) {
        TR.enter();
        return TR.exit(!x.equals(y));
    }

    public static boolean bigger(Fixed8 x, Fixed8 y) {
        TR.enter();
        return TR.exit(x.compareTo(y) > 0);
    }

    public static boolean smaller(Fixed8 x, Fixed8 y) {
        TR.enter();
        return TR.exit(x.compareTo(y) < 0);
    }

    public static boolean biggerOrEqual(Fixed8 x, Fixed8 y) {
        TR.enter();
        return TR.exit(x.compareTo(y) >= 0);
    }

    public static boolean smallerOrEqual(Fixed8 x, Fixed8 y) {
        TR.enter();
        return TR.exit(x.compareTo(y) <= 0);
    }

    public static Fixed8 multiply(Fixed8 x, Fixed8 y) {
        TR.enter();
        final Ulong QUO = new Ulong(1).shiftLeft(63).divide(new Ulong(D).shiftRight(1));
        final Ulong REM = Ulong.remainder(new Ulong(1).shiftLeft(63), new Ulong(D).shiftRight(1)).shiftLeft(1);
        int sign = Long.signum(x.value) * Long.signum(y.value);
        Ulong ux = new Ulong(Math.abs(x.value));
        Ulong uy = new Ulong(Math.abs(y.value));
        Ulong xh = ux.shiftRight(32);
        Ulong xl = ux.and(new Ulong(0x00000000ffffffffl));
        Ulong yh = uy.shiftRight(32);
        Ulong yl = uy.and(new Ulong(0x00000000ffffffffl));
        Ulong rh = xh.multiply(yh);
        Ulong rm = xh.multiply(yl).add(xl.multiply(yh));
        Ulong rl = xl.multiply(yl);
        Ulong rmh = rm.shiftRight(32);
        Ulong rml = rm.shiftLeft(32);
        rh = rh.add(rmh);
        rl = rl.add(rml);
        if (rl.compareTo(rml) < 0)
            rh = rh.add(new Ulong(1));
        if (rh.compareTo(new Ulong(D)) >= 0) {
            throw new NumberFormatException();
        }
        Ulong rd = rh.multiply(REM).add(rl);
        if (rd.compareTo(rl) < 0) {
            rh = rh.add(new Ulong(1));
        }
        Ulong r = rh.multiply(QUO).add(rd.divide(new Ulong(D)));
        x.value = r.longValue() * sign;
        return x;
    }

    public static Fixed8 multiply(Fixed8 x, long y) {
        TR.enter();
        return TR.exit(new Fixed8(Math.multiplyExact(x.value, y)));
    }

    public static Fixed8 divide(Fixed8 x, long y) {
        TR.enter();
        return TR.exit(new Fixed8(x.value / y));
    }

    public static Fixed8 add(Fixed8 x, Fixed8 y) {
        TR.enter();
        return TR.exit(new Fixed8(Math.addExact(x.value, y.value)));
    }

    public static Fixed8 subtract(Fixed8 x, Fixed8 y) {
        TR.enter();
        return TR.exit(new Fixed8(Math.subtractExact(x.value, y.value)));
    }

    public static Fixed8 negate(Fixed8 value) {
        TR.enter();
        return TR.exit(new Fixed8(-value.value));
    }
}
