package neo.network.p2p;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.io.Inet.SocketOption;
import akka.io.TcpSO;
import neo.common.GUID;
import neo.log.tr.TR;

/**
 * The peer class in the P2P network used by NEO. Describe the basic network functions of the node.
 */
public abstract class Peer extends UntypedActor {

    /**
     * Custom Akka message type, which represents the startup of the node, describes the node's
     * listening port, number of connections, etc.
     */
    public static class Start {
        /**
         * tcp/ip listening port
         */
        public int port;

        /**
         * min desired connection amount
         */
        public int minDesiredConnections;

        /**
         * max connection amount
         */
        public int maxConnections;
    }

    /**
     * Custom Akka message type, which represents adding a list of unconnected nodes, describing the
     * nodes that need to be added to the list of unconnected nodes.
     */
    public static class Peers {
        /**
         * a collection of peer nodes 's IP and Point
         */
        public Collection<InetSocketAddress> endPoints;
    }

    /**
     * Custom Akka message type, which represents the connection node, describes the IP and port
     * connected to the node, whether it is trusted, etc.
     */
    public static class Connect {
        /**
         * the IP and Point of the node which connects to.
         */
        public InetSocketAddress endPoint;

        /**
         * Determine if the connected node is trusted
         */
        public boolean isTrusted = false;
    }

    private static class Timer {
    }


    /**
     * Default min desired connection amount，default value is 10
     */
    public static final int DefaultMinDesiredConnections = 10;

    /**
     * Default max connection amount，default value is DefaultMinDesiredConnections*4
     */
    public static final int DefaultMaxConnections = DefaultMinDesiredConnections * 4;

    private static final int MaxConnectionsPerAddress = 3;

    private static ActorRef tcpManager = null;

    private ActorRef tcpListener;

    private Cancellable timer;  // C# is ICancelable

    private static final HashSet<InetAddress> localAddresses = new HashSet<>();
    private final HashMap<InetAddress, Integer> connectedAddresses = new HashMap<>();

    /**
     * Dictionary of active connections.Key is the reference to the active connection, and value is
     * the remote IP address and port number of the active connection.
     */
    protected final ConcurrentHashMap<ActorRef, InetSocketAddress> connectedPeers = new ConcurrentHashMap<>();

    /**
     * a unconnected known node set
     */
    protected HashSet<InetSocketAddress> unconnectedPeers = new HashSet<>(); // unmodifiable

    /**
     * a connecting known node set
     */
    protected HashSet<InetSocketAddress> connectingPeers = new HashSet<>(0); // unmodifiable


    /**
     * A collection of trusted IP addresses.  After the number of connections reaches the max amount
     * of connections, it is still allowed to create a connection to the  address in the
     * collection.
     */
    protected final HashSet<InetAddress> trustedIpAddresses = new HashSet<>(); // net to add get method


    private int listenerPort;
    private int minDesiredConnections = DefaultMinDesiredConnections;
    private int maxConnections = DefaultMaxConnections;

    /**
     * The max number of peers in the unconnected peer list. Default 1000
     */
    protected int unconnectedMax = 1000;


    /**
     * Constructor, it will init tcpManager and localAddresses when first created.
     */
    public Peer() {
        //c# code: tcpManager = Context.System.Tcp();

        // only execute once
        if (tcpManager == null) {
            tcpManager = Tcp.get(getContext().system()).manager();

            try {
                Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
                while (e.hasMoreElements()) {
                    NetworkInterface networkInterface = e.nextElement();
                    Enumeration<InetAddress> ee = networkInterface.getInetAddresses();
                    while (ee.hasMoreElements()) {
                        localAddresses.add(IpHelper.toIPv4(ee.nextElement()));
                    }
                }
            } catch (SocketException e1) {
                // just log the error
                TR.error(e1);
            }
        }
    }


    /**
     * max connecting amount
     */
    protected int getConnectingMax() {
        int allowedConnecting = minDesiredConnections * 4;
        if (maxConnections != -1 && allowedConnecting > maxConnections) {
            allowedConnecting = maxConnections;
        }
        return allowedConnecting - connectedPeers.size();
    }


    /**
     * Listening port
     *
     * @return Return this Peer object
     */
    public int getListenerPort() {
        return listenerPort;
    }

    /**
     * Min desired connection amount.Default value is 10.
     *
     * @return Min desired connection amount
     */
    public int getMinDesiredConnections() {
        return minDesiredConnections;
    }

    /**
     * Max connection amount
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * Get all active connections
     *
     * @return ActorSelection
     */
    protected ActorSelection getConnections() {
        // C# code
        //   protected ActorSelection connections; // =>Context.ActorSelection("connection_*");
        return context().actorSelection("connection_*");
    }


    /**
     * Add a peer collection to the list of local unconnected peers.
     */
    protected void addPeers(Collection<InetSocketAddress> peers) {
        if (unconnectedPeers.size() >= unconnectedMax) {
            return;
        }

        peers = peers.stream()
                .filter(p -> p.getPort() != listenerPort || !localAddresses.contains(p.getAddress()))
                .collect(Collectors.toList());

        synchronized (unconnectedPeers) {
            unconnectedPeers.addAll(peers);
            // C# code:  ImmutableInterlocked.Update(ref UnconnectedPeers, p => p.Union(peers));
        }
    }

    /**
     * Specify a Peer's IPEndPoint and try to connect.  If connect successfully, add it to the
     * connected peer list. If the Peer node is trusted, add the node's IP address to the local
     * trusted address list.
     *
     * @param endPoint a specify Peer's IPEndPoint
     */
    protected void connectToPeer(InetSocketAddress endPoint) {
        connectToPeer(endPoint, false);
    }


    /**
     * Specify a Peer's IPEndPoint and try to connect.  If connect successfully, add it to the
     * connected peer list. If the Peer node is trusted, add the node's IP address to the local
     * trusted address list.
     *
     * @param endPoint  a specify Peer's IPEndPoint
     * @param isTrusted whether the Peer node is trusted（Reserved), default is false.
     */
    protected void connectToPeer(InetSocketAddress endPoint, boolean isTrusted) {
        endPoint = IpHelper.toIPv4(endPoint);
        InetAddress address = endPoint.getAddress();

        if (endPoint.getPort() == listenerPort && localAddresses.contains(address)) {
            return;
        }
        if (isTrusted) {
            trustedIpAddresses.add(address);
        }
        if (connectedAddresses.containsKey(address)
                && connectedAddresses.get(address) >= MaxConnectionsPerAddress) {
            return;
        }
        if (connectedPeers.values().contains(endPoint)) {
            return;
        }

        synchronized (connectingPeers) {
            if ((connectingPeers.size() >= getConnectingMax() && !isTrusted)
                    || connectingPeers.contains(endPoint)) {
                // c# code return p
                return;
            }
            // C# code
            //  tcpManager.Tell(new Tcp.Connect(endPoint));
            tcpManager.tell(TcpMessage.connect(endPoint), getSelf());
            connectingPeers.add(endPoint);
        }
    }


    /**
     * Try to get more nodes
     *
     * @param count demand count
     */
    abstract protected void needMorePeers(int count);


    private void onStart(int port, int minDesiredConnections, int maxConnections) {
        this.listenerPort = port;
        this.minDesiredConnections = minDesiredConnections;
        this.maxConnections = maxConnections;

        ActorSystem system = context().system();
        timer = system.scheduler()
                .schedule(Duration.ZERO,
                        Duration.ofMillis(5000),
                        context().self(),
                        new Timer(),
                        system.dispatcher(),
                        ActorRef.noSender());

        if (port > 0) {
            Collection<SocketOption> options = Collections.singletonList(TcpSO.reuseAddress(true));
            InetSocketAddress address = new InetSocketAddress("localhost", port);
            Tcp.Command command = TcpMessage.bind(getSelf(), address, 100, options, false);
            tcpManager.tell(command, getSelf());
        }
    }

    private void onTcpConnected(InetSocketAddress remote, InetSocketAddress local) {
        synchronized (connectingPeers) {
            connectingPeers.remove(remote);
        }

        if (maxConnections != -1
                && connectedPeers.size() >= maxConnections
                && !trustedIpAddresses.contains(remote.getAddress())) {
            // C# Sender.Tell(Tcp.Abort.Instance);
            sender().tell(TcpMessage.abort(), getSelf());
            return;
        }
        InetAddress address = remote.getAddress();
        int count = connectedAddresses.containsKey(address) ? connectedAddresses.get(address) : 0;
        if (count >= MaxConnectionsPerAddress) {
            sender().tell(TcpMessage.abort(), getSelf());
        } else {
            connectedAddresses.put(address, count + 1);
            String actorName = String.format("connection_%s", GUID.newGuid());
            ActorRef connection = context().actorOf(protocolProps(getSender(), remote, local), actorName);
            context().watch(connection);
            sender().tell(TcpMessage.register(connection), getSelf());
            connectedPeers.put(connection, remote);
        }
    }

    private void onTcpCommandFailed(Tcp.Command cmd) {
        if (cmd instanceof Tcp.Connect) {
            Tcp.Connect connect = (Tcp.Connect) cmd;
            synchronized (connectingPeers) {
                InetSocketAddress remoteAddr = IpHelper.toIPv4(connect.remoteAddress());
                connectingPeers.remove(remoteAddr);
            }
        }
    }


    private void onTerminated(ActorRef actorRef) {
        InetSocketAddress endPoint = connectedPeers.remove(actorRef);
        if (endPoint != null) {
            int count = connectedAddresses.containsKey(endPoint) ? connectedAddresses.get(endPoint) : 0;
            if (count > 0) {
                count--;
            }
            if (count == 0) {
                connectedAddresses.remove(endPoint.getAddress());
            } else {
                connectedAddresses.put(endPoint.getAddress(), count);
            }
        }
    }

    private void onTimer() {
        if (connectedPeers.size() >= minDesiredConnections) {
            return;
        }
        if (unconnectedPeers.isEmpty()) {
            needMorePeers(minDesiredConnections - connectedPeers.size());
        }
        unconnectedPeers.stream()
                .limit(minDesiredConnections - connectedPeers.size())
                .forEach(inetSocketAddress -> {
                    synchronized (unconnectedPeers) {
                        unconnectedPeers.remove(inetSocketAddress);
                    }
                    connectToPeer(inetSocketAddress);
                });
    }


    /**
     * Stop delivering messages
     *
     * @throws Exception when dispose failed
     */
    @Override
    public void postStop() throws Exception {
        if (timer != null && !timer.isCancelled()) {
            timer.cancel();
        }
        if (tcpListener != null) {
            tcpListener.tell(TcpMessage.unbind(), getSelf());
        }
        super.postStop();
    }


    /**
     * Callback method for handling akka message
     *
     * @param message akka message
     */
    @Override
    public void onReceive(Object message) {
        if (message instanceof Start) {
            Start start = (Start) message;
            onStart(start.port, start.minDesiredConnections, start.maxConnections);
            return;
        }

        if (message instanceof Timer) {
            onTimer();
            return;
        }

        if (message instanceof Peers) {
            Peers peers = (Peers) message;
            addPeers(peers.endPoints);
            return;
        }

        if (message instanceof Connect) {
            Connect connect = (Connect) message;
            connectToPeer(connect.endPoint, connect.isTrusted);
            return;
        }

        if (message instanceof Tcp.Connected) {
            Tcp.Connected connected = (Tcp.Connected) message;
            InetSocketAddress remoteAddr = IpHelper.toIPv4(connected.remoteAddress());
            InetSocketAddress localAddr = IpHelper.toIPv4(connected.localAddress());
            onTcpConnected(remoteAddr, localAddr);
            return;
        }

        if (message instanceof Tcp.Bound) {
            tcpListener = getSender();
            return;
        }

        if (message instanceof Tcp.CommandFailed) {
            Tcp.CommandFailed commandFailed = (Tcp.CommandFailed) message;
            onTcpCommandFailed(commandFailed.cmd());
            return;
        }

        if (message instanceof Terminated) {
            Terminated terminated = (Terminated) message;
            onTerminated(terminated.getActor());
            return;
        }

    }

    /**
     * Create a RemoteNode object by an active TCP/IP connection and return its corresponding props
     * object
     *
     * @param connection active connection
     * @param remote     IP address of remote node
     * @param local      IP address of local node
     * @return corresponding props object
     */
    protected abstract Props protocolProps(ActorRef connection, InetSocketAddress remote, InetSocketAddress local);
}
