package neo;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Properties;

import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.log.notr.TR;

public class Helper {

    private static final Properties properties;

    /**
     * 初始化项目配置
     */
    static {
        TR.enter();
        properties = new Properties();
        try {
            properties.load(Helper.class.getClassLoader().getResourceAsStream("neo4j.properties"));
        } catch (IOException ioe) {
            TR.warn("Failed to load neo4j.properties. Use default values. : " + ioe.getMessage());
            properties.clear();
            properties.put("neo.version", "unknown");
        }
        TR.exit();
    }

    // TODO open this
//    private static readonly DateTime unixEpoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
//
    private static int bitLen(int w) {
        TR.enter();
        return TR.exit(w < 1 << 15 ? (w < 1 << 7
                ? (w < 1 << 3 ? (w < 1 << 1
                ? (w < 1 << 0 ? (w < 0 ? 32 : 0) : 1)
                : (w < 1 << 2 ? 2 : 3)) : (w < 1 << 5
                ? (w < 1 << 4 ? 4 : 5)
                : (w < 1 << 6 ? 6 : 7)))
                : (w < 1 << 11
                ? (w < 1 << 9 ? (w < 1 << 8 ? 8 : 9) : (w < 1 << 10 ? 10 : 11))
                : (w < 1 << 13 ? (w < 1 << 12 ? 12 : 13) : (w < 1 << 14 ? 14 : 15)))) : (w < 1 << 23 ? (w < 1 << 19
                ? (w < 1 << 17 ? (w < 1 << 16 ? 16 : 17) : (w < 1 << 18 ? 18 : 19))
                : (w < 1 << 21 ? (w < 1 << 20 ? 20 : 21) : (w < 1 << 22 ? 22 : 23))) : (w < 1 << 27
                ? (w < 1 << 25 ? (w < 1 << 24 ? 24 : 25) : (w < 1 << 26 ? 26 : 27))
                : (w < 1 << 29 ? (w < 1 << 28 ? 28 : 29) : (w < 1 << 30 ? 30 : 31)))));
    }

    public static int getBitLength(BigInteger i) {
        TR.enter();
        // TODO consider this : return i.bitLength();
        TR.fixMe("这里可能是个严重的 bug。或者是方法的语意的定义问题？具体请看测试方法。" +
                "如果没有人看见这行，那么这行代码是死代码。");
        byte[] b = i.toByteArray();
        return TR.exit((b.length - 1) * 8 + bitLen(i.signum() > 0 ? b[b.length - 1] : 255 - b[b.length - 1]));
    }

    public static int getLowestSetBit(BigInteger i) {
        TR.enter();
        if (i.signum() == 0) {
            return TR.exit(-1);
        }
        byte[] b = i.toByteArray();
        int w = 0;
        while (b[w] == 0) {
            w++;
        }
        for (int x = 0; x < 8; x++) {
            if ((b[w] & 1 << x) > 0) {
                return TR.exit(x + w * 8);
            }
        }
        TR.exit();
        throw new RuntimeException("bug");
    }

    public static String GetVersion() {
        return properties.getProperty("neo.version");
    }

    //    public static byte[] HexToBytes(this string value)
//    {
//        if (value == null || value.Length == 0)
//            return new byte[0];
//        if (value.Length % 2 == 1)
//            throw new FormatException();
//        byte[] result = new byte[value.Length / 2];
//        for (int i = 0; i < result.Length; i++)
//            result[i] = byte.Parse(value.Substring(i * 2, 2), NumberStyles.AllowHexSpecifier);
//        return result;
//    }
//
//    internal static BigInteger Mod(this BigInteger x, BigInteger y)
//    {
//        x %= y;
//        if (x.Sign < 0)
//            x += y;
//        return x;
//    }
//
//    internal static BigInteger ModInverse(this BigInteger a, BigInteger n)
//    {
//        BigInteger i = n, v = 0, d = 1;
//        while (a > 0)
//        {
//            BigInteger t = i / a, x = a;
//            a = i % x;
//            i = x;
//            x = d;
//            d = v - t * x;
//            v = x;
//        }
//        v %= n;
//        if (v < 0) v = (v + n) % n;
//        return v;
//    }
//
//    internal static BigInteger NextBigInteger(this Random rand, int sizeInBits)
//    {
//        if (sizeInBits < 0)
//            throw new ArgumentException("sizeInBits must be non-negative");
//        if (sizeInBits == 0)
//            return 0;
//        byte[] b = new byte[sizeInBits / 8 + 1];
//        rand.NextBytes(b);
//        if (sizeInBits % 8 == 0)
//            b[b.Length - 1] = 0;
//        else
//            b[b.Length - 1] &= (byte)((1 << sizeInBits % 8) - 1);
//        return new BigInteger(b);
//    }
//
//    internal static BigInteger NextBigInteger(this RandomNumberGenerator rng, int sizeInBits)
//    {
//        if (sizeInBits < 0)
//            throw new ArgumentException("sizeInBits must be non-negative");
//        if (sizeInBits == 0)
//            return 0;
//        byte[] b = new byte[sizeInBits / 8 + 1];
//        rng.GetBytes(b);
//        if (sizeInBits % 8 == 0)
//            b[b.Length - 1] = 0;
//        else
//            b[b.Length - 1] &= (byte)((1 << sizeInBits % 8) - 1);
//        return new BigInteger(b);
//    }
//
//    public static Fixed8 Sum(this IEnumerable<Fixed8> source)
//    {
//        long sum = 0;
//        checked
//        {
//            foreach (Fixed8 item in source)
//            {
//                sum += item.value;
//            }
//        }
//        return new Fixed8(sum);
//    }
//
//    public static Fixed8 Sum<TSource>(this IEnumerable<TSource> source, Func<TSource, Fixed8> selector)
//    {
//        return source.Select(selector).Sum();
//    }
//
//    internal static bool TestBit(this BigInteger i, int index)
//    {
//        return (i & (BigInteger.One << index)) > BigInteger.Zero;
//    }
//
//    public static DateTime ToDateTime(this uint timestamp)
//    {
//        return unixEpoch.AddSeconds(timestamp).ToLocalTime();
//    }
//
//    public static DateTime ToDateTime(this ulong timestamp)
//    {
//        return unixEpoch.AddSeconds(timestamp).ToLocalTime();
//    }
//
//    public static string ToHexString(this IEnumerable<byte> value)
//    {
//        StringBuilder sb = new StringBuilder();
//        foreach (byte b in value)
//        sb.AppendFormat("{0:x2}", b);
//        return sb.ToString();
//    }
//
//        [MethodImpl(MethodImplOptions.AggressiveInlining)]
//    unsafe internal static int ToInt32(this byte[] value, int startIndex)
//    {
//        fixed (byte* pbyte = &value[startIndex])
//        {
//            return *((int*)pbyte);
//        }
//    }
//
//        [MethodImpl(MethodImplOptions.AggressiveInlining)]
//    unsafe internal static long ToInt64(this byte[] value, int startIndex)
//    {
//        fixed (byte* pbyte = &value[startIndex])
//        {
//            return *((long*)pbyte);
//        }
//    }
//
//    public static uint ToTimestamp(this DateTime time)
//    {
//        return (uint)(time.ToUniversalTime() - unixEpoch).TotalSeconds;
//    }
//
//        [MethodImpl(MethodImplOptions.AggressiveInlining)]
//    unsafe internal static ushort ToUInt16(this byte[] value, int startIndex)
//    {
//        fixed (byte* pbyte = &value[startIndex])
//        {
//            return *((ushort*)pbyte);
//        }
//    }
//
    public static Uint toUint(byte[] value, int startIndex) {
        TR.enter();
        byte[] temp = Arrays.copyOfRange(value, startIndex, startIndex + Uint.BYTES);
        return TR.exit(BitConverter.toUint(temp));
    }

//        [MethodImpl(MethodImplOptions.AggressiveInlining)]
//    unsafe internal static ulong ToUInt64(this byte[] value, int startIndex)
//    {
//        fixed (byte* pbyte = &value[startIndex])
//        {
//            return *((ulong*)pbyte);
//        }
//    }
//
//    internal static IPAddress Unmap(this IPAddress address)
//    {
//        if (address.IsIPv4MappedToIPv6)
//            address = address.MapToIPv4();
//        return address;
//    }
//
//    internal static IPEndPoint Unmap(this IPEndPoint endPoint)
//    {
//        if (!endPoint.Address.IsIPv4MappedToIPv6)
//            return endPoint;
//        return new IPEndPoint(endPoint.Address.Unmap(), endPoint.Port);
//    }
//
//    internal static long WeightedAverage<T>(this IEnumerable<T> source, Func<T, long> valueSelector, Func<T, long> weightSelector)
//    {
//        long sum_weight = 0;
//        long sum_value = 0;
//        foreach (T item in source)
//        {
//            long weight = weightSelector(item);
//            sum_weight += weight;
//            sum_value += valueSelector(item) * weight;
//        }
//        if (sum_value == 0) return 0;
//        return sum_value / sum_weight;
//    }
//
//    internal static IEnumerable<TResult> WeightedFilter<T, TResult>(this IList<T> source, double start, double end, Func<T, long> weightSelector, Func<T, long, TResult> resultSelector)
//    {
//        if (source == null) throw new ArgumentNullException(nameof(source));
//        if (start < 0 || start > 1) throw new ArgumentOutOfRangeException(nameof(start));
//        if (end < start || start + end > 1) throw new ArgumentOutOfRangeException(nameof(end));
//        if (weightSelector == null) throw new ArgumentNullException(nameof(weightSelector));
//        if (resultSelector == null) throw new ArgumentNullException(nameof(resultSelector));
//        if (source.Count == 0 || start == end) yield break;
//        double amount = source.Sum(weightSelector);
//        long sum = 0;
//        double current = 0;
//        foreach (T item in source)
//        {
//            if (current >= end) break;
//            long weight = weightSelector(item);
//            sum += weight;
//            double old = current;
//            current = sum / amount;
//            if (current <= start) continue;
//            if (old < start)
//            {
//                if (current > end)
//                {
//                    weight = (long)((end - start) * amount);
//                }
//                else
//                {
//                    weight = (long)((current - start) * amount);
//                }
//            }
//            else if (current > end)
//            {
//                weight = (long)((end - old) * amount);
//            }
//            yield return resultSelector(item, weight);
//        }
//    }
}
