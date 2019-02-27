package neo.io.actors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import akka.actor.ActorRef;
import akka.dispatch.Envelope;
import akka.dispatch.MessageQueue;
import akka.dispatch.UnboundedMessageQueueSemantics;
import neo.function.FuncA2T;
import neo.function.FuncAB2T;
import neo.log.tr.TR;

public class PriorityMessageQueue implements MessageQueue, UnboundedMessageQueueSemantics {

    private final ConcurrentLinkedQueue<Envelope> high = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Envelope> low = new ConcurrentLinkedQueue<>();

    private AtomicInteger count = new AtomicInteger(1);

    private final FuncA2T<Object, Boolean> priorityGenerator;
    private final FuncAB2T<Object, Collection<Object>, Boolean> dropper;

    public PriorityMessageQueue(FuncAB2T<Object, Collection<Object>, Boolean> dropper, FuncA2T<Object, Boolean> priorityGenerator) {
        this.priorityGenerator = priorityGenerator;
        this.dropper = dropper;
    }

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
        if (dropper.get(msg, msgs)) {
            TR.exit();
            return;
        }
        ConcurrentLinkedQueue<Envelope> queue = priorityGenerator.get(msg) ? high : low;
        queue.add(handle);
        TR.exit();
    }

    @Override
    public Envelope dequeue() {
        TR.enter();

        if (!high.isEmpty()) return TR.exit(high.poll());
        if (!low.isEmpty()) return TR.exit(low.poll());

        if (count.getAndSet(0) > 0) {
            return TR.exit(new Envelope(Idle.instance(), ActorRef.noSender()));
        }
        return TR.exit(null);
    }

    @Override
    public int numberOfMessages() {
        TR.enter();

        return TR.exit(high.size() + low.size());
    }

    @Override
    public boolean hasMessages() {
        TR.enter();

        return TR.exit(!high.isEmpty() || !low.isEmpty());
    }

    @Override
    public void cleanUp(ActorRef owner, MessageQueue deadLetters) {
        TR.enter();
        TR.exit();
    }
}
