package neo.ledger;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.actor.ActorRef;
import neo.Fixed8;
import neo.Helper;
import neo.NeoSystem;
import neo.ProtocolSettings;
import neo.UInt160;
import neo.UInt256;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Out;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.exception.InvalidOperationException;
import neo.io.SerializeHelper;
import neo.io.actors.Idle;
import neo.io.actors.PriorityMailbox;
import neo.io.caching.DataCache;
import neo.io.caching.RelayCache;
import neo.io.wrappers.UInt32Wrapper;
import neo.log.tr.TR;
import neo.network.p2p.LocalNode;
import neo.network.p2p.TaskManager;
import neo.network.p2p.payloads.AssetType;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.ClaimTransaction;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ConsensusPayload;
import neo.network.p2p.payloads.EnrollmentTransaction;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.InvocationTransaction;
import neo.network.p2p.payloads.IssueTransaction;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.PublishTransaction;
import neo.network.p2p.payloads.RegisterTransaction;
import neo.network.p2p.payloads.StateDescriptor;
import neo.network.p2p.payloads.StateTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.TransactionResult;
import neo.network.p2p.payloads.TransactionType;
import neo.network.p2p.payloads.Witness;
import neo.persistence.Snapshot;
import neo.persistence.Store;
import neo.plugins.IPersistencePlugin;
import neo.plugins.Plugin;
import neo.smartcontract.Contract;
import neo.smartcontract.ApplicationEngine;
import neo.smartcontract.TriggerType;
import neo.vm.OpCode;

/**
 * The core Actor of blockChain
 */
public class Blockchain extends UntypedActor {

    public class Register {
    }

    public class ApplicationExecuted {
        public Transaction transaction;
        public ApplicationExecutionResult[] executionResults;
    }

    public class PersistCompleted {
        public Block block;
    }

    public class Import {
        public Block[] blocks;
    }

    public class ImportCompleted {
    }

    /**
     * The time for each block produce
     */
    public static final int SecondsPerBlock = ProtocolSettings.Default.secondsPerBlock;

    /**
     * The decrement interval of reward for each block m
     */
    public static final int[] GenerationAmount = {8, 7, 6, 5, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    /**
     * The decrement interval of reward for each block m
     */
    public static final int DecrementInterval = 2000000;

    /**
     * Maximum number of validators
     */
    public static final int MaxValidators = 1024;

    /**
     * The time for each block produce (TimeSpan)
     */
    public static final Duration TimePerBlock = Duration.ofSeconds(SecondsPerBlock);

    /**
     * The list of standby validators
     */
    public static final ECPoint[] StandbyValidators = ProtocolSettings.Default.standbyValidators.stream().map(p -> ECC.parseFromHexString(p)).toArray(ECPoint[]::new);

    /**
     * The definition of NEO token
     */
    public static final RegisterTransaction GoverningToken = new RegisterTransaction() {
        {
            assetType = AssetType.GoverningToken;
            name = "[{\"lang\":\"zh-CN\",\"name\":\"小蚁股\"},{\"lang\":\"en\",\"name\":\"AntShare\"}]";
            amount = Fixed8.fromDecimal(new BigDecimal(100000000));
            precision = 0;
            owner = new ECPoint(ECC.Secp256r1.getCurve().getInfinity());
            admin = UInt160.parseToScriptHash(new byte[]{OpCode.PUSHT.getCode()});
            attributes = new TransactionAttribute[0];
            inputs = new CoinReference[0];
            outputs = new TransactionOutput[0];
            witnesses = new Witness[0];
        }
    };

    /**
     * The definication of GAS token
     */
    public static final RegisterTransaction UtilityToken = new RegisterTransaction() {
        {
            assetType = AssetType.UtilityToken;
            name = "[{\"lang\":\"zh-CN\",\"name\":\"小蚁币\"},{\"lang\":\"en\",\"name\":\"AntCoin\"}]";
            amount = Fixed8.fromDecimal(BigDecimal.valueOf(Arrays.stream(GenerationAmount).mapToLong(p -> p * DecrementInterval).sum()));
            precision = 8;
            owner = new ECPoint(ECC.Secp256r1.getCurve().getInfinity());
            admin = UInt160.parseToScriptHash(new byte[]{OpCode.PUSHT.getCode()});
            attributes = new TransactionAttribute[0];
            inputs = new CoinReference[0];
            outputs = new TransactionOutput[0];
            witnesses = new Witness[0];
        }
    };

    public static final Block GenesisBlock = new Block() {
        {
            prevHash = UInt256.Zero;
            timestamp = new Uint(Long.toString(new Date("Jul 15 15:08:21 UTC 2016").getTime() / 1000));
            index = Uint.ZERO;
            consensusData = new Ulong(2083236893); //向比特币致敬
            nextConsensus = getConsensusAddress(StandbyValidators);
            witness = new Witness() {
                {
                    invocationScript = new byte[0];
                    verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                }
            };
            transactions = new Transaction[]{
                    new MinerTransaction() {
                        {
                            nonce = new Uint(2083236893);
                            attributes = new TransactionAttribute[0];
                            inputs = new CoinReference[0];
                            outputs = new TransactionOutput[0];
                            witnesses = new Witness[0];
                        }
                    },
                    GoverningToken,
                    UtilityToken,
                    new IssueTransaction() {
                        {
                            attributes = new TransactionAttribute[0];
                            inputs = new CoinReference[0];
                            outputs = new TransactionOutput[]{
                                    new TransactionOutput() {
                                        {
                                            assetId = GoverningToken.hash();
                                            value = GoverningToken.amount;
                                            scriptHash = UInt160.parseToScriptHash(Contract.createMultiSigRedeemScript(StandbyValidators.length / 2 + 1, StandbyValidators));
                                        }
                                    }
                            };
                            witnesses = new Witness[]{
                                    new Witness() {
                                        {
                                            invocationScript = new byte[0];
                                            verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                                        }
                                    }
                            };
                        }
                    }
            };
        }
    };

    private final int MemoryPoolMaxTransactions = 50_000;
    private final int MaxTxToReverifyPerIdle = 10;
    private static final Lock lockObj = new ReentrantLock();
    private NeoSystem system;
    private final ArrayList<UInt256> header_index = new ArrayList<UInt256>();
    private Uint stored_header_count = Uint.ZERO;
    private final ConcurrentHashMap<UInt256, Block> block_cache = new ConcurrentHashMap<UInt256, Block>();
    private final ConcurrentHashMap<Uint, LinkedList<Block>> block_cache_unverified = new ConcurrentHashMap<Uint, LinkedList<Block>>();
    private final RelayCache relayCache = new RelayCache(100);
    private final HashSet<ActorRef> subscribers = new HashSet<ActorRef>();
    private AtomicReference<Snapshot> currentSnapshot;

    private Store store;
    private MemoryPool memPool;

    public Uint height() {
        TR.enter();
        return TR.exit(currentSnapshot.get().getHeight());
    }

    public Uint headerHeight() {
        TR.enter();
        return TR.exit(new Uint(header_index.size() - 1));
    }

    public UInt256 currentBlockHash() {
        TR.enter();
        return TR.exit(currentSnapshot.get().getCurrentBlockHash());
    }

    public UInt256 CurrentHeaderHash() {
        TR.enter();
        return TR.exit(header_index.get(header_index.size() - 1));
    }

    /**
     * Constructor which create a core blockchain
     *
     * @param system NEO actor system
     * @param store  The storage for persistence
     */
    public Blockchain(NeoSystem system, Store store) {
        TR.enter();
        this.system = system;
        this.memPool = new MemoryPool(system, MemoryPoolMaxTransactions);
        this.store = store;
        lockObj.lock();
        try {
            if (singleton != null) {
                TR.exit();
                throw new InvalidOperationException();
            }
            //header_index.AddRange(store.GetHeaderHashList().Find().OrderBy(p => (uint)p.Key).SelectMany(p => p.Value.Hashes));
            ArrayList<Map.Entry<UInt32Wrapper, HeaderHashList>> rankedHeaderHashList = new ArrayList<Map.Entry<UInt32Wrapper, HeaderHashList>>(store.getHeaderHashList().find());
            Collections.sort(rankedHeaderHashList, new Comparator<Map.Entry<UInt32Wrapper, HeaderHashList>>() {
                public int compare(Map.Entry<UInt32Wrapper, HeaderHashList> a, Map.Entry<UInt32Wrapper, HeaderHashList> b) {
                    return a.getKey().toUint32().compareTo(b.getKey().toUint32());
                }
            });
            for (Map.Entry<UInt32Wrapper, HeaderHashList> entry : rankedHeaderHashList) {
                for (UInt256 value : entry.getValue().hashes) {
                    header_index.add(value);
                }
            }
            stored_header_count = stored_header_count.add(new Uint(header_index.size()));
            if (stored_header_count.equals(Uint.ZERO)) {
                //header_index.AddRange(store.GetBlocks().Find().OrderBy(p = > p.Value.TrimmedBlock.Index).Select(p = > p.Key));
                ArrayList<Map.Entry<UInt256, BlockState>> rankedBlockList = new ArrayList<Map.Entry<UInt256, BlockState>>(store.getBlocks().find());
                Collections.sort(rankedBlockList, new Comparator<Map.Entry<UInt256, BlockState>>() {
                    public int compare(Map.Entry<UInt256, BlockState> a, Map.Entry<UInt256, BlockState> b) {
                        return a.getValue().trimmedBlock.index.compareTo(b.getValue().trimmedBlock.index);
                    }
                });
                rankedBlockList.forEach(p -> header_index.add(p.getKey()));
            } else {
                HashIndexState hashIndex = store.getHeaderHashIndex().get();
                if (hashIndex.index.compareTo(stored_header_count) > 0) {
                    DataCache<UInt256, BlockState> cache = store.getBlocks();
                    for (UInt256 hash = hashIndex.hash; hash != header_index.get(stored_header_count.intValue() - 1); ) {
                        header_index.add(stored_header_count.intValue(), hash);
                        hash = cache.get(hash).trimmedBlock.prevHash;
                    }
                }
            }
            if (header_index.size() == 0) {
                persist(GenesisBlock);
            } else {
                updateCurrentSnapshot();
            }
            singleton = this;
        } catch (Exception e) {
            TR.error(e);
            throw new RuntimeException(e);
        } finally {
            lockObj.unlock();
        }
        TR.exit();
    }

    /**
     * Get store
     */
    public Store getStore() {
        return store;
    }

    /**
     * Get mempool
     */
    public MemoryPool getMemPool() {
        return memPool;
    }

    public boolean containsBlock(UInt256 hash) {
        TR.enter();
        if (block_cache.containsKey(hash)) {
            return TR.exit(true);
        }
        return TR.exit(getStore().containsBlock(hash));
    }

    public boolean containsTransaction(UInt256 hash) {
        TR.enter();
        if (memPool.containsKey(hash)) {
            return TR.exit(true);
        }
        return TR.exit(getStore().containsTransaction(hash));
    }

    private void distribute(Object message) {
        TR.enter();
        for (ActorRef subscriber : subscribers) {
            subscriber.tell(message, self());
        }
        TR.exit();
    }

    public Block getBlock(UInt256 hash) {
        TR.enter();
        Block block = block_cache.get(hash);
        if (block != null) {
            return TR.exit(block);
        }
        return TR.exit(getStore().getBlock(hash));
    }

    /**
     * Query block hash by block index
     *
     * @param index block index
     * @return block hash
     */
    public UInt256 getBlockHash(Uint index) {
        TR.enter();
        if (header_index.size() <= index.intValue()) {
            return TR.exit(null);
        }
        return TR.exit(header_index.get(index.intValue()));
    }

    public static UInt160 getConsensusAddress(ECPoint[] validators) {
        TR.enter();
        return TR.exit(UInt160.parseToScriptHash(Contract.createMultiSigRedeemScript(validators.length - (validators.length - 1) / 3, validators)));
    }

    public Snapshot getSnapshot() {
        TR.enter();
        return TR.exit(getStore().getSnapshot());
    }

    public Transaction getTransaction(UInt256 hash) {
        TR.enter();
        Out<Transaction> transaction = new Out<>();
        if (memPool.tryGetValue(hash, transaction)) {
            return TR.exit(transaction.get());
        }
        return TR.exit(getStore().getTransaction(hash));
    }

    private void onImport(Block[] blocks) {
        TR.enter();
        for (Block block : blocks) {
            if (block.index.compareTo(height()) <= 0) {
                continue;
            }
            if (!block.index.equals(height().add(Uint.ONE))) {
                TR.exit();
                throw new InvalidOperationException();
            }
            persist(block);
            saveHeaderHashList();
        }
        sender().tell(new ImportCompleted(), self());
        TR.exit();
    }

    private void addUnverifiedBlockToCache(Block block) {
        TR.enter();
        LinkedList<Block> blocks = block_cache_unverified.get(block.index);
        if (blocks == null) {
            blocks = new LinkedList<Block>();
            block_cache_unverified.put(block.index, blocks);
        }
        blocks.addLast(block);
        TR.exit();
    }

    private RelayResultReason onNewBlock(Block block) {
        TR.enter();
        if (block.index.compareTo(height()) <= 0) {
            return TR.exit(RelayResultReason.AlreadyExists);
        }
        if (block_cache.containsKey(block.hash())) {
            return TR.exit(RelayResultReason.AlreadyExists);
        }
        if (block.index.subtract(Uint.ONE).intValue() >= header_index.size()) {
            addUnverifiedBlockToCache(block);
            return TR.exit(RelayResultReason.UnableToVerify);
        }
        if (block.index.intValue() == header_index.size()) {
            if (!block.verify(currentSnapshot.get())) {
                return TR.exit(RelayResultReason.Invalid);
            }
        } else {
            if (!block.hash().equals(header_index.get(block.index.intValue()))) {
                return TR.exit(RelayResultReason.Invalid);
            }
        }
        if (block.index.equals(height().add(Uint.ONE))) {
            Block block_persist = block;
            ArrayList<Block> blocksToPersistList = new ArrayList<Block>();
            while (true) {
                blocksToPersistList.add(block_persist);
                if (block_persist.index.intValue() + 1 >= header_index.size()) {
                    break;
                }
                UInt256 hash = header_index.get(block_persist.index.intValue() + 1);
                block_persist = block_cache.get(hash);
                if (block_persist == null) {
                    break;
                }
            }

            int blocksPersisted = 0;
            for (Block blockToPersist : blocksToPersistList) {
                block_cache_unverified.remove(blockToPersist.index);
                persist(blockToPersist);

                if (blocksPersisted++ < blocksToPersistList.size() - 2) continue;
                // Relay most recent 2 blocks persisted

                if (blockToPersist.index.add(new Uint(100)).intValue() >= header_index.size()) {
                    system.localNode.tell(new LocalNode.relayDirectly() {
                        {
                            inventory = blockToPersist;
                        }
                    }, self());
                }
            }
            saveHeaderHashList();

            LinkedList<Block> unverifiedBlocks = block_cache_unverified.get(height().add(Uint.ONE));
            if (unverifiedBlocks == null) {
                for (Block unverifiedBlock : unverifiedBlocks) {
                    self().tell(unverifiedBlock, ActorRef.noSender());
                }
                block_cache_unverified.remove(height().add(Uint.ONE));
            }
        } else {
            block_cache.put(block.hash(), block);
            if (block.index.add(new Uint(100)).intValue() >= header_index.size()) {
                system.localNode.tell(new LocalNode.RelayDirectly() {
                    {
                        inventory = block;
                    }
                }, self());
            }
            if (block.index.intValue() == header_index.size()) {
                header_index.add(block.hash());
                Snapshot snapshot = getSnapshot();
                snapshot.getBlocks().add(block.hash(), new BlockState() {
                    {
                        systemFeeAmount = 0;
                        trimmedBlock = block.getHeader().trim();
                    }
                });
                snapshot.getHeaderHashIndex().getAndChange().hash = block.hash();
                snapshot.getHeaderHashIndex().getAndChange().index = block.index;
                saveHeaderHashList(snapshot);
                snapshot.commit();
                updateCurrentSnapshot();
            }
        }
        return TR.exit(RelayResultReason.Succeed);
    }

    private RelayResultReason onNewConsensus(ConsensusPayload payload) {
        TR.enter();
        if (!payload.verify(currentSnapshot.get())) {
            return TR.exit(RelayResultReason.Invalid);
        }
        if (system.consensus != null) {
            system.consensus.tell(payload, self());
        }
        relayCache.add(payload);
        system.localNode.tell(new LocalNode.RelayDirectly() {
            {
                inventory = payload;
            }
        }, self());
        return TR.exit(RelayResultReason.Succeed);
    }

    private void onNewHeaders(Header[] headers) {
        TR.enter();
        Snapshot snapshot = getSnapshot();
        for (Header header : headers) {
            if (header.index.intValue() - 1 >= header_index.size()) {
                break;
            }
            if (header.index.intValue() < header_index.size()) {
                continue;
            }
            if (!header.verify(snapshot)) {
                break;
            }
            header_index.add(header.hash());
            snapshot.getBlocks().add(header.hash(), new BlockState() {
                {
                    systemFeeAmount = 0;
                    trimmedBlock = header.trim();
                }
            });
            snapshot.getHeaderHashIndex().getAndChange().hash = header.hash();
            snapshot.getHeaderHashIndex().getAndChange().index = header.index;
        }
        saveHeaderHashList(snapshot);
        snapshot.commit();
        updateCurrentSnapshot();
        system.taskManager.tell(new TaskManager.headerTaskCompleted(), sender());
        TR.exit();
    }

    private RelayResultReason onNewTransaction(Transaction transaction) {
        TR.enter();
        if (transaction.type == TransactionType.MinerTransaction) {
            return TR.exit(RelayResultReason.Invalid);
        }
        if (containsTransaction(transaction.hash())) {
            return TR.exit(RelayResultReason.AlreadyExists);
        }
        if (!memPool.canTransactionFitInPool(transaction)) {
            return TR.exit(RelayResultReason.OutOfMemory);
        }
        if (!transaction.verify(currentSnapshot.get(), memPool.getVerifiedTransactions())) {
            return TR.exit(RelayResultReason.Invalid);
        }
        if (!Plugin.checkPolicy(transaction)) {
            return TR.exit(RelayResultReason.PolicyFail);
        }

        if (!memPool.tryAdd(transaction.hash(), transaction)) {
            return TR.exit(RelayResultReason.OutOfMemory);
        }

        system.localNode.tell(new LocalNode.RelayDirectly() {
            {
                inventory = transaction;
            }
        });
        return TR.exit(RelayResultReason.Succeed);
    }

    private void onPersistCompleted(Block inputBlock) {
        TR.enter();
        block_cache.remove(block.hash());
        memPool.updatePoolForBlockPersisted(block, currentSnapshot.get());
        PersistCompleted completed = new PersistCompleted() {
            {
                block = inputBlock;
            }
        };
        if (system.consensus != null) {
            system.consensus.tell(completed, self());
        }
        distribute(completed);
        TR.exit();
    }

    @Override
    public void onReceive(Object message) {
        TR.enter();
        if (message instanceof Register) {
            onRegister();
        } else if (message instanceof Import) {
            onImport(((Import) message).blocks);
        } else if (message instanceof Header[]) {
            onNewHeaders((Header[]) message);
        } else if (message instanceof Block) {
            sender().tell(onNewBlock((Block) message), self());
        } else if (message instanceof Transaction) {
            sender().tell(onNewTransaction((Transaction) message), self());
        } else if (message instanceof ConsensusPayload) {
            sender().tell(onNewConsensus((ConsensusPayload) message), self());
        } else if (message instanceof Idle) {
            if (memPool.reVerifyTopUnverifiedTransactionsIfNeeded(MaxTxToReverifyPerIdle, currentSnapshot.get())) {
                self().tell(Idle.instance(), ActorRef.noSender());
            }
        } else if (message instanceof Terminated) {
            subscribers.remove(((Terminated) message).getActor());
        }
        TR.exit();
    }

    private void onRegister() {
        TR.enter();
        subscribers.add(sender());
        context().watch(sender());
        TR.exit();
    }

    private void persist(Block block) {
        TR.enter();
        Snapshot snapshot = getSnapshot();
        ArrayList<ApplicationExecuted> all_application_executed = new ArrayList<ApplicationExecuted>();
        snapshot.setPersistingBlock(block);
        snapshot.getBlocks().add(block.hash(), new BlockState() {
            {

                systemFeeAmount = snapshot.getSysFeeAmount(block.prevHash) + Fixed8.toLong(Helper.sum(Arrays.asList(block.transactions), p -> p.getSystemFee()));
                trimmedBlock = block.trim();
            }
        });
        for (Transaction tx : block.transactions) {
            snapshot.getTransactions().add(tx.hash(), new TransactionState() {
                {
                    blockIndex = block.index;
                    transaction = tx;
                }
            });
            UnspentCoinState unspentCoinState = new UnspentCoinState();
            unspentCoinState.items = new CoinState[tx.outputs.length];
            Arrays.fill(unspentCoinState.items, CoinState.Confirmed);
            snapshot.getUnspentCoins().add(tx.hash(), unspentCoinState);
            for (TransactionOutput output : tx.outputs) {
                AccountState account = snapshot.getAccounts().getAndChange(output.scriptHash, () -> new AccountState(output.scriptHash));
                if (account.balances.containsKey(output.assetId)) {
                    account.balances.put(output.assetId, Fixed8.add(account.getBalance(output.assetId), output.value));
                } else {
                    account.balances.put(output.assetId, output.value);
                }
                if (output.assetId.equals(GoverningToken.hash()) && account.votes.length > 0) {
                    for (ECPoint pubkey : account.votes) {
                        ValidatorState state = snapshot.getValidators().getAndChange(pubkey, () -> new ValidatorState(pubkey));
                        state.votes = Fixed8.add(state.getVotes(), output.value);
                    }
                    ValidatorsCountState state = snapshot.getValidatorsCount().getAndChange();
                    state.votes[account.votes.length - 1] = Fixed8.add(state.votes[account.votes.length - 1], output.value);
                }
            }
            Arrays.stream(tx.inputs).collect(Collectors.groupingBy(p -> p.prevHash)).forEach((key, inputs) ->
                    {
                        TransactionState tx_prev = snapshot.getTransactions().get(key);
                        for (CoinReference input : inputs) {
                            UnspentCoinState state = snapshot.getUnspentCoins().getAndChange(input.prevHash);
                            state.items[input.prevIndex.intValue()] = new CoinState((byte) (state.items[input.prevIndex.intValue()].value() | CoinState.Spent.value()));
                            TransactionOutput out_prev = tx_prev.transaction.outputs[input.prevIndex.intValue()];
                            AccountState account = snapshot.getAccounts().getAndChange(out_prev.scriptHash);
                            if (out_prev.assetId.equals(GoverningToken.hash())) {
                                snapshot.getSpentCoins().getAndChange(input.prevHash, () -> new SpentCoinState() {
                                    {
                                        transactionHash = input.prevHash;
                                        transactionHeight = tx_prev.blockIndex;
                                        items = new HashMap<Ushort, Uint>();
                                    }
                                }).items.put(input.prevIndex, block.index);
                                if (account.votes.length > 0) {
                                    for (ECPoint pubkey : account.votes) {
                                        ValidatorState validator = snapshot.getValidators().getAndChange(pubkey);
                                        validator.votes = Fixed8.subtract(validator.votes, out_prev.value);
                                        if (!validator.registered && validator.votes.equals(Fixed8.ZERO))
                                            snapshot.getValidators().delete(pubkey);
                                    }
                                    ValidatorsCountState validatorsCountState = snapshot.getValidatorsCount().getAndChange();
                                    validatorsCountState.votes[account.votes.length - 1] = Fixed8.subtract(validatorsCountState.votes[account.votes.length - 1], out_prev.value);
                                }
                            }
                            account.balances.put(out_prev.assetId, out_prev.value);
                        }
                    }
            );
            ArrayList<ApplicationExecutionResult> execution_results = new ArrayList<ApplicationExecutionResult>();
            if (tx instanceof RegisterTransaction) {
                RegisterTransaction tx_register = (RegisterTransaction) tx;
                snapshot.getAssets().add(tx.hash(), new AssetState() {
                    {
                        assetId = tx_register.hash();
                        assetType = tx_register.assetType;
                        name = tx_register.name;
                        amount = tx_register.amount;
                        available = Fixed8.ZERO;
                        precision = tx_register.precision;
                        fee = Fixed8.ZERO;
                        feeAddress = new UInt160();
                        owner = tx_register.owner;
                        admin = tx_register.admin;
                        issuer = tx_register.admin;
                        expiration = block.index.add(new Uint(2 * 2000000));
                        isFrozen = false;
                    }
                });
            } else if (tx instanceof IssueTransaction) {
                for (TransactionResult result : tx.getTransactionResults().stream().filter(p -> p.amount.compareTo(Fixed8.ZERO) < 0).collect(Collectors.toList())) {
                    AssetState assetState = snapshot.getAssets().getAndChange(result.assetId);
                    assetState.available = Fixed8.subtract(assetState.available, result.amount);
                }
            } else if (tx instanceof ClaimTransaction) {
                for (CoinReference input : ((ClaimTransaction) tx).claims) {
                    SpentCoinState spentCoinState = snapshot.getSpentCoins().tryGet(input.prevHash);
                    if (spentCoinState != null) {
                        if (spentCoinState.items.remove(input.prevIndex) != null) {
                            snapshot.getSpentCoins().getAndChange(input.prevHash);
                        }
                    }
                }
            } else if (tx instanceof EnrollmentTransaction) {
                EnrollmentTransaction tx_enrollment = (EnrollmentTransaction) tx;
                snapshot.getValidators().getAndChange(tx_enrollment.publicKey, () -> new ValidatorState(tx_enrollment.publicKey)).registered = true;
            } else if (tx instanceof StateTransaction) {
                StateTransaction tx_state = (StateTransaction) tx;
                for (StateDescriptor descriptor : tx_state.descriptors) {
                    switch (descriptor.type) {
                        case Account:
                            processAccountStateDescriptor(descriptor, snapshot);
                            break;
                        case Validator:
                            processValidatorStateDescriptor(descriptor, snapshot);
                            break;
                    }
                }
            } else if (tx instanceof PublishTransaction) {
                PublishTransaction tx_publish = (PublishTransaction) tx;
                snapshot.getContracts().getOrAdd(tx_publish.getScriptHash(), () -> new ContractState() {
                    {
                        script = tx_publish.script;
                        parameterList = tx_publish.parameterList;
                        returnType = tx_publish.returnType;
                        contractProperties = (ContractPropertyState) Convert.toByte(tx_publish.needStorage);
                        name = tx_publish.name;
                        codeVersion = tx_publish.codeVersion;
                        author = tx_publish.author;
                        email = tx_publish.email;
                        description = tx_publish.description;
                    }
                });
            } else if (tx instanceof InvocationTransaction) {
                InvocationTransaction tx_invocation = (InvocationTransaction) tx;
                ApplicationEngine engine = new ApplicationEngine(TriggerType.Application, tx_invocation, snapshot.clone(), tx_invocation.gas);
                engine.loadScript(tx_invocation.script);
                if (engine.execute()) {
                    engine.service.commit();
                }
                execution_results.add(new ApplicationExecutionResult() {
                    {
                        trigger = TriggerType.Application;
                        scriptHash = UInt160.parseToScriptHash(tx_invocation.script);
                        vmState = engine.state;
                        gasConsumed = engine.gasConsumed;
                        stack = engine.resultStack.toArray();
                        notifications = engine.service.notifications.toArray();
                    }
                });
            }
            if (execution_results.size() > 0) {
                ApplicationExecuted application_executed = new ApplicationExecuted() {
                    {
                        transaction = tx;
                        executionResults = execution_results.toArray(new ApplicationExecutionResult[execution_results.size()]);
                    }
                };
                distribute(application_executed);
                all_application_executed.add(application_executed);
            }
        }
        snapshot.getBlockHashIndex().getAndChange().hash = block.hash();
        snapshot.getBlockHashIndex().getAndChange().index = block.index;
        if (block.index.intValue() == header_index.size()) {
            header_index.add(block.hash());
            snapshot.getHeaderHashIndex().getAndChange().hash = block.hash();
            snapshot.getHeaderHashIndex().getAndChange().index = block.index;
        }

        for (IPersistencePlugin plugin : Plugin.getPersistencePlugins()) {
            plugin.onPersist(snapshot, all_application_executed);
        }
        snapshot.commit();
        ArrayList<Exception> commitExceptions = null;
        for (IPersistencePlugin plugin : Plugin.getPersistencePlugins()) {
            try {
                plugin.onCommit(snapshot);
            } catch (Exception ex) {
                if (plugin.shouldThrowExceptionFromCommit(ex)) {
                    if (commitExceptions == null) {
                        commitExceptions = new ArrayList<Exception>();
                    }
                    commitExceptions.add(ex);
                }
            }
        }
        if (commitExceptions != null) {
            TR.exit();
            throw new AggregateException(commitExceptions);
        }
        updateCurrentSnapshot();
        onPersistCompleted(block);
        TR.exit();
    }

    @Override
    public void postStop() {
        try {
            TR.enter();
            super.postStop();
            if (currentSnapshot.get() != null) {
                currentSnapshot.get().dispose();
            }
        } catch (Exception e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
        TR.exit();
    }

    private void saveHeaderHashList(Snapshot snapshot) {
        TR.enter();
        if ((header_index.size() - stored_header_count.intValue() < 2000)) {
            TR.exit();
            return;
        }
        boolean snapshot_created = snapshot == null;
        if (snapshot_created) {
            snapshot = getSnapshot();
        }
        try {
            while (header_index.size() - stored_header_count.intValue() >= 2000) {
                snapshot.getHeaderHashList().add(new UInt32Wrapper(stored_header_count), new HeaderHashList() {
                    {
                        hashes = (UInt256[]) Arrays.copyOfRange(header_index.toArray(), stored_header_count.intValue(), stored_header_count.intValue() + 2000);
                    }
                });
                stored_header_count = Uint.add(stored_header_count, new Uint(2000));
            }
            if (snapshot_created) {
                snapshot.commit();
            }
        } finally {
            if (snapshot_created) {
                snapshot.dispose();
            }
            TR.exit();
        }
    }

    private void updateCurrentSnapshot() {
        TR.enter();
        Snapshot oldSnapShot = currentSnapshot.getAndSet(getSnapshot());
        if (oldSnapShot != null) {
            oldSnapShot.dispose();
        }
        TR.exit();
    }

    /**
     * Create the blockchain Actor Ref
     *
     * @param system NEO actor system
     * @param store  The storage for persistence
     * @return return a actorRef which is immutable and thread safe
     */
    public static Props props(NeoSystem system, Store store) {
        TR.enter();
        return TR.exit(Props.create(Blockchain.class, system, store).withMailbox("blockchain-mailbox"));
    }

    private static Blockchain singleton;

    public static Blockchain singleton() {
        // TODO 有待改进
        TR.enter();
        while (singleton == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                TR.error(e);
            }
        }
        return TR.exit(singleton);
    }


    private ArrayList<UInt256> headerIndex = new ArrayList<>();

    public static void processAccountStateDescriptor(StateDescriptor descriptor, Snapshot snapshot) {
        TR.enter();
        UInt160 hash = new UInt160(descriptor.key);
        AccountState account = snapshot.getAccounts().getAndChange(hash, () -> new AccountState(hash));
        switch (descriptor.field) {
            case "Votes":
                Fixed8 balance = account.getBalance(GoverningToken.hash());
                for (ECPoint pubkey : account.votes) {
                    ValidatorState validator = snapshot.getValidators().getAndChange(pubkey);
                    validator.votes = Fixed8.subtract(validator.votes, balance);
                    if (!validator.registered && validator.votes.equals(Fixed8.ZERO)) {
                        snapshot.getValidators().delete(pubkey);
                    }
                }

                ECPoint[] votes = SerializeHelper.asAsSerializableArray(descriptor.value, ECPoint[]::new, ECPoint::new);
                votes = Arrays.stream(votes).distinct().toArray(ECPoint[]::new);
                //   ECPoint[] votes = descriptor.value.AsSerializableArray < ECPoint > ().Distinct().ToArray();

                if (votes.length != account.votes.length) {
                    ValidatorsCountState count_state = snapshot.getValidatorsCount().getAndChange();
                    if (account.votes.length > 0) {
                        count_state.votes[account.votes.length - 1] = Fixed8.subtract(count_state.votes[account.votes.length - 1], balance);
                        // count_state.votes[account.votes.length - 1] -= balance;
                    }
                    if (votes.length > 0) {
                        count_state.votes[votes.length - 1] = Fixed8.add(count_state.votes[votes.length - 1], balance);
                        // count_state.votes[votes.length - 1] += balance;
                    }
                }
                account.votes = votes;
                for (ECPoint pubkey : account.votes) {
                    ValidatorState state = snapshot.getValidators().getAndChange(pubkey, () -> new ValidatorState(pubkey));
                    state.votes = Fixed8.add(state.votes, balance);
                }
                break;
        }
        TR.exit();
    }

    public static void processValidatorStateDescriptor(StateDescriptor descriptor, Snapshot snapshot) {
        TR.enter();
        ECPoint pubkey = ECPoint.fromBytes(descriptor.key, ECC.Secp256r1.getCurve());
        ValidatorState validator = snapshot.getValidators().getAndChange(pubkey, () -> new ValidatorState(pubkey));
        switch (descriptor.field) {
            case "Registered":
                validator.registered = BitConverter.toBoolean(descriptor.value, 0);
                break;
            default:
                break;
        }
        TR.exit();
    }
}

class BlockchainMailbox extends PriorityMailbox {
    @Override
    protected boolean isHighPriority(Object message) {
        TR.enter();
        if (message instanceof Header[] || message instanceof Block || message instanceof ConsensusPayload || message instanceof Terminated) {
            return TR.exit(true);
        } else {
            return TR.exit(false);
        }
    }
}
