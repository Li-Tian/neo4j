package neo.wallets.SQLite.sqlitJDBC;

/**
 * Created by doubi.liu on 2017/5/16.
 */
public class DataAccessException extends Exception{
    public DataAccessException() {
        super();
        //System.out.println("数据库异常");
    }

    public DataAccessException(String message) {
        super(message);
        //System.out.println("数据库异常"+"异常信息："+message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
        //System.out.println("数据库异常"+"异常信息："+message);
    }

    public DataAccessException(Throwable cause) {
        super(cause);
    }

    protected DataAccessException(String message, Throwable cause, boolean enableSuppression,
                                  boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        //System.out.println("数据库异常"+"异常信息："+message);
    }
}
