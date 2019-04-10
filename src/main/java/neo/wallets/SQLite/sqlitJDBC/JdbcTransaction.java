package neo.wallets.SQLite.sqlitJDBC;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by doubi.liu on 2017/5/18.
 * 数据库事务处理模板
 */
public class JdbcTransaction {
    public static void beginTransaction(Connection conn) throws DataAccessException {
        try {
            conn.setAutoCommit(false);//设置手动连接
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public static void commit(Connection conn) throws DataAccessException {
        try {
            conn.commit();//提交事务
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public static void rollback(Connection conn) throws DataAccessException {
        try {
            conn.rollback();//事务回滚
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }
}
