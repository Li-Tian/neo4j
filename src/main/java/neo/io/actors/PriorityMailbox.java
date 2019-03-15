package neo.io.actors;

import java.util.Collection;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.MailboxType;
import akka.dispatch.MessageQueue;
import akka.dispatch.ProducesMessageQueue;
import scala.Option;

/**
 * Customized priority mail box
 */
public abstract class PriorityMailbox implements MailboxType, ProducesMessageQueue<PriorityMessageQueue> {

    @Override
    public MessageQueue create(Option<ActorRef> owner, Option<ActorSystem> system) {
        return new PriorityMessageQueue((message, collect) -> shallDrop(message, collect), (message) -> isHighPriority(message));
    }

    /**
     * check whether the message is high priority
     *
     * @param message specific message
     * @return true - high priority, false - low priority
     */
    protected boolean isHighPriority(Object message) {
        return false;
    }

    /**
     * check whether to drop the message
     *
     * @param message the specific message
     * @param queue   the message queue of receiver
     * @return true - drop, false - reserve
     */
    protected boolean shallDrop(Object message, Collection<Object> queue) {
        return false;
    }
}
