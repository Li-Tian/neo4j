package neo.network.p2p.payloads;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import neo.persistence.AbstractBlockchainTest;
import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryWriter;
import neo.ledger.Blockchain;
import neo.ledger.HashIndexState;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;
import neo.vm.OpCode;

public class ConsensusPayloadTest  extends AbstractBlockchainTest {

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractBlockchainTest.tearDown();
    }

    @Test
    public void hash() {
        ConsensusPayload payload = new ConsensusPayload() {{
            version = Uint.ZERO;
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            blockIndex = new Uint(10);
            validatorIndex = new Ushort(1);
            timestamp = new Uint(10238328);
            data = new byte[]{0x00, 0x00, 0x00, 0x00};
            witness = new Witness() {{
                invocationScript = new byte[]{OpCode.PUSHT.getCode()};
                verificationScript = new byte[]{OpCode.PUSHT.getCode()};
            }};
        }};

        Assert.assertEquals("0xbf35d8bd7a00e8df59bcd2dddec01609baea056367fca8da4427d81d12f1c6f8", payload.hash().toString());
    }

    @Test
    public void inventoryType() {
        ConsensusPayload payload = new ConsensusPayload();
        Assert.assertEquals(InventoryType.Consensus, payload.inventoryType());
    }

    @Test
    public void verify() {
        ConsensusPayload payload = new ConsensusPayload() {{
            version = Uint.ZERO;
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            blockIndex = new Uint(10);
            validatorIndex = new Ushort(1);
            timestamp = new Uint(10238328);
            data = new byte[]{0x00, 0x00, 0x00, 0x00};
            witness = new Witness() {{
                invocationScript = new byte[]{OpCode.PUSHT.getCode()};
                verificationScript = new byte[]{OpCode.PUSHT.getCode()};
            }};
        }};

        // TODO waiting for smartcontract to complete
        Snapshot snapshot = store.getSnapshot();
        HashIndexState hashIndexState = snapshot.getBlockHashIndex().getAndChange();
        hashIndexState.index = new Uint(9);
        Assert.assertEquals(true, payload.verify(snapshot));
    }

    @Test
    public void getHashData() {
        ConsensusPayload payload = new ConsensusPayload() {{
            version = Uint.ZERO;
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            blockIndex = new Uint(10);
            validatorIndex = new Ushort(1);
            timestamp = new Uint(10238328);
            data = new byte[]{0x00, 0x00, 0x00, 0x00};
            witness = new Witness() {{
                invocationScript = new byte[]{OpCode.PUSHT.getCode()};
                verificationScript = new byte[]{OpCode.PUSHT.getCode()};
            }};
        }};

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(outputStream);
        payload.serializeUnsigned(writer);
        writer.flush();
        Assert.assertArrayEquals(outputStream.toByteArray(), payload.getHashData());
    }

    @Test
    public void setWitnesses() {
        ConsensusPayload payload = new ConsensusPayload();
        Witness witness1 = new Witness() {{
            invocationScript = new byte[]{OpCode.PUSHT.getCode()};
            verificationScript = new byte[]{OpCode.PUSHT.getCode()};
        }};
        payload.setWitnesses(new Witness[]{witness1});
        Witness witness2 = payload.getWitnesses()[0];
        Assert.assertEquals(witness1, witness2);
    }

    @Test
    public void size() {
        ConsensusPayload payload = new ConsensusPayload() {{
            version = Uint.ZERO;
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            blockIndex = new Uint(10);
            validatorIndex = new Ushort(1);
            timestamp = new Uint(10238328);
            data = new byte[]{0x00, 0x00, 0x00, 0x00};
            witness = new Witness() {{
                invocationScript = new byte[]{OpCode.PUSHT.getCode()};
                verificationScript = new byte[]{OpCode.PUSHT.getCode()};
            }};
        }};

        Assert.assertEquals(56, payload.size());
    }

    @Test
    public void serialize() {
        ConsensusPayload payload = new ConsensusPayload() {{
            version = Uint.ZERO;
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            blockIndex = new Uint(10);
            validatorIndex = new Ushort(1);
            timestamp = new Uint(10238328);
            data = new byte[]{0x00, 0x00, 0x00, 0x00};
            witness = new Witness() {{
                invocationScript = new byte[]{OpCode.PUSHT.getCode()};
                verificationScript = new byte[]{OpCode.PUSHT.getCode()};
            }};
        }};

        ConsensusPayload payload2 = Utils.copyFromSerialize(payload, ConsensusPayload::new);

        Assert.assertEquals(payload.version, payload2.version);
        Assert.assertEquals(payload.prevHash, payload2.prevHash);
        Assert.assertEquals(payload.blockIndex, payload2.blockIndex);
        Assert.assertEquals(payload.validatorIndex, payload2.validatorIndex);
        Assert.assertEquals(payload.timestamp, payload2.timestamp);
        Assert.assertArrayEquals(payload.data, payload2.data);
        Assert.assertArrayEquals(payload.witness.invocationScript, payload2.witness.invocationScript);
        Assert.assertArrayEquals(payload.witness.verificationScript, payload2.witness.verificationScript);
    }

    @Test
    public void getMessage() {
        ConsensusPayload payload = new ConsensusPayload() {{
            version = Uint.ZERO;
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            blockIndex = new Uint(10);
            validatorIndex = new Ushort(1);
            timestamp = new Uint(10238328);
            data = new byte[]{0x00, 0x00, 0x00, 0x00};
            witness = new Witness() {{
                invocationScript = new byte[]{OpCode.PUSHT.getCode()};
                verificationScript = new byte[]{OpCode.PUSHT.getCode()};
            }};
        }};

        Assert.assertArrayEquals(payload.getHashData(), payload.getMessage());
    }

    @Test
    public void getScriptHashesForVerifying() {
        ConsensusPayload payload = new ConsensusPayload() {{
            version = Uint.ZERO;
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            blockIndex = new Uint(10);
            validatorIndex = new Ushort(1);
            timestamp = new Uint(10238328);
            data = new byte[]{0x00, 0x00, 0x00, 0x00};
            witness = new Witness() {{
                invocationScript = new byte[]{OpCode.PUSHT.getCode()};
                verificationScript = new byte[]{OpCode.PUSHT.getCode()};
            }};
        }};

        UInt160 hashes[] = payload.getScriptHashesForVerifying(store.getSnapshot());
        Assert.assertEquals(1, hashes.length);


        ECPoint[] validatorPubkeys = Blockchain.StandbyValidators;
        Arrays.sort(validatorPubkeys);
        byte[] scriptBytes = Contract.createSignatureRedeemScript(validatorPubkeys[payload.validatorIndex.intValue()]);
        UInt160 scriptHash = UInt160.parseToScriptHash(scriptBytes);

        Assert.assertEquals(scriptHash, hashes[0]);
    }
}