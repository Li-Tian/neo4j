package neo.network.p2p.payloads;

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
import neo.io.SerializeHelper;
import neo.ledger.AccountState;
import neo.ledger.Blockchain;
import neo.ledger.ValidatorState;
import neo.persistence.Snapshot;

public class StateDescriptorTest extends AbstractBlockchainTest {

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
        // account for voting
        ECPoint pubkey = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");
        StateDescriptor stateDescriptor = new StateDescriptor() {{
            type = StateType.Account;
            field = "Votes";
            key = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01").toArray();
            value = pubkey.getEncoded(true);
        }};

        // 1 + key(1 +) + field(1+) + value(1+)
        // 1 + 21 + 6 + 34
        Assert.assertEquals(62, stateDescriptor.size());

        // apply for become a validator
        stateDescriptor = new StateDescriptor() {{
            type = StateType.Validator;
            field = "Registered";
            key = pubkey.getEncoded(true);
            value = new byte[]{0x00};
        }};
        // 1 + key(1 +) + field(1+) + value(1+)
        // 1 + 34 + 11 + 2 = 48
        Assert.assertEquals(48, stateDescriptor.size());
    }

    @Test
    public void serialize() {
        ECPoint pubkey = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");
        StateDescriptor stateDescriptor = new StateDescriptor() {{
            type = StateType.Account;
            field = "Votes";
            key = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01").toArray();
            value = pubkey.getEncoded(true);
        }};

        StateDescriptor copy = Utils.copyFromSerialize(stateDescriptor, StateDescriptor::new);
        Assert.assertEquals(stateDescriptor.field, copy.field);
        Assert.assertEquals(stateDescriptor.type, copy.type);
        Assert.assertArrayEquals(stateDescriptor.key, copy.key);
        Assert.assertArrayEquals(stateDescriptor.value, copy.value);

        stateDescriptor = new StateDescriptor() {{
            type = StateType.Validator;
            field = "Registered";
            key = pubkey.getEncoded(true);
            value = new byte[]{0x00};
        }};
        copy = Utils.copyFromSerialize(stateDescriptor, StateDescriptor::new);
        Assert.assertEquals(stateDescriptor.field, copy.field);
        Assert.assertEquals(stateDescriptor.type, copy.type);
        Assert.assertArrayEquals(stateDescriptor.key, copy.key);
        Assert.assertArrayEquals(stateDescriptor.value, copy.value);
    }

    @Test
    public void getSystemFee() {
        // case 1: account for voting, system fee = 0
        ECPoint pubkey = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");
        StateDescriptor stateDescriptor = new StateDescriptor() {{
            type = StateType.Account;
            field = "Votes";
            key = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01").toArray();
            value = pubkey.getEncoded(true);
        }};
        Assert.assertEquals(new Fixed8(0), stateDescriptor.getSystemFee());

        // case 2: cancel applicant for a validator, system fee = 0
        stateDescriptor = new StateDescriptor() {{
            type = StateType.Validator;
            field = "Registered";
            key = pubkey.getEncoded(true);
            value = new byte[]{0x00};
        }};
        Assert.assertEquals(new Fixed8(0), stateDescriptor.getSystemFee());

        // case 3: apply for a validator, system fee = 1000
        stateDescriptor = new StateDescriptor() {{
            type = StateType.Validator;
            field = "Registered";
            key = pubkey.getEncoded(true);
            value = new byte[]{0x01};
        }};
        Assert.assertEquals(Fixed8.fromDecimal(new BigDecimal(1000)), stateDescriptor.getSystemFee());
    }

    @Test
    public void verify() {
        // prepare data
        ECPoint pubkey = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");

        Snapshot snapshot = store.getSnapshot();
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

        // case 1: account for voting
        StateDescriptor stateDescriptor = new StateDescriptor() {
            {
                type = StateType.Account;
                field = "Votes";
                key = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01").toArray();
                value = SerializeHelper.toBytes(new ECPoint[]{pubkey});
            }
        };
        Assert.assertTrue(stateDescriptor.verify(snapshot));


        // case 2: apply for validator
        stateDescriptor = new StateDescriptor() {{
            type = StateType.Validator;
            field = "Registered";
            key = pubkey.getEncoded(true);
            value = new byte[]{0x01};
        }};
        Assert.assertTrue(stateDescriptor.verify(snapshot));

        // clear data
        snapshot.getValidators().delete(pubkey);
        snapshot.getAccounts().delete(accountState.scriptHash);
        snapshot.commit();
    }

    @Test
    public void toJson() {
        ECPoint pubkey = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");
        StateDescriptor stateDescriptor = new StateDescriptor() {{
            type = StateType.Account;
            field = "Votes";
            key = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01").toArray();
            value = pubkey.getEncoded(true);
        }};


        JsonObject jsonObject = stateDescriptor.toJson();

        Assert.assertEquals(StateType.Account.value(), jsonObject.get("type").getAsInt());
        Assert.assertEquals("01ff00ff00ff00ff00ff00ff00ff00ff00ff00a4", jsonObject.get("key").getAsString());
        Assert.assertEquals("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007", jsonObject.get("value").getAsString());
    }
}