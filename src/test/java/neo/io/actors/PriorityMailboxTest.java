package neo.io.actors;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

import akka.actor.ActorRef;
import akka.dispatch.Envelope;
import akka.dispatch.MessageQueue;

import static org.junit.Assert.*;

public class PriorityMailboxTest {

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

    @Test
    public void create() {
        MyPriorityMailbox mailbox = new MyPriorityMailbox();
        MessageQueue queue = mailbox.create(null, null);

        Assert.assertNotNull(queue);
    }

    @Test
    public void isHighPriority() {
        MyPriorityMailbox mailbox = new MyPriorityMailbox();
        MessageQueue queue = mailbox.create(null, null);

        queue.enqueue(null, new Envelope(1, ActorRef.noSender()));
        Assert.assertEquals(1, queue.numberOfMessages());

        queue.enqueue(null, new Envelope(Idle.instance(), ActorRef.noSender()));
        Assert.assertEquals(1, queue.numberOfMessages());

        queue.enqueue(null, new Envelope(7, ActorRef.noSender()));
        Assert.assertEquals(1, queue.numberOfMessages());
    }

    @Test
    public void shallDrop() {
        MyPriorityMailbox mailbox = new MyPriorityMailbox();
        MessageQueue queue = mailbox.create(null, null);

        queue.enqueue(null, new Envelope(1, ActorRef.noSender()));
        Assert.assertEquals(1, queue.numberOfMessages());

        queue.enqueue(null, new Envelope(Idle.instance(), ActorRef.noSender()));
        Assert.assertEquals(1, queue.numberOfMessages());

        queue.enqueue(null, new Envelope(7, ActorRef.noSender()));
        Assert.assertEquals(1, queue.numberOfMessages());
    }
}