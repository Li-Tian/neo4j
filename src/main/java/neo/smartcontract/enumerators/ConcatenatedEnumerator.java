package neo.smartcontract.enumerators;

import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: ConcatenatedEnumerator
 * @Package neo.smartcontract.enumerators
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 16:30 2019/3/12
 */
public class ConcatenatedEnumerator implements IEnumerator {

    private IEnumerator first;
    private IEnumerator second;
    private IEnumerator current;

    public ConcatenatedEnumerator(IEnumerator first, IEnumerator second) {
        this.current = first;
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean next() {
        if (current.next()) {
            return true;
        }
        current = second;
        return current.next();
    }

    @Override
    public StackItem value() {
        return current.value();
    }

    @Override
    public void dispose() {
        first.dispose();
        second.dispose();
    }
}