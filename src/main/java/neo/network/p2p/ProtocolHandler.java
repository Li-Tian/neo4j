package neo.network.p2p;

import com.typesafe.config.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import neo.NeoSystem;
import neo.UInt256;
import neo.cryptography.BloomFilter;
import neo.cryptography.Helper;
import neo.csharp.Uint;
import neo.exception.ProtocolViolationException;
import neo.io.SerializeHelper;
import neo.io.actors.PriorityMailbox;
import neo.io.caching.DataCache;
import neo.ledger.BlockState;
import neo.ledger.Blockchain;
import neo.network.p2p.payloads.AddrPayload;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.ConsensusPayload;
import neo.network.p2p.payloads.FilterAddPayload;
import neo.network.p2p.payloads.FilterLoadPayload;
import neo.network.p2p.payloads.GetBlocksPayload;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.HeadersPayload;
import neo.network.p2p.payloads.IInventory;
import neo.network.p2p.payloads.InvPayload;
import neo.network.p2p.payloads.MerkleBlockPayload;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.NetworkAddressWithTime;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.VersionPayload;
import neo.persistence.Snapshot;

import static neo.network.p2p.payloads.InventoryType.Block;

/**
 * Neo network protocol handler, for more detail, @see
 * <a href="https://docs.neo.org/developerguide/en/articles/network_protocol.html">
 * https://docs.neo.org/developerguide/en/articles/network_protocol.html</a>
 */
public class ProtocolHandler extends AbstractActor {

    /**
     * Customized akka message, it means the related remote node send version message. and this
     * message will be transfer to RemoteNode{@link RemoteNode} for processing.
     */
    public static class SetVersion {
        public VersionPayload version;
    }

    /**
     * Customized akka message, it means the related remote has been response to the `version`
     * command. Also this message will be send to RemoteNode{@link RemoteNode} for processing.
     */
    public static class SetVerack {
    }

    /**
     * Customized akka message, it means local node received the related remote node about `filter`
     * command. Also this message will be send to RemoteNode{@link RemoteNode} for processing.
     */
    public static class SetFilter {
        public BloomFilter filter;
    }

    private final NeoSystem system;

    // inventory data received already
    private final HashSet<UInt256> knownHashes = new HashSet<>();

    // inventory data send already
    private final HashSet<UInt256> sentHashes = new HashSet<>();

    private VersionPayload version;
    private boolean verack = false;
    private BloomFilter bloomFilter;


    /**
     * Constructor, build a neo network protocol handler
     *
     * @param system neo akka system
     */
    public ProtocolHandler(NeoSystem system) {
        this.system = system;
    }


    private void onAddrMessageReceived(AddrPayload payload) {
        Peer.Peers peers = new Peer.Peers() {{
            endPoints = Arrays.stream(payload.addressList)
                    .map(p -> p.endPoint)
                    .collect(Collectors.toList());
        }};
        system.localNode.tell(peers, self());
    }

    private void onFilterAddMessageReceived(FilterAddPayload payload) {
        if (bloomFilter != null)
            bloomFilter.add(payload.data);
    }

    private void onFilterClearMessageReceived() {
        bloomFilter = null;
        SetFilter setFilter = new SetFilter() {
            {
                filter = null;
            }
        };
        context().parent().tell(setFilter, self());
    }

    private void onFilterLoadMessageReceived(FilterLoadPayload payload) {
        bloomFilter = new BloomFilter(payload.filter.length * 8, payload.k, payload.tweak, payload.filter);
        SetFilter filter = new SetFilter() {{
            filter = bloomFilter;
        }};
        context().parent().tell(filter, self());
    }

    private void onGetAddrMessageReceived() {
        Collection<RemoteNode> peers = LocalNode.singleton().getRemoteNodes().stream()
                .filter(p -> p.getListenerPort() > 0)
                .collect(Collectors.groupingBy(p -> p.remote.getAddress()))
                .values()
                .stream()
                .sorted((a, b) -> ThreadLocalRandom.current().nextInt(-1, 2))
                .limit(AddrPayload.MAX_COUNT_TO_SEND)
                .map(p -> p.get(0))
                .collect(Collectors.toList());

        // C# code
        //    IEnumerable<RemoteNode> peers = LocalNode.Singleton.RemoteNodes.Values
        //          .Where(p => p.ListenerPort > 0)
        //          .GroupBy(p => p.Remote.Address, (k, g) => g.First())
        //          .OrderBy(p => rand.Next())
        //          .Take(AddrPayload.MaxCountToSend);

        NetworkAddressWithTime[] networkAddresses = peers.stream()
                .map(p -> NetworkAddressWithTime.create(p.getListener(), p.version.services, p.version.timestamp))
                .toArray(NetworkAddressWithTime[]::new);
        if (networkAddresses.length == 0) {
            return;
        }
        context().parent().tell(Message.create("addr", AddrPayload.create(networkAddresses)), self());
    }

    private void onGetBlocksMessageReceived(GetBlocksPayload payload) {
        UInt256 hash = payload.hashStart[0];
        if (hash.equals(payload.hashStop)) {
            return;
        }

        Blockchain blockchain = Blockchain.singleton();

        BlockState state = blockchain.getStore().getBlocks().tryGet(hash);
        if (state == null) {
            return;
        }

        ArrayList<UInt256> hashes = new ArrayList<>();
        for (int i = 1; i <= InvPayload.MaxHashesCount; i++) {
            Uint index = state.trimmedBlock.index.add(new Uint(i));
            if (index.compareTo(blockchain.getHeight()) > 0) {
                break;
            }

            hash = blockchain.getBlockHash(index);
            if (hash == null || hash.equals(payload.hashStop)) {
                break;
            }

            hashes.add(hash);
        }
        if (hashes.isEmpty()) {
            return;
        }

        InvPayload invPayload = InvPayload.create(Block, hashes.toArray(new UInt256[hashes.size()]));
        Message message = Message.create("inv", invPayload);
        context().parent().tell(message, self());
    }


    private void onGetDataMessageReceived(InvPayload payload) {
        for (UInt256 hash : payload.hashes) {
            if (!sentHashes.contains(hash)) {
                continue;
            }

            Blockchain blockchain = Blockchain.singleton();
            IInventory inventory = blockchain.relayCache.tryGet(hash);

            switch (payload.type) {
                case Tx:
                    if (inventory == null) {
                        inventory = blockchain.getTransaction(hash);
                    }
                    if (inventory instanceof Transaction) {
                        context().parent().tell(Message.create("tx", inventory), self());
                    }
                    break;
                case Block:
                    if (inventory == null) {
                        inventory = blockchain.getBlock(hash);
                    }
                    if (inventory instanceof Block) {
                        if (bloomFilter == null) {
                            context().parent().tell(Message.create("block", inventory), self());
                        } else {
                            // C# code:
                            // BitArray flags = new BitArray(block.Transactions.Select(p => bloom_filter.Test(p)).ToArray());
                            Block block = (neo.network.p2p.payloads.Block) inventory;

                            int size = block.transactions.length;
                            BitSet flags = new BitSet(size);
                            for (int i = 0; i < size; i++) {
                                flags.set(i, Helper.test(bloomFilter, block.transactions[i]));
                            }

                            Message message = Message.create("block", MerkleBlockPayload.create(block, flags));
                            context().parent().tell(message, self());
                        }
                    }
                    break;
                case Consensus:
                    if (inventory != null) {
                        Message message = Message.create("consensus", inventory);
                        context().parent().tell(message, self());
                    }
                    break;
            }
        }
    }


    private void onGetHeadersMessageReceived(GetBlocksPayload payload) {
        UInt256 hash = payload.hashStart[0];
        if (hash.equals(payload.hashStop)) {
            return;
        }
        Blockchain blockchain = Blockchain.singleton();
        DataCache<UInt256, BlockState> cache = blockchain.getStore().getBlocks();
        BlockState state = cache.tryGet(hash);
        if (state == null) {
            return;
        }

        ArrayList<Header> headers = new ArrayList<>();
        for (int i = 1; i <= HeadersPayload.MaxHeadersCount; i++) {
            int index = state.trimmedBlock.index.intValue() + i;

            hash = blockchain.getBlockHash(new Uint(index));
            if (hash == null || hash.equals(payload.hashStop)) {
                break;
            }

            BlockState blockState = cache.tryGet(hash);
            if (blockState != null) {
                break;
            }
            Header header = blockState.trimmedBlock.getHeader();
            if (header == null) {
                break;
            }
            headers.add(header);
        }
        if (headers.isEmpty()) {
            return;
        }

        Message message = Message.create("headers", HeadersPayload.create(headers));
        context().parent().tell(message, self());
    }

    private void onHeadersMessageReceived(HeadersPayload payload) {
        if (payload.headers.length == 0) {
            return;
        }
        system.blockchain.tell(payload.headers, context().parent());
    }


    private void onInventoryReceived(IInventory inventory) {
        TaskManager.TaskCompleted cmd = new TaskManager.TaskCompleted();
        cmd.hash = inventory.hash();
        system.taskManager.tell(cmd, context().parent());

        if (inventory instanceof MinerTransaction) {
            return;
        }
        LocalNode.Relay relay = new LocalNode.Relay();
        relay.inventory = inventory;
        system.localNode.tell(relay, self());
    }


    private void onInvMessageReceived(InvPayload payload) {
        // C# code UInt256[] hashes = payload.hashes.Where(p = > knownHashes.Add(p)).ToArray();
        UInt256[] hashes = Arrays.stream(payload.hashes)
                .filter(p -> knownHashes.add(p))
                .toArray(UInt256[]::new);

        if (hashes.length == 0) {
            return;
        }

        Snapshot snapshot = Blockchain.singleton().getStore().getSnapshot();
        switch (payload.type) {
            case Block:
                hashes = Arrays.stream(hashes).filter(p -> !snapshot.containsBlock(p)).toArray(UInt256[]::new);
                break;
            case Tx:
                hashes = Arrays.stream(hashes).filter(p -> !snapshot.containsTransaction(p)).toArray(UInt256[]::new);
                break;
            default:
                break;
        }

        if (hashes.length == 0) {
            return;
        }
        TaskManager.NewTasks newTasks = new TaskManager.NewTasks();
        newTasks.payload = InvPayload.create(payload.type, hashes);
        system.taskManager.tell(newTasks, context().parent());
    }

    private void onMemPoolMessageReceived() {
        // TODO waiting for mempool
//        foreach (InvPayload payload in InvPayload.CreateGroup(InventoryType.TX, Blockchain.Singleton.MemPool.GetVerifiedTransactions().Select(p => p.Hash).ToArray()))
//        Context.Parent.Tell(Message.Create("inv", payload));
    }

    private void onVerackMessageReceived() {
        verack = true;
        context().parent().tell(new SetVerack(), self());
    }

    private void onVersionMessageReceived(VersionPayload payload) {
        version = payload;
        SetVersion setVersion = new SetVersion() {{
            version = payload;
        }};
        context().parent().tell(setVersion, self());
    }


    /**
     * build a ProtocolHandler object
     *
     * @param system neo system
     * @return a AKKA reference to ProtocolHandler objects
     */
    public static Props props(NeoSystem system) {
        return Props.create(ProtocolHandler.class, system).withMailbox("protocol-handler-mailbox");
    }


    /**
     * ProtocolHandler priority mailbox, high priority commands are: consensus, filteradd,
     * filterclear, filterload, verack, version, alert(unimplemented). the others are low priority.
     */
    public static class ProtocolHandlerMailbox extends PriorityMailbox {

        public ProtocolHandlerMailbox(ActorSystem.Settings setting, Config config) {
            super();
        }

        @Override
        protected boolean isHighPriority(Object object) {
            if (!(object instanceof Message)) {
                return true;
            }

            Message message = (Message) object;
            switch (message.command) {
                case "consensus":
                case "filteradd":
                case "filterclear":
                case "filterload":
                case "verack":
                case "version":
                case "alert":
                    return true;
                default:
                    return false;
            }
        }

        @Override
        protected boolean shallDrop(Object object, Collection<Object> queue) {
            if (!(object instanceof Message)) {
                return false;
            }

            Message message = (Message) object;
            switch (message.command) {
                case "getaddr":
                case "getblocks":
                case "getdata":
                case "getheaders":
                case "mempool":
                    // C# code: return queue.OfType < Message > ().Any(p = > p.Command == msg.Command);
                    return queue.stream().anyMatch(p -> (p instanceof Message) && (((Message) p).command.equals(message.command)));
                default:
                    return false;
            }
        }
    }


    /**
     * create a receiver builder
     *
     * @return Receive
     */
    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder().match(Message.class, msg -> handleMsg(msg)).build();
    }

    private void handleMsg(Message msg) {
        if (version == null) {
            if (!"version".equals(msg.command)) {
                throw new ProtocolViolationException();
            }
            onVersionMessageReceived(SerializeHelper.parse(VersionPayload::new, msg.payload));
            return;
        }

        if (!verack) {
            if (!"verack".equals(msg.command)) {
                throw new ProtocolViolationException();
            }
            onVerackMessageReceived();
            return;
        }

        switch (msg.command) {
            case "addr":
                // Answers getaddr message with at most 200 records of succesfully connected node addresses and port numbers.
                onAddrMessageReceived(SerializeHelper.parse(AddrPayload::new, msg.payload));
                break;

            case "block":
                // Answers getdata message with Block of specified hash.
                onInventoryReceived(SerializeHelper.parse(Block::new, msg.payload));
                break;

            case "consensus": // Answers getdata message with consensus data of specified hash.
                onInventoryReceived(SerializeHelper.parse(ConsensusPayload::new, msg.payload));
                break;

            case "filteradd": // Adds data to bloom_filter for SPV wallet.
                onFilterAddMessageReceived(SerializeHelper.parse(FilterAddPayload::new, msg.payload));
                break;

            case "filterclear": // Remove bloom_filter for SPV wallet.
                onFilterClearMessageReceived();
                break;

            case "filterload": // Initialize bloom_filter for SPV wallet.
                onFilterLoadMessageReceived(SerializeHelper.parse(FilterLoadPayload::new, msg.payload));
                break;

            case "getaddr": // Queries address and port numer of other nodes.
                onGetAddrMessageReceived();
                break;

            case "getblocks": // Specify the start and end hash values ​​to get the details of several consecutive blocks.
                onGetBlocksMessageReceived(SerializeHelper.parse(GetBlocksPayload::new, msg.payload));
                break;

            case "getdata":
                /*
                    Queries other nodes for Inventory objects of specified type and hash.
                    Current usage:
                    1)Sending get-transaction query during consensus process.
                    2)Sending getdata message upon receiving inv message.
                 */
                onGetDataMessageReceived(SerializeHelper.parse(InvPayload::new, msg.payload));
                break;

            case "getheaders": // Node with fewer information queries for block head after two nodes establish connection.
                onGetHeadersMessageReceived(SerializeHelper.parse(GetBlocksPayload::new, msg.payload));
                break;

            case "headers": // 	Answers getheaders message with at most 2000 block header items.
                onHeadersMessageReceived(SerializeHelper.parse(HeadersPayload::new, msg.payload));
                break;

            case "inv":
                /*
                	Send Inventory hash array of specified type and hash (only hash value rather than complete information).
                	 Inventory types include Block, Transaction, and Consensus). Currently inv message is used in these scenarios:
                    1)Sending transaction in consensus process.
                    2)Replying getblocks message with no more than 500 blocks.
                    3) Replying mempool message with all transactions in memory pool.
                    4) Relaying an Inventory.
                    5) Relaying a batch of transactions.
                 */
                onInvMessageReceived(SerializeHelper.parse(InvPayload::new, msg.payload));
                break;

            case "mempool":
                // Query for all transactions in the memory pool of the connected node.
                onMemPoolMessageReceived();
                break;

            case "tx":
                // Answering getdata message for a transaction with specified hash.
                if (msg.payload.length <= Transaction.MaxTransactionSize)
                    onInventoryReceived(Transaction.deserializeFrom(msg.payload));
                break;

            case "verack":
            case "version":
                throw new ProtocolViolationException();
            case "alert":
            case "merkleblock":
            case "notfound":
            case "ping":
            case "pong":
            case "reject":
            default:
                //暂时忽略
                break;
        }
    }
}
