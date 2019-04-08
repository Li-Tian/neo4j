package neo.Wallets.SQLite.sqlitJDBC.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import neo.Wallets.SQLite.Key;
import neo.Wallets.SQLite.sqlitJDBC.DataAccessException;
import neo.Wallets.SQLite.sqlitJDBC.JdbcTemplate;
import neo.Wallets.SQLite.sqlitJDBC.PreparedStatementSetter;
import neo.Wallets.SQLite.sqlitJDBC.RowCallBackHandler;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: KeyDao
 * @Package neo.Wallets.SQLite.sqlitJDBC.dao
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 13:46 2019/3/26
 */
public class KeyDao {

    public int createTable(Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="CREATE TABLE IF NOT EXISTS \"Key\" (\n" +
                "  \"Name\" VarChar NOT NULL,\n" +
                "  \"Value\" VarBinary NOT NULL,\n" +
                "  CONSTRAINT \"PK_Key\" PRIMARY KEY (\"Name\")\n" +
                ");";
        return jt.update(sql,null);
    }

    public int deleteTable(Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="DROP TABLE IF EXISTS Key;";
        return jt.update(sql,null);
    }

    public int insertKey(Key key, Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="insert into Key(Name,Value) values(?,?);";
        return jt.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setString(1,key.getName());
                pstmt.setBytes(2,key.getValue());
            }
        });
    }

    public int updateKey(Key key, Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="Update Key set Value=? where Name=?;";
        return jt.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,key.getValue());
                pstmt.setString(2,key.getName());
            }
        });
    }

    public int deleteKey(Key key, Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="delete from Key where Name=?;";
        return jt.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setString(1,key.getName());
            }
        });
    }

    public List<Key> queryKeyByKey(Key key, Connection conn) throws DataAccessException {
        List<Key> resultlist=new ArrayList<>();
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="select Name,Value from Key where Name=?;";
        jt.query(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setString(1,key.getName());
            }
        }, new RowCallBackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                while(rs.next()){
                    Key tempKey=new Key();
                    tempKey.setName(rs.getString(1));
                    tempKey.setValue(rs.getBytes(2));
                    resultlist.add(tempKey);
                }
            }
        });
        return resultlist;
    }
}