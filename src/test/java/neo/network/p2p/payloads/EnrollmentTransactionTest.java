package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import neo.AbstractBlockchainTest;
import neo.UInt160;
import neo.Utils;
import neo.cryptography.ecc.ECC;

public class EnrollmentTransactionTest  extends AbstractBlockchainTest {

    @Test
    public void getScriptHash() {
        EnrollmentTransaction transaction = new EnrollmentTransaction() {{
            publicKey = ECC.parseFromHexString("03b209fd4f53a7170ea4444e0cb0a6bb6a53c2bd016926989cf85f9b0fba17a70c");
        }};
        UInt160 scriptHsah = transaction.getScriptHash();
        Assert.assertEquals("AWHX6wX5mEJ4Vwg7uBcqESeq3NggtNFhzD", scriptHsah.toAddress());
    }

    @Test
    public void size() {
        EnrollmentTransaction transaction = new EnrollmentTransaction() {{
            publicKey = ECC.parseFromHexString("03b209fd4f53a7170ea4444e0cb0a6bb6a53c2bd016926989cf85f9b0fba17a70c");
        }};

        Assert.assertEquals(39, transaction.size());
    }

    @Test
    public void getScriptHashesForVerifying() {
        EnrollmentTransaction transaction = new EnrollmentTransaction() {{
            publicKey = ECC.parseFromHexString("03b209fd4f53a7170ea4444e0cb0a6bb6a53c2bd016926989cf85f9b0fba17a70c");
        }};
        UInt160[] hashes = transaction.getScriptHashesForVerifying(store.getSnapshot());
        Assert.assertEquals(1, hashes.length);
        Assert.assertEquals(transaction.getScriptHash(), hashes[0]);
    }

    @Test
    public void serializeExclusiveData() {
        EnrollmentTransaction transaction = new EnrollmentTransaction() {{
            publicKey = ECC.parseFromHexString("03b209fd4f53a7170ea4444e0cb0a6bb6a53c2bd016926989cf85f9b0fba17a70c");
        }};

        EnrollmentTransaction transaction1 =  Utils.copyFromSerialize(transaction, EnrollmentTransaction::new);

        Assert.assertEquals(transaction.publicKey, transaction1.publicKey);
    }

    @Test
    public void toJson() {
        EnrollmentTransaction transaction = new EnrollmentTransaction() {{
            publicKey = ECC.parseFromHexString("03b209fd4f53a7170ea4444e0cb0a6bb6a53c2bd016926989cf85f9b0fba17a70c");
        }};

        JsonObject jsonObject = transaction.toJson();
        Assert.assertEquals(transaction.hash().toString(), jsonObject.get("txid").getAsString());
        Assert.assertEquals(transaction.publicKey.toString(), jsonObject.get("pubkey").getAsString());
    }

    @Test
    public void verify() {
        EnrollmentTransaction transaction = new EnrollmentTransaction() {{
            publicKey = ECC.parseFromHexString("03b209fd4f53a7170ea4444e0cb0a6bb6a53c2bd016926989cf85f9b0fba17a70c");
        }};
        Assert.assertFalse(transaction.verify(null, null));
    }
}