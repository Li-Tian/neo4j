package neo.Wallets.SQLite.sqlitJDBC.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import neo.Wallets.SQLite.Account;
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
public class AccountDao {

    public int createTable(Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="CREATE TABLE IF NOT EXISTS \"Account\" (\n" +
                "  \"PublicKeyHash\" Binary NOT NULL,\n" +
                "  \"PrivateKeyEncrypted\" VarBinary NOT NULL,\n" +
                "  CONSTRAINT \"PK_Account\" PRIMARY KEY (\"PublicKeyHash\")\n" +
                ");";
        return jt.update(sql,null);
    }

    public int deleteTable(Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="DROP TABLE Account;";
        return jt.update(sql,null);
    }


    public int insertAccount(Account account, Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="insert into Account(PublicKeyHash,PrivateKeyEncrypted) values(?,?);";
        return jt.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,account.getPublicKeyHash());
                pstmt.setBytes(2,account.getPrivateKeyEncrypted());
            }
        });
    }

    public int updateAccount(Account account, Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="Update Account set PrivateKeyEncrypted=? where PublicKeyHash=?;";
        return jt.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,account.getPrivateKeyEncrypted());
                pstmt.setBytes(2,account.getPublicKeyHash());
            }
        });
    }

    public int deleteAccount(Account account, Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="delete from Account where PublicKeyHash=?;";
        return jt.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,account.getPublicKeyHash());
            }
        });
    }

    public List<Account> queryAccountByPublicKeyHash(Account account, Connection conn) throws
            DataAccessException {
        List<Account> resultlist=new ArrayList<>();
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="select PublicKeyHash,PrivateKeyEncrypted from Account where PublicKeyHash=?;";
        jt.query(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,account.getPublicKeyHash());
            }
        }, new RowCallBackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                while(rs.next()){
                    Account tempAccount=new Account();
                    tempAccount.setPublicKeyHash(rs.getBytes(1));
                    tempAccount.setPrivateKeyEncrypted(rs.getBytes(2));
                    resultlist.add(tempAccount);
                }
            }
        });
        return resultlist;
    }
}