package neo.smartcontract;

import java.util.ArrayList;
import java.util.List;

import neo.log.notr.TR;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: EventHandler
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:54 2019/3/13
 */
public class EventHandler<T> {
    private List<Listener<T>> listeners=new ArrayList<>();

    public void invoke(Object sender, T eventArgs){
        for (Listener listener:listeners){
            try {
                listener.doWork(sender,eventArgs);
            }catch (Exception e){
                TR.fixMe("c#委托代理移植");
                throw new RuntimeException(e);
            }

        }
    }

    public void addListener(Listener<T> listener){
        listeners.add(listener);
    }

    public void removeListener(Listener<T> listener){
        listeners.remove(listener);
    }

    public void clear(){
        listeners.clear();
    }

    public interface Listener<T>{
        public void doWork(Object sender, T eventArgs);
    }
}