package neo.network.p2p;


import neo.ProtocolSettings;
import neo.cryptography.Crypto;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;
import neo.io.SerializeHelper;
import neo.log.notr.TR;

/**
 * an entity class describing the message structure used for data transmission in NEO p2p network
 */
public class Message implements ISerializable {

    /**
     * message header size
     */
    public static final int HeaderSize = Uint.BYTES + 12 + Integer.BYTES + Uint.BYTES;

    /**
     * message body size limitation
     */
    public static final int PayloadMaxSize = 0x02000000;

    /**
     * magic string，avoid network conflicts
     */
    public static final Uint Magic = ProtocolSettings.Default.magic;

    /**
     * command name。Fixed 12 bytes.When the length is insufficient, append 0x00
     */
    public String command;

    /**
     * Payload verification to avoid tampering and transmission errors
     */
    public Uint checksum;

    /**
     * The body content of the message varies according to the type of message
     */
    public byte[] payload;

    /**
     * Message size, equal to the size of the header + the size of the body
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(HeaderSize + payload.length);
    }


    /**
     * get message checksum by hash256
     *
     * @param value message
     * @return hash256 checksum
     */
    private static Uint getChecksum(byte[] value) {
        TR.enter();
        return TR.exit(BitConverter.toUint(Crypto.Default.hash256(value)));
    }

    /**
     * Serialize method
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeUint(Magic);
        writer.writeFixedString(command, 12);
        writer.writeInt(payload.length);
        writer.writeUint(checksum);
        writer.write(payload);
        TR.exit();
    }

    /**
     * Deserialize method
     *
     * @param reader BinaryReader
     * @throws FormatException it will throw this exception, when deserialize failed, eg: message
     *                         length over than the PayloadMaxSize or checksum is invalid.
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        if (!reader.readUint().equals(Magic)) {
            throw new FormatException();
        }

        this.command = reader.readFixedString(12);
        int length = reader.readInt();
        if (length > PayloadMaxSize) {
            throw new FormatException("message over the maximum limitation");
        }
        this.checksum = reader.readUint();
        this.payload = reader.readFully(length);

        if (!getChecksum(payload).equals(checksum)) {
            throw new FormatException("checksum verify failed");
        }
        TR.exit();
    }


    /**
     * create a message object
     *
     * @param command command
     * @return message object
     */
    public static Message create(String command) {
        TR.enter();
        return TR.exit(create(command, new byte[0]));
    }

    /**
     * create a message object
     *
     * @param command command
     * @param payload the body content of the message（object type data）
     * @return message object
     */
    public static Message create(String command, ISerializable payload) {
        TR.enter();
        if (payload == null) {
            return create(command, new byte[0]);
        }
        return TR.exit(create(command, SerializeHelper.toBytes(payload)));
    }

    /**
     * create a message object
     *
     * @param command command
     * @param payload the body content of the message（byte array type）
     * @return message object
     */
    public static Message create(String command, byte[] payload) {
        TR.enter();
        if (payload == null) {
            payload = new byte[0];
        }

        Message message = new Message();
        message.command = command;
        message.checksum = getChecksum(payload);
        message.payload = payload;
        return TR.exit(message);
    }
}
