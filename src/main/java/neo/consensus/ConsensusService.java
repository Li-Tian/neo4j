package neo.consensus;

import com.typesafe.config.Config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import neo.TimeProvider;
import neo.UInt256;
import neo.wallets.Wallet;
import neo.cryptography.Crypto;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Out;
import neo.csharp.Uint;
import neo.io.SerializeHelper;
import neo.io.actors.PriorityMailbox;
import neo.ledger.Blockchain;
import neo.network.p2p.LocalNode;
import neo.network.p2p.Message;
import neo.network.p2p.TaskManager;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.ConsensusPayload;
import neo.network.p2p.payloads.InvPayload;
import neo.network.p2p.payloads.InventoryType;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionType;
import neo.plugins.LogLevel;
import neo.plugins.Plugin;

import neo.log.notr.TR;


/**
 * Consensus sevice, implemented the dBFT algorithm. for more details:
 * http://docs.neo.org/en-us/basic/consensus/whitepaper.html
 */
public class ConsensusService extends AbstractActor {

    /**
     * Start consensus activity message(custom AKKA message type)
     */
    public static class Start {
    }

    /**
     * Change the current view number message (AKKA customized message type)
     */
    public static class SetViewNumber {
        public byte viewNumber;
    }

    /**
     * Time out message (AKKA customized message type). It contains the `Height` and `ViewNumber` of
     * the timeout block.
     */
    public static class Timer {
        public Uint height;
        public byte viewNumber;
    }

    private ConsensusContext context;
    private ActorRef localNode;
    private ActorRef taskManager;
    private Cancellable timerToken;
    private Date blockReceivedTime;


    /**
     * Construct a ConsensusService
     *
     * @param localNode   local node
     * @param taskManager task manager
     * @param wallet      wallet
     */
    public ConsensusService(ActorRef localNode, ActorRef taskManager, Wallet wallet) {
        this.localNode = localNode;
        this.taskManager = taskManager;
        this.context = new ConsensusContext(wallet);
    }

    /**
     * Construct a ConsensusService
     *
     * @param localNode   local node
     * @param taskManager task manager
     * @param context     consensus context
     */
    public ConsensusService(ActorRef localNode, ActorRef taskManager, ConsensusContext context) {
        this.localNode = localNode;
        this.taskManager = taskManager;
        this.context = context;
    }


    /**
     * Create ActorRef with mail box `consensus-service-mailbox`
     *
     * @param localNode   local node
     * @param taskManager task manager
     * @param wallet      wallet
     * @return Akka.Actor.props
     */
    public static Props props(ActorRef localNode, ActorRef taskManager, Wallet wallet) {
        TR.enter();
        return TR.exit(Props.create(ConsensusService.class, localNode, taskManager, wallet)
                .withMailbox("consensus-service-mailbox"));
    }

    /**
     * Create ActorRef with mail box `consensus-service-mailbox`
     *
     * @param localNode   local node
     * @param taskManager task manager
     * @param context     consensus context
     * @return Akka.Actor.props
     */
    public static Props props(ActorRef localNode, ActorRef taskManager, ConsensusContext context) {
        TR.enter();
        return TR.exit(Props.create(ConsensusService.class, localNode, taskManager, context)
                .withMailbox("consensus-service-mailbox"));
    }

    @Override
    public Receive createReceive() {
        TR.enter();
        return TR.exit(receiveBuilder().match(Start.class, start -> onStart())
                .match(SetViewNumber.class, setView -> initializeConsensus(setView.viewNumber))
                .match(Timer.class, timer -> onTimer(timer))
                .match(ConsensusPayload.class, payload -> onConsensusPayload(payload))
                .match(Transaction.class, tx -> onTransaction(tx))
                .match(Blockchain.PersistCompleted.class, completed -> onPersistCompleted(completed.block))
                .build());
    }


    /**
     * Note, if the proposal block's transactions are all received, the PrepareResponse will be
     * send. If the verification fails, the ChangeView will be send.
     *
     * @param tx     tx to be added
     * @param verify Whether or not to the verify the transaction
     */
    /// 大致算法过程解释如下：
    /// 1. 若以下3个条件满足任意一条，这条交易会被拒收
    /// 1) 当前上下文的快照中已经包含这条交易；
    /// 2）传入的verify参数为True，意味着要验证；且验证的结果为false;
    /// 3) 这条交易不满足Plugin中的某条policy；
    ///
    /// 2. 将这条交易放入Transactions数组；
    /// 3. 当提案block的交易全部收齐，即TransactionHashes的长度和Transactions数组的长度一致时：
    ///    若通过了上下文中的VerifyRequest测试，该节点将发送PrepareReponse消息；否则该节点将发送ChangeView请求；
    ///    VerifyRequest测试的具体内容见ConsensusContext.cs中的VerifyRequest方法。
    private boolean addTransaction(Transaction tx, boolean verify) {
        TR.enter();
        if (verify && !context.verifyTransaction(tx)) {
            log(LogLevel.Warning, "Invalid transaction: %s \n%s", tx.hash(), BitConverter.toHexString(SerializeHelper.toBytes(tx)));
            requestChangeView();
            return TR.exit(false);
        }
        if (!Plugin.checkPolicy(tx)) {
            log(LogLevel.Warning, "reject tx: %s\n %s", tx.hash(), BitConverter.toHexString(SerializeHelper.toBytes(tx)));
            requestChangeView();
            return TR.exit(false);
        }
        context.transactions.put(tx.hash(), tx);
        if (context.transactionHashes.length == context.transactions.size()) {
            if (context.verifyRequest()) {
                log(LogLevel.Info, "send prepare response");
                context.state = context.state.or(ConsensusState.SignatureSent);
                context.signHeader();
                localNode.tell(new LocalNode.SendDirectly(context.makePrepareResponse(context.signatures[context.myIndex])), self());
                checkSignatures();
            } else {
                requestChangeView();
                return TR.exit(false);
            }
        }
        return TR.exit(true);
    }

    /**
     * Change timer
     *
     * @param delay `delay` seconds timeout
     */
    private void changeTimer(Duration delay) {
        TR.enter();
        if (timerToken != null && !timerToken.isCancelled()) {
            timerToken.cancel();
        }
        Timer timer = new Timer() {{
            height = context.blockIndex;
            viewNumber = context.viewNumber;
        }};
        timerToken = context().system().scheduler().scheduleOnce(delay, self(), timer, context().dispatcher(), ActorRef.noSender());
        TR.exit();
    }


    /**
     * Check the exptected view number array
     *
     * @param view_number the current expected view number
     * @doc If there are at least M nodes meeting the EV[i] == view_number, the view change
     * completed.
     */
    private void CheckExpectedView(byte view_number) {
        TR.enter();
        if (context.viewNumber == view_number) return;
        int sum = 0;
        for (int i = 0; i < context.expectedView.length; i++) {
            sum += (context.expectedView[i] & 0xff); // 转成 uint, 确保都是正数
        }
        if (sum >= context.getM()) {
            initializeConsensus(view_number);
        }
        TR.exit();
    }


    /**
     * Check signatures
     *
     * @doc if there are at least M signatures, the proposal block will be accepted and send the
     * full block
     */
    private void checkSignatures() {
        TR.enter();
        long validSignatureSize = Arrays.stream(context.signatures).filter(p -> p != null).count();
        boolean isAllTxsExist = Arrays.stream(context.transactionHashes).allMatch(p -> context.transactions.containsKey(p));

        if (validSignatureSize >= context.getM() && isAllTxsExist) {
            Block block = context.createBlock();
            log(LogLevel.Info, "relay block: {block.Hash}");
            localNode.tell(new LocalNode.Relay(block), self());
            context.state = context.state.or(ConsensusState.BlockSent);
        }
        TR.exit();
    }


    /**
     * Initialize the consensus activity with view_number
     *
     * @param view_number view number
     * @doc If view number is zero, it means a new block in the first round, then reset context,
     * else change view.<br/> If Myindex of context is less than zero, not a validator, then not
     * participate the consensus activity.<br/> If node is the Primary/Speaker, set context's state
     * `ConsensusState.Primary` flag true, then reset the timer as block time is 15 seconds.<br/> If
     * node is the Backup/Delegates, set context's state `ConsensusState.Backup` flag true, then
     * reset the timer with 15 << view_number +1) seconds to timeout, to avoid frequent view
     * change.
     */
    private void initializeConsensus(byte view_number) {
        TR.enter();
        if (view_number == 0) {
            context.reset();
        } else {
            context.changeView(view_number);
        }
        if (context.myIndex < 0) {
            TR.exit();
            return;
        }

        int uint_view_number = view_number & 0xff;
        if (view_number > 0) {
            int primaryIndex = context.getPrimaryIndex((byte) (uint_view_number - 1)).intValue();
            ECPoint primary = context.validators[primaryIndex];
            log(LogLevel.Warning, "changeview: view = %d primary = %s", uint_view_number, primary);
        }

        log(LogLevel.Info, "initialize: height=%d view=%d index=%d role=%s",
                context.blockIndex.intValue(), uint_view_number, context.myIndex, (context.myIndex == context.primaryIndex.intValue() ? "Primary" : "Backup"));

        if (context.myIndex == context.primaryIndex.intValue()) {
            context.state = context.state.or(ConsensusState.Primary);

            long interval = Blockchain.TimePerBlock.toMillis();
            if (blockReceivedTime != null) {
                interval = TimeProvider.current().utcNow().getTime() - blockReceivedTime.getTime();
            }

            Duration span = Duration.ofMillis(interval);
            if (span.compareTo(Blockchain.TimePerBlock) >= 0) {
                changeTimer(Duration.ofMillis(0));
            } else {
                changeTimer(Blockchain.TimePerBlock.minus(span));
            }
        } else {
            context.state = ConsensusState.Backup;
            changeTimer(Duration.ofSeconds(Blockchain.SecondsPerBlock << (view_number + 1)));
        }
        TR.exit();
    }

    private void log(LogLevel level, String message) {
        Plugin.pluginLog(ConsensusService.class.getSimpleName(), level, message);
    }

    private void log(LogLevel level, String message, Object... params) {
        Plugin.pluginLog(ConsensusService.class.getSimpleName(), level, String.format(message, params));
    }


    /**
     * ChangeView processing
     */
    private void onChangeViewReceived(ConsensusPayload payload, ChangeView message) {
        TR.enter();
        int uintViewNumber = message.viewNumber & 0xff;
        int uintNewViewNumber = message.newViewNumber & 0xff;
        int uintExpectedViewNumber = 0xff & context.expectedView[payload.validatorIndex.intValue()];
        if (uintNewViewNumber <= uintExpectedViewNumber) {
            TR.exit();
            return;
        }
        log(LogLevel.Info, "onChangeViewReceived: height = %d view = %d index = %d nv = %d ",
                payload.blockIndex.intValue(), uintViewNumber, payload.validatorIndex.intValue(), uintNewViewNumber);

        context.expectedView[payload.validatorIndex.intValue()] = message.newViewNumber;
        CheckExpectedView(message.newViewNumber);
        TR.exit();
    }


    /**
     * Consensus message checking and processing
     *
     * @param payload consensus message payload
     * @docs This node will ignore the message when one the below cases happens: <ul>
     * <li>1. This node has already send the full prosoal block before enter the next round. </li>
     * <li> 2. The payload was sent from myself.</li>
     * <li> 3. The payload's version is not equal to the current context's version.</li>
     * <li>4. If the context's BlockIndex is less than the payload's, then send a sync log and
     * ignore. </li>
     * <li> 5. If the index of the payload's validator is more than the number of current context's
     * validators, then ignore.</li>
     * <li> 6. Deserialize consensus message from the payload's `Data`, if the consensus message's
     * view number is same to the context's view number and the conosensus message is not
     * `ChangeView`, then ignore.</li>
     * <li> 7. If the consensus message is not a `ChangeView`, `PrepareResponse` or
     * `PrepareResponse`, then ignore</li>
     * </ul>
     */
    private void onConsensusPayload(ConsensusPayload payload) {
        TR.enter();
        if (context.state.hasFlag(ConsensusState.BlockSent)) {
            TR.exit();
            return;
        }
        if (payload.validatorIndex.intValue() == context.myIndex) {
            TR.exit();
            return;
        }
        if (payload.version != ConsensusContext.VERSION) {
            TR.exit();
            return;
        }
        if (!payload.prevHash.equals(context.prevHash) || !payload.blockIndex.equals(context.blockIndex)) {
            if (context.blockIndex.compareTo(payload.blockIndex) < 0) {
                log(LogLevel.Warning, "chain sync: expected=%d current=%d nodes=%d",
                        payload.blockIndex.intValue(), context.blockIndex.intValue() - 1, LocalNode.singleton().getConnectedCount());
            }
            TR.exit();
            return;
        }
        if (payload.validatorIndex.intValue() >= context.validators.length) {
            TR.exit();
            return;
        }
        ConsensusMessage message;
        try {
            message = ConsensusMessage.deserializeFrom(payload.data);
        } catch (Exception e) {
            // just ignore this message
            TR.exit();
            return;
        }
        if (message.viewNumber != context.viewNumber && message.type != ConsensusMessageType.ChangeView)
            return;
        switch (message.type) {
            case ChangeView:
                onChangeViewReceived(payload, (ChangeView) message);
                break;
            case PrepareRequest:
                onPrepareRequestReceived(payload, (PrepareRequest) message);
                break;
            case PrepareResponse:
                OnPrepareResponseReceived(payload, (PrepareResponse) message);
                break;
        }
        TR.exit();
    }

    /**
     * BlockPersistComleted message proccessing
     */
    private void onPersistCompleted(Block block) {
        TR.enter();
        log(LogLevel.Info, "persist block: %s", block.hash());
        blockReceivedTime = TimeProvider.current().utcNow();
        initializeConsensus((byte) 0);
        TR.exit();
    }


    /**
     * PrepareRequest message proccessing
     *
     * @param payload consensus p2p message payload
     * @param message prepare-request message
     * @doc 1. Validate the message<br/> 2. Reserve the proposal block's data<br/> 3. Filter
     * signatures <br/>  4. Send inv message to acquire the missing txs
     */
    private void onPrepareRequestReceived(ConsensusPayload payload, PrepareRequest message) {
        TR.enter();
        if (context.state.hasFlag(ConsensusState.RequestReceived)) {
            TR.exit();
            return;
        }
        if (payload.validatorIndex.intValue() != context.primaryIndex.intValue()) {
            TR.exit();
            return;
        }
        log(LogLevel.Info, "OnPrepareRequestReceived: height=%d view=%d index=%d tx=%d",
                payload.blockIndex.intValue(), message.viewNumber & 0xff,
                payload.validatorIndex.intValue(), message.transactionHashes.length);

        if (!context.state.hasFlag(ConsensusState.Backup)) {
            return;
        }
        if (payload.timestamp.longValue() <= context.getPrevHeader().timestamp.longValue()
                || payload.timestamp.longValue() > TimeProvider.current().utcNow().getTime() + Duration.ofMinutes(10).toMillis()) {
            log(LogLevel.Warning, "Timestamp incorrect: %d", payload.timestamp.intValue());
            TR.exit();
            return;
        }
        if (Arrays.stream(message.transactionHashes).anyMatch(p -> context.transactionExists(p))) {
            log(LogLevel.Warning, "Invalid request: transaction already exists");
            TR.exit();
            return;
        }

        context.state = context.state.or(ConsensusState.RequestReceived);
        context.timestamp = payload.timestamp;
        context.nonce = message.nonce;
        context.nextConsensus = message.nextConsensus;
        context.transactionHashes = message.transactionHashes;
        context.transactions = new HashMap<>();

        byte[] hashData = context.makeHeader().getHashData();
        byte[] publicKeyBytes = context.validators[payload.validatorIndex.intValue()].getEncoded(false);

        if (!Crypto.Default.verifySignature(hashData, message.signature, publicKeyBytes)) {
            TR.exit();
            return;
        }

        for (int i = 0; i < context.signatures.length; i++) {
            if (context.signatures[i] != null) {
                if (!Crypto.Default.verifySignature(hashData, context.signatures[i], context.validators[i].getEncoded(false))) {
                    context.signatures[i] = null;
                }
            }
        }

        context.signatures[payload.validatorIndex.intValue()] = message.signature;
        Map<UInt256, Transaction> mempoolVerified = Blockchain.singleton().getMemPool().getVerifiedTransactions()
                .stream().collect(Collectors.toMap(p -> p.hash(), p -> p));

        ArrayList<Transaction> unverified = new ArrayList<>();
        for (UInt256 hash : Arrays.stream(context.transactionHashes).skip(1).collect(Collectors.toList())) {
            if (mempoolVerified.containsKey(hash)) {
                if (!addTransaction(mempoolVerified.get(hash), false)) {
                    TR.exit();
                    return;
                }
            } else {
                Out<Transaction> out = new Out<>();
                if (Blockchain.singleton().getMemPool().tryGetValue(hash, out)) {
                    unverified.add(out.get());
                }
            }
        }
        for (Transaction tx : unverified) {
            if (!addTransaction(tx, true)) {
                TR.exit();
                return;
            }
        }

        if (!addTransaction(message.minerTransaction, true)) {
            TR.exit();
            return;
        }

        if (context.transactions.size() < context.transactionHashes.length) {
            UInt256[] hashes = Arrays.stream(context.transactionHashes)
                    .filter(p -> !context.transactions.containsKey(p))
                    .toArray(UInt256[]::new);

            taskManager.tell(new TaskManager.RestartTasks() {{
                payload = InvPayload.create(InventoryType.Tx, hashes);
            }}, self());
        }
        TR.exit();
    }

    /**
     * PrepareResponse message processing
     *
     * @param payload consensus p2p message
     * @param message prepare-response message
     * @doc If the PrepareRequest received before, verify the signature, otherwise, reserve the
     * signature and will filter it in PrepareRequest processing.
     */
    private void OnPrepareResponseReceived(ConsensusPayload payload, PrepareResponse message) {
        TR.enter();
        int validatorIndex = payload.validatorIndex.intValue();
        if (context.signatures[validatorIndex] != null) {
            TR.exit();
            return;
        }

        log(LogLevel.Info, "OnPrepareResponseReceived: height=%d view=%d index=%d",
                payload.blockIndex.longValue(), message.viewNumber & 0xff, validatorIndex);

        byte[] hashData = null;
        Block header = context.makeHeader();
        if (header != null) {
            hashData = header.getHashData();
        }

        byte[] publicKeyBytes = context.validators[validatorIndex].getEncoded(false);
        if (hashData == null) {
            context.signatures[validatorIndex] = message.signature;
        } else if (Crypto.Default.verifySignature(hashData, message.signature, publicKeyBytes)) {
            context.signatures[validatorIndex] = message.signature;
            checkSignatures();
        }
        TR.exit();
    }

    /**
     * State message processing, initialize consensus activity
     */
    private void onStart() {
        TR.enter();
        log(LogLevel.Info, "OnStart");
        initializeConsensus((byte) 0);
        TR.exit();
    }


    /**
     * Timeout processing
     *
     * @doc If it's Primary and has not send PrepareRequest message, then send PrepareRequest
     * message, otherwise, send ChangeView
     */
    private void onTimer(Timer timer) {
        TR.enter();
        if (context.state.hasFlag(ConsensusState.BlockSent)) {
            TR.exit();
            return;
        }
        if (timer.height != context.blockIndex || timer.viewNumber != context.viewNumber) {
            TR.exit();
            return;
        }
        log(LogLevel.Info, "timeout: height={timer.Height} view={timer.ViewNumber} state={context.State}");

        if (context.state.hasFlag(ConsensusState.Primary) && !context.state.hasFlag(ConsensusState.RequestSent)) {
            log(LogLevel.Info, "send prepare request: height=%d view=%d",
                    timer.height.intValue(), timer.viewNumber & 0xff);
            context.state = context.state.or(ConsensusState.RequestSent);
            if (!context.state.hasFlag(ConsensusState.SignatureSent)) {
                context.fill();
                context.signHeader();
            }
            localNode.tell(new LocalNode.SendDirectly(context.makePrepareRequest()), self());
            if (context.transactionHashes.length > 1) {
                for (InvPayload payload : InvPayload.createGroup(InventoryType.Tx, Arrays.stream(context.transactionHashes).skip(1).toArray(UInt256[]::new))) {
                    localNode.tell(Message.create("inv", payload), self());
                }
            }
            changeTimer(Duration.ofSeconds(Blockchain.SecondsPerBlock << (timer.viewNumber + 1)));
        } else if ((context.state.hasFlag(ConsensusState.Primary) && context.state.hasFlag(ConsensusState.RequestSent))
                || context.state.hasFlag(ConsensusState.Backup)) {
            requestChangeView();
        }
        TR.exit();
    }

    /**
     * New transaction message processing
     */
    private void onTransaction(Transaction transaction) {
        TR.enter();
        if (transaction.type == TransactionType.MinerTransaction) {
            TR.exit();
            return;
        }
        if (!context.state.hasFlag(ConsensusState.Backup)
                || !context.state.hasFlag(ConsensusState.RequestReceived)
                || context.state.hasFlag(ConsensusState.SignatureSent)
                || context.state.hasFlag(ConsensusState.ViewChanging)
                || context.state.hasFlag(ConsensusState.BlockSent)) {
            TR.exit();
            return;
        }
        if (context.transactions.containsKey(transaction.hash())) {
            TR.exit();
            return;
        }
        if (!Arrays.stream(context.transactionHashes).anyMatch(p -> p.equals(transaction.hash()))) {
            TR.exit();
            return;
        }

        addTransaction(transaction, true);
        TR.exit();
    }

    @Override
    public void postStop() throws Exception {
        TR.enter();
        log(LogLevel.Info, "OnStop");
        context.dispose();

        if (timerToken != null && !timerToken.isCancelled()) {
            timerToken.cancel();
        }
        super.postStop();
        TR.exit();
    }


    /**
     * Send ChangeView message
     */
    private void requestChangeView() {
        TR.enter();
        context.state = context.state.or(ConsensusState.ViewChanging);
        context.expectedView[context.myIndex]++;

        log(LogLevel.Info, "request change view: height=%d view=%d nv=%d state=%d",
                context.blockIndex.longValue(), context.viewNumber & 0xff,
                context.expectedView[context.myIndex] & 0xff, context.state.value() & 0xff);

        changeTimer(Duration.ofSeconds(Blockchain.SecondsPerBlock << (context.expectedView[context.myIndex] + 1)));
        localNode.tell(new LocalNode.SendDirectly(context.makeChangeView()), self());
        CheckExpectedView(context.expectedView[context.myIndex]);
        TR.exit();
    }

    /**
     * ConsensusService priority mailbox, high priority commands are: ConsensusPayload,
     * ConsensusService.SetViewNumber, ConsensusService.Timer, Blockchain.PersistCompleted. the
     * others are low priority.
     */
    public static class ConsensusServiceMailbox extends PriorityMailbox {

        public ConsensusServiceMailbox(ActorSystem.Settings settings, Config config) {
        }

        @Override
        protected boolean isHighPriority(Object message) {
            if (message instanceof ConsensusPayload
                    || message instanceof ConsensusService.SetViewNumber
                    || message instanceof ConsensusService.Timer
                    || message instanceof Blockchain.PersistCompleted) {
                return true;
            }
            return false;
        }
    }


}
