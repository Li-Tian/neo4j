package neo.smartcontract;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: EventHandlerTest
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 15:44 2019/3/29
 */
public class EventHandlerTest {
    @Test
    public void invoke() throws Exception {
        EventHandler<String> handler=new EventHandler<String>();
        handler.invoke(this,"lalalal");
    }

    @Test
    public void addListener() throws Exception {
        List<String> list=new ArrayList<>();
        EventHandler<String> handler=new EventHandler<String>();
        handler.addListener(new EventHandler.Listener<String>() {
            @Override
            public void doWork(Object sender, String eventArgs) {
                 list.add(eventArgs);
            }
        });
        handler.invoke(this,"lalalal");
        Assert.assertEquals(1,list.size());
    }

    @Test
    public void removeListener() throws Exception {
        List<String> list=new ArrayList<>();
        EventHandler<String> handler=new EventHandler<String>();
        EventHandler.Listener listener=new EventHandler.Listener<String>() {
            @Override
            public void doWork(Object sender, String eventArgs) {
                list.add(eventArgs);
            }
        };
        handler.addListener(listener);
        handler.removeListener(listener);
        handler.invoke(this,"lalalal");
        Assert.assertEquals(0,list.size());
    }

    @Test
    public void clear() throws Exception {
        List<String> list=new ArrayList<>();
        EventHandler<String> handler=new EventHandler<String>();
        handler.addListener(new EventHandler.Listener<String>() {
            @Override
            public void doWork(Object sender, String eventArgs) {
                list.add(eventArgs);
            }
        });
        handler.clear();
        handler.invoke(this,"lalalal");
        Assert.assertEquals(0,list.size());
    }

}