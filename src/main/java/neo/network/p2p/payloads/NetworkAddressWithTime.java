package neo.network.p2p.payloads;

import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;
import neo.log.notr.TR;

/**
 * The node address and the recent active time
 */
public class NetworkAddressWithTime implements ISerializable {

//    TODO waiting for network

    /**
     * The node type constant: the normal network node. There are many types of nodes in the Bitcoin
     * network, compared to the NEO network, which currently has only one type of node.
     */
    public static Ulong NODE_NETWORK = new Ulong(1);

    /**
     * The recent active time which begin calculation from the EPOCH(1970/1/1 00:00:00), and the
     * unit is second
     */
    public Uint timestamp;

    /**
     * The node type. Currently NEO only has the normal network node
     */
    public Ulong services;

    /**
     * The address info of node, including the IP address and port
     */
    // TODO java
//    public IPEndPoint EndPoint;

    /**
     * Get the length of data when transfer
     */
    @Override
    public int size() {
        TR.enter();
        // C# code:  sizeof(uint) + sizeof(ulong) + 16 + sizeof(ushort);
        return TR.exit(Uint.BYTES + Ulong.BYTES + 16 + Ushort.BYTES);
    }

    /**
     * serialization
     *
     * @param writer The binary output writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeUint(timestamp);
        writer.writeUlong(services);
        TR.exit();
//        writer.Write(EndPoint.Address.MapToIPv6().GetAddressBytes());
//        writer.write(BitConverter.getBytes((ushort) EndPoint.Port).Reverse().ToArray());
    }


    /**
     * Deserialization
     *
     * @param reader The binary input reader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        timestamp = reader.readUint();
        services = reader.readUlong();
        byte[] data = reader.readFully(16);
        if (data.length != 16) throw new FormatException();
        TR.exit();
        // C# code
        //        IPAddress address = new IPAddress(data).Unmap();
        //        data = reader.ReadBytes(2);
        //        if (data.Length != 2) throw new FormatException();
        //        ushort port = data.Reverse().ToArray().ToUInt16(0);
        //        EndPoint = new IPEndPoint(address, port);
    }

    /**
     * Create a networkandaddressTime object
     *
//     * @param endpoint  address information
     * @param services  service type
     * @param timestamp The recent activity time
     * @return an addressWithTime object
     */
    public static NetworkAddressWithTime create(Ulong services, Uint timestamp) {
        TR.enter();
        NetworkAddressWithTime time = new NetworkAddressWithTime();
        time.timestamp = timestamp;
        time.services = services;
        //        time.endpoint = endpoint;
        return TR.exit(time);
    }
}
