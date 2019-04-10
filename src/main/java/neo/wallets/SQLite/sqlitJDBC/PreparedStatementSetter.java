package neo.wallets.SQLite.sqlitJDBC;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by doubi.liu on 2017/5/18.
 * 使用preparestatement处理
 */
public interface PreparedStatementSetter {
    // 通过PreparedStatement来设置一些值来替换占位符"?"
    void setValues(PreparedStatement pstmt) throws SQLException;
}
