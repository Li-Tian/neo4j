package neo.persistence;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import neo.Fixed8;
import neo.Helper;
import neo.UInt160;
import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.io.caching.DataCache;
import neo.io.caching.MetaDataCache;
import neo.io.wrappers.UInt32Wrapper;
import neo.ledger.AccountState;
import neo.ledger.AssetState;
import neo.ledger.BlockState;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.ledger.ContractState;
import neo.ledger.HashIndexState;
import neo.ledger.HeaderHashList;
import neo.ledger.SpentCoin;
import neo.ledger.SpentCoinState;
import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.ledger.TransactionState;
import neo.ledger.UnspentCoinState;
import neo.ledger.ValidatorState;
import neo.ledger.ValidatorsCountState;
import neo.log.notr.TR;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.cryptography.ecc.ECPoint;
import neo.network.p2p.payloads.EnrollmentTransaction;
import neo.network.p2p.payloads.StateDescriptor;
import neo.network.p2p.payloads.StateTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionOutput;
import neo.vm.IScriptTable;


/**
 * Snapshot
 */
public abstract class Snapshot extends AbstractPersistence implements IScriptTable {

    protected Block persistingBlock;
    protected DataCache<UInt256, BlockState> blocks;
    protected DataCache<UInt256, TransactionState> transactions;
    protected DataCache<UInt160, AccountState> accounts;
    protected DataCache<UInt256, UnspentCoinState> unspentCoins;
    protected DataCache<UInt256, SpentCoinState> spentCoins;
    protected DataCache<ECPoint, ValidatorState> validators;
    protected DataCache<UInt256, AssetState> assets;
    protected DataCache<UInt160, ContractState> contracts;
    protected DataCache<StorageKey, StorageItem> storages;
    protected DataCache<UInt32Wrapper, HeaderHashList> headerHashList;
    protected MetaDataCache<ValidatorsCountState> validatorsCount;
    protected MetaDataCache<HashIndexState> blockHashIndex;
    protected MetaDataCache<HashIndexState> headerHashIndex;


    /**
     * free resource
     */
    public void close() throws IOException {
        TR.enter();
        TR.exit();
    }

    /**
     * get block cache
     */
    @Override
    public DataCache<UInt256, BlockState> getBlocks() {
        return this.blocks;
    }

    /**
     * get transaction cache
     */
    @Override
    public DataCache<UInt256, TransactionState> getTransactions() {
        return this.transactions;
    }

    /**
     * get account cache
     */
    @Override
    public DataCache<UInt160, AccountState> getAccounts() {
        return this.accounts;
    }

    /**
     * get utxo cache
     */
    @Override
    public DataCache<UInt256, UnspentCoinState> getUnspentCoins() {
        return this.unspentCoins;
    }

    /**
     * get spent transaction cache
     */
    @Override
    public DataCache<UInt256, SpentCoinState> getSpentCoins() {
        return this.spentCoins;
    }

    /**
     * get validators cache
     */
    @Override
    public DataCache<ECPoint, ValidatorState> getValidators() {
        return this.validators;
    }

    /**
     * get asset cache
     */
    @Override
    public DataCache<UInt256, AssetState> getAssets() {
        return this.assets;
    }

    /**
     * get contract cache
     */
    @Override
    public DataCache<UInt160, ContractState> getContracts() {
        return this.contracts;
    }

    /**
     * get contact's storage cache
     */
    @Override
    public DataCache<StorageKey, StorageItem> getStorages() {
        return this.storages;
    }

    /**
     * get header hash list cache
     */
    @Override
    public DataCache<UInt32Wrapper, HeaderHashList> getHeaderHashList() {
        return this.headerHashList;
    }

    /**
     * get validator count cache
     */
    @Override
    public MetaDataCache<ValidatorsCountState> getValidatorsCount() {
        return this.validatorsCount;
    }

    /**
     * get block hash cache
     */
    @Override
    public MetaDataCache<HashIndexState> getBlockHashIndex() {
        return this.blockHashIndex;
    }

    /**
     * get header hash cache
     */
    @Override
    public MetaDataCache<HashIndexState> getHeaderHashIndex() {
        return this.headerHashIndex;
    }

    /**
     * Get the block being persisted currently
     */
    public Block getPersistingBlock() {
        return this.persistingBlock;
    }

    /**
     * Set the block being persisted currently
     *
     * @param block the persisting block
     */
    public void setPersistingBlock(Block block) {
        this.persistingBlock = block;
    }


    /**
     * Current block height
     */
    public Uint getHeight() {
        return getBlockHashIndex().get().index;
    }

    /**
     * Block header height
     */
    public UInt256 getHeaderHeight() {
        return getBlockHashIndex().get().hash;
    }

    /**
     * Current block hash
     */
    public UInt256 getCurrentBlockHash() {
        return getBlockHashIndex().get().hash;
    }

    /**
     * Current block header hash
     */
    public UInt256 getCurrentHeaderHash() {
        return getHeaderHashIndex().get().hash;
    }

    /**
     * Clone snapshot
     *
     * @return snapshot
     */
    @Override
    public Snapshot clone() {
        return new CloneSnapshot(this);
    }

    /**
     * Persist to disk
     */
    public void commit() {
        TR.enter();
        // C# code
        // accounts.DeleteWhere((k, v) =>
        //        !v.IsFrozen && v.Votes.Length == 0 && v.Balances.All(p = > p.Value <= Fixed8.Zero));
        //  unspentCoins.DeleteWhere((k, v) =>v.Items.All(p = > p.HasFlag(CoinState.Spent)));
        //        spentCoins.DeleteWhere((k, v) =>v.Items.Count == 0);

        accounts.deleteWhere((k, v) -> !v.isFrozen
                && v.votes.length == 0
                && v.balances.entrySet().stream().allMatch(p -> p.getValue().compareTo(Fixed8.ZERO) <= 0));
        unspentCoins.deleteWhere((k, v) -> Arrays.stream(v.items).allMatch(p -> p.hasFlag(CoinState.Spent)));
        spentCoins.deleteWhere((k, v) -> v.items.size() == 0);

        blocks.commit();
        transactions.commit();
        accounts.commit();
        unspentCoins.commit();
        spentCoins.commit();
        validators.commit();
        assets.commit();
        contracts.commit();
        storages.commit();
        headerHashList.commit();
        validatorsCount.commit();
        blockHashIndex.commit();
        headerHashIndex.commit();
        TR.exit();
    }


    /**
     * Get a dictionary for outputs of transactions that have not yet been claimed. The key of this
     * dictionary is the ordinal number of the output of the transaction, and the value is the spent
     * state of the output.
     *
     * @param hash Transaction hash
     * @return Return a dictionary of outputs that have not been claimed, or null if the transaction
     * does not exist
     */
    public HashMap<Ushort, SpentCoin> getUnclaimed(UInt256 hash) {
        TR.enter();
        /*
            C# code
             return coin_state.items.ToDictionary(p = > p.Key,p =>new SpentCoin
            {
                Output = tx_state.Transaction.Outputs[p.Key],
                        StartHeight = tx_state.BlockIndex,
                        EndHeight = p.Value
            });
         */
        TransactionState tx_state = getTransactions().tryGet(hash);
        if (tx_state == null) return TR.exit(null);
        SpentCoinState coin_state = getSpentCoins().tryGet(hash);
        if (coin_state == null) {
            return TR.exit(new HashMap<>());
        }
        HashMap<Ushort, SpentCoin> resultMap = new HashMap<>(coin_state.items.size());
        for (Map.Entry<Ushort, Uint> entry : coin_state.items.entrySet()) {
            SpentCoin spentCoin = new SpentCoin();
            spentCoin.output = tx_state.transaction.outputs[entry.getKey().intValue()];
            spentCoin.startHeight = tx_state.blockIndex;
            spentCoin.endHeight = entry.getValue();
            resultMap.put(entry.getKey(), spentCoin);
        }
        return TR.exit(resultMap);
    }

    /**
     * Calculate the GAS bonus that can be claimed
     *
     * @param inputs The collection of transactions to which Claim refers
     * @return The amount of GAS that can be claimed
     * @throws IllegalArgumentException IgnoreClaimed when set to false, if one of the following
     *                                  situations occurs: <br/> 1) claimable is null, or its number
     *                                  is zero; <br/> 2) the input that has been claimed is found;
     *                                  <br/> Throw the exception.
     */
    public Fixed8 calculateBonus(Collection<CoinReference> inputs) {
        TR.enter();
        return TR.exit(calculateBonus(inputs, true));
    }


    /**
     * Calculate the GAS bonus that can be claimed
     *
     * @param inputs        The collection of transactions to which Claim refers
     * @param ignoreClaimed Whether to ignore the input that has already been claimed, <br/> If the
     *                      UTXO indicated by the parameter inputs does not exist or has been
     *                      claimed,<br/> If ignoreClaimed is specified as true at this point, it
     *                      ignores the error and moves on to the rest of the calculation. <br/> If
     *                      ignoreClaimed specifies false, an exception is thrown.
     * @return The amount of GAS that can be claimed
     * @throws IllegalArgumentException IgnoreClaimed when set to false, if one of the following
     *                                  situations occurs: <br/> 1) claimable is null, or its number
     *                                  is zero; <br/> 2) the input that has been claimed is found;
     *                                  <br/> Throw the exception.
     */
    public Fixed8 calculateBonus(Collection<CoinReference> inputs, boolean ignoreClaimed) {
        TR.enter();
        // C# code
        //        foreach (var group in inputs.GroupBy(p => p.PrevHash))
        //        {
        //            Dictionary<ushort, SpentCoin> claimable = GetUnclaimed(group.Key);
        //            if (claimable == null || claimable.Count == 0)
        //                if (ignoreClaimed)
        //                    continue;
        //                else
        //                    throw new ArgumentException();
        //            foreach (CoinReference claim in group)
        //            {
        //                if (!claimable.TryGetValue(claim.PrevIndex, out SpentCoin claimed))
        //                    if (ignoreClaimed)
        //                        continue;
        //                    else
        //                        throw new ArgumentException();
        //                unclaimed.Add(claimed);
        //            }
        //        }
        //        return CalculateBonusInternal(unclaimed);

        ArrayList<SpentCoin> unclaimed = new ArrayList<>();
        inputs.stream().collect(Collectors.groupingBy(p -> p.prevHash)).forEach((key, group) -> {
            HashMap<Ushort, SpentCoin> claimable = getUnclaimed(key);
            if (claimable == null || claimable.isEmpty()) {
                if (ignoreClaimed) {
                    TR.exit();
                    return;
                }
                throw new IllegalArgumentException();
            }

            for (CoinReference claim : group) {
                if (!claimable.containsKey(claim.prevIndex)) {
                    if (ignoreClaimed) {
                        continue;
                    }
                    throw new IllegalArgumentException();
                }
                SpentCoin claimed = claimable.get(claim.prevIndex);
                unclaimed.add(claimed);
            }
        });
        return TR.exit(calculateBonusInternal(unclaimed));
    }

    /**
     * Calculate the GAS rewards that can be claimed
     *
     * @param inputs     The transaction set pointed to by Claim
     * @param height_end The height to be spent
     * @return The amount of GAS that can be claimed
     * @throws IllegalArgumentException When the pointed transaction does not exist, or the
     *                                  transaction output serial number is not legal, or the
     *                                  transaction output is not NEO asset, Throw an exception.
     */
    public Fixed8 calculateBonus(Collection<CoinReference> inputs, Uint height_end) {
        TR.enter();
        /*
            C# code
            foreach(var group in inputs.GroupBy(p = > p.PrevHash))
            {
                TransactionState tx_state = Transactions.TryGet(group.Key);
                if (tx_state == null) throw new ArgumentException();
                if (tx_state.BlockIndex == height_end) continue;
                foreach(CoinReference claim in group)
                {
                    if (claim.PrevIndex >= tx_state.Transaction.Outputs.Length ||
                    !tx_state.Transaction.Outputs[claim.PrevIndex].AssetId.Equals(Blockchain.GoverningToken.Hash))
                        throw new ArgumentException();
                    unclaimed.Add(new SpentCoin
                    {
                        Output = tx_state.Transaction.Outputs[claim.PrevIndex],
                                StartHeight = tx_state.BlockIndex,
                                EndHeight = height_end
                    });
                }
            }
         */

        ArrayList<SpentCoin> unclaimed = new ArrayList<>();
        inputs.stream().collect(Collectors.groupingBy(p -> p.prevHash)).forEach((key, group) -> {
            TransactionState tx_state = getTransactions().tryGet(key);
            if (tx_state == null) throw new IllegalArgumentException();
            if (!tx_state.blockIndex.equals(height_end)) {
                for (CoinReference claim : group) {
                    // required the asset be NEO
                    if (claim.prevIndex.intValue() >= tx_state.transaction.outputs.length
                            || !tx_state.transaction.outputs[claim.prevIndex.intValue()]
                            .assetId.equals(Blockchain.GoverningToken.hash())) {
                        throw new IllegalArgumentException();
                    }
                    SpentCoin spentCoin = new SpentCoin();
                    spentCoin.output = tx_state.transaction.outputs[claim.prevIndex.intValue()];
                    spentCoin.startHeight = tx_state.blockIndex;
                    spentCoin.endHeight = height_end;
                    unclaimed.add(spentCoin);
                }
            }
        });
        return TR.exit(calculateBonusInternal(unclaimed));
    }

    private Fixed8 calculateBonusInternal(Collection<SpentCoin> unclaimed) {
        TR.enter();
        /*
            C# code:
            Fixed8 amount_claimed = Fixed8.Zero;
            foreach (var group in unclaimed.GroupBy(p => new { p.StartHeight, p.EndHeight }))
            {
                uint amount = 0;
                uint ustart = group.Key.StartHeight / Blockchain.DecrementInterval;
                if (ustart < Blockchain.GenerationAmount.Length)
                {
                    uint istart = group.Key.StartHeight % Blockchain.DecrementInterval;
                    uint uend = group.Key.EndHeight / Blockchain.DecrementInterval;
                    uint iend = group.Key.EndHeight % Blockchain.DecrementInterval;
                    if (uend >= Blockchain.GenerationAmount.Length)
                    {
                        uend = (uint)Blockchain.GenerationAmount.Length;
                        iend = 0;
                    }
                    if (iend == 0)
                    {
                        uend--;
                        iend = Blockchain.DecrementInterval;
                    }
                    while (ustart < uend)
                    {
                        amount += (Blockchain.DecrementInterval - istart) * Blockchain.GenerationAmount[ustart];
                        ustart++;
                        istart = 0;
                    }
                    amount += (iend - istart) * Blockchain.GenerationAmount[ustart];
                }
                amount += (uint)(this.GetSysFeeAmount(group.Key.EndHeight - 1) - (group.Key.StartHeight == 0
                ? 0 : this.GetSysFeeAmount(group.Key.StartHeight - 1)));
                amount_claimed += group.Sum(p => p.Value) / 100000000 * amount;
            }
            return amount_claimed;
         */
        // TODO group first then calculate bonus
        TR.fixMe("group by tx.preHash then calculate bonus");
        Fixed8 amount_claimed = Fixed8.ZERO;
        for (SpentCoin coin : unclaimed) {
            int amount = 0;
            int ustart = coin.startHeight.intValue() / Blockchain.DecrementInterval;
            if (ustart < Blockchain.GenerationAmount.length) {
                int istart = coin.startHeight.intValue() % Blockchain.DecrementInterval;
                int uend = coin.endHeight.intValue() / Blockchain.DecrementInterval;
                int iend = coin.endHeight.intValue() % Blockchain.DecrementInterval;
                if (uend >= Blockchain.GenerationAmount.length) {
                    uend = Blockchain.GenerationAmount.length;
                    iend = 0;
                }
                if (iend == 0) {
                    uend--;
                    iend = Blockchain.DecrementInterval;
                }
                while (ustart < uend) {
                    amount += (Blockchain.DecrementInterval - istart) * Blockchain.GenerationAmount[ustart];
                    ustart++;
                    istart = 0;
                }
                amount += (iend - istart) * Blockchain.GenerationAmount[ustart];
            }
            long leftSysFee = coin.startHeight.equals(Uint.ZERO)
                    ? 0
                    : this.getSysFeeAmount(coin.startHeight.subtract(new Uint(1)));
            amount += this.getSysFeeAmount(coin.endHeight.subtract(new Uint(1))) - leftSysFee;
            Fixed8 sub_amount_claimed = Fixed8.multiply(Fixed8.divide(coin.value(), 100000000), amount);
            amount_claimed = Fixed8.add(amount_claimed, sub_amount_claimed);

        }
        return TR.exit(amount_claimed);
    }

    private ECPoint[] validatorPubkeys = null;


    /**
     * Get the current Validators participating in consensus
     *
     * @return The list of Validators for the consensus in point's (x,y) ascending order.
     */
    public ECPoint[] getValidatorPubkeys() {
        TR.enter();
        if (validatorPubkeys == null) {
            Collection<ECPoint> points = getValidators(Collections.emptyList());
            validatorPubkeys = new ECPoint[points.size()];
            points.toArray(validatorPubkeys);
        }
        return TR.exit(validatorPubkeys);
    }

    /**
     * Get a list of Validators participating in the consensus。
     *
     * @param others Packaged transaction. The impact of these transactions is included when the
     *               Validators Counting.
     * @return The list of Validators participating in the consensus
     */
    public Collection<ECPoint> getValidators(Collection<Transaction> others) {
        TR.enter();
        Snapshot snapshot = clone();

        // 计算因交易产生的票数变化
        for (Transaction tx : others) {
            for (TransactionOutput output : tx.outputs) {
                AccountState account = snapshot.getAccounts().getAndChange(output.scriptHash,
                        () -> new AccountState(output.scriptHash));
                account.increaseBalance(output.assetId, output.value);

                if (output.assetId.equals(Blockchain.GoverningToken.hash()) && account.votes.length > 0) {
                    for (ECPoint pubkey : account.votes) {
                        ValidatorState validator = snapshot.getValidators().getAndChange(pubkey,
                                () -> new ValidatorState(pubkey));
                        validator.votes = Fixed8.add(validator.votes, output.value);
                    }
                    ValidatorsCountState countState = snapshot.getValidatorsCount().getAndChange();
                    int index = account.votes.length - 1;
                    countState.votes[index] = Fixed8.subtract(countState.votes[index], output.value);
                }
            }

            Arrays.stream(tx.inputs).collect(Collectors.groupingBy(p -> p.prevHash)).forEach((key, group) -> {
                Transaction tx_prev = snapshot.getTransaction(key);
                for (CoinReference input : group) {
                    TransactionOutput output_prev = tx_prev.outputs[input.prevIndex.intValue()];
                    AccountState account = snapshot.getAccounts().getAndChange(output_prev.scriptHash);
                    if (output_prev.assetId.equals(Blockchain.GoverningToken.hash())) {
                        if (account.votes.length > 0) {
                            for (ECPoint pubkey : account.votes) {
                                ValidatorState validator = snapshot.getValidators().getAndChange(pubkey);
                                validator.votes = Fixed8.subtract(validator.votes, output_prev.value);
                                if (!validator.registered && validator.votes.equals(Fixed8.ZERO)) {
                                    snapshot.getValidators().delete(pubkey);
                                }
                            }
                            ValidatorsCountState countState = snapshot.getValidatorsCount().getAndChange();
                            int index = account.votes.length - 1;
                            countState.votes[index] = Fixed8.subtract(countState.votes[index], output_prev.value);
                        }
                    }
                    account.increaseBalance(output_prev.assetId, Fixed8.negate(output_prev.value));
                }
            });

            if (tx instanceof EnrollmentTransaction) {
                EnrollmentTransaction tx_enrollment = (EnrollmentTransaction) tx;
                ValidatorState validator = snapshot.getValidators().getAndChange(tx_enrollment.publicKey,
                        () -> new ValidatorState(tx_enrollment.publicKey));
                validator.registered = true;
            } else if (tx instanceof StateTransaction) {
                StateTransaction tx_state = (StateTransaction) tx;
                for (StateDescriptor descriptor : tx_state.descriptors) {
                    switch (descriptor.type) {
                        case Account:
                            Blockchain.processAccountStateDescriptor(descriptor, snapshot);
                            break;
                        case Validator:
                            Blockchain.processValidatorStateDescriptor(descriptor, snapshot);
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        // 计算 见证人
        // 1) 先计算见证人的个数
        //      C# code
        //        int count = (int) snapshot.ValidatorsCount.Get().Votes.Select((p, i) =>new
        //        {
        //            Count = i,
        //                    Votes = p
        //        }).Where(p = > p.Votes > Fixed8.Zero).
        //        ToArray().WeightedFilter(0.25, 0.75, p = > p.Votes.GetData(), (p, w) =>new
        //        {
        //            p.Count,
        //                    Weight = w
        //        }).WeightedAverage(p = > p.Count, p =>p.Weight);
        //        count = Math.Max(count, Blockchain.StandbyValidators.Length);

        Fixed8[] votes = snapshot.validatorsCount.get().votes;
        ArrayList<AbstractMap.SimpleEntry<Integer, Fixed8>> list = new ArrayList<>();
        for (int i = 0; i < votes.length; i++) {
            Fixed8 count = votes[i];
            if (Fixed8.smaller(Fixed8.ZERO, count)) {
                list.add(new AbstractMap.SimpleEntry<>(i, votes[i]));
            }
        }
        Collection<AbstractMap.SimpleEntry<Integer, Long>> rangList = Helper.weightedFilter(list, 0.25, 0.75,
                p -> p.getValue(),
                (p, w) -> new AbstractMap.SimpleEntry<>(p.getKey(), w)
        );
        long count = Helper.weightedAverage(rangList, p -> Long.valueOf(p.getKey()), p -> p.getValue());
        count = Math.max(count, Blockchain.StandbyValidators.length);

        // 2) 再计算具体的见证人
        // C3 code
        //   HashSet<ECPoint> sv = new HashSet<ECPoint>(Blockchain.StandbyValidators);
        //        ECPoint[] pubkeys = snapshot.Validators.Find().Select(p = > p.Value).
        //        Where(p = > (p.Registered && p.Votes > Fixed8.Zero) || sv.Contains(p.PublicKey)).
        //        OrderByDescending(p = > p.Votes).ThenBy(p = > p.PublicKey).Select(p = > p.PublicKey).
        //        Take(count).ToArray();
        //        IEnumerable<ECPoint> result;
        //        if (pubkeys.Length == count) {
        //            result = pubkeys;
        //        } else {
        //            HashSet<ECPoint> hashSet = new HashSet<ECPoint>(pubkeys);
        //            for (int i = 0; i < Blockchain.StandbyValidators.Length && hashSet.Count < count; i++)
        //                hashSet.Add(Blockchain.StandbyValidators[i]);
        //            result = hashSet;
        //        }
        //        return result.OrderBy(p = > p);

        HashSet<ECPoint> sv = Helper.array2HashSet(Blockchain.StandbyValidators);
        List<ECPoint> results = snapshot.getValidators().find().stream()
                .map(p -> p.getValue())
                .filter(p -> (p.registered && Fixed8.smaller(Fixed8.ZERO, p.votes)) || sv.contains(p.publicKey))
                .sorted(Comparator.comparing(ValidatorState::getVotes)
                        .thenComparing(p -> p.publicKey)
                        .reversed())
                .map(p -> p.publicKey).limit(count).collect(Collectors.toList());

        if (results.size() == count) {
            return TR.exit(results);
        } else {
            HashSet<ECPoint> set = new HashSet<>(results);
            for (int i = 0; i < Blockchain.StandbyValidators.length && set.size() < count; i++) {
                set.add(Blockchain.StandbyValidators[i]);
            }
            results.clear();
            results.addAll(set);
        }
        Collections.sort(results);
        return TR.exit(results);
    }


    /**
     * get script by script hash
     *
     * @param script_hash script hash
     * @return script source code
     */
    public byte[] getScript(byte[] script_hash) {
        TR.enter();
        UInt160 hash = new UInt160(script_hash);
        return TR.exit(getContracts().get(hash).script);
    }

}
