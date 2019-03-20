package neo.network.p2p;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;


public class TaskManagerTest {

    private ActorSystem system;
    private ActorRef taskManager;

    @Before
    public void before() {
        Config config = ConfigFactory.load("akka.conf");
        system = ActorSystem.create("NeoSystem", config);
        taskManager = system.actorOf(TaskManager.props(new NeoSystemDemo(null)));
    }


    @Test
    public void postStop() {

    }

    @Test
    public void onReceive() throws InterruptedException {
        TaskManager.HeaderTaskCompleted headerTaskCompleted = new TaskManager.HeaderTaskCompleted();
        taskManager.tell(headerTaskCompleted, ActorRef.noSender());
    }
}