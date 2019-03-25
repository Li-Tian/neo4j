package neo.network.p2p;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import akka.actor.ActorRef;
import neo.UInt256;
import neo.log.notr.TR;
import neo.network.p2p.payloads.VersionPayload;

/**
 * Task session, store each remote node's task. It has two main task list, the `tasks` is the
 * executing task list, the `availableTasks` is the available block task to be executed.
 */
public class TaskSession {

    /**
     * The related remote node
     */
    public final ActorRef remoteNode;

    /**
     * The VERSION of the related remote node
     */
    public final VersionPayload version;

    /**
     * Currently, executing task list
     */
    public final HashMap<UInt256, Date> tasks = new HashMap<>();

    /**
     * Available block task to be executed, here it maybe use for task retry mechanism
     */
    //TODO 确认 availableTasks 的用途以及命名是否正确
    public final HashSet<UInt256> availableTasks = new HashSet<>();

    /**
     * create a TaskSession
     *
     * @param node    the related remote node
     * @param version the VERSION of the remote node
     */
    public TaskSession(ActorRef node, VersionPayload version) {
        this.remoteNode = node;
        this.version = version;
    }

    /**
     * has task of synchronized blocks
     */
    public boolean hasTask() {
        TR.enter();
        return TR.exit(!tasks.isEmpty());
    }

    /**
     * has task of synchronized headers
     */
    public boolean headerTask() {
        TR.enter();
        return TR.exit(tasks.containsKey(UInt256.Zero));
    }
}
