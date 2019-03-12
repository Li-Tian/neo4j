package neo.exception;

import neo.log.tr.TR;

public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String info) {
        super(info);
        TR.enter();
        TR.exit();
    }

    public InvalidOperationException(String format, Object... params) {
        super(String.format(format, params));
        TR.enter();
        TR.exit();
    }

    public InvalidOperationException() {
        super();
        TR.enter();
        TR.exit();
    }
}
