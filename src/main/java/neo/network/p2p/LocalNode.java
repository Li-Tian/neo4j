package neo.network.p2p;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import neo.NeoSystem;
import neo.ProtocolSettings;
import neo.csharp.Uint;
import neo.csharp.io.ISerializable;
import neo.exception.InvalidOperationException;
import neo.ledger.RelayResultReason;
import neo.log.notr.TR;
import neo.network.p2p.payloads.IInventory;
import neo.network.p2p.payloads.Transaction;

/**
 * A class describing the local node.It is a subclass of the Peer class
 */
public class LocalNode extends Peer {

    /**
     * Protocol Version, currently is fixed value Zero.
     */
    public static final Uint ProtocolVersion = Uint.ZERO;

    /**
     * Customized Akka message type that describes the data to be relayed, which will transfer to
     * inner Blockchain to handle this inventory.
     */
    public static class Relay {
        /**
         * Inventory data
         */
        public IInventory inventory;
    }


    /**
     * Relay the inventory to all the connected peer. Firstly, it will send inv message, then the
     * inventory message.
     */
    static public class RelayDirectly {
        public IInventory inventory;
    }

    /**
     * Send inventory to all the connected peers, be careful with the difference between
     * RelayDirectly and SendDirectly.
     */
    static class SendDirectly {
        public IInventory inventory;
    }

    private static final Object lockObj = new Object();
    private final NeoSystem system;
    private final ConcurrentHashMap<ActorRef, RemoteNode> remoteNodes = new ConcurrentHashMap<>();

    /**
     * Local node nonce, as the identity code
     */
    public static final Uint NONCE = new Uint(new Random().nextInt());

    /**
     * User agent as the description for the local node
     */
    public static final String USER_AGENT = String.format("/neo-java:/2.9.1");


    private static LocalNode singleton;

    /**
     * Get single LocalNode
     *
     * @return single localnode, it may be blocked when the LocalNode is not created.
     */
    public static LocalNode singleton() {
        while (singleton == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // just print the error and waiting
                TR.error(e);
            }
        }
        return singleton;
    }

    /**
     * constructor
     *
     * @param system NeoSystem object
     * @throws InvalidOperationException This object only allows one instance to be created,
     *                                   throwing an exception when creating the second instance
     */
    public LocalNode(NeoSystem system) {
        synchronized (lockObj) {
            if (singleton != null)
                throw new InvalidOperationException();
            this.system = system;
            singleton = this;

            //TODO 处理版本问题
            // C# code: UserAgent = $"/{Assembly.GetExecutingAssembly().GetName().Name}:{Assembly.GetExecutingAssembly().GetVersion()}/";
            // USER_AGENT = String.format("/neo-java:%s/2.9.1");
        }
    }

    /**
     * Get count of current connected peers
     *
     * @return the count of current connected peers
     */
    public int getConnectedCount() {
        return remoteNodes.size();
    }

    /**
     * get count of current unconnected peers.
     *
     * @return the count of current unconnected peers.
     */
    public int getUnconnectedCount() {
        return unconnectedPeers.size();
    }

    private void broadcastMessage(String command, ISerializable payload) {
        broadcastMessage(Message.create(command, payload));
    }

    private void broadcastMessage(Message message) {
        // C# code Connections.Tell(message);
        getConnections().tell(message, self());
    }

    /**
     * Read a specified number of InetSocketAdress from the specific hostNameOrAddress
     *
     * @param hostNameOrAddress host name or address
     * @param port              network port
     * @return InetSocketAddress
     */
    private static InetSocketAddress getIPEndpointFromHostPort(String hostNameOrAddress, int port) {
        try {
            InetAddress[] inetAddresses = InetAddress.getAllByName(hostNameOrAddress);
            if (inetAddresses != null && inetAddresses.length > 0) {
                return new InetSocketAddress(inetAddresses[0], port);
            }
            InetAddress inetAddress = InetAddress.getByAddress(hostNameOrAddress.trim().getBytes());
            return new InetSocketAddress(inetAddress, port);
        } catch (UnknownHostException e) {
            // just log the error, and return null
            TR.error(e);
            return null;
        }
        // C# code:
        //        InetAddress.pa()
        //        if (IPAddress.TryParse(hostNameOrAddress, out IPAddress ipAddress))
        //            return new IPEndPoint(ipAddress, port);
        //        IPHostEntry entry;
        //        try {
        //            entry = Dns.GetHostEntry(hostNameOrAddress);
        //        } catch (SocketException) {
        //            return null;
        //        }
        //        ipAddress = entry.AddressList.FirstOrDefault(p = > p.AddressFamily == AddressFamily.InterNetwork || p.IsIPv6Teredo)
        //        if (ipAddress == null) return null;
        //        return new IPEndPoint(ipAddress, port);
    }


    /**
     * Read a specified number of spare nodes from the seed node list
     *
     * @param seedsToTake specified number to read
     * @return spare nodes set
     */
    private static Collection<InetSocketAddress> getIPEndPointsFromSeedList(int seedsToTake) {
        ArrayList<InetSocketAddress> list = new ArrayList<>(20);

        if (seedsToTake <= 0) {
            return list;
        }

        List<String> seedList = ProtocolSettings.Default.seedList;
        Random rand = new Random();
        List<String> randomSeeds = rand.ints(seedList.size(), 0, seedList.size())
                .mapToObj(i -> seedList.get(i))
                .collect(Collectors.toList());
        // C# code: ProtocolSettings.Default.SeedList.OrderBy(p => rand.Next())

        for (String hostAndPort : randomSeeds) {
            if (seedsToTake == 0) break;

            String[] p = hostAndPort.split(":");
            InetSocketAddress seed;
            try {
                seed = getIPEndpointFromHostPort(p[0], Integer.parseInt(p[1]));
            } catch (Exception e) {
                // just ignore this exception
                continue;
            }
            if (seed == null) {
                continue;
            }
            list.add(seed);
            seedsToTake--;
        }
        return list;
    }

    /**
     * get remote node collection
     *
     * @return remote node collection
     */
    public Collection<RemoteNode> getRemoteNodes() {
        return remoteNodes.values();
    }

    /**
     * Register a remote node in the LocalNode
     *
     * @param remoteNode remote node
     */
    public void registerRemoteNode(RemoteNode remoteNode) {
        remoteNodes.put(remoteNode.self(), remoteNode);
    }


    /**
     * unregister remote node from the LocalNode
     *
     * @param remoteNode remote node
     */
    public void unregisterRemoteNode(RemoteNode remoteNode) {
        remoteNodes.remove(remoteNode.self());
    }


    /**
     * get unconnected node collection
     *
     * @return unconnected node collection
     */
    public Collection<InetSocketAddress> getUnconnectedPeers() {
        return unconnectedPeers;
    }


    /**
     * get more nodes
     * <ul>
     * <li>1, when the local node has a connection with other nodes, it will request a list of
     * unconected nodes</li>
     * <li>2, when the local node is not connected to other nodes, it will read the list of
     * unconected nodes from the configuration file.</li>
     * </ul>
     *
     * @param count demand count
     */
    @Override
    protected void needMorePeers(int count) {
        count = Math.max(count, 5);
        if (connectedPeers.size() > 0) {
            broadcastMessage("getaddr", null);
        } else {
            addPeers(getIPEndPointsFromSeedList(count));
        }
    }

    private void onRelay(IInventory inventory) {
        if (inventory instanceof Transaction) {
            Transaction transaction = (Transaction) inventory;
            // TODO waiting for consensus
            // system.Consensus ?.Tell(transaction);
        }
        system.blockchain.tell(inventory, self());
    }


    private void onRelayDirectly(IInventory inventory) {
        RemoteNode.Relay relay = new RemoteNode.Relay();
        relay.inventory = inventory;
        getConnections().tell(relay, self());
    }

    private void onSendDirectly(IInventory inventory) {
        getConnections().tell(inventory, self());
    }

    /**
     * build a LocalNode object（AKKA Framework）
     *
     * @param system NeoSystem object
     * @return LocalNode object
     */
    public static Props Props(NeoSystem system) {
        return Props.create(LocalNode.class, system);
    }


    /**
     * build a RemoteNode object
     *
     * @param connection a connection object
     * @param remote     IP and port of remote node
     * @param local      IP and port of local node
     * @return a AKKA reference to remote node objects
     */
    @Override
    protected Props protocolProps(ActorRef connection, InetSocketAddress remote, InetSocketAddress local) {
        return RemoteNode.props(system, connection, remote, local);
    }

    /**
     * create a message receiver
     *
     * @docs message customized message as the following:
     * <ul>
     * <li>Peer.Start: startup of the node</li>
     * <li>Peer.Timer: timer</li>
     * <li>Peer.Peers: adding a list of unconnected nodes</li>
     * <li>Peer.Connect: the connected node</li>
     * <li>Terminated: the actor close</li>
     * <li>Tcp.Bound: tcp bound</li>
     * <li>Tcp.CommandFailed: tcp command failed</li>
     * <li>Tcp.Connected: tcp connected</li>
     * <li>Message: neo p2p message</li>
     * <li>LocalNode.Relay: the data to be relayed</li>
     * <li>LocalNode.RelayDirectly: relay the inventory to all the connected peer</li>
     * <li>LocalNode.SendDirectly: send inventory to all the connected peers</li>
     * <li>LocalNode.RelayResultReason: the result of the relay message</li>
     * </ul>
     */
    @Override
    public AbstractActor.Receive createReceive() {
        return super.getReceiveBuilder()
                .match(Message.class, msg -> broadcastMessage(msg))
                .match(Relay.class, relay -> onRelay(relay.inventory))
                .match(RelayDirectly.class, relayDirectly -> onRelayDirectly(relayDirectly.inventory))
                .match(SendDirectly.class, sendDirectly -> onSendDirectly(sendDirectly.inventory))
                .match(RelayResultReason.class, msg -> { })
                .build();
    }


}
