package neo.exception;

public class RpcException extends RuntimeException {
    public RpcException(int code, String message) {
        super(message);
        //HResult = code;
    }
}