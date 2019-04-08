package neo.network.p2p;


import com.typesafe.config.Config;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Terminated;
import neo.NeoSystem;
import neo.TimeProvider;
import neo.UInt256;
import neo.csharp.Uint;
import neo.io.actors.PriorityMailbox;
import neo.ledger.Blockchain;
import neo.network.p2p.payloads.GetBlocksPayload;
import neo.network.p2p.payloads.InvPayload;
import neo.network.p2p.payloads.InventoryType;
import neo.network.p2p.payloads.VersionPayload;

import neo.log.notr.TR;

/**
 * Task manager, it has two main functions: 1) sync header and block with all the connected peers.
 * 2) handle the `inv` message to avoid repeat request.
 *
 * @docs Customized message as the following:
 * <ul>
 * <li>Register: a remote peer connected message</li>
 * <li>NewTasks: a new inv message received</li>
 * <li>TaskCompleted: the inv message's data has been received</li>
 * <li>HeaderTaskCompleted: the block header has been synchronized </li>
 * <li>RestartTasks: restart to request the inv message </li>
 * <li>Terminated: the remote peer offline</li>
 * </ul>
 */
public class TaskManager extends AbstractActor {

    /**
     * Customized akka message, it represents a new remote peer connected, taskManager will check
     * the remote peer's VERSION to sync header and block.
     */
    public static class Register {
        public VersionPayload version;
    }

    /**
     * Customized akka message, it represents a new inv message received, taskManager will request
     * the inventory from the remote node directly. if the inv message is block, it will add the
     * task into `availableTasks` list for retry mechanism. As the block data may be too large to be
     * failed.
     */
    public static class NewTasks {
        public InvPayload payload;
    }

    /**
     * Customized akka message, it represents a inventory received ( inv message's specific data),
     * taskManager will remove the task from the `globalTasks` and `AvailableTasks` and the sender's
     * session.task, at last it will be added into the `knownHashes`(inventory data received
     * already).
     */
    public static class TaskCompleted {
        public UInt256 hash;
    }


    /**
     * Customized akka message, it represents the end of block header synchronization, taskManager
     * will remove the task from the sender's taskSession.
     */
    public static class HeaderTaskCompleted {
    }

    /**
     * Restart to request inv message, which will be send by consensus module.
     */
    public static class RestartTasks {
        public InvPayload payload;
    }

    /**
     * Timer, the interval is 30 seconds to check the task list to executed.
     */
    public static class Timer {
    }


    private static final Duration TimerInterval = Duration.ofSeconds(30);
    private static final Duration TaskTimeout = Duration.ofMinutes(1);

    private static final int MaxConcurrentTasks = 3;

    private final NeoSystem system;

    // inventory data received already
    private final HashSet<UInt256> knownHashes = new HashSet<>();

    // all the executing task currently
    private final HashMap<UInt256, Integer> globalTasks = new HashMap<>();

    // task session list
    private final HashMap<ActorRef, TaskSession> sessions = new HashMap<>();

    // timer for extracting available task to be executed
    private final Cancellable timer = context().system().scheduler().schedule(TimerInterval,
            TimerInterval,
            self(),
            new Timer(),
            context().system().dispatcher(),
            ActorRef.noSender());

    // header task hash,
    private final UInt256 HeaderTaskHash = UInt256.Zero;

    private boolean hasHeaderTask() {
        return globalTasks.containsKey(HeaderTaskHash);
    }


    /**
     * Create a TaskManager
     *
     * @param system neo akka system
     */
    public TaskManager(NeoSystem system) {
        this.system = system;
    }

    private void onHeaderTaskCompleted() {
        TR.enter();

        if (!sessions.containsKey(sender())){
            TR.exit();
            return;
        }
        TaskSession session = sessions.get(sender());
        session.tasks.remove(HeaderTaskHash);

        decrementGlobalTask(HeaderTaskHash);
        requestTasks(session);
        TR.exit();
    }

    private void onNewTasks(InvPayload payload) {
        TR.enter();
        if (!sessions.containsKey(sender())) {
            TR.exit();
            return;
        }
        TaskSession session = sessions.get(sender());

        Uint blockHeight = Blockchain.singleton().getHeight();
        Uint headerHeight = Blockchain.singleton().getHeaderHeight();
        if (payload.type == InventoryType.Tx && blockHeight.compareTo(headerHeight) < 0) {
            // sync block first, when the inv is tx
            requestTasks(session);
            TR.exit();
            return;
        }
        HashSet<UInt256> hashes = new HashSet<>(Arrays.asList(payload.hashes));
        hashes.removeAll(knownHashes);

        // only when the type is block, just add the task into the available task list?
        if (payload.type == InventoryType.Block) {
            // if it's block, we need to add it into the availableTasks list for retry mechanism.
            // As the block data is too large, it may fail when sync data.
            session.availableTasks.addAll(hashes.stream()
                    .filter(globalTasks::containsKey)
                    .collect(Collectors.toSet()));
        }

        hashes.remove(globalTasks.keySet());
        if (hashes.isEmpty()) {
            requestTasks(session);
            TR.exit();
            return;
        }

        for (UInt256 hash : hashes) {// add hash into the executing task list
            incrementGlobalTask(hash);
            session.tasks.put(hash, TimeProvider.current().utcNow());
        }
        // request the specific inventory from the sender
        for (InvPayload invPayload : InvPayload.createGroup(payload.type, hashes)) {
            sender().tell(Message.create("getdata", invPayload), self());
        }
        TR.exit();
    }

    private void onRegister(VersionPayload version) {
        TR.enter();

        ActorRef sender = sender();
        context().watch(sender);

        TaskSession session = new TaskSession(sender, version);
        sessions.put(sender, session);

        requestTasks(session);

        TR.exit();
    }

    private void onRestartTasks(InvPayload payload) {
        TR.enter();

        // restart to request those inventory data
        knownHashes.removeAll(Arrays.asList(payload.hashes));
        for (UInt256 hash : payload.hashes) {
            globalTasks.remove(hash);
        }
        for (InvPayload group : InvPayload.createGroup(payload.type, payload.hashes)) {
            system.localNode.tell(Message.create("getdata", group), self());
        }

        TR.exit();
    }

    private void onTaskCompleted(UInt256 hash) {
        TR.enter();

        knownHashes.add(hash);
        globalTasks.remove(hash);
        for (TaskSession ms : sessions.values()) {
            ms.availableTasks.remove(hash);
        }
        if (sessions.containsKey(sender())) {
            TaskSession session = sessions.get(sender());
            session.tasks.remove(hash);
            requestTasks(session);
        }

        TR.exit();
    }


    //    MethodImpl(MethodImplOptions.AggressiveInlining)]
    private void decrementGlobalTask(UInt256 hash) {
        TR.enter();

        if (globalTasks.containsKey(hash)) {
            int count = globalTasks.get(hash);

            if (count == 1) {
                globalTasks.remove(hash);
            } else {
                globalTasks.put(hash, count - 1);
            }
        }
        TR.exit();
    }

    //    MethodImpl(MethodImplOptions.AggressiveInlining)]
    private boolean incrementGlobalTask(UInt256 hash) {
        TR.enter();

        if (!globalTasks.containsKey(hash)) {
            globalTasks.put(hash, 1);
            return TR.exit(true);
        }
        int count = globalTasks.get(hash);
        if (count >= MaxConcurrentTasks) {
            return TR.exit(false);
        }

        globalTasks.put(hash, count + 1);
        return TR.exit(true);
    }

    private void onTerminated(ActorRef actor) {
        TR.enter();

        if (!sessions.containsKey(actor)) {
            TR.exit();
            return;
        }
        TaskSession session = sessions.get(actor);
        sessions.remove(actor);
        for (UInt256 hash : session.tasks.keySet()) {
            decrementGlobalTask(hash);
        }
        TR.exit();
    }

    private void onTimer() {
        TR.enter();
        for (TaskSession session : sessions.values()) {
            for (Map.Entry<UInt256, Date> entry : session.tasks.entrySet()) {
                // C# code: if (DateTime.UtcNow - task.Value > TaskTimeout
                if (TimeProvider.current().utcNow().getTime() - entry.getValue().getTime() > TaskTimeout.toMillis())
                    if (session.tasks.remove(entry.getKey()) != null) {
                        decrementGlobalTask(entry.getKey());
                    }
            }
        }
        for (TaskSession session : sessions.values()) {
            requestTasks(session);
        }
        TR.exit();
    }

    /**
     * Extract a new task to execute
     *
     * @param session task session
     */
    private void requestTasks(TaskSession session) {
        TR.enter();

        if (session.hasTask()) {
            TR.exit();
            return;
        }

        Blockchain blockchain = Blockchain.singleton();
        if (session.availableTasks.size() > 0) {
            TR.fixMe("TODO 检查该处，是否是死代码!，以及确认 availableTasks 是否正确和命名是否ok ");

            session.availableTasks.removeAll(knownHashes);
            session.availableTasks.removeIf(p -> blockchain.containsBlock(p));

            HashSet<UInt256> hashes = new HashSet<>(session.availableTasks);
            // remove the task are over request, keep the available task

            if (hashes.size() > 0) {
                hashes.removeIf(hash -> !incrementGlobalTask(hash));

                session.availableTasks.removeAll(hashes);
                for (UInt256 hash : hashes) {
                    session.tasks.put(hash, TimeProvider.current().utcNow());
                }

                for (InvPayload group : InvPayload.createGroup(InventoryType.Block, hashes)) {
                    session.remoteNode.tell(Message.create("getdata", group), self());
                }
                TR.exit();
                return;
            }
        }
        Uint headerHeight = blockchain.getHeaderHeight();
        Uint blockHeight = blockchain.getHeight();
        Uint otherStartHeight = session.version.startHeight;

        if ((!hasHeaderTask() || globalTasks.get(HeaderTaskHash) < MaxConcurrentTasks)
                && headerHeight.compareTo(otherStartHeight) < 0) {
            // C# code: session.Tasks[HeaderTaskHash] =DateTime.UtcNow;
            // Firstly, sync block headers, then block data
            session.tasks.put(HeaderTaskHash, TimeProvider.current().utcNow());
            incrementGlobalTask(HeaderTaskHash);
            GetBlocksPayload blocksPayload = GetBlocksPayload.create(blockchain.getCurrentHeaderHash());
            session.remoteNode.tell(Message.create("getheaders", blocksPayload), self());
        } else if (blockHeight.compareTo(otherStartHeight) < 0) {
            // sync block data
            UInt256 hash = blockchain.getCurrentBlockHash();
            for (Uint i = blockchain.getHeight().add(Uint.ONE); i.compareTo(blockchain.getHeaderHeight()) <= 0; i = i.add(Uint.ONE)) {
                hash = blockchain.getBlockHash(i);
                if (!globalTasks.containsKey(hash)) {
                    // find the first hash not in GlobalTask to request
                    hash = blockchain.getBlockHash(i.subtract(Uint.ONE));
                    break;
                }
            }
            session.remoteNode.tell(Message.create("getblocks", GetBlocksPayload.create(hash)), self());
        }
        TR.exit();
    }

    /**
     * TaskManager's priority mailbox. When messages are Register or NewTasks with block or
     * consensus, is high priority. (this class will be create a instance by akka)
     */
    public static class TaskManagerMailbox extends PriorityMailbox {

        public TaskManagerMailbox(ActorSystem.Settings setting, Config config) {
            super();
        }

        @Override
        protected boolean isHighPriority(Object message) {
            TR.enter();

            if (message instanceof TaskManager.Register) {
                return TR.exit(true);
            }

            if (message instanceof TaskManager.NewTasks) {
                NewTasks tasks = (NewTasks) message;
                InventoryType type = tasks.payload.type;
                if (type == InventoryType.Block || type == InventoryType.Consensus) {
                    return TR.exit(true);
                } else {
                    return TR.exit(false);
                }
            }
            return TR.exit(false);
        }
    }


    /**
     * build a RemoteNode object（AKKA Framework）
     *
     * @param system NeoSystem object
     * @return RemoteNode object
     */
    public static Props props(NeoSystem system) {
        TR.enter();
        return TR.exit(Props.create(TaskManager.class, system).withMailbox("task-manager-mailbox"));
    }


    /**
     * after stop the actor, release the timer
     *
     * @throws Exception when some errors occur
     */
    @Override
    public void postStop() throws Exception {
        TR.enter();

        if (timer != null && !timer.isCancelled()) {
            timer.cancel();
        }
        super.postStop();

        TR.exit();
    }

    /**
     * create a message receiver
     *
     * @docs message customized message as the following:
     * <ul>
     * <li>Register: a remote peer connected message</li>
     * <li>NewTasks: a new inv message received</li>
     * <li>TaskCompleted: the inv message's data has been received</li>
     * <li>HeaderTaskCompleted: the block header has been synchronized </li>
     * <li>RestartTasks: restart to request the inv message </li>
     * <li>Timer: timer for check the session's task list </li>
     * <li>Terminated: the remote peer offline</li>
     * </ul>
     */
    @Override
    public Receive createReceive() {
        TR.enter();

        return TR.exit(receiveBuilder()
                .match(Register.class, register -> onRegister(register.version))
                .match(NewTasks.class, tasks -> onNewTasks(tasks.payload))
                .match(TaskCompleted.class, completed -> onTaskCompleted(completed.hash))
                .match(HeaderTaskCompleted.class, task -> onHeaderTaskCompleted())
                .match(RestartTasks.class, restartTasks -> onRestartTasks(restartTasks.payload))
                .match(Timer.class, timer -> onTimer())
                .match(Terminated.class, terminated -> onTerminated(terminated.actor()))
                .build());
    }

}
