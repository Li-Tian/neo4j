package neo.io.actors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;

import akka.actor.ActorRef;
import akka.dispatch.Envelope;
import akka.dispatch.MessageQueue;
import akka.dispatch.UnboundedMessageQueueSemantics;
import neo.log.tr.TR;

/**
 * Customized priority message queue, has defined two priorities: high priority, and low priority.
 * Also,it support for the Idle message, but do nothing.
 */
public class PriorityMessageQueue implements MessageQueue, UnboundedMessageQueueSemantics {

    private final ConcurrentLinkedQueue<Envelope> high = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Envelope> low = new ConcurrentLinkedQueue<>();
    private final AtomicInteger count = new AtomicInteger(1);

    private final Function<Object, Boolean> priorityGenerator;
    private final BiPredicate<Object, Collection<Object>> dropper;

    /**
     * Create a PriorityMessageQueue
     *
     * @param dropper           drop generator, check whether to drop the current message.
     * @param priorityGenerator priority generator to get the current message priority.
     */
    public PriorityMessageQueue(BiPredicate<Object, Collection<Object>> dropper, Function<Object, Boolean> priorityGenerator) {
        this.priorityGenerator = priorityGenerator;
        this.dropper = dropper;
    }

    /**
     * Add a message to the message queue
     *
     * @param receiver message receiver
     * @param handle   specific message
     * @note If the message of the envelope is an Idle message, it will do nothing.
     */
    @Override
    public void enqueue(ActorRef receiver, Envelope handle) {
        TR.enter();

        count.incrementAndGet();

        Object msg = handle.message();
        if (msg instanceof Idle) {
            TR.exit();
            return;
        }
        // 过滤空袭消息

        ArrayList<Object> msgs = new ArrayList<>(high.size() + low.size());
        msgs.addAll(high);
        msgs.addAll(low);
        if (dropper.test(msg, msgs)) {
            TR.exit();
            return;
        }
        ConcurrentLinkedQueue<Envelope> queue = priorityGenerator.apply(msg) ? high : low;
        queue.add(handle);
        TR.exit();
    }

    /**
     * Extract an envelope from the message queue
     *
     * @return envelope
     * @note it will end with an envelope with an idle message, when the high priority and low
     * priority queue is empty.
     */
    @Override
    public Envelope dequeue() {
        TR.enter();

        if (!high.isEmpty()) {
            return TR.exit(high.poll());
        }
        if (!low.isEmpty()) {
            return TR.exit(low.poll());
        }
        if (count.getAndSet(0) > 0) {
            return TR.exit(new Envelope(Idle.instance(), ActorRef.noSender()));
        }
        return TR.exit(null);
    }

    /**
     * get the number of messages
     *
     * @return the number of messages
     */
    @Override
    public int numberOfMessages() {
        TR.enter();

        return TR.exit(high.size() + low.size());
    }

    /**
     * has messages in the priority queue
     */
    @Override
    public boolean hasMessages() {
        TR.enter();

        return TR.exit(!high.isEmpty() || !low.isEmpty());
    }

    /**
     * clean up the message queue, but currently it's a empty method, do nothing.
     *
     * @param owner       owner of the message queue
     * @param deadLetters dead letters
     */
    @Override
    public void cleanUp(ActorRef owner, MessageQueue deadLetters) {
        TR.enter();
        TR.exit();
    }
}
