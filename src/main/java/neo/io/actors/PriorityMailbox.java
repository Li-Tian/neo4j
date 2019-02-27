package neo.io.actors;

import java.util.Collection;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.MailboxType;
import akka.dispatch.MessageQueue;
import akka.dispatch.ProducesMessageQueue;
import scala.Option;

public abstract class PriorityMailbox implements MailboxType, ProducesMessageQueue<PriorityMessageQueue> {

    @Override
    public MessageQueue create(Option<ActorRef> owner, Option<ActorSystem> system) {
        return new PriorityMessageQueue((message, collect) -> shallDrop(message, collect), (message) -> isHighPriority(message));
    }

    protected abstract boolean isHighPriority(Object message);

    protected abstract boolean shallDrop(Object message, Collection<Object> queue);
}
