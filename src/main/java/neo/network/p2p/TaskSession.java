package neo.network.p2p;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import akka.actor.ActorRef;
import neo.UInt256;
import neo.network.p2p.payloads.VersionPayload;

/**
 * Task session, store each remote node's task of synchronized blocks and headers
 */
public class TaskSession {

    /**
     * The related remote node
     */
    public final ActorRef remoteNode;

    /**
     * The version of the related remote node
     */
    public final VersionPayload version;

    /**
     * task set, bind with the start date of the task.
     */
    public final HashMap<UInt256, Date> tasks = new HashMap<>();

    /**
     * current
     */
    public final HashSet<UInt256> availableTasks = new HashSet<>();

    /**
     * create a TaskSession
     *
     * @param node    the related remote node
     * @param version the version of the remote node
     */
    public TaskSession(ActorRef node, VersionPayload version) {
        this.remoteNode = node;
        this.version = version;
    }

    /**
     * has task of synchronized blocks
     */
    public boolean hasTask() {
        return !tasks.isEmpty();
    }

    /**
     * has task of synchronized headers
     */
    public boolean headerTask() {
        return tasks.containsKey(UInt256.Zero);
    }
}
