package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import neo.persistence.AbstractBlockchainTest;
import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryWriter;
import neo.io.caching.DataCache;
import neo.ledger.BlockState;
import neo.ledger.Blockchain;
import neo.ledger.TransactionState;
import neo.ledger.TrimmedBlock;
import neo.persistence.Snapshot;
import neo.vm.OpCode;

public class BlockTest  extends AbstractBlockchainTest {

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractBlockchainTest.tearDown();
    }
    @Test
    public void inventoryType() {
        Block block = new Block();
        Assert.assertEquals(InventoryType.Block, block.inventoryType());
    }

    @Test
    public void getHeader() {
        Block block = new Block();
        Assert.assertNotNull(block.getHeader());
    }

    @Test
    public void size() {
        Block block = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x01, 0x02};
                verificationScript = new byte[]{0x03, 0x04};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        block.rebuildMerkleRoot();

        // block header size = 111, minertx = 10 contractTx = 6
        // block size = 111 + 1 + 10 + 6 = 128
        Assert.assertEquals(128, block.size());
    }

    @Test
    public void calculateNetFee() {
        // prepare data
        Snapshot snapshot = store.getSnapshot();
        DataCache<UInt256, TransactionState> txs = snapshot.getTransactions();

        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = new Fixed8(10000);
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = new Fixed8(20000);
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = new Fixed8(30000);
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
                    }},
            };
        }};
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(10);
            transaction = minerTransaction;
        }};
        txs.add(minerTransaction.hash(), txState);
        snapshot.commit();


        // check
        ArrayList<Transaction> txList = new ArrayList<>(2);
        txList.add(new ContractTransaction() {{
            inputs = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(0);
                    }}
            };
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = new Fixed8(1000);
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff04");
                    }}
            };
        }});
        txList.add(new ContractTransaction() {{
            inputs = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(2);
                    }}
            };
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = new Fixed8(10000);
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff05");
                    }}
            };
        }});
        Fixed8 netfee = Block.calculateNetFee(txList);
        Assert.assertEquals(new Fixed8(29000), netfee);

        // clear data
        txs.delete(minerTransaction.hash());
        snapshot.commit();
    }


    @Test
    public void serialize() {
        Block block = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x01, 0x02};
                verificationScript = new byte[]{0x03, 0x04};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        block.rebuildMerkleRoot();

        Block tmp = Utils.copyFromSerialize(block, Block::new);

        Assert.assertEquals(block.version, tmp.version);
        Assert.assertEquals(block.prevHash, tmp.prevHash);
        Assert.assertEquals(block.timestamp, tmp.timestamp);
        Assert.assertEquals(block.index, tmp.index);
        Assert.assertEquals(block.consensusData, tmp.consensusData);
        Assert.assertEquals(block.nextConsensus, tmp.nextConsensus);
        Assert.assertEquals(block.transactions.length, tmp.transactions.length);
        Assert.assertArrayEquals(block.witness.invocationScript, tmp.witness.invocationScript);
        Assert.assertArrayEquals(block.witness.verificationScript, tmp.witness.verificationScript);
    }

    @Test
    public void toJson() {
        Block block = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x01, 0x02};
                verificationScript = new byte[]{0x03, 0x04};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        block.rebuildMerkleRoot();

        JsonObject jsonObject = block.toJson();

        Assert.assertEquals(128, jsonObject.get("size").getAsInt());
        Assert.assertEquals(block.hash().toString(), jsonObject.get("hash").getAsString());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", jsonObject.get("previousblockhash").getAsString());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", jsonObject.get("nextconsensus").getAsString());
        Assert.assertEquals("0102", jsonObject.getAsJsonObject("script").get("invocation").getAsString());
        Assert.assertEquals("0304", jsonObject.getAsJsonObject("script").get("verification").getAsString());
    }

    @Test
    public void equals() {
        // prepare data
        Block block1 = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x01, 0x02};
                verificationScript = new byte[]{0x03, 0x04};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        block1.rebuildMerkleRoot();

        Block block2 = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x01, 0x02};
                verificationScript = new byte[]{0x03, 0x04};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        block2.rebuildMerkleRoot();

        Assert.assertTrue(block1.equals(block2));
        Assert.assertEquals(block1.hashCode(), block2.hashCode());

        Block block3 = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858585);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x01, 0x02};
                verificationScript = new byte[]{0x03, 0x04};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        block3.rebuildMerkleRoot();
        Assert.assertFalse(block1.equals(block3));
    }

    @Test
    public void trim() {
        Block block1 = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x01, 0x02};
                verificationScript = new byte[]{0x03, 0x04};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        block1.rebuildMerkleRoot();

        TrimmedBlock trimmedBlock = block1.trim();
        Assert.assertEquals(2, trimmedBlock.hashes.length);
        Assert.assertEquals(block1.transactions[0].hash(), trimmedBlock.hashes[0]);
        Assert.assertEquals(block1.transactions[1].hash(), trimmedBlock.hashes[1]);
    }

    @Test
    public void getWitnesses() {
        Block block1 = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x01, 0x02};
                verificationScript = new byte[]{0x03, 0x04};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        Witness witness = new Witness() {{
            invocationScript = new byte[]{0x00};
            verificationScript = new byte[]{0x00};
        }};
        block1.setWitnesses(new Witness[]{witness});
        Witness[] witnesses = block1.getWitnesses();
        Assert.assertEquals(1, witnesses.length);
        Assert.assertArrayEquals(witness.invocationScript, witnesses[0].invocationScript);
        Assert.assertArrayEquals(witness.verificationScript, witnesses[0].verificationScript);
    }

    @Test
    public void getMessage() {
        Block block = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x01, 0x02};
                verificationScript = new byte[]{0x03, 0x04};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        block.rebuildMerkleRoot();

        byte[] bytes = block.getMessage();
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        block.serializeUnsigned(new BinaryWriter(writer));
        Assert.assertArrayEquals(writer.toByteArray(), bytes);
    }

    @Test
    public void getHashData() {
        Block block = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x01, 0x02};
                verificationScript = new byte[]{0x03, 0x04};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        block.rebuildMerkleRoot();

        byte[] bytes = block.getHashData();
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        block.serializeUnsigned(new BinaryWriter(writer));
        Assert.assertArrayEquals(writer.toByteArray(), bytes);
    }

    @Test
    public void verify() {
        // prepare
        Snapshot snapshot = store.getSnapshot();
        DataCache<UInt256, BlockState> blockdb = snapshot.getBlocks();

        Block preBlock = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(9);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x00};
                verificationScript = new byte[]{0x00};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        preBlock.rebuildMerkleRoot();
        BlockState blockState = new BlockState() {{
            systemFeeAmount = 1000;
            trimmedBlock = preBlock.trim();
        }};
        blockdb.add(preBlock.hash(), blockState);


        Block block = new Block() {{
            version = new Uint(1);
            prevHash = preBlock.hash();
            timestamp = new Uint(14859624);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{OpCode.PUSHT.getCode()};
                verificationScript = new byte[]{OpCode.PUSHT.getCode()};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        block.rebuildMerkleRoot();

        boolean result = block.verify(snapshot);
        Assert.assertTrue(result);

        // clear data
        blockdb.delete(preBlock.hash());
        snapshot.commit();
    }

    @Test
    public void getScriptHashesForVerifying() {
        Snapshot snapshot = store.getSnapshot();
        DataCache<UInt256, BlockState> blockdb = snapshot.getBlocks();

        Block preBlock = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(9);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x00};
                verificationScript = new byte[]{0x00};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        preBlock.rebuildMerkleRoot();
        BlockState blockState = new BlockState() {{
            systemFeeAmount = 1000;
            trimmedBlock = preBlock.trim();
        }};
        blockdb.add(preBlock.hash(), blockState);


        Block block = new Block() {{
            version = new Uint(1);
            prevHash = preBlock.hash();
            timestamp = new Uint(14859624);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x00};
                verificationScript = new byte[]{0x00};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction(), // 6 + 4 = 10
                    new ContractTransaction() // 6
            };
        }};
        block.rebuildMerkleRoot();

        UInt160[] verifyHashes = block.getScriptHashesForVerifying(snapshot);

        Assert.assertEquals(1, verifyHashes.length);
        Assert.assertEquals(preBlock.nextConsensus, verifyHashes[0]);
    }
}