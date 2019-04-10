package neo.wallets.SQLite.sqlitJDBC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by doubi.liu on 2017/5/16.
 * 数据库连接关闭工具，用于关闭与数据库的connect和结果集等
 */
public class DBUtils {
	public static void close(ResultSet rs, 
					Statement stmt, Connection conn) throws DataAccessException {
		if(rs != null){
			try {
				rs.close();
			} catch (SQLException e) {
				throw new DataAccessException(e.getMessage(),e);
			}
		}
		
		if(stmt != null){
			try {
				stmt.close();
			} catch (SQLException e) {
				throw new DataAccessException(e.getMessage(),e);
			}
		}
		
		if(conn != null){
			try {
				conn.close();
			} catch (SQLException e) {
				throw new DataAccessException(e.getMessage(),e);
			}
		}

	}
	
	public static void close(ResultSet rs, Statement stmt) throws DataAccessException {
		close(rs, stmt, null);
	}
	
	public static void close(Connection conn) throws DataAccessException {
		close(null, null, conn);
	}
	
	public static void close(Statement stmt) throws DataAccessException {
		close(null, stmt, null);
	}
	
	
}
