package neo.wallets.SQLite.sqlitJDBC.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import neo.wallets.SQLite.Address;
import neo.wallets.SQLite.sqlitJDBC.DataAccessException;
import neo.wallets.SQLite.sqlitJDBC.JdbcTemplate;
import neo.wallets.SQLite.sqlitJDBC.PreparedStatementSetter;
import neo.wallets.SQLite.sqlitJDBC.RowCallBackHandler;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: KeyDao
 * @Package neo.wallets.SQLite.sqlitJDBC.dao
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 13:46 2019/3/26
 */
public class AddressDao {

    public int createTable(Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="CREATE TABLE IF NOT EXISTS \"Address\" (\n" +
                "  \"ScriptHash\" Binary NOT NULL,\n" +
                "  CONSTRAINT \"PK_Address\" PRIMARY KEY (\"ScriptHash\")\n" +
                ");";
        return jt.update(sql,null);
    }

    public int deleteTable(Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="DROP TABLE IF EXISTS Address;";
        return jt.update(sql,null);
    }

    public int insertAddress(Address address, Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="insert into Address(ScriptHash) values(?);";
        return jt.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,address.getScriptHash());
            }
        });
    }

    public int deleteAddress(Address address, Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="delete from Address where ScriptHash=?;";
        return jt.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,address.getScriptHash());
            }
        });
    }

    public List<Address> queryAddressByAddress(Address address, Connection conn) throws
            DataAccessException {
        List<Address> resultlist=new ArrayList<>();
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="select ScriptHash from Address where ScriptHash=?;";
        jt.query(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,address.getScriptHash());
            }
        }, new RowCallBackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                while(rs.next()){
                    Address tempAddress=new Address();
                    tempAddress.setScriptHash(rs.getBytes(1));
                    resultlist.add(tempAddress);
                }
            }
        });
        return resultlist;
    }

    public List<Address> queryAddressAll(Connection conn) throws
            DataAccessException {
        List<Address> resultlist=new ArrayList<>();
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="select ScriptHash from Address;";
        jt.query(sql, new RowCallBackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                while(rs.next()){
                    Address tempAddress=new Address();
                    tempAddress.setScriptHash(rs.getBytes(1));
                    resultlist.add(tempAddress);
                }
            }
        });
        return resultlist;
    }
}