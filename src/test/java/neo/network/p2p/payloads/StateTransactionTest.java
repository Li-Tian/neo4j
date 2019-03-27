package neo.network.p2p.payloads;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

import neo.persistence.AbstractBlockchainTest;
import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.io.SerializeHelper;
import neo.ledger.AccountState;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.ledger.TransactionState;
import neo.ledger.UnspentCoinState;
import neo.ledger.ValidatorState;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;

public class StateTransactionTest extends AbstractBlockchainTest {

    private ECPoint pubkey = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");

    private StateDescriptor voteDesciptor = new StateDescriptor() {{
        type = StateType.Account;
        field = "Votes";
        key = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01").toArray();
        value = SerializeHelper.toBytes(new ECPoint[]{pubkey});
    }};

    private StateDescriptor validatorDescriptor = new StateDescriptor() {{
        type = StateType.Validator;
        field = "Registered";
        key = pubkey.getEncoded(true);
        value = new byte[]{0x01};
    }};

    private StateTransaction transaction = new StateTransaction();

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractBlockchainTest.tearDown();
    }

    @Test
    public void size() {
        // Size => base.Size + Descriptors.GetVarSize();
        // 6 + 1 + 1 + key(1 +) + field(1+) + value(2+)
        // = 8 + 21 + 6 + 35 = 70
        transaction.descriptors = new StateDescriptor[]{voteDesciptor};
        Assert.assertEquals(70, transaction.size());

    }

    @Test
    public void getSystemFee() {
        transaction.descriptors = new StateDescriptor[]{voteDesciptor};
        Fixed8 fee = transaction.getSystemFee();
        Assert.assertEquals(Fixed8.ZERO, fee);

        transaction.descriptors = new StateDescriptor[]{validatorDescriptor};
        fee = transaction.getSystemFee();
        Assert.assertEquals(Fixed8.fromDecimal(new BigDecimal(1000)), fee);
    }

    @Test
    public void getScriptHashesForVerifying() {
        transaction.descriptors = new StateDescriptor[]{voteDesciptor};
        UInt160[] hashes = transaction.getScriptHashesForVerifying(store.getSnapshot());

        Assert.assertEquals(1, hashes.length);
        Assert.assertEquals(new UInt160(voteDesciptor.key), hashes[0]);

        transaction.descriptors = new StateDescriptor[]{validatorDescriptor};
        UInt160 scriptHash = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(pubkey));
        hashes = transaction.getScriptHashesForVerifying(store.getSnapshot());
        Assert.assertEquals(1, hashes.length);
        Assert.assertEquals(scriptHash, hashes[0]);
    }

    @Test
    public void serializeExclusiveData() {
        transaction.descriptors = new StateDescriptor[]{
                voteDesciptor,
                validatorDescriptor
        };

        StateTransaction copy = Utils.copyFromSerialize(transaction, StateTransaction::new);

        Assert.assertEquals(transaction.descriptors.length, copy.descriptors.length);
        Assert.assertEquals(transaction.descriptors[0].field, copy.descriptors[0].field);
        Assert.assertArrayEquals(transaction.descriptors[0].key, copy.descriptors[0].key);
        Assert.assertArrayEquals(transaction.descriptors[0].value, copy.descriptors[0].value);

        Assert.assertEquals(transaction.descriptors[1].field, copy.descriptors[1].field);
        Assert.assertArrayEquals(transaction.descriptors[1].key, copy.descriptors[1].key);
        Assert.assertArrayEquals(transaction.descriptors[1].value, copy.descriptors[1].value);
    }

    @Test
    public void toJson() {
        transaction.descriptors = new StateDescriptor[]{
                voteDesciptor,
                validatorDescriptor
        };

        JsonObject jsonObject = transaction.toJson();
        Assert.assertEquals(transaction.hash().toString(), jsonObject.get("txid").getAsString());
        JsonArray descriptorArray = jsonObject.getAsJsonArray("descriptors");

        Assert.assertEquals(2, descriptorArray.size());
        Assert.assertEquals("Votes", descriptorArray.get(0).getAsJsonObject().get("field").getAsString());
        Assert.assertEquals("Registered", descriptorArray.get(1).getAsJsonObject().get("field").getAsString());
    }

    @Test
    public void verify() {
        // prepare data
        Snapshot snapshot = store.getSnapshot();

        // 1.1 set gas and utxo
        // set gas tx and utxo
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1002));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(1);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);

        // set uxto
        UnspentCoinState utxo = new UnspentCoinState() {{
            items = new CoinState[]{
                    CoinState.Confirmed
            };
        }};
        snapshot.getUnspentCoins().add(minerTransaction.hash(), utxo);

        // 1.2 set account and validator
        AccountState accountState = new AccountState() {{
            scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            isFrozen = false;
            votes = new ECPoint[0];
            balances = new ConcurrentHashMap<UInt256, Fixed8>(2) {{
                put(Blockchain.GoverningToken.hash(), Fixed8.fromDecimal(BigDecimal.valueOf(10000)));
            }};
        }};
        snapshot.getAccounts().add(accountState.scriptHash, accountState);

        ValidatorState validatorState = new ValidatorState() {{
            publicKey = pubkey;
            registered = true;
            votes = Fixed8.ZERO;
        }};
        snapshot.getValidators().add(validatorState.publicKey, validatorState);
        snapshot.commit();

        // 2. check
        transaction.descriptors = new StateDescriptor[]{
                voteDesciptor,
                validatorDescriptor
        };
        transaction.inputs = new CoinReference[]{
                new CoinReference() {{
                    prevHash = minerTransaction.hash();
                    prevIndex = new Ushort(0);
                }}
        };
        boolean result = transaction.verify(store.getSnapshot());
        Assert.assertTrue(result);

        // 3. clear data
        snapshot.getValidators().delete(pubkey);
        snapshot.getAccounts().delete(accountState.scriptHash);
        snapshot.commit();
    }
}