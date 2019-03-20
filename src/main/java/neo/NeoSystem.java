package neo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import neo.ledger.Blockchain;
import neo.log.tr.TR;
import neo.network.p2p.LocalNode;
import neo.network.p2p.Peer;
import neo.network.p2p.TaskManager;
import neo.network.RpcServer;
import neo.persistence.Store;
import neo.plugins.Plugin;

/**
 * NEO core system class for controlling and running NEO functions
 */
public class NeoSystem extends UntypedActor {
    private Peer.Start start_message = null;
    private boolean suspend = false;
    public ActorSystem actorSystem = ActorSystem.create(NeoSystem.class.getSimpleName());

    public ActorRef blockchain;

    public ActorRef localNode;

    public ActorRef taskManager;

    public ActorRef consensus;

    public RpcServer rpcServer;

    public NeoSystem(Store store) {
        TR.enter();
        blockchain = actorSystem.actorOf(Blockchain.props(this, store));
        localNode = ActorSystem.actorOf(LocalNode.props(this));
        taskManager = ActorSystem.actorOf(TaskManager.props(this));
        Plugin.loadPlugins(this);
        TR.exit();
    }

    public void dispose() {
        TR.enter();
        if (rpcServer != null) {
            rpcServer.dispose();
        }
        actorSystem.stop(localNode);
        actorSystem.terminate();
        TR.exit();
    }

    public void resumeNodeStartup() {
        TR.enter();
        suspend = false;
        if (start_message != null) {
            localNode.tell(start_message, self());
            start_message = null;
        }
        TR.exit();
    }

    public void startConsensus(Wallet wallet) {
        TR.enter();
        consensus = ActorSystem.actorOf(ConsensusService.Props(this.localNode, this.taskManager, wallet));
        consensus.tell(new ConsensusService.start());
        TR.exit();
    }

    public void startNode(int inputPort, int inputWSPort, int inputMinDesiredConnections, int inputMaxConnections) {
        TR.enter();
        start_message = new Peer.Start() {
            {
                port = inputPort;
                wsPort = inputWSPort;
                minDesiredConnections = inputMinDesiredConnections;
                maxConnections = inputMaxConnections;
            }
        };
        if (!suspend) {
            localNode.tell(start_message, self());
            start_message = null;
        }
        TR.exit();
    }

    public void startRpc(IPAddress bindAddress, int port, Wallet wallet, String sslCert, String password,
                         String[] trustedAuthorities, Fixed8 maxGasInvoke)
    {
        TR.enter();
        rpcServer = new RpcServer(this, wallet, maxGasInvoke);
        rpcServer.start(bindAddress, port, sslCert, password, trustedAuthorities);
        TR.exit();
    }

    private void suspendNodeStartup() {
        TR.enter();
        suspend = true;
        TR.exit();
    }

    @Override
    public void onReceive(Object message) {
        TR.enter();
        TR.exit();
    }
}
