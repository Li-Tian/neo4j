package neo.network.p2p;

import java.net.InetSocketAddress;
import java.time.Duration;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;
import neo.log.notr.TR;

/**
 * An abstract class that describes a connection established between the local node and the remote
 * node.
 */
public abstract class Connection extends AbstractActor {

    static class Timer {
        public static Timer Instance = new Timer();
    }

    static class Ack implements Tcp.Event {
        public static Ack Instance = new Ack();
    }

    /**
     * IP and port of remote node
     */
    public final InetSocketAddress remote;

    /**
     * IP and port of local node
     */
    public final InetSocketAddress local;

    private Cancellable timer;
    private final ActorRef tcp;
    private boolean disconnected = false;


    /**
     * constructor
     *
     * @param connection a TCP/IP connection object or a WebSocket connection object
     * @param remote     IP and port of remote node
     * @param local      IP and port of local node
     */
    protected Connection(ActorRef connection, InetSocketAddress remote, InetSocketAddress local) {
        this.tcp = connection;
        this.remote = remote;
        this.local = local;
        this.timer = context().system().scheduler().schedule(Duration.ofSeconds(0),
                Duration.ofSeconds(10),
                getSelf(),
                Timer.Instance,
                context().system().dispatcher(),
                ActorRef.noSender());
    }

    /**
     * get listener port
     *
     * @return listener port
     */
    public abstract int getListenerPort();

    /**
     * Disconnect
     *
     * @param abort whether to stop directly
     */
    public void disconnect(boolean abort) {
        disconnected = true;
        if (tcp != null) {
            Object msg = abort ? (Tcp.CloseCommand) TcpMessage.abort() : TcpMessage.close();
            tcp.tell(msg, ActorRef.noSender());
        }
        context().stop(getSelf());
    }

    /**
     * Processing method when receiving an ACK signal transmitted by a TCP connection
     */
    protected void onAck() {
    }


    /**
     * Processing received data from the network
     *
     * @param data received data from the network
     */
    protected abstract void onData(ByteString data);


    /**
     * handle the network data
     *
     * @param data network transport data
     */
    private void onReceived(ByteString data) {
        if (!timer.isCancelled()) {
            timer.cancel();
        }
        timer = context().system().scheduler().schedule(Duration.ofSeconds(0),
                Duration.ofMinutes(1),
                getSelf(),
                Timer.Instance,
                context().system().dispatcher(),
                ActorRef.noSender());

        try {
            onData(data);
        } catch (Exception e) {
            // just catch the error ad log
            TR.error(e);
            disconnect(true);
        }
    }

    /**
     * Stop the connection and sending the data.(AKKA framework method)
     *
     * @throws Exception if execute failed, it will throw this exception
     */
    @Override
    public void postStop() throws Exception {
        if (!disconnected && tcp != null) {
            tcp.tell(TcpMessage.close(), ActorRef.noSender());
        }
        if (!timer.isCancelled()) {
            timer.cancel();
        }
        super.postStop();
    }

    /**
     * SendData
     *
     * @param data the data needed to be send
     */
    protected void sendData(ByteString data) {
        if (tcp != null) {
            tcp.tell(TcpMessage.write(data, Ack.Instance), getSelf());
        }
    }

    /**
     * create a message receiver, the message as following:
     * <ul>
     * <li>Connection.Timer: timer</li>
     * <li>Connection.Ack: tcp ack message received</li>
     * <li>Tcp.Received: data received </li>
     * <li>Tcp.ConnectionClosed: TCP connection is closed</li>
     * </ul>
     */
    @Override
    public Receive createReceive() {
        return getReceiveBuilder().build();
    }


    /**
     * get a receiver builder
     */
    protected ReceiveBuilder getReceiveBuilder() {
        return receiveBuilder()
                .match(Connection.Timer.class, timer -> disconnect(true))
                .match(Connection.Ack.class, ack -> onAck())
                .match(Tcp.Received.class, received -> onReceived(received.data()))
                .match(Tcp.ConnectionClosed.class, task -> context().stop(self()));
    }
}
