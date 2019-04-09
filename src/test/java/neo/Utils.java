package neo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Random;
import java.util.function.Supplier;

import neo.Wallets.KeyPair;
import neo.Wallets.WalletAccount;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

public class Utils {

    public static <T extends ISerializable> T copyFromSerialize(T serializable, Supplier<T> generator) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(output);
        serializable.serialize(writer);

        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        BinaryReader reader = new BinaryReader(input);
        T t = generator.get();
        t.deserialize(reader);
        return t;
    }

    public static KeyPair getRandomKeyPair() {
        byte[] privateKey = new byte[32];
        Random rng = new Random();

        rng.nextBytes(privateKey);
        return new KeyPair(privateKey);
    }


    public static void deleteFolder(String path) {
        File file = new File(path);
        if (file.exists()) {
            for (File subFile : file.listFiles()) {
                subFile.delete();
            }
            file.delete();
        }
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }
}
