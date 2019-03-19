package neo.cryptography;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.exception.FormatException;
import neo.io.SerializeHelper;
import neo.log.notr.TR;
import neo.network.p2p.payloads.RegisterTransaction;
import neo.network.p2p.payloads.Transaction;


public class Helper {
    public static byte[] aes256Decrypt(byte[] block, byte[] key) {
        try {
            TR.enter();
            SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            return TR.exit(cipher.doFinal(block));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public static byte[] aes256Encrypt(byte[] block, byte[] key) {
        try {
            TR.enter();
            SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            return TR.exit(cipher.doFinal(block));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesDecrypt(byte[] data, byte[] key, byte[] iv) {
        try {
            TR.enter();
            if (data == null || key == null || iv == null) {
                TR.exit();
                throw new IllegalArgumentException();
            }
            if (data.length % 16 != 0 || key.length != 32 || iv.length != 16) {
                TR.exit();
                throw new IllegalArgumentException();
            }
            SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.getExemptionMechanism();
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
            return TR.exit(cipher.doFinal(data));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesEncrypt(byte[] data, byte[] key, byte[] iv) {
        try {
            TR.enter();
            if (data == null || key == null || iv == null) {
                TR.exit();
                throw new IllegalArgumentException();
            }
            if (data.length % 16 != 0 || key.length != 32 || iv.length != 16) {
                TR.exit();
                throw new IllegalArgumentException();
            }
            SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
            return TR.exit(cipher.doFinal(data));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public static byte[] base58CheckDecode(String input) {
        TR.enter();
        byte[] buffer = Base58.decode(input);
        if (buffer.length < 4) {
            TR.exit();
            throw new FormatException();
        }
        byte[] checksum = sha256(sha256(buffer, 0, buffer.length - 4));
        if (!Arrays.equals(Arrays.copyOfRange(buffer, buffer.length - 4, buffer.length), Arrays.copyOfRange(checksum, 0, 4))) {
            TR.exit();
            throw new FormatException();
        }
        return TR.exit(Arrays.copyOfRange(buffer, 0, buffer.length - 4));
    }

    public static String base58CheckEncode(byte[] data) {
        TR.enter();
        byte[] checksum = sha256(sha256(data));
        byte[] buffer = new byte[data.length + 4];
        System.arraycopy(data, 0, buffer, 0, data.length);
        System.arraycopy(checksum, 0, buffer, data.length, 4);
        return TR.exit(Base58.encode(buffer));
    }

    /**
     * RipeMD160消息摘要
     *
     * @param data 待处理的消息摘要数据
     * @return byte[] 消息摘要
     * @date:2019/03/11
     */
    public static byte[] ripeMD160(byte[] data) {
        TR.enter();
        RIPEMD160Digest processsor = new RIPEMD160Digest();
        processsor.update(data, 0, data.length);
        byte[] output = new byte[processsor.getDigestSize()];
        processsor.doFinal(output, 0);
        return TR.exit(output);
    }

    public static Uint murmur32(byte[] value, Uint seed) {
        TR.enter();
        Uint result;
        Murmur3 murmur = new Murmur3(seed);
        try {
            byte[] buf = murmur.computeHash(value);
            result = BitConverter.toUint(buf);
        } finally {
            murmur.dispose();
        }
        return TR.exit(result);
    }

    /**
     * @param bytes 待处理的字符串
     * @return String  处理后的消息hash
     * @Author:doubi.liu
     * @description:对数据做Sha256
     * @date:2018/10/11
     */
    public static byte[] sha256(byte[] bytes) {
        try {
            TR.enter();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return TR.exit(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha256(byte[] bytes, int offset, int count) {
        try {
            TR.enter();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return TR.exit(md.digest(Arrays.copyOfRange(bytes, offset, offset + count)));
        } catch (NoSuchAlgorithmException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public static boolean test(BloomFilter filter, Transaction tx) {
        TR.enter();
        if (filter.check(tx.hash().toArray())) {
            return TR.exit(true);
        }
        //if (tx.Outputs.Any(p => filter.Check(p.ScriptHash.ToArray())))
        if (Arrays.stream(tx.outputs).anyMatch(p -> filter.check(p.scriptHash.toArray()))) {
            return TR.exit(true);
        }
        //if (tx.Inputs.Any(p => filter.Check(p.ToArray())))
        if (Arrays.stream(tx.inputs).anyMatch(p -> filter.check(SerializeHelper.toBytes(p)))) {
            return TR.exit(true);
        }
        //if (tx.Witnesses.Any(p => filter.Check(p.ScriptHash.ToArray())))
        if (Arrays.stream(tx.witnesses).anyMatch(p -> filter.check(p.scriptHash().toArray()))) {
            return TR.exit(true);
        }
        if (tx instanceof RegisterTransaction) {
            if (filter.check(((RegisterTransaction) tx).admin.toArray())) {
                return TR.exit(true);
            }
        }
        return TR.exit(false);
    }

    public static byte[] toAesKey(String password) {
        try {
            TR.enter();
            byte[] passwordBytes = password.getBytes("UTF-8");
            byte[] passwordHash = sha256(passwordBytes);
            byte[] passwordHash2 = sha256(passwordHash);
            Arrays.fill(passwordBytes, (byte) 0);
            Arrays.fill(passwordHash, (byte) 0);
            return TR.exit(passwordHash2);
        } catch (UnsupportedEncodingException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }
}