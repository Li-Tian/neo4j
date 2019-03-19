package neo.exception;

import neo.log.notr.TR;

public class DeserializeFailedException extends RuntimeException {

    public DeserializeFailedException(String info) {
        super(info);
        TR.enter();
        TR.exit();
    }

}
