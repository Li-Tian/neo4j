package neo.network.p2p.payloads;

import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.tr.TR;

/**
 * This class describing a transport packet when replying to the addr message after the node
 * receives the getaddr message from other nodes. The addr meesage contains the other nodes' IP
 * address
 */
public class AddrPayload implements ISerializable {

    /**
     * The max number of records sent at a time. The fixed value is 200.
     */
    public static final int MAX_COUNT_TO_SEND = 200;

    /**
     * other known node address information. Includes the IP address of these nodes, the listening
     * port, and the last active time.
     */
    public NetworkAddressWithTime[] addressList;


    /**
     * Get the  length of the entire packet. Unit: Byte
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(BitConverter.getVarSize(addressList));
    }

    /**
     * Serialize
     *
     * @param writer Binary writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeArray(addressList);
        TR.exit();
    }

    /**
     * Deserialize
     *
     * @param reader BinaryReader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        addressList = reader.readArray(NetworkAddressWithTime[]::new, NetworkAddressWithTime::new);
        TR.exit();
    }

    /**
     * Create an AddrPayload object
     *
     * @param addresses List of known nodes
     * @return an AddrPayload object
     */
    public static AddrPayload create(NetworkAddressWithTime[] addresses) {
        TR.enter();
        AddrPayload payload = new AddrPayload();
        payload.addressList = addresses;
        return TR.exit(payload);
    }
}
