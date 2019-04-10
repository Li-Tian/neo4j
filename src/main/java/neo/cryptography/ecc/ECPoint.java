package neo.cryptography.ecc;

import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;

import java.math.BigInteger;

import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;
import neo.log.notr.TR;

public class ECPoint extends org.bouncycastle.math.ec.ECPoint.Fp implements Comparable<ECPoint>, ISerializable {

    public int size() {
        TR.enter();
        return (TR.exit(isInfinity()) ? 1 : 33);
    }

    public ECPoint() {
        this(null, null, ECC.Secp256r1.getCurve(), false);
        TR.enter();
        TR.exit();
    }

    public ECPoint(org.bouncycastle.math.ec.ECPoint input) {
        this(input.getXCoord(), input.getYCoord(), input.getCurve(), input.isCompressed());
        TR.enter();
        TR.exit();
    }

    public ECPoint(ECFieldElement x, ECFieldElement y, ECCurve curve, boolean withCompression) {
        super(curve, x, y, withCompression);
        TR.enter();
        if ((x != null && y == null) || (x == null && y != null)) {
            TR.exit();
            throw new IllegalArgumentException("Exactly one of the field elements is null");
        }
        TR.exit();
    }

    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        ECPoint result = deserializeFrom(reader, getCurve());
        super.x = result.getXCoord();
        super.y = result.getYCoord();
        TR.exit();
    }

    public static ECPoint deserializeFrom(BinaryReader reader, ECCurve curve) {
        TR.enter();
        int expectedLength = (((ECCurve.Fp) curve).getFieldSize() + 7) / 8;
        byte[] buffer = null;
        byte firstByte = (byte) reader.readByte();
        switch (firstByte) {
            case 0x00:
                return TR.exit(new ECPoint(curve.getInfinity()));
            case 0x02:
            case 0x03:
                buffer = new byte[expectedLength + 1];
                buffer[0] = firstByte;
                reader.readFully(buffer, 1, expectedLength);
                return TR.exit(new ECPoint(curve.decodePoint(buffer)));
            case 0x04:
            case 0x06:
            case 0x07:
                buffer = new byte[expectedLength * 2 + 1];
                buffer[0] = firstByte;
                reader.readFully(buffer, 1, expectedLength * 2);
                return TR.exit(new ECPoint(curve.decodePoint(buffer)));
            default:
                TR.exit();
                throw new FormatException("Invalid point encoding " + buffer[0]);
        }
    }

    public static ECPoint fromBytes(byte[] pubkey, ECCurve curve) {
        TR.enter();
        byte[] input = new byte[65];
        input[0] = 0x04;
        switch (pubkey.length) {
            case 33:
            case 65:
                return TR.exit(new ECPoint(curve.decodePoint(pubkey)));
            case 64:
            case 72:
                System.arraycopy(pubkey, pubkey.length - 64, input, 1, 64);
                return TR.exit(new ECPoint(curve.decodePoint(input)));
            case 96:
            case 104:
                System.arraycopy(pubkey, pubkey.length - 96, input, 1, 64);
                return TR.exit(new ECPoint(curve.decodePoint(input)));
            default:
                TR.exit();
                throw new FormatException();
        }
    }

    public int getHashCode() {
        TR.enter();
        return TR.exit(getXCoord().toBigInteger().hashCode() + getYCoord().toBigInteger().hashCode());
    }

    public static ECPoint parse(String value, ECCurve curve) {
        TR.enter();
        return TR.exit(new ECPoint(curve.decodePoint(BitConverter.hexToBytes(value))));
    }

    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.write(getEncoded(true));
        TR.exit();
    }

    @Override
    public int compareTo(ECPoint other) {
        TR.enter();
        if (this == other) return TR.exit(0);
        int result = getXCoord().toBigInteger().compareTo(other.getXCoord().toBigInteger());
        if (result != 0) return TR.exit(result);
        return TR.exit(getYCoord().toBigInteger().compareTo(other.getYCoord().toBigInteger()));
    }

    /**
     * to hexstring with compressed format.
     *
     * @return hexstring
     */
    @Override
    public String toString() {
        TR.enter();
        return TR.exit(BitConverter.toHexString(getEncoded(true)));
    }

    public static boolean tryParse(String value, ECCurve curve, ECPoint out_point) {
        TR.enter();
        try {
            ECPoint point = parse(value, curve);
            out_point.x = point.getXCoord();
            out_point.y = point.getYCoord();
            return TR.exit(true);
        } catch (FormatException e) {
            return TR.exit(false);
        }
    }

    public ECPoint multiply (byte[] key) {
        TR.enter();
        return TR.exit(new ECPoint(multiply(new BigInteger(1, key)).normalize()));
    }
}