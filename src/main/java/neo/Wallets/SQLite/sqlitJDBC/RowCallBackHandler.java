package neo.Wallets.SQLite.sqlitJDBC;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by doubi.liu on 2017/5/18.
 * 结果集处理接口
 */
public interface RowCallBackHandler {
    void processRow(ResultSet rs) throws SQLException;
}
