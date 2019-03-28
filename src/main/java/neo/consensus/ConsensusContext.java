package neo.consensus;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import neo.Fixed8;
import neo.TimeProvider;
import neo.UInt160;
import neo.UInt256;
import neo.Wallets.KeyPair;
import neo.Wallets.Wallet;
import neo.Wallets.WalletAccount;
import neo.cryptography.MerkleTree;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.csharp.common.IDisposable;
import neo.exception.InvalidOperationException;
import neo.io.SerializeHelper;
import neo.ledger.Blockchain;
import neo.log.notr.TR;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ConsensusPayload;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.IVerifiable;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.TransactionType;
import neo.network.p2p.payloads.Witness;
import neo.persistence.Snapshot;
import neo.plugins.IPolicyPlugin;
import neo.plugins.Plugin;
import neo.smartcontract.Contract;
import neo.smartcontract.ContractParametersContext;

/**
 * Consensus context, it records the data in current consensus activity.
 */
public class ConsensusContext implements IDisposable {

    /**
     * Consensus message version, it's fixed to 0 currently
     */
    public static final Uint VERSION = Uint.ZERO;

    /**
     * Context state
     */
    public ConsensusState state;

    /**
     * The previous block's hash
     */
    public UInt256 prevHash;

    /**
     * The proposal block's height
     */
    public Uint blockIndex;

    /**
     * Current view number
     */
    public byte viewNumber;

    /**
     * The public keys of consensus nodes in current round
     */
    public ECPoint[] validators;

    /**
     * My index in the validators array
     */
    public int myIndex;

    /**
     * The Speaker index in the validators array
     */
    public Uint primaryIndex;

    /**
     * The proposal block's Timestamp
     */
    public Uint timestamp;

    /**
     * The proposal block's nonce
     */
    public Ulong nonce = Ulong.ZERO;

    /**
     * The proposal block's NextConsensus, which binding the consensus nodes in the next round
     */
    public UInt160 nextConsensus;

    /**
     * The hash list of current proposal block's txs
     */
    public UInt256[] transactionHashes;

    /**
     * The proposal block's txs
     */
    public Map<UInt256, Transaction> transactions;

    /**
     * Store the proposal block's signatures recevied
     */
    public byte[][] signatures;

    /**
     * The expected view number of consensus nodes, mainly used in ChangeView processing. The index
     * of the array is crresponding to the index of nodes.
     */
    public byte[] expectedView;


    /**
     * Snapshot of persistence layer
     */
    private Snapshot snapshot;

    /**
     * Key pair
     */
    private KeyPair keyPair;

    /**
     * Wallet
     */
    private Wallet wallet;


    private Block header = null;


    /**
     * constructor a consensus context
     */
    public ConsensusContext(Wallet wallet) {
        this.wallet = wallet;
    }


    /**
     * get the minimum threshold of the normal nodes.
     */
    public int getM() {
        TR.enter();
        return TR.exit(validators.length - (validators.length - 1) / 3);
    }

    /**
     * get the previous block header
     */
    public Header getPrevHeader() {
        TR.enter();
        return TR.exit(snapshot.getHeader(prevHash));
    }

    /**
     * check whether the tx is exist
     *
     * @param hash tx hash
     */
    public boolean transactionExists(UInt256 hash) {
        TR.enter();
        return TR.exit(snapshot.containsTransaction(hash));
    }

    /**
     * verify the tx
     *
     * @param tx transaction
     */
    public boolean verifyTransaction(Transaction tx) {
        TR.enter();
        return TR.exit(tx.verify(snapshot, transactions.values()));
    }


    /**
     * Change view number
     *
     * @param view_number new view number
     * @docs 1. Update the context ViewNumber, PrimaryIndex and ExpectedView[Myindex] <br/> 2. If
     * the node has the SignatureSent flag, reserve the signatures array (Mybe some signatures are
     * arrived before the PrepareRequest received), else reset it
     */
    public void changeView(byte view_number) {
        TR.enter();
        // C# code: state &= ConsensusState.SignatureSent;
        state = state.and(ConsensusState.SignatureSent);
        viewNumber = view_number;
        primaryIndex = getPrimaryIndex(view_number);
        if (state == ConsensusState.Initial) {
            transactionHashes = null;
            signatures = new byte[validators.length][];
        }
        if (myIndex >= 0)
            expectedView[myIndex] = view_number;
        header = null;
        TR.exit();
    }

    /**
     * Create a full block with the context data
     *
     * @return block
     */
    public Block createBlock() {
        TR.enter();
        Block block = makeHeader();
        if (block == null) {
            return TR.exit(null);
        }
        Contract contract = Contract.createMultiSigContract(getM(), validators);
        ContractParametersContext sc = new ContractParametersContext(block);
        for (int i = 0, j = 0; i < validators.length && j < getM(); i++)
            if (signatures[i] != null) {
                sc.addSignature(contract, validators[i], signatures[i]);
                j++;
            }
        sc.verifiable.setWitnesses(sc.getWitnesses());
        block.transactions = Arrays.stream(transactionHashes).map(hash -> transactions.get(hash)).toArray(Transaction[]::new);
        return TR.exit(block);
    }


    /**
     * Construct the block header with context data
     *
     * @return block header
     */
    public Block makeHeader() {
        TR.enter();
        if (transactionHashes == null) {
            TR.exit();
            return null;
        }

        if (header == null) {
            header = new Block();
            header.version = VERSION;
            header.prevHash = prevHash;
            header.merkleRoot = MerkleTree.computeRoot(transactionHashes);
            header.timestamp = timestamp;
            header.index = blockIndex;
            header.consensusData = nonce;
            header.nextConsensus = nextConsensus;
            header.transactions = new Transaction[0];
        }
        return TR.exit(header);
    }

    /**
     * Create ConsensusPayload which contains the ConsensusMessage
     *
     * @param message consensus message
     * @return ConsensusPayload
     */
    private ConsensusPayload makeSignedPayload(ConsensusMessage message) {
        TR.enter();
        message.viewNumber = viewNumber;
        ConsensusPayload payload = new ConsensusPayload();

        payload.version = VERSION;
        payload.prevHash = prevHash;
        payload.blockIndex = blockIndex;
        payload.validatorIndex = new Ushort(myIndex);
        payload.timestamp = timestamp;
        payload.data = SerializeHelper.toBytes(message);

        signPayload(payload);
        return TR.exit(payload);
    }

    /**
     * Sign the block header
     */
    public void signHeader() {
        TR.enter();
        Block header = makeHeader();
        signatures[myIndex] = header == null ? null : IVerifiable.sign(header, keyPair);
        TR.exit();
    }


    private void signPayload(ConsensusPayload payload) {
        TR.enter();
        ContractParametersContext sc;
        try {
            sc = new ContractParametersContext(payload);
            wallet.sign(sc);
        } catch (InvalidOperationException ex) {
            TR.exit();
            return;
        }
        sc.verifiable.setWitnesses(sc.getWitnesses());
        TR.exit();
    }

    /**
     * Create PrepareRequest message paylaod
     *
     * @return ConsensusPayload
     */
    public ConsensusPayload makePrepareRequest() {
        TR.enter();
        PrepareRequest prepareRequest = new PrepareRequest();
        prepareRequest.nonce = nonce;
        prepareRequest.nextConsensus = nextConsensus;
        prepareRequest.transactionHashes = transactionHashes;
        prepareRequest.minerTransaction = (MinerTransaction) transactions.get(transactionHashes[0]);
        prepareRequest.signature = signatures[myIndex];
        return TR.exit(makeSignedPayload(prepareRequest));
    }


    /**
     * Create ChangeView message payload
     *
     * @return ConsensusPayload
     */
    public ConsensusPayload makeChangeView() {
        TR.enter();
        ConsensusPayload payload = makeSignedPayload(new ChangeView() {{
            newViewNumber = expectedView[myIndex];
        }});
        return TR.exit(payload);
    }


    /**
     * construct a p2p consensus message payload
     *
     * @param signature proposal block signature
     * @return ConsensusPayload with PrepareResponse
     */
    public ConsensusPayload makePrepareResponse(byte[] signature) {
        TR.enter();
        PrepareResponse response = new PrepareResponse();
        response.signature = signature;
        return TR.exit(makeSignedPayload(response));
    }


    /**
     * Get the Speaker index = (BlockIndex - view_number) % Validators.Length
     *
     * @param view_number current view number
     * @return primary index
     */
    public Uint getPrimaryIndex(byte view_number) {
        TR.enter();
        int uint_view_number = view_number & 0xff;
        int p = (blockIndex.intValue() - uint_view_number) % validators.length;
        Uint primaryIndex = p >= 0 ? new Uint(p) : new Uint(p + validators.length);
        return TR.exit(primaryIndex);
    }


    /**
     * Reset the context
     *
     * @docs <ul>
     * <li>1. Require the blockchain snapshot  </li>
     * <li>2. Initial the context state </li>
     * <li>3. Update snapshot and PreHash, BlockIndex </li>
     * <li>4. Reset ViewNumber zero</li>
     * <li>5. Get the latest validators</li>
     * <li>6. Calculate the PriamryIndex, MyIndex and keyPair</li>
     * <li>7. Clear Signatures, ExpectedView</li>
     * </ul>
     */
    public void reset() {
        TR.enter();
        if (snapshot != null) {
            snapshot.dispose();
        }
        snapshot = Blockchain.singleton().getStore().getSnapshot();
        state = ConsensusState.Initial;
        prevHash = snapshot.getCurrentBlockHash();
        blockIndex = snapshot.getHeight().add(Uint.ONE);
        viewNumber = 0;
        validators = snapshot.getValidatorPubkeys();
        myIndex = -1;
        primaryIndex = new Uint(blockIndex.intValue() % validators.length);
        transactionHashes = null;
        signatures = new byte[validators.length][];
        expectedView = new byte[validators.length];
        keyPair = null;
        for (int i = 0; i < validators.length; i++) {
            WalletAccount account = wallet.getAccount(validators[i]);
            if (account != null && account.hasKey() == true) {
                myIndex = i;
                keyPair = account.getKey();
                break;
            }
        }
        header = null;

        TR.exit();
    }


    /**
     * Fill the proposal block, contains txs, MinerTransaction, NextConsensus
     *
     * @docs <ul>
     * <li>1. Transaction, load from memory pool , sort and filter using plugin. </li>
     * <li>2. MinerTransaction and Reward (Reward = Inputs.GAS - outputs.GAS - txs.systemfee) </li>
     * <li>3. NextConsensus, calculated by combining the proposal block's txs with previous voting
     * of the validotars</li>
     * </ul>
     */
    public void fill() {
        TR.enter();
        Collection<Transaction> mem_pool = Blockchain.singleton().getMemPool().getEnumerator();
        for (IPolicyPlugin plugin : Plugin.getPolicies()) {
            mem_pool = plugin.filterForBlock(mem_pool);
        }
        ArrayList<Transaction> txWithMx = new ArrayList<>(mem_pool);
        Fixed8 amount_netfee = Block.calculateNetFee(txWithMx);
        TransactionOutput[] outputs = (amount_netfee.compareTo(Fixed8.ZERO) == 0) ? new TransactionOutput[0] : new TransactionOutput[]
                {
                        new TransactionOutput() {{
                            assetId = Blockchain.UtilityToken.hash();
                            value = amount_netfee;
                            scriptHash = wallet.getChangeAddress();
                        }}
                };
        while (true) {
            Ulong nonce = getNonce();
            MinerTransaction tx = new MinerTransaction();
            tx.nonce = new Uint((int) (nonce.longValue() % Uint.MAX_VALUE + 1));
            // C# code: tx.nonce = (uint) (nonce % (uint.MaxValue + 1 ul));
            tx.attributes = new TransactionAttribute[0];
            tx.inputs = new CoinReference[0];
            tx.outputs = outputs;
            tx.witnesses = new Witness[0];

            if (!snapshot.containsTransaction(tx.hash())) {
                this.nonce = nonce;
                txWithMx.add(0, tx);
                break;
            }
        }
        transactionHashes = txWithMx.stream().map(p -> p.hash()).toArray(UInt256[]::new);
        transactions = txWithMx.stream().collect(Collectors.toMap(tx -> tx.hash(), tx -> tx));
        nextConsensus = Blockchain.getConsensusAddress(snapshot.getValidators(txWithMx));
        timestamp = new Uint((int) Math.max(TimeProvider.current().utcNow().getTime(), getPrevHeader().timestamp.longValue() + 1));

        TR.exit();
    }


    /**
     * Get a new nonce
     */
    private static Ulong getNonce() {
        TR.enter();
        byte[] nonce = new byte[Ulong.BYTES];
        SecureRandom rand = new SecureRandom();
        rand.nextBytes(nonce);
        return TR.exit(BitConverter.toUlong(nonce));
    }


    /**
     * Verify the proposal block, after received the PrepareRequest message
     *
     * @return If valid, then return true
     * @docs 1. If hasn't received the `PrepareRequest` message, return false <br/> 2. Check whether
     * the proposal block's NextConsensus is the same to the result, calculated by the current
     * blockchain snapshot's validators <br/>  3. Check whether the MinerTransaction.output.value is
     * equal to the proposal block's txs network fee
     */
    public boolean verifyRequest() {
        TR.enter();
        if (!state.hasFlag(ConsensusState.RequestReceived)) {
            return TR.exit(false);
        }
        Collection<ECPoint> validatorPublicKeys = snapshot.getValidators(transactions.values());
        if (!Blockchain.getConsensusAddress(validatorPublicKeys).equals(nextConsensus)) {
            return TR.exit(false);
        }
        Transaction tx_gen = transactions.values().stream()
                .filter(p -> p.type == TransactionType.MinerTransaction)
                .findFirst().get();

        Fixed8 amountNetfee = Block.calculateNetFee(transactions.values());
        Fixed8 sumOut = Fixed8.ZERO;

        for (TransactionOutput output : tx_gen.outputs) {
            sumOut = Fixed8.add(sumOut, output.value);
        }

        if (sumOut.compareTo(amountNetfee) != 0) {
            return TR.exit(false);
        }
        return TR.exit(true);
    }

    @Override
    public void dispose() {
        TR.enter();
        if (snapshot != null) {
            snapshot.dispose();
        }
        TR.exit();
    }
}
