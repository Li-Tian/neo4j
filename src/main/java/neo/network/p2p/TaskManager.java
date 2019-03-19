package neo.network.p2p;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
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

public class TaskManager extends UntypedActor {

    public static class Register {
        public VersionPayload version;
    }

    public static class NewTasks {
        public InvPayload payload;
    }

    public static class TaskCompleted {
        public UInt256 hash;
    }

    public static class HeaderTaskCompleted {
    }

    public static class RestartTasks {
        public InvPayload payload;
    }

    private static class Timer {
    }


    private static final Duration TimerInterval = Duration.ofSeconds(30);
    private static final Duration TaskTimeout = Duration.ofMinutes(1);

    private static final int MaxConcurrentTasks = 3;

    private final NeoSystem system;

    //
    private final HashSet<UInt256> knownHashes = new HashSet<>();

    // all the task currently
    private final HashMap<UInt256, Integer> globalTasks = new HashMap<>();
    private final HashMap<ActorRef, TaskSession> sessions = new HashMap<>();

    // timer for cleaning timeout task
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
        if (!sessions.containsKey(sender())) {
            return;
        }
        TaskSession session = sessions.get(sender());
        session.tasks.remove(HeaderTaskHash);

        decrementGlobalTask(HeaderTaskHash);
        requestTasks(session);
    }

    private void onNewTasks(InvPayload payload) {
        if (!sessions.containsKey(sender())) {
            return;
        }
        TaskSession session = sessions.get(sender());

        Uint blockHeight = Blockchain.singleton().getHeight();
        Uint headerHeight = Blockchain.singleton().getHeaderHeight();
        if (payload.type == InventoryType.Tx && blockHeight.compareTo(headerHeight) < 0) {
            requestTasks(session);
            return;
        }
        HashSet<UInt256> hashes = new HashSet<>(Arrays.asList(payload.hashes));
        hashes.removeAll(knownHashes);
        if (payload.type == InventoryType.Block) {
            session.availableTasks.addAll(hashes.stream().filter(globalTasks::containsKey).collect(Collectors.toSet()));
        }

        hashes.remove(globalTasks.keySet());
        if (hashes.isEmpty()) {
            requestTasks(session);
            return;
        }

        for (UInt256 hash : hashes) {
            incrementGlobalTask(hash);
            session.tasks.put(hash, TimeProvider.current().utcNow());
        }
        for (InvPayload invPayload : InvPayload.createGroup(payload.type, hashes)) {
            sender().tell(Message.create("getdata", invPayload), self());
        }
    }

    private void onRegister(VersionPayload version) {
        context().watch(getSender());
        TaskSession session = new TaskSession(getSender(), version);
        sessions.put(getSender(), session);
        requestTasks(session);
    }

    private void onRestartTasks(InvPayload payload) {
        knownHashes.removeAll(Arrays.asList(payload.hashes));
        for (UInt256 hash : payload.hashes) {
            globalTasks.remove(hash);
        }
        for (InvPayload group : InvPayload.createGroup(payload.type, payload.hashes)) {
            system.localNode.tell(Message.create("getdata", group), self());
        }
    }

    private void onTaskCompleted(UInt256 hash) {
        knownHashes.add(hash);
        globalTasks.remove(hash);
        for (TaskSession ms : sessions.values()) {
            ms.availableTasks.remove(hash);
        }
        if (sessions.containsKey(getSender())) {
            TaskSession session = sessions.get(getSender());
            session.tasks.remove(hash);
            requestTasks(session);
        }
    }


    //    MethodImpl(MethodImplOptions.AggressiveInlining)]
    private void decrementGlobalTask(UInt256 hash) {
        if (globalTasks.containsKey(hash)) {
            int count = globalTasks.get(hash);

            if (count == 1) {
                globalTasks.remove(hash);
            } else {
                globalTasks.put(hash, count - 1);
            }
        }
    }

    //    MethodImpl(MethodImplOptions.AggressiveInlining)]
    private boolean incrementGlobalTask(UInt256 hash) {
        if (!globalTasks.containsKey(hash)) {
            globalTasks.put(hash, 1);
            return true;
        }
        int count = globalTasks.get(hash);
        if (count >= MaxConcurrentTasks) {
            return false;
        }

        globalTasks.put(hash, count + 1);
        return true;
    }

    private void onTerminated(ActorRef actor) {
        if (!sessions.containsKey(actor)) {
            return;
        }
        TaskSession session = sessions.get(actor);
        sessions.remove(actor);
        for (UInt256 hash : session.tasks.keySet()) {
            decrementGlobalTask(hash);
        }
    }

    private void onTimer() {
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
    }

    private void requestTasks(TaskSession session) {
        if (session.hasTask()) {
            return;
        }

        Blockchain blockchain = Blockchain.singleton();
        if (session.availableTasks.size() > 0) {
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
                return;
            }
        }
        Uint headerHeight = blockchain.getHeaderHeight();
        Uint blockHeight = blockchain.getHeight();
        Uint otherStartHeight = session.version.startHeight;

        if ((!hasHeaderTask() || globalTasks.get(HeaderTaskHash) < MaxConcurrentTasks)
                && headerHeight.compareTo(otherStartHeight) < 0) {
            // C# code: session.Tasks[HeaderTaskHash] =DateTime.UtcNow;
            session.tasks.put(HeaderTaskHash, TimeProvider.current().utcNow());
            incrementGlobalTask(HeaderTaskHash);
            session.remoteNode.tell(Message.create("getheaders", GetBlocksPayload.create(blockchain.getCurrentHeaderHash())), self());
        } else if (blockHeight.compareTo(otherStartHeight) < 0) {
            UInt256 hash = blockchain.getCurrentBlockHash();
            for (Uint i = blockchain.getHeight().add(Uint.ONE); i.compareTo(blockchain.getHeaderHeight()) <= 0; i = i.add(Uint.ONE)) {
                hash = blockchain.getBlockHash(i);
                if (!globalTasks.containsKey(hash)) {
                    hash = blockchain.getBlockHash(i.subtract(Uint.ONE));
                    break;
                }
            }
            session.remoteNode.tell(Message.create("getblocks", GetBlocksPayload.create(hash)), self());
        }
    }

    public static class TaskManagerMailbox extends PriorityMailbox {

        @Override
        protected boolean isHighPriority(Object message) {
            if (message instanceof TaskManager.Register) {
                return true;
            }

            if (message instanceof TaskManager.NewTasks) {
                NewTasks tasks = (NewTasks) message;
                InventoryType type = tasks.payload.type;
                if (type == InventoryType.Block || type == InventoryType.Consensus) {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
    }


    @Override
    public void postStop() throws Exception {
        if (timer != null && !timer.isCancelled()) {
            timer.cancel();
        }
        super.postStop();
    }


    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof Register) {
            Register register = (Register) message;
            onRegister(register.version);
            return;
        }

        if (message instanceof NewTasks) {
            NewTasks tasks = (NewTasks) message;
            onNewTasks(tasks.payload);
            return;
        }

        if (message instanceof TaskCompleted) {
            TaskCompleted completed = (TaskCompleted) message;
            onTaskCompleted(completed.hash);
            return;
        }

        if (message instanceof HeaderTaskCompleted) {
            onHeaderTaskCompleted();
            return;
        }

        if (message instanceof RestartTasks) {
            RestartTasks restartTasks = (RestartTasks) message;
            onRestartTasks(restartTasks.payload);
            return;
        }

        if (message instanceof Timer) {
            onTimer();
            return;
        }

        if (message instanceof Terminated) {
            Terminated terminated = (Terminated) message;
            onTerminated(terminated.actor());
            return;
        }
    }
}
