package neo;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;

public class Fixed8Test {
    @Test
    public void abs() {
        Fixed8 result = new Fixed8(-10).abs();
        Assert.assertEquals(new Fixed8(10), result);
        result = new Fixed8(100).abs();
        Assert.assertEquals(new Fixed8(100), result);
    }

    @Test
    public void size() {
        Assert.assertEquals(8, new Fixed8(1).size());
    }

    @Test
    public void ceiling() {
        Assert.assertEquals(new Fixed8(200000000), new Fixed8(100030000).ceiling());
        Assert.assertEquals(new Fixed8(-100000000), new Fixed8(-10400000).ceiling());
    }

    @Test
    public void compareTo() {
        Assert.assertEquals(1, new Fixed8(200000000).compareTo(new Fixed8(100030000)));
        Assert.assertEquals(-1, new Fixed8(-200000000).compareTo(new Fixed8(-10400000)));
        Assert.assertEquals(0, new Fixed8(30).compareTo(new Fixed8(30)));
    }
    
    @Test
    public void equals() {
        Assert.assertEquals(false, new Fixed8(200000000).equals(new Fixed8(100030000)));
        Assert.assertEquals(true, new Fixed8(200000000).equals(new Fixed8(200000000)));
        Assert.assertEquals(false, new Fixed8(200000000).equals(9l));
    }

    @Test
    public void fromDecimal() {
        Assert.assertEquals(new Fixed8(300000000L), Fixed8.fromDecimal(new BigDecimal(3)));
    }

    @Test
    public void getData() {
        Assert.assertEquals(300000000L, new Fixed8(300000000L).getData());
    }

    @Test
    public void hashCodeTest() {
        Assert.assertEquals(Long.hashCode(300000000L), new Fixed8(300000000L).hashCode());
    }

    @Test
    public void max() {
        Assert.assertEquals(new Fixed8(100L), Fixed8.max(new Fixed8(1L), new Fixed8[]{new Fixed8(3L), new Fixed8(6L), new Fixed8(100L)}));
        Assert.assertEquals(new Fixed8(100L), Fixed8.max(new Fixed8(100L), new Fixed8[]{new Fixed8(3L), new Fixed8(6L), new Fixed8(30L)}));
    }

    @Test
    public void min() {
        Assert.assertEquals(new Fixed8(1L), Fixed8.min(new Fixed8(1L), new Fixed8[]{new Fixed8(3L), new Fixed8(6L), new Fixed8(100L)}));
        Assert.assertEquals(new Fixed8(1L), Fixed8.min(new Fixed8(10L), new Fixed8[]{new Fixed8(3L), new Fixed8(6L), new Fixed8(1L)}));
    }

    @Test
    public void parse() {
        Assert.assertEquals(new Fixed8(300000000L), Fixed8.parse("3"));
    }

    @Test
    public void serialize() {
        Fixed8 fixed8 = new Fixed8(100L);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        fixed8.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        Fixed8 copy = new Fixed8();
        copy.deserialize(new BinaryReader(inputStream));

        Assert.assertEquals(100L, copy.getData());
    }

    @Test
    public void toStringTest() {
        Assert.assertEquals("3.00000000", new Fixed8(300000000L));
    }

    @Test
    public void tryParse() {
        Fixed8 fixed8 = new Fixed8();
        Assert.assertEquals(true, Fixed8.tryParse("3", fixed8));
        Assert.assertEquals(new Fixed8(300000000L), fixed8);

        Assert.assertEquals(true, Fixed8.tryParse("hello", fixed8));
        Assert.assertEquals(new Fixed8(0L), fixed8);
    }

    @Test
    public void toBigDecimal() {
        Assert.assertEquals(new BigDecimal(3L), Fixed8.toBigDecimal(new Fixed8(300000000L)));
    }

    @Test
    public void toLong() {
        Assert.assertEquals(3L, Fixed8.toLong(new Fixed8(300000123L)));
    }

    @Test
    public void equal() {
        Assert.assertEquals(true, Fixed8.equal(new Fixed8(3L), new Fixed8(3L)));
        Assert.assertEquals(false, Fixed8.equal(new Fixed8(3L), new Fixed8(20L)));
    }

    @Test
    public void notEqual() {
        Assert.assertEquals(false, Fixed8.equal(new Fixed8(3L), new Fixed8(3L)));
        Assert.assertEquals(true, Fixed8.equal(new Fixed8(3L), new Fixed8(20L)));
    }

    @Test
    public void bigger() {
        Assert.assertEquals(false, Fixed8.bigger(new Fixed8(3L), new Fixed8(3L)));
        Assert.assertEquals(false, Fixed8.bigger(new Fixed8(3L), new Fixed8(20L)));
        Assert.assertEquals(true, Fixed8.bigger(new Fixed8(30L), new Fixed8(20L)));
    }

    @Test
    public void smaller() {
        Assert.assertEquals(false, Fixed8.smaller(new Fixed8(3L), new Fixed8(3L)));
        Assert.assertEquals(true, Fixed8.smaller(new Fixed8(3L), new Fixed8(20L)));
        Assert.assertEquals(false, Fixed8.smaller(new Fixed8(30L), new Fixed8(20L)));
    }

    @Test
    public void biggerOrEqual() {
        Assert.assertEquals(true, Fixed8.biggerOrEqual(new Fixed8(3L), new Fixed8(3L)));
        Assert.assertEquals(false, Fixed8.biggerOrEqual(new Fixed8(3L), new Fixed8(20L)));
        Assert.assertEquals(true, Fixed8.biggerOrEqual(new Fixed8(30L), new Fixed8(20L)));
    }

    @Test
    public void smallerOrEqual() {
        Assert.assertEquals(true, Fixed8.smallerOrEqual(new Fixed8(3L), new Fixed8(3L)));
        Assert.assertEquals(true, Fixed8.smallerOrEqual(new Fixed8(3L), new Fixed8(20L)));
        Assert.assertEquals(false, Fixed8.smallerOrEqual(new Fixed8(30L), new Fixed8(20L)));
    }

    @Test
    public void multiply() {
        Assert.assertEquals(new Fixed8(20000000000L), Fixed8.multiply(new Fixed8(100000000L), new Fixed8(200000000L)));
        Assert.assertEquals(new Fixed8(300L), Fixed8.multiply(new Fixed8(30L), 10L));
    }

    @Test
    public void divide() {
        Assert.assertEquals(new Fixed8(30L), Fixed8.divide(new Fixed8(300L), 10L));
    }

    @Test
    public void add() {
        Assert.assertEquals(new Fixed8(50L), Fixed8.add(new Fixed8(30L), new Fixed8(20L)));
    }

    @Test
    public void subtract() {
        Assert.assertEquals(new Fixed8(10L), Fixed8.subtract(new Fixed8(30L), new Fixed8(20L)));
    }

    @Test
    public void negate() {
        Assert.assertEquals(new Fixed8(-10L), Fixed8.negate(new Fixed8(10L)));
    }
}
