package neo.exception;

import neo.log.tr.TR;

public class FormatException extends RuntimeException {
    public FormatException(String info) {
        super(info);
        TR.enter();
        TR.exit();
    }

    public FormatException() {
        super();
        TR.enter();
        TR.exit();
    }
}
