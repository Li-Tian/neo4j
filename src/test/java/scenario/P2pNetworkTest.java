package scenario;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.io.Tcp;
import akka.io.TcpConnection;
import akka.io.TcpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;
import neo.UInt256;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;
import neo.io.SerializeHelper;
import neo.ledger.Blockchain;
import neo.network.p2p.Connection;
import neo.network.p2p.LocalNode;
import neo.network.p2p.Message;
import neo.network.p2p.Peer;
import neo.network.p2p.payloads.GetBlocksPayload;
import neo.network.p2p.payloads.HeadersPayload;
import neo.network.p2p.payloads.InvPayload;
import neo.network.p2p.payloads.VersionPayload;

/**
 * P2p connection test case
 *
 * @docs MyPeer as the LocalNode(server) MyRemoteNode as the RemoteNode(client)
 *
 * MyPeer --------- MyRemoteNode
 * <code>
 * <-- version -- --- version -->
 *
 * ---- verack --> <---- verack ---
 *
 * <---- getheaders --- ----- headers ----->
 * </code>
 */
public class P2pNetworkTest {

    private static ActorSystem actorSystem = ActorSystem.create("test");

    protected static Gson gson = new GsonBuilder().create();
    protected static String host = "localhost";
    protected static int port = 10339;

    private static boolean done = false;

    @Test
    public void testP2pNetwork() throws InterruptedException {
        // start a local node
        ActorRef server = actorSystem.actorOf(MyServer.props());

        // start a remote node
        ActorRef actorRef = actorSystem.actorOf(MyPeer.props());
        Peer.Connect connect = new Peer.Connect() {{
//            endPoint = new InetSocketAddress("seed1.neo.org", 10333);
//            endPoint = new InetSocketAddress("localhost", 10333);
            endPoint = new InetSocketAddress(host, port);
            isTrusted = true;
        }};
        actorRef.tell(connect, ActorRef.noSender());

        while (!done){
            Thread.sleep(1000 * 1);
        }
    }

    static class MyServer extends AbstractActor {

        public static Props props() {
            return Props.create(MyServer.class);
        }

        @Override
        public void preStart() {
            final ActorRef tcp = Tcp.get(getContext().getSystem()).manager();
            tcp.tell(TcpMessage.bind(self(), new InetSocketAddress(host, port), 100), getSelf());
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(
                            Tcp.Bound.class,
                            msg -> {
                            })
                    .match(
                            Tcp.CommandFailed.class,
                            msg -> {
                                getContext().stop(self());
                            })
                    .match(
                            Tcp.Connected.class,
                            conn -> {
                                System.err.println("client connected success");
                                sender().tell(TcpMessage.register(self()), self());

                                VersionPayload versionPayload = VersionPayload.create(10333,
                                        LocalNode.NONCE,
                                        LocalNode.USER_AGENT,
                                        Uint.ZERO);
                                System.err.println("server start to send version message");
                                ByteString data = ByteString.fromArray(SerializeHelper.toBytes(Message.create("version", versionPayload)));
                                sender().tell(TcpMessage.write(data), getSelf());
                            })
                    .match(Tcp.Received.class,
                            msg -> {
                                final ByteString data = msg.data();
                                Message message = SerializeHelper.parse(Message::new, data.toArray());
                                System.err.println("server received msg: " + gson.toJson(message));
                                switch (message.command) {
                                    case "version":
                                        sender().tell(TcpMessage.write(ByteString.fromArray(SerializeHelper.toBytes(Message.create("verack")))), getSelf());
                                        break;
                                    case "getheaders":
                                        Blockchain.GenesisBlock.rebuildMerkleRoot();
                                        HeadersPayload payload = HeadersPayload.create(Collections.singleton(Blockchain.GenesisBlock.getHeader()));
                                        Message headersMsg = Message.create("headers", payload);
                                        System.err.println("send header msg: " + BitConverter.toHexString(SerializeHelper.toBytes(payload)));
                                        System.err.println("send header msg payload: " + BitConverter.toHexString(SerializeHelper.toBytes(payload)));
                                        sender().tell(TcpMessage.write(ByteString.fromArray(SerializeHelper.toBytes(headersMsg))), self());
                                        break;
                                    default:
                                        break;
                                }
                            })
                    .match(
                            Tcp.ConnectionClosed.class,
                            msg -> {
                                getContext().stop(getSelf());
                            })
                    .build();
        }
    }


    public static class MyPeer extends Peer {

        @Override
        protected void needMorePeers(int count) {
            ArrayList<InetSocketAddress> seeds = new ArrayList<>();
            seeds.add(new InetSocketAddress("seed1.neo.org", 10333));
            seeds.add(new InetSocketAddress("seed2.neo.org", 10333));
            seeds.add(new InetSocketAddress("seed3.neo.org", 10333));
            seeds.add(new InetSocketAddress("seed4.neo.org", 10333));
            unconnectedPeers.addAll(seeds);
        }

        public static Props props() {
            return Props.create(MyPeer.class);
        }

        @Override
        protected Props protocolProps(ActorRef connection, InetSocketAddress remote, InetSocketAddress local) {
            return MyRemoteNode.props(connection, remote, local);
        }
    }

    public static class MyRemoteNode extends Connection {

        private final Queue<Message> messageQueue = new LinkedBlockingQueue<>();
        private ByteString msgBuffer = ByteString.empty();
        public VersionPayload version;
        private HashSet<UInt256> invSet = new HashSet<>();

        protected MyRemoteNode(ActorRef tcp, InetSocketAddress remote, InetSocketAddress local) {
            super(tcp, remote, local);
            VersionPayload versionPayload = VersionPayload.create(8080,
                    LocalNode.NONCE,
                    LocalNode.USER_AGENT,
                    Uint.ONE);
            System.err.println("client send local version: " + gson.toJson(versionPayload));
            sendMessage(Message.create("version", versionPayload));
        }

        @Override
        public int getListenerPort() {
            return version.port.intValue();
        }

        @Override
        protected void onData(ByteString data) {
            msgBuffer = msgBuffer.concat(data);
            for (Message message = tryParseMessage(); message != null; message = tryParseMessage()) {
                System.err.println("client received " + message.command + ": " + gson.toJson(message));

                switch (message.command) {
                    case "version":
                        version = SerializeHelper.parse(VersionPayload::new, message.payload);
                        System.err.println("client received version: " + gson.toJson(version));
                        sendMessage(Message.create("verack"));
                        try {
                            Thread.sleep(1000 * 2);
                            sendGetHeader();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "verack":
                        checkMessageQueue();
                        break;
                    case "headers":
                        HeadersPayload payload = SerializeHelper.parse(HeadersPayload::new, message.payload);
                        System.err.println("client received headers: " + gson.toJson(payload));
                        Assert.assertEquals(1, payload.headers.length);
                        Assert.assertEquals(Blockchain.GenesisBlock.getHeader().hash(), payload.headers[0].hash());
                        done = true;
                        break;
                    case "inv":
                        InvPayload invPayload = SerializeHelper.parse(InvPayload::new, message.payload);
                        System.err.println("client received inv " + invPayload.type.name());
                        ArrayList<UInt256> notFound = new ArrayList<>(20);
                        for (UInt256 hash : invPayload.hashes) {
                            if (invSet.add(hash)) {
                                notFound.add(hash);
                            }
                        }
                        for (InvPayload payload1 : InvPayload.createGroup(invPayload.type, notFound)) {
                            System.err.println("start to send getdata message with hash: " + payload1.hashes[0].toString());
                            sender().tell(Message.create("getdata", payload1), self());
                        }
                    default:
                        break;
                }
            }
        }

        private void sendGetHeader() {
            GetBlocksPayload blocksPayload = new GetBlocksPayload() {{
                // test genesis block hash
                hashStart = new UInt256[]{UInt256.parse("d42561e3d30e15be6400b6df2f328e02d2bf6354c41dce433bc57687c82144bf")};
                hashStop = UInt256.Zero;
            }};
            sendMessage(Message.create("getheaders", blocksPayload));
        }


        private void enqueueMessage(String command, ISerializable payload) {
            enqueueMessage(Message.create(command, payload));
        }

        private void enqueueMessage(Message message) {
            System.err.println("received msg: " + gson.toJson(message));
            messageQueue.add(message);
            checkMessageQueue();
        }


        private void checkMessageQueue() {
            Queue<Message> queue = messageQueue;
            if (!queue.isEmpty()) {
                sendMessage(queue.poll());
            }
        }

        private Message tryParseMessage() {
            if (msgBuffer.size() < Uint.BYTES) {
                return null;
            }

            Uint magic = BitConverter.toUint(msgBuffer.slice(0, Uint.BYTES).toArray());
            if (!magic.equals(Message.Magic)) {
                throw new FormatException();
            }
            if (msgBuffer.size() < Message.HeaderSize) {
                return null;
            }

            // get the payload size
            int length = BitConverter.toInt(msgBuffer.slice(16, 16 + Uint.BYTES).toArray());
            if (length > Message.PayloadMaxSize) {
                throw new FormatException();
            }

            length += Message.HeaderSize;   // total size = header size + payload size
            if (msgBuffer.size() < length) {
                return null;
            }

            Message message = SerializeHelper.parse(Message::new, msgBuffer.slice(0, length).toArray());
            // C# code: msgBuffer = msgBuffer.slice(length).compact()
            msgBuffer = msgBuffer.drop(length).compact(); // drop the bytes which be read
            return message;
        }

        @Override
        protected void onAck() {
            checkMessageQueue();
        }

        private void sendMessage(Message message) {
            System.err.println("client start to send message: " + message.command + "  " + gson.toJson(message));
            sendData(ByteString.fromArray(SerializeHelper.toBytes(message)));
        }


        public static Props props(ActorRef tcp, InetSocketAddress remote, InetSocketAddress local) {
            return Props.create(MyRemoteNode.class, tcp, remote, local);
        }

        @Override
        public AbstractActor.Receive createReceive() {
            ReceiveBuilder builder = super.getReceiveBuilder();
            return builder
                    .match(TcpConnection.class, con -> sender().tell(TcpMessage.register(self()), self()))
                    .match(Message.class, msg -> enqueueMessage(msg))
                    .build();
        }

    }

}
