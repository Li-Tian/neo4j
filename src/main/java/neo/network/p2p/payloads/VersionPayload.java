package neo.network.p2p.payloads;

import neo.TimeProvider;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.notr.TR;
import neo.network.p2p.LocalNode;

/**
 * Record VERSION data and block height
 */
public class VersionPayload implements ISerializable {

    /**
     * The VERSION number
     */
    public Uint version;

    /**
     * The descriptor of the function of node. The fixed value is one
     */
    public Ulong services;

    /**
     * The timestamp, which is the seconds from the epoch time
     */
    public Uint timestamp;

    /**
     * The listening port of server side
     */
    public Ushort port;

    /**
     * A random number which stands for localNode
     */
    public Uint nonce;

    /**
     * The name of the node software and the description of VERSION
     */
    public String userAgent;

    /**
     * The height of block
     */
    public Uint startHeight;

    /**
     * If has the relay function. The default value is true
     */
    public boolean relay;

    /**
     * Size of data block
     */
    @Override
    public int size() {
        TR.enter();
        //C# code Size => sizeof(uint) + sizeof(ulong) + sizeof(uint) + sizeof(ushort) + sizeof(uint)
        // + UserAgent.GetVarSize() + sizeof(uint) + sizeof(bool);
        // 4 + 8 + 4 + 2 + 4 + usergent + 4 + 1 =
        return TR.exit(Uint.BYTES + Ulong.BYTES + Uint.BYTES + Ushort.BYTES + Uint.BYTES
                + BitConverter.getVarSize(userAgent) + Uint.BYTES + Byte.BYTES);
    }

    /**
     * Serialization
     *
     * @param writer The binary output writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeUint(version);
        writer.writeUlong(services);
        writer.writeUint(timestamp);
        writer.writeUshort(port);
        writer.writeUint(nonce);
        writer.writeVarString(userAgent);
        writer.writeUint(startHeight);
        writer.writeBoolean(relay);
        TR.exit();
    }

    /**
     * Deserialization
     *
     * @param reader binary input reader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        version = reader.readUint();
        services = reader.readUlong();
        timestamp = reader.readUint();
        port = reader.readUshort();
        nonce = reader.readUint();
        userAgent = reader.readVarString(1024);
        startHeight = reader.readUint();
        relay = reader.readBoolean();
        TR.exit();
    }

    /**
     * Build a VERSION payload object
     *
     * @param port        The port of listener
     * @param nonce       The random number of local node
     * @param userAgent   The name of node software and the description of the VERSION
     * @param startHeight The height of blocks
     * @return The VersionPayload build with these parameter
     */
    public static VersionPayload create(int port, Uint nonce, String userAgent, Uint startHeight) {
        TR.enter();
        VersionPayload payload = new VersionPayload();
        payload.version = LocalNode.ProtocolVersion;
        payload.services = NetworkAddressWithTime.NODE_NETWORK;
        //  DateTime.Now.ToTimestamp(),
        payload.timestamp = new Uint(Long.valueOf(TimeProvider.current().utcNow().getTime()).intValue());
        payload.port = new Ushort(port);
        payload.nonce = nonce;
        payload.userAgent = userAgent;
        payload.startHeight = startHeight;
        payload.relay = true;
        return TR.exit(payload);
    }
}
