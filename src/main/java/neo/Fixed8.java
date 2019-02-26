package neo;

import java.io.IOException;
import java.math.BigDecimal;

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

    public Fixed8 clone(Fixed8 input) {
        TR.enter();
        return TR.exit(new Fixed8(input.value));
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

    public static BigDecimal toBigDecimal(final String value) {
        TR.enter();
        if (value != null) {
            boolean negativeNumber = false;

            if (value.contains("(") && value.contains(")"))
                negativeNumber = true;
            if (value.endsWith("-") || value.startsWith("-"))
                negativeNumber = true;

            String parsedValue = value.replaceAll("[^0-9\\,\\.]", EMPTY);

            if (negativeNumber)
                parsedValue = "-" + parsedValue;

            int lastPointPosition = parsedValue.lastIndexOf(POINT);
            int lastCommaPosition = parsedValue.lastIndexOf(COMMA);

            if (lastPointPosition == -1 && lastCommaPosition == -1)
                return TR.exit(new BigDecimal(parsedValue));
            if (lastPointPosition > -1 && lastCommaPosition == -1) {
                int firstPointPosition = parsedValue.indexOf(POINT);
                if (firstPointPosition != lastPointPosition)
                    return TR.exit(new BigDecimal(parsedValue.replace(POINT_AS_STRING, EMPTY)));
                else
                    return TR.exit(new BigDecimal(parsedValue));
            }
            if (lastPointPosition == -1 && lastCommaPosition > -1) {
                int firstCommaPosition = parsedValue.indexOf(COMMA);
                if (firstCommaPosition != lastCommaPosition)
                    return TR.exit(new BigDecimal(parsedValue.replace(COMMA_AS_STRING, EMPTY)));
                else
                    return TR.exit(new BigDecimal(parsedValue.replace(COMMA, POINT)));
            }
            if (lastPointPosition < lastCommaPosition) {
                parsedValue = parsedValue.replace(POINT_AS_STRING, EMPTY);
                return TR.exit(new BigDecimal(parsedValue.replace(COMMA, POINT)));
            }
            if (lastCommaPosition < lastPointPosition) {
                parsedValue = parsedValue.replace(COMMA_AS_STRING, EMPTY);
                return TR.exit(new BigDecimal(parsedValue));
            }
            TR.exit();
            throw new NumberFormatException("Unexpected number format. Cannot convert '" + value + "' to BigDecimal.");
        }
        return TR.exit(null);
    }

    @Override
    public void deserialize(InputStream reader) throws IOException {
        TR.enter();
        byte[] input = new byte[8];
        value = reader.read(input);
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
        return TR.exit(fromDecimal(toBigDecimal(s)));
    }

    @Override
    public void serialize(OutputStream writer) throws IOException {
        TR.enter();
        byte[] byteNum = new byte[8];
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((this.value >> offset) & 0xff);
        }
        writer.write(byteNum);
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
            return true;
        } catch (NumberFormatException | ArithmeticException ex) {
            return false;
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

    //Return the byte result, pretending x and y are unsigned long type
    static long add(long x, long y) {
        TR.enter();
        long result = (x & Long.MAX_VALUE) + (y & Long.MAX_VALUE);
        if (Long.signum(x) * Long.signum(y) < 0) {
            result ^= Long.MIN_VALUE;
        }
        return TR.exit(result);
    }

    //Return the byte result, pretending x and y are unsigned long type
    static long multiply(long x, long y) {
        TR.enter();
        long result = (x & Long.MAX_VALUE) * (y & Long.MAX_VALUE);
        if (x % 2 != 0 && y < 0) {
            result ^= Long.MIN_VALUE;
        }
        if (y % 2 != 0 && x < 0) {
            result ^= Long.MIN_VALUE;
        }
        return TR.exit(result);
    }

    //Return the comparation result, pretending x and y are unsigned long type
    static long compare(long x, long y) {
        long result = (x >> 1) - (y >> 1);
        if (result > 0) return TR.exit(1);
        else if (result < 0) return TR.exit(-1);
        return TR.exit(x & 1l - y & 1l);
    }

    public static Fixed8 multiply(Fixed8 x, Fixed8 y) {
        TR.enter();
        final long QUO = -((1l << 63) / (D >> 1));//2^41
        final long REM = (-((1l << 63) % (D >> 1))) << 1;//2^25
        int sign = Long.signum(x.value) * Long.signum(y.value);
        long ux = Math.abs(x.value);
        long uy = Math.abs(y.value);
        long xh = ux >> 32;//2^22
        long xl = ux & 0x00000000ffffffffl;//2^32
        long yh = uy >> 32;//2^22
        long yl = uy & 0x00000000ffffffffl;//2^32
        long rh = xh * yh;//2^44
        long rm = xh * yl + xl * yh;//2^55
        long rl = xl * yl;//2^64
        long rmh = rm >> 32;//2^23
        long rml = rm << 32;//2^64
        rh += rmh;
        rl = add(rl, rml);
        if (compare(rl, rml) < 0)
            ++rh;
        if (rh >= D)
            throw new NumberFormatException();

        long rd = multiply(rh, REM) + rl;
        if (compare(rd, rl) < 0)
            ++rh;
        long r = multiply(rh, QUO) + rd / D;
        x.value = (long) r * sign;
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
