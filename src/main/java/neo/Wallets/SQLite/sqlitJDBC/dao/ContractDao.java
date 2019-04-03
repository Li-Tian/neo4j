package neo.Wallets.SQLite.sqlitJDBC.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import neo.Wallets.SQLite.Account;
import neo.Wallets.SQLite.Contract;
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
public class ContractDao {

    public int createTable(Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="CREATE TABLE IF NOT EXISTS \"Contract\" (\n" +
                "  \"ScriptHash\" Binary NOT NULL,\n" +
                "  \"PublicKeyHash\" Binary NOT NULL,\n" +
                "  \"RawData\" VarBinary NOT NULL,\n" +
                "  CONSTRAINT \"PK_Contract\" PRIMARY KEY (\"ScriptHash\"),\n" +
                "  CONSTRAINT \"FK_Contract_Account_PublicKeyHash\" FOREIGN KEY (\"PublicKeyHash\") REFERENCES \"Account\" (\"PublicKeyHash\") ON DELETE CASCADE ON UPDATE NO ACTION,\n" +
                "  CONSTRAINT \"FK_Contract_Address_ScriptHash\" FOREIGN KEY (\"ScriptHash\") REFERENCES \"Address\" (\"ScriptHash\") ON DELETE CASCADE ON UPDATE NO ACTION\n" +
                ");";
        return jt.update(sql,null);
    }

    public int deleteTable(Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="DROP TABLE Contract;";
        return jt.update(sql,null);
    }

    public int insertContract(Contract contract, Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="insert into Contract(ScriptHash,PublicKeyHash,RawData) values(?,?,?);";
        return jt.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,contract.getScriptHash());
                pstmt.setBytes(2,contract.getPublicKeyHash());
                pstmt.setBytes(3,contract.getRawData());
            }
        });
    }

    public int updateContract(Contract contract, Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="Update Contract set PublicKeyHash=?,RawData=? where ScriptHash=?;";
        return jt.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,contract.getPublicKeyHash());
                pstmt.setBytes(2,contract.getRawData());
                pstmt.setBytes(3,contract.getScriptHash());
            }
        });
    }

    public int deleteContract(Contract contract, Connection conn) throws DataAccessException {
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="delete from Contract where ScriptHash=?;";
        return jt.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,contract.getScriptHash());
            }
        });
    }

    public List<Contract> queryContractByScriptHash(Contract contract, Connection conn) throws
            DataAccessException {
        List<Contract> resultlist=new ArrayList<>();
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="select ScriptHash,PublicKeyHash,RawData from Contract where ScriptHash=?;";
        jt.query(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pstmt) throws SQLException {
                pstmt.setBytes(1,contract.getScriptHash());
            }
        }, new RowCallBackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                while(rs.next()){
                    Contract tempContract=new Contract();
                    tempContract.setScriptHash(rs.getBytes(1));
                    tempContract.setPublicKeyHash(rs.getBytes(2));
                    tempContract.setRawData(rs.getBytes(3));
                    resultlist.add(tempContract);
                }
            }
        });
        return resultlist;
    }

    public List<Contract> queryContractMapperAccount(Connection conn) throws
            DataAccessException {
        List<Contract> resultlist=new ArrayList<>();
        JdbcTemplate jt=new JdbcTemplate(conn);
        String sql="select c.ScriptHash,c.PublicKeyHash,c.RawData,a.PrivateKeyEncrypted " +
                "from Contract c LEFT JOIN Account a where c.PublicKeyHash=a.PublicKeyHash;";
        jt.query(sql,new RowCallBackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                while(rs.next()){
                    Contract tempContract=new Contract();
                    tempContract.setScriptHash(rs.getBytes(1));
                    tempContract.setPublicKeyHash(rs.getBytes(2));
                    tempContract.setRawData(rs.getBytes(3));
                    Account tempAccount=new Account();
                    tempAccount.setPublicKeyHash(rs.getBytes(2));
                    tempAccount.setPrivateKeyEncrypted(rs.getBytes(4));
                    tempContract.setAccount(tempAccount);
                    resultlist.add(tempContract);
                }
            }
        });
        return resultlist;
    }
}