package neo.Wallets.SQLite;

import java.sql.Connection;
import java.util.List;

import neo.Wallets.SQLite.sqlitJDBC.ConnectionFactory;
import neo.Wallets.SQLite.sqlitJDBC.DBUtils;
import neo.Wallets.SQLite.sqlitJDBC.DataAccessException;
import neo.Wallets.SQLite.sqlitJDBC.JdbcTemplate;
import neo.Wallets.SQLite.sqlitJDBC.JdbcTransaction;
import neo.Wallets.SQLite.sqlitJDBC.dao.AccountDao;
import neo.Wallets.SQLite.sqlitJDBC.dao.AddressDao;
import neo.Wallets.SQLite.sqlitJDBC.dao.ContractDao;
import neo.Wallets.SQLite.sqlitJDBC.dao.KeyDao;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletDataContext
 * @Package neo.Wallets.SQLite
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 18:40 2019/3/14
 */
public class WalletDataContext {
    public AddressDao addressDao = new AddressDao();
    public KeyDao keyDao = new KeyDao();
    public AccountDao accountDao = new AccountDao();
    public ContractDao contractDao = new ContractDao();

    private final String filename;
    private Connection connection;

    public WalletDataContext(String filename) throws DataAccessException {
        this.filename = filename;
        connection = createConnection(filename);
    }


    //1建立连接
    //创建Sqlite数据库连接
    public Connection createConnection(String localAddress) throws DataAccessException {
        return ConnectionFactory.getConnection(localAddress);
    }

    //2事务启动、关闭、回滚
    public void beginTranscation() throws DataAccessException {
        JdbcTransaction.beginTransaction(connection);
    }

    public void commitTranscation() throws DataAccessException {
        JdbcTransaction.commit(connection);
    }

    public void rollBack() throws DataAccessException {
        JdbcTransaction.rollback(connection);
    }

    //3增删改查
    //3.0.1增表
    public void createDB() throws DataAccessException {
        addressDao.createTable(connection);
        keyDao.createTable(connection);
        accountDao.createTable(connection);
        contractDao.createTable(connection);
    }

    //3.0.2删表
    public void deleteDB() throws DataAccessException {
        contractDao.deleteTable(connection);
        accountDao.deleteTable(connection);
        keyDao.deleteTable(connection);
        addressDao.deleteTable(connection);
    }

    //3.1Account相关
    //3.1.1查询第一个Account
    public Account firstOrDefaultAccount(byte[] publicKeyHash) throws DataAccessException {
        Account pAccount = new Account();
        pAccount.setPublicKeyHash(publicKeyHash);
        List<Account> list = accountDao.queryAccountByPublicKeyHash(pAccount, connection);
        if (list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    //3.1.2插入Account
    public Account insertAccount(Account account) throws DataAccessException {
        int count = accountDao.insertAccount(account, connection);
        if (count == 0) {
            return null;
        } else {
            return account;
        }
    }

    //3.1.3更改Account
    public Account updateAccount(Account account) throws DataAccessException {

        int count = accountDao.updateAccount(account, connection);
        if (count == 0) {
            return null;
        } else {
            return account;
        }
    }

    //3.1.4删除Account
    public Account deleteAccount(Account account) throws DataAccessException {

        if (account == null) {
            return null;
        }
        int count = accountDao.deleteAccount(account, connection);
        if (count == 0) {
            return null;
        } else {
            return account;
        }
    }


    //3.2Address相关
    //3.2.1查询第一个Address
    public Address firstOrDefaultAddress(byte[] scriptHash) throws DataAccessException {
        Address pAddress = new Address();
        pAddress.setScriptHash(scriptHash);
        List<Address> list = addressDao.queryAddressByAddress(pAddress, connection);
        if (list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    //3.2.2插入Address
    public Address insertAddress(Address address) throws DataAccessException {
        int count = addressDao.insertAddress(address, connection);
        if (count == 0) {
            return null;
        } else {
            return address;
        }
    }

    //3.2.3删除Address
    public Address deleteAddress(Address address) throws DataAccessException {

        if (address == null) {
            return null;
        }
        int count = addressDao.deleteAddress(address, connection);
        if (count == 0) {
            return null;
        } else {
            return address;
        }
    }

    //3.2.1查询所有Address
    public List<Address> queryAddressAll() throws DataAccessException {
        return addressDao.queryAddressAll(connection);
    }


    //3.3Contract相关
    //3.3.1查询第一个Contract
    public Contract firstOrDefaultContract(byte[] scriptHash) throws DataAccessException {
        Contract pContract = new Contract();
        pContract.setScriptHash(scriptHash);
        List<Contract> list = contractDao.queryContractByScriptHash(pContract, connection);
        if (list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    //3.3.2插入Contract
    public Contract insertContract(Contract contract) throws DataAccessException {
        int count = contractDao.insertContract(contract, connection);
        if (count == 0) {
            return null;
        } else {
            return contract;
        }
    }

    //3.3.3左外连接Account表
    public List<Contract> include() throws DataAccessException {
        return contractDao.queryContractMapperAccount(connection);
    }

    //3.3.4更改Contract
    public Contract updateContract(Contract contract) throws DataAccessException {

        int count = contractDao.updateContract(contract, connection);
        if (count == 0) {
            return null;
        } else {
            return contract;
        }
    }

    //3.3.5删除Contract
    public Contract deleteContract(Contract contract) throws DataAccessException {

        if (contract == null) {
            return null;
        }
        int count = contractDao.deleteContract(contract, connection);
        if (count == 0) {
            return null;
        } else {
            return contract;
        }
    }

    //3.4Key相关
    //3.4.1查询第一个Key
    public Key firstOrDefaultKey(String name) throws DataAccessException {
        Key pKey = new Key();
        pKey.setName(name);
        List<Key> list = keyDao.queryKeyByKey(pKey, connection);
        if (list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    //3.4.2插入Key
    public Key insertKey(Key key) throws DataAccessException {
        int count = keyDao.insertKey(key, connection);
        if (count == 0) {
            return null;
        } else {
            return key;
        }
    }

    //3.4.2更新Key
    public Key updateKey(Key key) throws DataAccessException {

        int count = keyDao.updateKey(key, connection);
        if (count == 0) {
            return null;
        } else {
            return key;
        }
    }

    //3.4.3删除Key
    public Key deleteKey(Key key) throws DataAccessException {

        if (key == null) {
            return null;
        }
        int count = keyDao.deleteKey(key, connection);
        if (count == 0) {
            return null;
        } else {
            return key;
        }
    }

    //5关闭连接
    public void close() throws DataAccessException {
        DBUtils.close(connection);
    }
}