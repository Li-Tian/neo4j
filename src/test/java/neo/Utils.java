package neo;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.function.Supplier;

import neo.wallets.KeyPair;
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

    public static byte[] readContentFromFile(String path) throws IOException {
        File file = new File(path);
        long fileSize = file.length();
        FileInputStream input = new FileInputStream(file);
        byte[] buffer = new byte[(int) fileSize];

        int offset = 0;
        int numRead = 0;
        while (offset < buffer.length
                && (numRead = input.read(buffer, offset, buffer.length - offset)) >= 0) {
            offset += numRead;
        }

        if (offset != buffer.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        input.close();
        return buffer;
    }
}
