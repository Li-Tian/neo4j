package neo;

import com.typesafe.config.ConfigFactory;

import java.net.InetSocketAddress;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import neo.wallets.Wallet;
import neo.consensus.ConsensusService;
import neo.ledger.Blockchain;
import neo.log.tr.TR;
import neo.network.p2p.LocalNode;
import neo.network.p2p.Peer;
import neo.network.p2p.TaskManager;
import neo.network.rpc.RpcServer;
import neo.persistence.Store;
import neo.plugins.Plugin;

/**
 * NEO core system class for controlling and running NEO functions
 */
public class NeoSystem {
    private Peer.Start start_message = null;
    private boolean suspend = false;

    public ActorSystem actorSystem = ActorSystem.create(NeoSystem.class.getSimpleName(),
            ConfigFactory.load("akka.conf"));

    public ActorRef blockchain;

    public ActorRef localNode;

    public ActorRef taskManager;

    public ActorRef consensus;

    public RpcServer rpcServer;

    public NeoSystem(Store store) {
        TR.enter();
        init(store);
        TR.exit();
    }

    public void init(Store store) {
        blockchain = actorSystem.actorOf(Blockchain.props(this, store));
        localNode = actorSystem.actorOf(LocalNode.props(this));
        taskManager = actorSystem.actorOf(TaskManager.props(this));
        Plugin.loadPlugins(this);
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
            localNode.tell(start_message, ActorRef.noSender());
            start_message = null;
        }
        TR.exit();
    }

    public void startConsensus(Wallet wallet) {
        TR.enter();
        consensus = actorSystem.actorOf(ConsensusService.props(this.localNode, this.taskManager, wallet));
        consensus.tell(new ConsensusService.Start(), ActorRef.noSender());
        TR.exit();
    }

    public void startNode(int inputPort, int inputMinDesiredConnections, int inputMaxConnections) {
        TR.enter();
        start_message = new Peer.Start() {
            {
                port = inputPort;
                minDesiredConnections = inputMinDesiredConnections;
                maxConnections = inputMaxConnections;
            }
        };
        if (!suspend) {
            localNode.tell(start_message, ActorRef.noSender());
            start_message = null;
        }
        TR.exit();
    }

    public void startRpc(InetSocketAddress bindAddress, Wallet wallet, String sslCert, String password,
                         String[] trustedAuthorities, Fixed8 maxGasInvoke)
    {
        rpcServer = new RpcServer(this, wallet, maxGasInvoke);
        rpcServer.start(bindAddress, sslCert, password, trustedAuthorities);
    }

    public void suspendNodeStartup() {
        TR.enter();
        suspend = true;
        TR.exit();
    }
}
