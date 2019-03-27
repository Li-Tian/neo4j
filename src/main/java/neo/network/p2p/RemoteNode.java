package neo.network.p2p;

import com.typesafe.config.Config;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.io.Tcp;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;
import neo.NeoSystem;
import neo.UInt256;
import neo.cryptography.BloomFilter;
import neo.cryptography.Helper;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;
import neo.io.SerializeHelper;
import neo.io.actors.PriorityMailbox;
import neo.ledger.Blockchain;
import neo.log.tr.TR;
import neo.network.p2p.payloads.IInventory;
import neo.network.p2p.payloads.InvPayload;
import neo.network.p2p.payloads.InventoryType;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.VersionPayload;

/**
 * Describe the various properties and functions of the remote node.
 *
 * @docs 1. RemoteNode {@link RemoteNode}  receive network message by super class -- Connection
 * {@link Connection}, which will parse data and invoke the abstract onData method, then RemoteNode
 * will send the message to ProtocolHandler for dispatch. <br/> 2. RemoteNode also receive message
 * from the internal system, it will process these message by the message type, and send data to the
 * remote node through the tcp/ip network finally.
 */
public class RemoteNode extends Connection {

    /**
     * relay the inventory data
     */
    public static class Relay {
        public IInventory inventory;
    }

    private final NeoSystem system;
    private final ActorRef protocol;
    private final Queue<Message> messageQueueHigh = new LinkedBlockingQueue<>();
    private final Queue<Message> messageQueueLow = new LinkedBlockingQueue<>();
    private ByteString msgBuffer = ByteString.empty();
    private boolean ack = true;
    private BloomFilter bloomFilter;
    private boolean verack = false;

    /**
     * Record the current node's VERSION data and block height
     */
    public VersionPayload version;


    /**
     * constructor，create a RemoteNode object and sending the VERSION data of the local node to the
     * connected remote node.
     *
     * @param system Neo core system
     * @param tcp    a TCP/IP or WebSocket connection
     * @param remote IP and port of remote node
     * @param local  IP and port of local node
     */
    public RemoteNode(NeoSystem system, ActorRef tcp, InetSocketAddress remote, InetSocketAddress local) {
        super(tcp, remote, local);

        this.system = system;
        this.protocol = context().actorOf(ProtocolHandler.props(system));
        // register the remote node in the Localnode
        LocalNode.singleton().registerRemoteNode(this);

        VersionPayload versionPayload = VersionPayload.create(LocalNode.singleton().listenerPort,
                LocalNode.NONCE,
                LocalNode.USER_AGENT,
                Blockchain.singleton().getHeight());

        sendMessage(Message.create("version", versionPayload));
    }

    /**
     * build a RemoteNode object
     *
     * @param system     neo system
     * @param connection a connection object
     * @param remote     IP and port of remote node
     * @param local      IP and port of local node
     * @return a AKKA reference to remote node objects
     */
    static Props props(NeoSystem system, ActorRef connection, InetSocketAddress remote, InetSocketAddress local) {
        return Props.create(RemoteNode.class, system, connection, remote, local).withMailbox("remote-node-mailbox");
    }

    /**
     * IP and listening port of the remote node being monitored
     *
     * @return InetSocketAddress
     */
    public InetSocketAddress getListener() {
        TR.enter();
        return TR.exit(new InetSocketAddress(remote.getAddress(), getListenerPort()));
    }

    /**
     * listening port
     */
    @Override
    public int getListenerPort() {
        TR.enter();
        return TR.exit(version != null ? version.port.intValue() : 0);
    }

    /**
     * Parse the prepared data and send it through the actor reference
     *
     * @param data the prepared data
     */
    @Override
    protected void onData(ByteString data) {
        TR.enter();

        msgBuffer = msgBuffer.concat(data);
        for (Message message = tryParseMessage(); message != null; message = tryParseMessage()) {
            protocol.tell(message, self());
        }
        TR.exit();
    }

    private void enqueueMessage(String command, ISerializable payload) {
        TR.enter();
        enqueueMessage(Message.create(command, payload));
        TR.exit();
    }

    private void enqueueMessage(Message message) {
        TR.enter();

        boolean is_single = false;
        switch (message.command) {
            case "addr":
            case "getaddr":
            case "getblocks":
            case "getheaders":
            case "mempool":
                is_single = true;
                break;
        }
        Queue<Message> messageQueue;
        switch (message.command) {
            case "alert":
            case "consensus":
            case "filteradd":
            case "filterclear":
            case "filterload":
            case "getaddr":
            case "mempool":
                messageQueue = messageQueueHigh;
                break;
            default:
                messageQueue = messageQueueLow;
                break;
        }
        if (!is_single || messageQueue.stream().allMatch(p -> !message.command.equals(p.command))) {
            messageQueue.add(message);
        }
        checkMessageQueue();

        TR.exit();
    }


    private void checkMessageQueue() {
        TR.enter();

        if (!verack || !ack) {
            TR.exit();
            return;
        }
        Queue<Message> queue = messageQueueHigh;
        if (queue.isEmpty()) queue = messageQueueLow;
        if (queue.isEmpty()) {
            TR.exit();
            return;
        }
        sendMessage(queue.poll());

        TR.exit();
    }

    private Message tryParseMessage() {
        TR.enter();

        if (msgBuffer.size() < Uint.BYTES) {
            return TR.exit(null);
        }

        Uint magic = BitConverter.toUint(msgBuffer.slice(0, Uint.BYTES).toArray());
        if (!magic.equals(Message.Magic)) {
            throw new FormatException();
        }
        if (msgBuffer.size() < Message.HeaderSize) {
            return TR.exit(null);
        }

        // get the payload size
        int length = BitConverter.toInt(msgBuffer.slice(16, 16 + Uint.BYTES).toArray());
        if (length > Message.PayloadMaxSize) {
            throw new FormatException();
        }

        length += Message.HeaderSize;   // total size = header size + payload size
        if (msgBuffer.size() < length) {
            return TR.exit(null);
        }

        Message message = SerializeHelper.parse(Message::new, msgBuffer.slice(0, length).toArray());
        // C# code: msgBuffer = msgBuffer.slice(length).compact()
        msgBuffer = msgBuffer.drop(length).compact(); // drop the bytes which be read
        return TR.exit(message);
    }


    private void onRelay(IInventory inventory) {
        TR.enter();

        if (version == null || !version.relay) {
            TR.exit();
            return;
        }

        if (inventory.inventoryType() == InventoryType.Tx) {
            if (bloomFilter != null && !Helper.test(bloomFilter, (Transaction) inventory)) {
                TR.exit();
                return;
            }
        }

        UInt256[] hashes = new UInt256[]{inventory.hash()};
        enqueueMessage("inv", InvPayload.create(inventory.inventoryType(), hashes));

        TR.exit();
    }

    private void onSend(IInventory inventory) {
        TR.enter();

        if (version == null || !version.relay) {
            TR.exit();
            return;
        }
        if (inventory.inventoryType() == InventoryType.Tx) {
            if (bloomFilter != null && !Helper.test(bloomFilter, (Transaction) inventory)) {
                TR.exit();
                return;
            }
        }
        enqueueMessage(inventory.inventoryType().toString().toLowerCase(), inventory);

        TR.exit();
    }

    private void onSetFilter(BloomFilter filter) {
        TR.enter();

        bloomFilter = filter;

        TR.exit();
    }


    /**
     * A callback function to handle ack type message.   Set the ack flag to true, then check the
     * message in the message queue. and pop up and send the oldest message that first entered the
     * message queue.
     */
    @Override
    protected void onAck() {
        TR.enter();

        ack = true;
        checkMessageQueue();

        TR.exit();
    }

    private void onSetVerack() {
        TR.enter();

        verack = true;
        TaskManager.Register register = new TaskManager.Register();
        register.version = version;
        system.taskManager.tell(register, self());
        checkMessageQueue();

        TR.exit();
    }

    private void onSetVersion(VersionPayload version) {
        TR.enter();

        this.version = version;

        if (version.nonce.equals(LocalNode.NONCE)) {
            disconnect(true);
            TR.exit();
            return;
        }

        boolean repeatRemote = LocalNode.singleton()
                .getRemoteNodes()
                .stream()
                .filter(p -> p != this)
                .anyMatch(p -> p.remote.getAddress().equals(remote.getAddress())
                        && p.version != null
                        && p.version.nonce.equals(version.nonce));

        if (repeatRemote) {
            // connect repeat
            disconnect(true);
            TR.exit();
            return;
        }
        sendMessage(Message.create("verack"));

        TR.exit();
    }


    private void sendMessage(Message message) {
        TR.enter();

        ack = false;
        sendData(ByteString.fromArray(SerializeHelper.toBytes(message)));

        TR.exit();
    }

    /**
     * Stop the connection and data transmission,  and remove this remote node from the local node's
     * list of RemoteNodes
     */
    @Override
    public void postStop() throws Exception {
        TR.enter();

        LocalNode.singleton().unregisterRemoteNode(this);
        super.postStop();

        TR.exit();
    }


    /**
     * create a message receiver
     *
     * @docs message customized message as the following:
     * <ul>
     * <li>Connection.Timer: timer</li>
     * <li>Connection.Ack: tcp ack message received</li>
     * <li>Tcp.Received: data received </li>
     * <li>Tcp.ConnectionClosed: TCP connection is closed</li>
     * <li>Message: block message</li>
     * <li>IInventory: a new inv message received</li>
     * <li>Relay: relay the inventory data</li>
     * <li>ProtocolHandler.SetVersion:  the related remote node send VERSION message.</li>
     * <li>ProtocolHandler.SetVerack:  the related remote has been response to the `VERSION`
     * command
     * </li>
     * <li>ProtocolHandler.SetFilter:  received the related remote node about `filter` command
     * </li>
     * </ul>
     */
    @Override
    public AbstractActor.Receive createReceive() {
        TR.enter();

        ReceiveBuilder builder = super.getReceiveBuilder();
        return TR.exit(builder
                .match(Message.class, msg -> enqueueMessage(msg))
                .match(IInventory.class, inventory -> onSend(inventory))
                .match(RemoteNode.Relay.class, relay -> onRelay(relay.inventory))
                .match(ProtocolHandler.SetVersion.class, setVersion -> onSetVersion(setVersion.version))
                .match(ProtocolHandler.SetVerack.class, setVerack -> onSetVerack())
                .match(ProtocolHandler.SetFilter.class, setFilter -> onSetFilter(setFilter.filter))
                .build());
    }

    /**
     * Use OneForOneStrategy for anomalous child actor,and directly stop it
     *
     * @return Returns a SupervisorStrategy to handle the error of anomalous child actor
     */
    @Override
    public SupervisorStrategy supervisorStrategy() {
        TR.enter();

        return new OneForOneStrategy(10,
                Duration.ofMinutes(1),
                param -> {
                    disconnect(true);
                    return TR.exit(SupervisorStrategy.stop());
                });
    }


    /**
     * Remote priority mailbox. Only the Tcp.ConnectionClosed is high priority.
     */
    public static class RemoteNodeMailbox extends PriorityMailbox {

        public RemoteNodeMailbox(ActorSystem.Settings setting, Config config) {
            super();
        }

        @Override
        protected boolean isHighPriority(Object message) {
            TR.enter();

            if (message instanceof Tcp.ConnectionClosed
                    || message instanceof Connection.Timer) {
                return TR.exit(true);
            }
            return TR.exit(false);
        }
    }
}
