package neo.smartcontract;

import org.bouncycastle.math.ec.ECCurve;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.io.BinaryWriter;
import neo.io.SerializeHelper;
import neo.ledger.AccountState;
import neo.ledger.AssetState;
import neo.ledger.ContractPropertyState;
import neo.ledger.ContractState;
import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.log.notr.TR;
import neo.network.p2p.payloads.AssetType;
import neo.network.p2p.payloads.BlockBase;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.InvocationTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.persistence.Snapshot;
import neo.smartcontract.enumerators.ConcatenatedEnumerator;
import neo.smartcontract.enumerators.IEnumerator;
import neo.smartcontract.enumerators.IteratorValuesWrapper;
import neo.smartcontract.iterators.ArrayWrapper;
import neo.smartcontract.iterators.IIterator;
import neo.smartcontract.iterators.MapWrapper;
import neo.smartcontract.iterators.StorageIterator;
import neo.vm.ExecutionEngine;
import neo.vm.ICollection;
import neo.vm.StackItem;
import neo.vm.Types.Array;
import neo.vm.Types.InteropInterface;
import neo.vm.Types.Map;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NeoService
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:11 2019/3/12
 */
public class NeoService extends StandardService {

    public NeoService(TriggerType trigger, Snapshot snapshot) {
        super(trigger, snapshot);
        Register("Neo.Runtime.GetTrigger", this::runtimeGetTrigger, 1);
        Register("Neo.Runtime.CheckWitness", this::runtimeCheckWitness, 200);
        Register("Neo.Runtime.Notify", this::runtimeNotify, 1);
        Register("Neo.Runtime.Log", this::runtimeLog, 1);
        Register("Neo.Runtime.GetTime", this::runtimeGetTime, 1);
        Register("Neo.Runtime.Serialize", this::runtimeSerialize, 1);
        Register("Neo.Runtime.Deserialize", this::runtimeDeserialize, 1);
        Register("Neo.Blockchain.GetHeight", this::blockchainGetHeight, 1);
        Register("Neo.Blockchain.GetHeader", this::blockchainGetHeader, 100);
        Register("Neo.Blockchain.GetBlock", this::blockchainGetBlock, 200);
        Register("Neo.Blockchain.GetTransaction", this::blockchainGetTransaction, 100);
        Register("Neo.Blockchain.GetTransactionHeight", this::blockchainGetTransactionHeight, 100);
        Register("Neo.Blockchain.GetAccount", this::blockchainGetAccount, 100);
        Register("Neo.Blockchain.GetValidators", this::blockchainGetValidators, 200);
        Register("Neo.Blockchain.GetAsset", this::blockchainGetAsset, 100);
        Register("Neo.Blockchain.GetContract", this::blockchainGetContract, 100);
        Register("Neo.Header.GetHash", this::headerGetHash, 1);
        Register("Neo.Header.GetVersion", this::headerGetVersion, 1);
        Register("Neo.Header.GetPrevHash", this::headerGetPrevHash, 1);
        Register("Neo.Header.GetMerkleRoot", this::headerGetMerkleRoot, 1);
        Register("Neo.Header.GetTimestamp", this::headerGetTimestamp, 1);
        Register("Neo.Header.GetIndex", this::headerGetIndex, 1);
        Register("Neo.Header.GetConsensusData", this::headerGetConsensusData, 1);
        Register("Neo.Header.GetNextConsensus", this::headerGetNextConsensus, 1);
        Register("Neo.Block.GetTransactionCount", this::blockGetTransactionCount, 1);
        Register("Neo.Block.GetTransactions", this::blockGetTransactions, 1);
        Register("Neo.Block.GetTransaction", this::blockGetTransaction, 1);
        Register("Neo.Transaction.GetHash", this::transactionGetHash, 1);
        Register("Neo.Transaction.GetType", this::transactionGetType, 1);
        Register("Neo.Transaction.GetAttributes", this::transactionGetAttributes, 1);
        Register("Neo.Transaction.GetInputs", this::transactionGetInputs, 1);
        Register("Neo.Transaction.GetOutputs", this::transactionGetOutputs, 1);
        Register("Neo.Transaction.GetReferences", this::transactionGetReferences, 200);
        Register("Neo.Transaction.GetUnspentCoins", this::transactionGetUnspentCoins, 200);
        Register("Neo.Transaction.GetWitnesses", this::transactionGetWitnesses, 200);
        Register("Neo.InvocationTransaction.GetScript", this::invocationTransactionGetScript, 1);
        Register("Neo.Witness.GetVerificationScript", this::witnessGetVerificationScript, 100);
        Register("Neo.Attribute.GetUsage", this::attributeGetUsage, 1);
        Register("Neo.Attribute.GetData", this::attributeGetData, 1);
        Register("Neo.Input.GetHash", this::inputGetHash, 1);
        Register("Neo.Input.GetIndex", this::inputGetIndex, 1);
        Register("Neo.Output.GetAssetId", this::outputGetAssetId, 1);
        Register("Neo.Output.GetValue", this::outputGetValue, 1);
        Register("Neo.Output.GetScriptHash", this::outputGetScriptHash, 1);
        Register("Neo.Account.GetScriptHash", this::accountGetScriptHash, 1);
        Register("Neo.Account.GetVotes", this::accountGetVotes, 1);
        Register("Neo.Account.GetBalance", this::accountGetBalance, 1);
        Register("Neo.Account.IsStandard", this::accountIsStandard, 100);
        Register("Neo.Asset.Create", this::assetCreate);
        Register("Neo.Asset.Renew", this::assetRenew);
        Register("Neo.Asset.GetAssetId", this::assetGetAssetId, 1);
        Register("Neo.Asset.GetAssetType", this::assetGetAssetType, 1);
        Register("Neo.Asset.GetAmount", this::assetGetAmount, 1);
        Register("Neo.Asset.GetAvailable", this::assetGetAvailable, 1);
        Register("Neo.Asset.GetPrecision", this::assetGetPrecision, 1);
        Register("Neo.Asset.GetOwner", this::assetGetOwner, 1);
        Register("Neo.Asset.GetAdmin", this::assetGetAdmin, 1);
        Register("Neo.Asset.GetIssuer", this::assetGetIssuer, 1);
        Register("Neo.Contract.Create", this::contractCreate);
        Register("Neo.Contract.Migrate", this::contractMigrate);
        Register("Neo.Contract.Destroy", this::contractDestroy, 1);
        Register("Neo.Contract.GetScript", this::contractGetScript, 1);
        Register("Neo.Contract.IsPayable", this::contractIsPayable, 1);
        Register("Neo.Contract.GetStorageContext", this::contractGetStorageContext, 1);
        Register("Neo.Storage.GetContext", this::storageGetContext, 1);
        Register("Neo.Storage.GetReadOnlyContext", this::storageGetReadOnlyContext, 1);
        Register("Neo.Storage.Get", this::storageGet, 100);
        Register("Neo.Storage.Put", this::storagePut);
        Register("Neo.Storage.Delete", this::storageDelete, 100);
        Register("Neo.Storage.Find", this::storageFind, 1);
        Register("Neo.StorageContext.AsReadOnly", this::storageContextAsReadOnly, 1);
        Register("Neo.Enumerator.Create", this::enumeratorCreate, 1);
        Register("Neo.Enumerator.Next", this::enumeratorNext, 1);
        Register("Neo.Enumerator.Value", this::enumeratorValue, 1);
        Register("Neo.Enumerator.Concat", this::enumeratorConcat, 1);
        Register("Neo.Iterator.Create", this::iteratorCreate, 1);
        Register("Neo.Iterator.Key", this::iteratorKey, 1);
        Register("Neo.Iterator.Keys", this::iteratorKeys, 1);
        Register("Neo.Iterator.Values", this::iteratorValues, 1);

        //region Aliases
        Register("Neo.Iterator.Next", this::enumeratorNext, 1);
        Register("Neo.Iterator.Value", this::enumeratorValue, 1);
        //region Old APIs
        Register("AntShares.Runtime.CheckWitness", this::runtimeCheckWitness, 200);
        Register("AntShares.Runtime.Notify", this::runtimeNotify, 1);
        Register("AntShares.Runtime.Log", this::runtimeLog, 1);
        Register("AntShares.Blockchain.GetHeight", this::blockchainGetHeight, 1);
        Register("AntShares.Blockchain.GetHeader", this::blockchainGetHeader, 100);
        Register("AntShares.Blockchain.GetBlock", this::blockchainGetBlock, 200);
        Register("AntShares.Blockchain.GetTransaction", this::blockchainGetTransaction, 100);
        Register("AntShares.Blockchain.GetAccount", this::blockchainGetAccount, 100);
        Register("AntShares.Blockchain.GetValidators", this::blockchainGetValidators, 200);
        Register("AntShares.Blockchain.GetAsset", this::blockchainGetAsset, 100);
        Register("AntShares.Blockchain.GetContract", this::blockchainGetContract, 100);
        Register("AntShares.Header.GetHash", this::headerGetHash, 1);
        Register("AntShares.Header.GetVersion", this::headerGetVersion, 1);
        Register("AntShares.Header.GetPrevHash", this::headerGetPrevHash, 1);
        Register("AntShares.Header.GetMerkleRoot", this::headerGetMerkleRoot, 1);
        Register("AntShares.Header.GetTimestamp", this::headerGetTimestamp, 1);
        Register("AntShares.Header.GetConsensusData", this::headerGetConsensusData, 1);
        Register("AntShares.Header.GetNextConsensus", this::headerGetNextConsensus, 1);
        Register("AntShares.Block.GetTransactionCount", this::blockGetTransactionCount, 1);
        Register("AntShares.Block.GetTransactions", this::blockGetTransactions, 1);
        Register("AntShares.Block.GetTransaction", this::blockGetTransaction, 1);
        Register("AntShares.Transaction.GetHash", this::transactionGetHash, 1);
        Register("AntShares.Transaction.GetType", this::transactionGetType, 1);
        Register("AntShares.Transaction.GetAttributes", this::transactionGetAttributes, 1);
        Register("AntShares.Transaction.GetInputs", this::transactionGetInputs, 1);
        Register("AntShares.Transaction.GetOutputs", this::transactionGetOutputs, 1);
        Register("AntShares.Transaction.GetReferences", this::transactionGetReferences, 200);
        Register("AntShares.Attribute.GetUsage", this::attributeGetUsage, 1);
        Register("AntShares.Attribute.GetData", this::attributeGetData, 1);
        Register("AntShares.Input.GetHash", this::inputGetHash, 1);
        Register("AntShares.Input.GetIndex", this::inputGetIndex, 1);
        Register("AntShares.Output.GetAssetId", this::outputGetAssetId, 1);
        Register("AntShares.Output.GetValue", this::outputGetValue, 1);
        Register("AntShares.Output.GetScriptHash", this::outputGetScriptHash, 1);
        Register("AntShares.Account.GetScriptHash", this::accountGetScriptHash, 1);
        Register("AntShares.Account.GetVotes", this::accountGetVotes, 1);
        Register("AntShares.Account.GetBalance", this::accountGetBalance, 1);
        Register("AntShares.Asset.Create", this::assetCreate);
        Register("AntShares.Asset.Renew", this::assetRenew);
        Register("AntShares.Asset.GetAssetId", this::assetGetAssetId, 1);
        Register("AntShares.Asset.GetAssetType", this::assetGetAssetType, 1);
        Register("AntShares.Asset.GetAmount", this::assetGetAmount, 1);
        Register("AntShares.Asset.GetAvailable", this::assetGetAvailable, 1);
        Register("AntShares.Asset.GetPrecision", this::assetGetPrecision, 1);
        Register("AntShares.Asset.GetOwner", this::assetGetOwner, 1);
        Register("AntShares.Asset.GetAdmin", this::assetGetAdmin, 1);
        Register("AntShares.Asset.GetIssuer", this::assetGetIssuer, 1);
        Register("AntShares.Contract.Create", this::contractCreate);
        Register("AntShares.Contract.Migrate", this::contractMigrate);
        Register("AntShares.Contract.Destroy", this::contractDestroy, 1);
        Register("AntShares.Contract.GetScript", this::contractGetScript, 1);
        Register("AntShares.Contract.GetStorageContext", this::contractGetStorageContext, 1);
        Register("AntShares.Storage.GetContext", this::storageGetContext, 1);
        Register("AntShares.Storage.Get", this::storageGet, 100);
        Register("AntShares.Storage.Put", this::storagePut);
        Register("AntShares.Storage.Delete", this::storageDelete, 100);
    }

    private boolean blockchainGetAccount(ExecutionEngine engine) {
        UInt160 hash = new UInt160(engine.getCurrentContext().getEvaluationStack().pop()
                .getByteArray());
        //LINQ START
        //AccountState account = Snapshot.Accounts.GetOrAdd(hash, () => new AccountState(hash));
        AccountState account = snapshot.getAccounts().getOrAdd(hash, () -> {
            return new AccountState
                    (hash);
        });
        //LINQ END
        engine.getCurrentContext().getEvaluationStack().push(StackItem.fromInterface(account));
        return true;
    }

    private boolean blockchainGetValidators(ExecutionEngine engine) {
        ECPoint[] validators = snapshot.getValidatorPubkeys();
        //LINQ START
/*        engine.getCurrentContext().getEvaluationStack().push(validators.Select(p = > (StackItem) p
                .EncodePoint(true)).ToArray());*/
// TODO: 2019/3/28
        StackItem[] tempArray = Arrays.asList(validators).stream()
                .map(p -> StackItem.getStackItem(p.getEncoded(true))).toArray(StackItem[]::new);
        engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(tempArray));
        //LINQ END
        return true;
    }

    private boolean blockchainGetAsset(ExecutionEngine engine) {
        UInt256 hash = new UInt256(engine.getCurrentContext().getEvaluationStack().pop()
                .getByteArray());
        AssetState asset = snapshot.getAssets().tryGet(hash);
        if (asset == null) return false;
        engine.getCurrentContext().getEvaluationStack().push(StackItem.fromInterface(asset));
        return true;
    }

    private boolean headerGetVersion(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            BlockBase header = ((InteropInterface<BlockBase>) _interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(header
                    .version)));
            return true;
        }
        return false;
    }

    private boolean headerGetMerkleRoot(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            BlockBase header = ((InteropInterface<BlockBase>) _interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(header
                    .merkleRoot.toArray()));
            return true;
        }
        return false;
    }

    private boolean headerGetConsensusData(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            BlockBase header = ((InteropInterface<BlockBase>) _interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(header
                    .consensusData)));
            return true;
        }
        return false;
    }

    private boolean headerGetNextConsensus(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            BlockBase header = ((InteropInterface<BlockBase>) _interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(header
                    .nextConsensus.toArray()));
            return true;
        }
        return false;
    }

    private boolean transactionGetType(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            Transaction tx = ((InteropInterface<Transaction>) _interface).getInterface();
            if (tx == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(tx.type
                    .value())));
            return true;
        }
        return false;
    }

    private boolean transactionGetAttributes(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            Transaction tx = ((InteropInterface<Transaction>) _interface).getInterface();
            if (tx == null) return false;
            if (tx.attributes.length > ApplicationEngine.MaxArraySize.intValue())
                return false;
            //LINQ START
/*            engine.getCurrentContext().getEvaluationStack().push(tx.attributes.Select(p = >
                    StackItem.FromInterface(p)).ToArray());*/
            StackItem[] tempArray = Arrays.asList(tx.attributes).stream()
                    .map(p -> StackItem.fromInterface(p)).toArray(StackItem[]::new);
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(tempArray));
            //LINQ END
            return true;
        }
        return false;
    }

    private boolean transactionGetInputs(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            Transaction tx = ((InteropInterface<Transaction>) _interface).getInterface();
            if (tx == null) return false;
            if (tx.inputs.length > ApplicationEngine.MaxArraySize.intValue())
                return false;
            //LINQ START
/*            engine.getCurrentContext().getEvaluationStack().push(tx.inputs.Select(p = > StackItem
                    .FromInterface(p)).ToArray());*/
            StackItem[] tempArray = Arrays.asList(tx.inputs).stream()
                    .map(p -> StackItem.fromInterface(p)).toArray(StackItem[]::new);
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(tempArray));
            //LINQ END
            return true;
        }
        return false;
    }

    private boolean transactionGetOutputs(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            Transaction tx = ((InteropInterface<Transaction>) _interface).getInterface();
            if (tx == null) return false;
            if (tx.outputs.length > ApplicationEngine.MaxArraySize.intValue())
                return false;
            //LINQ START
/*            engine.getCurrentContext().getEvaluationStack().push(tx.outputs.Select(p = > StackItem
                    .FromInterface(p)).ToArray());*/
            StackItem[] tempArray = Arrays.asList(tx.outputs).stream()
                    .map(p -> StackItem.fromInterface(p)).toArray(StackItem[]::new);
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(tempArray));
            //LINQ END
            return true;
        }
        return false;
    }

    private boolean transactionGetReferences(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            Transaction tx = ((InteropInterface<Transaction>) _interface).getInterface();
            if (tx == null) return false;
            if (tx.inputs.length > ApplicationEngine.MaxArraySize.intValue())
                return false;
            //LINQ START
/*            engine.getCurrentContext().getEvaluationStack().push(tx.inputs.Select(p = > StackItem
                    .FromInterface(tx.References[p])).ToArray())
            ;*/
            StackItem[] tempArray = Arrays.asList(tx.inputs).stream()
                    .map(p -> StackItem.fromInterface(tx.getReferences().get(p))).toArray(StackItem[]::new);
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(tempArray));
            //LINQ END
            return true;
        }
        return false;
    }

    private boolean transactionGetUnspentCoins(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            Transaction tx = ((InteropInterface<Transaction>) _interface).getInterface();
            if (tx == null) return false;
            TransactionOutput[] outputs = snapshot.getUnspent(tx.hash()).toArray(new
                    TransactionOutput[0]);
            if (outputs.length > ApplicationEngine.MaxArraySize.intValue())
                return false;
            //LINQ START
/*            engine.getCurrentContext().getEvaluationStack().push(outputs.Select(p = > StackItem
                    .FromInterface(p)).ToArray());*/
            StackItem[] tempArray = Arrays.asList(outputs).stream()
                    .map(p -> StackItem.fromInterface(p)).toArray(StackItem[]::new);
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(tempArray));
            //LINQ END
            return true;
        }
        return false;
    }

    private boolean transactionGetWitnesses(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            Transaction tx = ((InteropInterface<Transaction>) _interface).getInterface();
            if (tx == null) return false;
            if (tx.witnesses.length > ApplicationEngine.MaxArraySize.intValue())
                return false;

            //LINQ START
/*            engine.getCurrentContext().getEvaluationStack().push(WitnessWrapper.create(tx, snapshot)
                    .Select(p = > StackItem.FromInterface(p)).ToArray());*/

            StackItem[] tempArray = Arrays.asList(WitnessWrapper.create(tx, snapshot)).stream()
                    .map(p -> StackItem.fromInterface(p)).toArray(StackItem[]::new);
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(tempArray));
            //LINQ END
            return true;
        }
        return false;
    }

    private boolean invocationTransactionGetScript(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            InvocationTransaction tx = ((InteropInterface<InvocationTransaction>) _interface).getInterface();
            if (tx == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(tx.script));
            return true;
        }
        return false;
    }

    private boolean witnessGetVerificationScript(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            WitnessWrapper witness = ((InteropInterface<WitnessWrapper>) _interface).getInterface();
            if (witness == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(witness
                    .verificationScript));
            return true;
        }
        return false;
    }


    private boolean attributeGetUsage(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            TransactionAttribute attr = ((InteropInterface<TransactionAttribute>) _interface).getInterface();
            if (attr == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(attr
                    .usage.value())));
            return true;
        }
        return false;
    }

    private boolean attributeGetData(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            TransactionAttribute attr = ((InteropInterface<TransactionAttribute>) _interface).getInterface();
            if (attr == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(attr.data));
            return true;
        }
        return false;
    }

    private boolean inputGetHash(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            CoinReference input = ((InteropInterface<CoinReference>) _interface).getInterface();
            if (input == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(input
                    .prevHash.toArray()));
            return true;
        }
        return false;
    }

    private boolean inputGetIndex(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            CoinReference input = ((InteropInterface<CoinReference>) _interface).getInterface();
            if (input == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(input
                    .prevIndex.intValue())));
            return true;
        }
        return false;
    }

    private boolean outputGetAssetId(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            TransactionOutput output = ((InteropInterface<TransactionOutput>) _interface)
                    .getInterface();
            if (output == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(output
                    .assetId.toArray()));
            return true;
        }
        return false;
    }

    private boolean outputGetValue(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            TransactionOutput output = ((InteropInterface<TransactionOutput>) _interface)
                    .getInterface();
            if (output == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(output
                    .value.getData())));
            return true;
        }
        return false;
    }

    private boolean outputGetScriptHash(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            TransactionOutput output = ((InteropInterface<TransactionOutput>) _interface)
                    .getInterface();
            if (output == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(output
                    .scriptHash.toArray()));
            return true;
        }
        return false;
    }

    private boolean accountGetScriptHash(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AccountState account = ((InteropInterface<AccountState>) _interface).getInterface();
            if (account == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(account
                    .scriptHash.toArray()));
            return true;
        }
        return false;
    }

    private boolean accountGetVotes(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AccountState account = ((InteropInterface<AccountState>) _interface).getInterface();
            if (account == null) return false;
            //LINQ START
/*            engine.getCurrentContext().getEvaluationStack().push(account.Votes.Select(p = >
                    (StackItem) p.EncodePoint(true)).ToArray());*/

            StackItem[] tempArray = Arrays.asList(account.votes).stream()
                    .map(p -> StackItem.getStackItem(p.getEncoded(true))).toArray(StackItem[]::new);
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(tempArray));

            //LINQ END
            return true;
        }
        return false;
    }

    private boolean accountGetBalance(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AccountState account = ((InteropInterface<AccountState>) _interface).getInterface();
            UInt256 asset_id = new UInt256(engine.getCurrentContext().getEvaluationStack().pop()
                    .getByteArray());
            if (account == null) return false;
            Fixed8 value = account.balances.getOrDefault(asset_id, null);
            Fixed8 balance = value != null ? value : Fixed8.ZERO;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(balance
                    .getData())));
            return true;
        }
        return false;
    }

    private boolean accountIsStandard(ExecutionEngine engine) {
        UInt160 hash = new UInt160(engine.getCurrentContext().getEvaluationStack().pop()
                .getByteArray());
        ContractState contract = snapshot.getContracts().tryGet(hash);
        boolean isStandard = ((contract == null) || (Helper.isStandardContract(contract.script)));
        engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(isStandard));
        return true;
    }

    private boolean assetCreate(ExecutionEngine engine) {
        if (trigger != TriggerType.Application) return false;
        InvocationTransaction tx = (InvocationTransaction) engine.scriptContainer;
        AssetType asset_type=null;
        try {
            asset_type = AssetType.parse(engine.getCurrentContext().getEvaluationStack()
                    .pop().getBigInteger().byteValue());
        }catch (IllegalArgumentException e){
            TR.error(e);
            asset_type=null;
        }
        if (asset_type==null || asset_type == AssetType.CreditFlag
                || asset_type == AssetType.DutyFlag || asset_type == AssetType.GoverningToken || asset_type == AssetType.UtilityToken)
            return false;
        if (engine.getCurrentContext().getEvaluationStack().peek().getByteArray().length > 1024)
            return false;
        String name = null;
        try {
            name = new String(engine.getCurrentContext().getEvaluationStack().pop()
                    .getByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常");
            throw new RuntimeException(e);
        }
        Fixed8 amount = new Fixed8(engine.getCurrentContext().getEvaluationStack().pop()
                .getBigInteger().longValue());
        if (amount == Fixed8.ZERO || amount.compareTo(Fixed8.negate(Fixed8.SATOSHI)) < 0)
            return false;
        if (asset_type == AssetType.Invoice && amount != Fixed8.negate(Fixed8.SATOSHI))
            return false;
        byte precision = engine.getCurrentContext().getEvaluationStack().pop()
                .getBigInteger().byteValue();
        if (precision > 8) return false;
        if (asset_type == AssetType.Share && precision != 0) return false;
        if (amount != Fixed8.negate(Fixed8.SATOSHI) && amount.getData() % (long) Math.pow(10, 8 -
                precision)
                != 0)
            return false;
        ECPoint owner = new ECPoint(ECC.Secp256r1.getCurve().decodePoint(engine.getCurrentContext()
                .getEvaluationStack().pop()
                .getByteArray()));
        if (owner.isInfinity()) return false;
        if (!checkWitness(engine, owner))
            return false;
        UInt160 admin = new UInt160(engine.getCurrentContext().getEvaluationStack().pop()
                .getByteArray());
        UInt160 issuer = new UInt160(engine.getCurrentContext().getEvaluationStack().pop()
                .getByteArray());
        String finalName = name;
        AssetType finalAsset_type = asset_type;
        AssetState asset = snapshot.getAssets().getOrAdd(tx.hash(), () -> {
            AssetState tempState = new AssetState();
            tempState.assetId = tx.hash();
            tempState.assetType = finalAsset_type;
            tempState.name = finalName;
            tempState.amount = amount;
            tempState.available = Fixed8.ZERO;
            tempState.precision = precision;
            tempState.fee = Fixed8.ZERO;
            tempState.feeAddress = new UInt160();
            tempState.owner = owner;
            tempState.admin = admin;
            tempState.issuer = issuer;
            tempState.expiration = snapshot.getHeight().add(Uint.ONE).add(new Uint(2000000));
            tempState.isFrozen = false;
            return tempState;
        });
        engine.getCurrentContext().getEvaluationStack().push(StackItem.fromInterface(asset));
        return true;
    }

    private boolean assetRenew(ExecutionEngine engine) {
        if (trigger != TriggerType.Application) return false;
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AssetState asset = ((InteropInterface<AssetState>) _interface).getInterface();
            if (asset == null) return false;
            byte years = engine.getCurrentContext().getEvaluationStack().pop()
                    .getBigInteger().byteValue();
            asset = snapshot.getAssets().getAndChange(asset.assetId);
            if (asset.expiration.compareTo(snapshot.getHeight().add(Uint.ONE)) < 0)
                asset.expiration = snapshot.getHeight().add(Uint.ONE);

                asset.expiration = new Uint(String.valueOf(Math.addExact(asset.expiration.longValue(),Math
                        .multiplyExact(new Uint(years).longValue(),new Uint(2000000).longValue()))));
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(asset
                    .expiration)));
            return true;
        }
        return false;
    }

    private boolean assetGetAssetId(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AssetState asset = ((InteropInterface<AssetState>) _interface).getInterface();
            if (asset == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(asset
                    .assetId.toArray()));
            return true;
        }
        return false;
    }

    private boolean assetGetAssetType(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AssetState asset = ((InteropInterface<AssetState>) _interface).getInterface();
            if (asset == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(asset
                    .assetType.value())));
            return true;
        }
        return false;
    }

    private boolean assetGetAmount(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AssetState asset = ((InteropInterface<AssetState>) _interface).getInterface();
            if (asset == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(asset
                    .amount.getData())));
            return true;
        }
        return false;
    }

    private boolean assetGetAvailable(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AssetState asset = ((InteropInterface<AssetState>) _interface).getInterface();
            if (asset == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(asset
                    .available.getData())));
            return true;
        }
        return false;
    }

    private boolean assetGetPrecision(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AssetState asset = ((InteropInterface<AssetState>) _interface).getInterface();
            if (asset == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(StackItem.getStackItem(asset
                    .precision)));
            return true;
        }
        return false;
    }

    private boolean assetGetOwner(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AssetState asset = ((InteropInterface<AssetState>) _interface).getInterface();
            if (asset == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(asset
                    .owner.getEncoded(true)));
            return true;
        }
        return false;
    }

    private boolean assetGetAdmin(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AssetState asset = ((InteropInterface<AssetState>) _interface).getInterface();
            if (asset == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(asset
                    .admin.toArray()));
            return true;
        }
        return false;
    }

    private boolean assetGetIssuer(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            AssetState asset = ((InteropInterface<AssetState>) _interface).getInterface();
            if (asset == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(asset
                    .issuer.toArray()));
            return true;
        }
        return false;
    }

    private boolean contractCreate(ExecutionEngine engine) {
        if (trigger != TriggerType.Application) return false;
        byte[] script = engine.getCurrentContext().getEvaluationStack().pop().getByteArray();
        if (script.length > 1024 * 1024) return false;
        ContractParameterType[] parameter_list = Arrays.asList(engine.getCurrentContext()
                .getEvaluationStack().pop().getByteArray()).stream().map(p ->
                ContractParameterType.parse(p)).toArray(ContractParameterType[]::new);
        if (parameter_list.length > 252) return false;
        ContractParameterType return_type = ContractParameterType.parse(engine.getCurrentContext()
                .getEvaluationStack().pop().getBigInteger().byteValue());
        ContractPropertyState contract_properties = new ContractPropertyState(engine
                .getCurrentContext().getEvaluationStack().pop().getBigInteger().byteValue());
        if (engine.getCurrentContext().getEvaluationStack().peek().getByteArray().length > 252)
            return false;
        String name = null;
        try {
            name = new String(engine.getCurrentContext().getEvaluationStack().pop()
                    .getByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常");
            throw new RuntimeException(e);
        }
        if (engine.getCurrentContext().getEvaluationStack().peek().getByteArray().length > 252)
            return false;
        String version = null;
        try {
            version = new String(engine.getCurrentContext().getEvaluationStack().pop()
                    .getByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常");
            throw new RuntimeException(e);
        }
        if (engine.getCurrentContext().getEvaluationStack().peek().getByteArray().length > 252)
            return false;
        String author = null;
        try {
            author = new String(engine.getCurrentContext().getEvaluationStack().pop()
                    .getByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常");
            throw new RuntimeException(e);
        }
        if (engine.getCurrentContext().getEvaluationStack().peek().getByteArray().length > 252)
            return false;
        String email = null;
        try {
            email = new String(engine.getCurrentContext().getEvaluationStack().pop()
                    .getByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常");
            throw new RuntimeException(e);
        }
        if (engine.getCurrentContext().getEvaluationStack().peek().getByteArray().length > 65536)
            return false;
        String description = null;
        try {
            description = new String(engine.getCurrentContext().getEvaluationStack().pop()
                    .getByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常");
            throw new RuntimeException(e);
        }
        UInt160 hash = Helper.toScriptHash(script);
        ContractState contract = snapshot.getContracts().tryGet(hash);
        if (contract == null) {
            contract = new ContractState();
            contract.script = script;
            contract.parameterList = parameter_list;
            contract.returnType = return_type;
            contract.contractProperties = contract_properties;
            contract.name = name;
            contract.codeVersion = version;
            contract.author = author;
            contract.email = email;
            contract.description = description;
            snapshot.getContracts().add(hash, contract);
            contractsCreated.put(hash, new UInt160(engine.getCurrentContext().getScriptHash()));
        }
        engine.getCurrentContext().getEvaluationStack().push(StackItem.fromInterface(contract));
        return true;
    }

    private boolean contractMigrate(ExecutionEngine engine) {
        if (trigger != TriggerType.Application) return false;
        byte[] script = engine.getCurrentContext().getEvaluationStack().pop().getByteArray();
        if (script.length > 1024 * 1024) return false;
        ContractParameterType[] parameter_list = Arrays.asList(engine.getCurrentContext()
                .getEvaluationStack().pop().getByteArray()).stream().map(p ->
                ContractParameterType.parse(p)).toArray(ContractParameterType[]::new);
        if (parameter_list.length > 252) return false;
        ContractParameterType return_type = ContractParameterType.parse(engine.getCurrentContext()
                .getEvaluationStack().pop().getBigInteger().byteValue());
        ContractPropertyState contract_properties = new ContractPropertyState(engine
                .getCurrentContext().getEvaluationStack().pop().getBigInteger().byteValue());
        if (engine.getCurrentContext().getEvaluationStack().peek().getByteArray().length > 252)
            return false;
        String name = null;
        try {
            name = new String(engine.getCurrentContext().getEvaluationStack().pop()
                    .getByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常，一般不发生");
            throw new RuntimeException(e);
        }
        if (engine.getCurrentContext().getEvaluationStack().peek().getByteArray().length > 252)
            return false;
        String version = null;
        try {
            version = new String(engine.getCurrentContext().getEvaluationStack()
                    .pop().getByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常，一般不发生");
            throw new RuntimeException(e);
        }
        if (engine.getCurrentContext().getEvaluationStack().peek().getByteArray().length > 252)
            return false;
        String author = null;
        try {
            author = new String(engine.getCurrentContext().getEvaluationStack()
                    .pop().getByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常，一般不发生");
            throw new RuntimeException(e);
        }
        if (engine.getCurrentContext().getEvaluationStack().peek().getByteArray().length > 252)
            return false;
        String email = null;
        try {
            email = new String(engine.getCurrentContext().getEvaluationStack().pop()
                    .getByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常，一般不发生");
            throw new RuntimeException(e);
        }
        if (engine.getCurrentContext().getEvaluationStack().peek().getByteArray().length > 65536)
            return false;
        String description = null;
        try {
            description = new String(engine.getCurrentContext().getEvaluationStack()
                    .pop().getByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常，一般不发生");
            throw new RuntimeException(e);
        }
        UInt160 hash = Helper.toScriptHash(script);
        ContractState contract = snapshot.getContracts().tryGet(hash);
        if (contract == null) {
            contract = new ContractState();
            contract.script = script;
            contract.parameterList = parameter_list;
            contract.returnType = return_type;
            contract.contractProperties = contract_properties;
            contract.name = name;
            contract.codeVersion = version;
            contract.author = author;
            contract.email = email;
            contract.description = description;
            snapshot.getContracts().add(hash, contract);
            contractsCreated.put(hash, new UInt160(engine.getCurrentContext().getScriptHash()));
            if (contract.hasStorage()) {
                for (java.util.Map.Entry<StorageKey, StorageItem> pair : snapshot.getStorages().find(engine
                        .getCurrentContext().getScriptHash())) {
                    StorageKey storageKey = new StorageKey();
                    storageKey.scriptHash = hash;
                    storageKey.key = pair.getKey().key;
                    StorageItem storageItem = new StorageItem();
                    storageItem.value = pair.getValue().value;
                    storageItem.isConstant = false;
                    snapshot.getStorages().add(storageKey, storageItem);
                }
            }
        }
        engine.getCurrentContext().getEvaluationStack().push(StackItem.fromInterface(contract));
        return contractDestroy(engine);
    }

    private boolean contractGetScript(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            ContractState contract = ((InteropInterface<ContractState>) _interface).getInterface();
            if (contract == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(contract
                    .script));
            return true;
        }
        return false;
    }

    private boolean contractIsPayable(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            ContractState contract = ((InteropInterface<ContractState>) _interface).getInterface();
            if (contract == null) return false;
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem(contract
                    .payable()));
            return true;
        }
        return false;
    }

    private boolean storageFind(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            StorageContext context = ((InteropInterface<StorageContext>) _interface)
                    .getInterface();
            if (!checkStorageContext(context)) return false;
            byte[] prefix = engine.getCurrentContext().getEvaluationStack().pop().getByteArray();
            byte[] prefix_key;
            ByteArrayOutputStream temp=new ByteArrayOutputStream();
            BinaryWriter ms = new BinaryWriter(temp);
            int index = 0;
            int remain = prefix.length;
            while (remain >= 16) {
                ms.write(prefix, index, 16);
                ms.write(new byte[]{0x00});
                index += 16;
                remain -= 16;
            }
            if (remain > 0)
                ms.write(prefix, index, remain);
            prefix_key = BitConverter.merge(context.scriptHash.toArray(),temp.toByteArray());
            //LINQ START
/*            StorageIterator iterator = new StorageIterator(snapshot.getStorages().find(prefix_key)
                    .Where(p-> p.Key.Key.Take(prefix.length).SequenceEqual(prefix))
                    .GetEnumerator());*/

            StorageIterator iterator = new StorageIterator(snapshot.getStorages().find(prefix_key)
                    .stream().filter(p -> {
                        if (Arrays.equals(BitConverter.subBytes(p.getKey().key, 0, prefix.length - 1), prefix)) {
                            return true;
                        } else {
                            return false;
                        }
                    }).collect(Collectors.toList()).iterator());
            //LINQ END
            engine.getCurrentContext().getEvaluationStack().push(StackItem.fromInterface(iterator));
            disposables.add(iterator);
            return true;
        }
        return false;
    }

    private boolean enumeratorCreate(ExecutionEngine engine) {
        StackItem array = engine.getCurrentContext().getEvaluationStack().pop();
        if (array instanceof Array) {

            List<StackItem> temp=StreamSupport.stream(((Array) array).getEnumerator()
                    .spliterator(),false).collect(Collectors.toList());
            IEnumerator enumerator = new ArrayWrapper(temp);
            engine.getCurrentContext().getEvaluationStack().push(StackItem.fromInterface
                    (enumerator));
            return true;
        }
        return false;
    }

    private boolean enumeratorNext(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            IEnumerator enumerator = ((InteropInterface<IEnumerator>) _interface).getInterface();
            engine.getCurrentContext().getEvaluationStack().push(StackItem.getStackItem
                    (enumerator.next()));
            return true;
        }
        return false;
    }

    private boolean enumeratorValue(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            IEnumerator enumerator = ((InteropInterface<IEnumerator>) _interface).getInterface();
            engine.getCurrentContext().getEvaluationStack().push(enumerator.value());
            return true;
        }
        return false;
    }

    private boolean enumeratorConcat(ExecutionEngine engine) {
        StackItem _interface1 = engine.getCurrentContext().getEvaluationStack().pop();
        if (!(_interface1 instanceof InteropInterface)) return false;
        StackItem _interface2 = engine.getCurrentContext().getEvaluationStack().pop();
        if (!(_interface2 instanceof InteropInterface)) return false;
        IEnumerator first = ((InteropInterface<IEnumerator>) _interface1).getInterface();
        IEnumerator second = ((InteropInterface<IEnumerator>) _interface2).getInterface();
        IEnumerator result = new ConcatenatedEnumerator(first, second);
        engine.getCurrentContext().getEvaluationStack().push(StackItem.fromInterface(result));
        return true;
    }

    private boolean iteratorCreate(ExecutionEngine engine) {
        IIterator iterator;
        StackItem stackItem = engine.getCurrentContext().getEvaluationStack().pop();
        if (stackItem instanceof Array) {
            List<StackItem> temp=StreamSupport.stream(((Array) stackItem).getEnumerator()
                    .spliterator(),false).collect(Collectors.toList());
            iterator = new ArrayWrapper(temp);
        } else if (stackItem instanceof Map) {
            iterator = new MapWrapper((Map) stackItem);
        } else {
            return false;
        }
        engine.getCurrentContext().getEvaluationStack().push(StackItem.fromInterface(iterator));
        return true;
    }

    private boolean iteratorKey(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            IIterator iterator = ((InteropInterface<IIterator>) _interface).getInterface();
            engine.getCurrentContext().getEvaluationStack().push(iterator.key());
            return true;
        }
        return false;
    }

    private boolean iteratorKeys(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            IIterator iterator = ((InteropInterface<IIterator>) _interface).getInterface();
            engine.getCurrentContext().getEvaluationStack().push(StackItem.fromInterface(new
                    IteratorValuesWrapper(iterator)));
            return true;
        }
        return false;
    }

    private boolean iteratorValues(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().getEvaluationStack().pop();
        if (_interface instanceof InteropInterface) {
            IIterator iterator = ((InteropInterface<IIterator>) _interface).getInterface();
            engine.getCurrentContext().getEvaluationStack().push(StackItem.fromInterface(new
                    IteratorValuesWrapper(iterator)));
            return true;
        }
        return false;
    }
}