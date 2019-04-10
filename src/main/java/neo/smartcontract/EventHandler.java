package neo.smartcontract;

import java.util.ArrayList;
import java.util.List;

import neo.log.notr.TR;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: EventHandler
 * @Package neo.smartcontract
 * @Description: 一个监听器，委托代理方法的实现
 * @date Created in 17:54 2019/3/13
 */
public class EventHandler<T> {
    //监视者存储器
    private List<Listener<T>> listeners = new ArrayList<>();

    /**
     * @param sender    消息的发送者
     * @param eventArgs 消息参数
     * @Author:doubi.liu
     * @description:监听器的执行方法
     * @date:2019/3/29
     */
    public void invoke(Object sender, T eventArgs) {
        TR.enter();
        for (Listener listener : listeners) {
            try {
                listener.doWork(sender, eventArgs);
            } catch (Exception e) {
                TR.fixMe("c#委托代理移植");
                throw TR.exit(new RuntimeException(e));
            }

        }
        TR.exit();
    }

    /**
     * @param listener 新的监视者对象
     * @Author:doubi.liu
     * @description:添加新的监视者对象
     * @date:2019/3/29
     */
    public void addListener(Listener<T> listener) {
        TR.enter();
        listeners.add(listener);
        TR.exit();
    }

    /**
     * @param listener 现有的监视者对象
     * @Author:doubi.liu
     * @description:移除现有的监视者对象
     * @date:2019/3/29
     */
    public void removeListener(Listener<T> listener) {
        TR.enter();
        listeners.remove(listener);
        TR.exit();
    }

    /**
     * @Author:doubi.liu
     * @description:清空现有的监视者
     * @date:2019/3/29
     */
    public void clear() {
        TR.enter();
        listeners.clear();
        TR.exit();
    }

    public interface Listener<T> {
        /**
         * @param sender    消息的发送者
         * @param eventArgs 消息参数
         * @Author:doubi.liu
         * @description:监视者统一执行接口
         * @date:2019/3/29
         */
        public void doWork(Object sender, T eventArgs);
    }
}