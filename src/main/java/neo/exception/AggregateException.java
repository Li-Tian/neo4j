package neo.exception;

import java.util.ArrayList;

public class AggregateException extends RuntimeException {
    public ArrayList<Exception> exceptionList = null;
    public AggregateException(ArrayList<Exception> inputExceptionList) {
        super();
        exceptionList = inputExceptionList;
    }
}