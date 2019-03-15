package neo.io.actors;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiPredicate;
import java.util.function.Function;

import akka.actor.ActorRef;
import akka.dispatch.Envelope;
import akka.dispatch.MessageQueue;

import static org.junit.Assert.*;

public class PriorityMessageQueueTest {

    static class MyPriorityMailbox extends PriorityMailbox {

        @Override
        protected boolean isHighPriority(Object obj) {
            if (obj instanceof Integer) {
                Integer value = (Integer) obj;
                return value <= 2;
            }
            return false;
        }

        @Override
        protected boolean shallDrop(Object obj, Collection<Object> queue) {
            if (obj instanceof Integer) {
                Integer message = (Integer) obj;
                return message % 7 == 0;
            }
            return false;
        }
    }

    private BiPredicate<Object, Collection<Object>> dropper7 = (obj, objects) -> {
        if (obj instanceof Integer) {
            Integer message = (Integer) obj;
            return message % 7 == 0;
        }
        return false;
    };


    private Function<Object, Boolean> priorityGenerator = obj -> {
        if (obj instanceof Integer) {
            Integer value = (Integer) obj;
            return value <= 2;
        }
        return false;
    };


    @Test
    public void enqueue() {
        PriorityMessageQueue queue = new PriorityMessageQueue(dropper7, priorityGenerator);
        Envelope envelope = new Envelope(1, ActorRef.noSender());
        queue.enqueue(null, envelope);

        Envelope envelope1 = queue.dequeue();
        Assert.assertEquals(envelope.message(), envelope1.message());
    }

    @Test
    public void numberOfMessages() {
        MessageQueue queue = new PriorityMessageQueue(dropper7, priorityGenerator);
        queue.enqueue(null, new Envelope(1, ActorRef.noSender()));

        Assert.assertEquals(1, queue.numberOfMessages());

        queue.enqueue(null, new Envelope(Idle.instance(), ActorRef.noSender()));
        Assert.assertEquals(1, queue.numberOfMessages());

        queue.enqueue(null, new Envelope(7, ActorRef.noSender()));
        Assert.assertEquals(1, queue.numberOfMessages());
    }

    @Test
    public void hasMessages() {
        PriorityMessageQueue queue = new PriorityMessageQueue(dropper7, priorityGenerator);
        Assert.assertFalse(queue.hasMessages());

        queue.enqueue(null, new Envelope(1, ActorRef.noSender()));
        Assert.assertTrue(queue.hasMessages());

        queue.enqueue(null, new Envelope(Idle.instance(), ActorRef.noSender()));
        queue.enqueue(null, new Envelope(Idle.instance(), ActorRef.noSender()));
        Assert.assertTrue(queue.hasMessages());

        Envelope envelope = queue.dequeue();
        Assert.assertNotNull(envelope);
        Assert.assertEquals(1, envelope.message());
        Assert.assertFalse(queue.hasMessages());

        envelope = queue.dequeue();
        Assert.assertNotNull(envelope);
        Assert.assertTrue(envelope.message() instanceof Idle);
        Assert.assertFalse(queue.hasMessages());

        envelope = queue.dequeue();
        Assert.assertNull(envelope);
        Assert.assertFalse(queue.hasMessages());
    }

    @Test
    public void cleanUp() {
        PriorityMessageQueue queue = new PriorityMessageQueue(dropper7, priorityGenerator);
        queue.enqueue(null, new Envelope(1, ActorRef.noSender()));

        queue.cleanUp(null, null);
        Assert.assertEquals(1, queue.numberOfMessages());
    }
}