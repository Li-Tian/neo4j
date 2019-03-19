package neo.exception;

import neo.log.notr.TR;

public class FormatException extends RuntimeException {
    
    public FormatException(String info) {
        super(info);
        TR.enter();
        TR.exit();
    }

    public FormatException(String format, Object... params) {
        super(String.format(format, params));
        TR.enter();
        TR.exit();
    }

    public FormatException() {
        super();
        TR.enter();
        TR.exit();
    }
}
