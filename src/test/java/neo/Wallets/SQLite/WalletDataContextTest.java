package neo.Wallets.SQLite;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import neo.Wallets.SQLite.sqlitJDBC.DataAccessException;

public class WalletDataContextTest {

    private static WalletDataContext context;

    @BeforeClass
    public static void setup() throws DataAccessException {
        String path = WalletDataContextTest.class.getClassLoader().getResource("").getPath();
        String db3Path = path + "test.db3";
        context = new WalletDataContext(db3Path);
        try {
            context.beginTranscation();
            context.createDB();
            context.commitTranscation();
        } catch (Exception e) {
            context.rollBack();
        }

    }

    @AfterClass
    public static void tearDown() throws DataAccessException {
        context.beginTranscation();
        context.deleteDB();
        context.commitTranscation();

        context.close();
    }


    @Test
    public void testAccount() throws DataAccessException {
        // create Account
        Account account = new Account() {{
            publicKeyHash = new byte[]{0x01, 0x02, 0x03};
            privateKeyEncrypted = new byte[]{0x02, 0x02};
        }};
        // test insertAccount
        context.beginTranscation();
        Assert.assertNotNull(context.insertAccount(account));

        // test firstOrDefaultAccount
        Account other = context.firstOrDefaultAccount(account.publicKeyHash);
        Assert.assertArrayEquals(account.privateKeyEncrypted, other.privateKeyEncrypted);

        // test update
        account.privateKeyEncrypted = new byte[]{0x03, 0x03};
        context.updateAccount(account);

        other = context.firstOrDefaultAccount(account.publicKeyHash);
        Assert.assertArrayEquals(account.privateKeyEncrypted, other.privateKeyEncrypted);

        // test delete
        Assert.assertNotNull(context.deleteAccount(account));

        other = context.firstOrDefaultAccount(account.publicKeyHash);
        Assert.assertNull(other);

        context.commitTranscation();
    }


    @Test
    public void testAddress() throws DataAccessException {
        // test insertKey
        Address address = new Address() {{
            ScriptHash = "test".getBytes();
        }};

        // test insertAddress
        Assert.assertNotNull(context.insertAddress(address));

        // test firstOrDefaultAddress
        Address otherAddress = context.firstOrDefaultAddress(address.ScriptHash);
        Assert.assertArrayEquals(address.ScriptHash, otherAddress.ScriptHash);

        // test queryAddressAll
        Address address2 = new Address() {{
            ScriptHash = "word".getBytes();
        }};
        Assert.assertNotNull(context.insertAddress(address2));
        otherAddress = context.firstOrDefaultAddress(address2.ScriptHash);
        Assert.assertArrayEquals(address2.ScriptHash, otherAddress.ScriptHash);

        List<Address> addressList = context.queryAddressAll();
        Assert.assertEquals(2, addressList.size());
        Assert.assertArrayEquals(address.ScriptHash, addressList.get(0).ScriptHash);
        Assert.assertArrayEquals(address2.ScriptHash, addressList.get(1).ScriptHash);

        // test deleteAddress
        Assert.assertNotNull(context.deleteAddress(address));
        otherAddress = context.firstOrDefaultAddress(address.ScriptHash);
        Assert.assertNull(otherAddress);

        Assert.assertNotNull(context.deleteAddress(address2));
        otherAddress = context.firstOrDefaultAddress(address2.ScriptHash);
        Assert.assertNull(otherAddress);
    }


    @Test
    public void testContract() throws DataAccessException {
        // create insert Contract
        final Account account = new Account() {{
            privateKeyEncrypted = "private key".getBytes();
            publicKeyHash = "public key".getBytes();
        }};
        final Address address = new Address() {{
            ScriptHash = "script hash".getBytes();
        }};

        Contract contract = new Contract();
        contract.rawData = "this is contract data".getBytes();
        contract.scriptHash = address.ScriptHash;
        contract.publicKeyHash = account.publicKeyHash;

        Assert.assertNotNull(context.insertAccount(account));
        Assert.assertNotNull(context.insertAddress(address));
        Assert.assertNotNull(context.insertContract(contract));


        // test firstOrDefaultContract
        Contract otherContract = context.firstOrDefaultContract(contract.scriptHash);
        Assert.assertArrayEquals(contract.rawData, otherContract.rawData);
        Assert.assertArrayEquals(contract.scriptHash, otherContract.scriptHash);
        Assert.assertArrayEquals(contract.publicKeyHash, otherContract.publicKeyHash);

        // test include
        Address address2 = new Address() {{
            ScriptHash = "address script2 ".getBytes();
        }};
        Contract contract2 = new Contract();
        contract2.rawData = "this is contract data 2".getBytes();
        contract2.scriptHash = address2.ScriptHash;
        contract2.publicKeyHash = account.publicKeyHash;

        Assert.assertNotNull(context.insertContract(contract2));
        List<Contract> contracts = context.include();
        Assert.assertEquals(2, contracts.size());
        Assert.assertArrayEquals(contract.rawData, contracts.get(0).rawData);


        // test updateContract
        contract.rawData = "new raw data".getBytes();
        Assert.assertNotNull(context.updateContract(contract));
        otherContract = context.firstOrDefaultContract(contract.scriptHash);
        Assert.assertArrayEquals(contract.rawData, otherContract.rawData);
        Assert.assertArrayEquals(contract.scriptHash, otherContract.scriptHash);
        Assert.assertArrayEquals(contract.publicKeyHash, otherContract.publicKeyHash);

        // test deleteContract
        Assert.assertNotNull(context.deleteContract(contract));
        Assert.assertNotNull(context.deleteContract(contract2));

        contracts = context.include();
        Assert.assertEquals(0, contracts.size());
    }


    @Test
    public void testKey() throws DataAccessException {
        // test insertKey
        Key key = new Key() {{
            name = "test";
            value = "value".getBytes();
        }};

        // test insert
        Assert.assertNotNull(context.insertKey(key));

        // test firstOrDefaultKey
        Key otherKey = context.firstOrDefaultKey(key.name);
        Assert.assertArrayEquals(key.value, otherKey.value);

        // test updateKey
        key.value = "hello".getBytes();
        Assert.assertNotNull(context.updateKey(key));
        otherKey = context.firstOrDefaultKey(key.name);
        Assert.assertArrayEquals(key.value, otherKey.value);

        // test deleteKey
        Assert.assertNotNull(context.deleteKey(key));
        otherKey = context.firstOrDefaultKey(key.name);
        Assert.assertNull(otherKey);
    }
}