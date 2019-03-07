package neo.persistence;

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
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.cryptography.ECC.ECPoint;
import neo.network.p2p.payloads.EnrollmentTransaction;
import neo.network.p2p.payloads.StateDescriptor;
import neo.network.p2p.payloads.StateTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionOutput;


/**
 * 快照
 */
public abstract class Snapshot extends AbstractPersistence {

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

    @Override
    public DataCache<UInt256, BlockState> getBlocks() {
        return this.blocks;
    }

    @Override
    public DataCache<UInt256, TransactionState> getTransactions() {
        return this.transactions;
    }

    @Override
    public DataCache<UInt160, AccountState> getAccounts() {
        return this.accounts;
    }

    @Override
    public DataCache<UInt256, UnspentCoinState> getUnspentCoins() {
        return this.unspentCoins;
    }

    @Override
    public DataCache<UInt256, SpentCoinState> getSpentCoins() {
        return this.spentCoins;
    }

    @Override
    public DataCache<ECPoint, ValidatorState> getValidators() {
        return this.validators;
    }

    @Override
    public DataCache<UInt256, AssetState> getAssets() {
        return this.assets;
    }

    @Override
    public DataCache<UInt160, ContractState> getContracts() {
        return this.contracts;
    }

    @Override
    public DataCache<StorageKey, StorageItem> getStorages() {
        return this.storages;
    }

    @Override
    public DataCache<UInt32Wrapper, HeaderHashList> getHeaderHashList() {
        return this.headerHashList;
    }

    @Override
    public MetaDataCache<ValidatorsCountState> getValidatorsCount() {
        return this.validatorsCount;
    }

    @Override
    public MetaDataCache<HashIndexState> getBlockHashIndex() {
        return this.blockHashIndex;
    }

    @Override
    public MetaDataCache<HashIndexState> getHeaderHashIndex() {
        return this.headerHashIndex;
    }

    /**
     * 当前正在持久化的区块
     */
    public Block getPersistingBlock() {
        return this.persistingBlock;
    }

    /**
     * 设置正在持久化的区块
     *
     * @param block 待持久化的区块
     */
    public void setPersistingBlock(Block block) {
        this.persistingBlock = block;
    }


    /**
     * 当前区块高度
     */
    public Uint getHeight() {
        return getBlockHashIndex().get().index;
    }

    /**
     * 区块头高度
     */
    public UInt256 getHeaderHeight() {
        return getBlockHashIndex().get().hash;
    }

    /**
     * 当前区块hash
     */
    public UInt256 getCurrentBlockHash() {
        return getBlockHashIndex().get().hash;
    }

    /**
     * 当前区块头hash
     */
    public UInt256 getCurrentHeaderHash() {
        return getHeaderHashIndex().get().hash;
    }

    /**
     * 克隆快照
     *
     * @return 快照
     */
    @Override
    public Snapshot clone() {
        return new CloneSnapshot(this);
    }

    /**
     * 持久化到磁盘
     */
    public void commit() {
        // TODO
//        accounts.DeleteWhere((k, v) =>
//        !v.IsFrozen && v.Votes.Length == 0 && v.Balances.All(p = > p.Value <= Fixed8.Zero));
//        unspentCoins.DeleteWhere((k, v) =>v.Items.All(p = > p.HasFlag(CoinState.Spent)));
//        spentCoins.DeleteWhere((k, v) =>v.Items.Count == 0);
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
    }


    /**
     * 获取一笔交易尚未Claim的outputs的字典。 此字典的key为该交易的output的的序号，而value是这个output的花费状态。
     *
     * @param hash 待查询交易hash
     * @return 返回尚未Claim的outputs字典，如果交易不存在，则返回null
     */
    public HashMap<Ushort, SpentCoin> getUnclaimed(UInt256 hash) {
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
        if (tx_state == null) return null;
        SpentCoinState coin_state = getSpentCoins().tryGet(hash);
        if (coin_state == null) {
            return new HashMap<>();
        }
        HashMap<Ushort, SpentCoin> resultMap = new HashMap<>(coin_state.items.size());
        for (Map.Entry<Ushort, Uint> entry : coin_state.items.entrySet()) {
            SpentCoin spentCoin = new SpentCoin();
            spentCoin.output = tx_state.transaction.outputs[entry.getKey().intValue()];
            spentCoin.startHeight = tx_state.blockIndex;
            spentCoin.endHeight = entry.getValue();
            resultMap.put(entry.getKey(), spentCoin);
        }
        return resultMap;
    }

    /**
     * 计算可以Claim的GAS奖励
     *
     * @param inputs Claim指向的交易集合
     * @return 可以Claim的GAS数量
     * @throws IllegalArgumentException ignoreClaimed设置为false时，若出现以下一种情况：<br/> 1）claimable为null，或者其数量为零<br/>
     *                                  2）发现有已经claim的input时
     */
    public Fixed8 calculateBonus(Collection<CoinReference> inputs) {
        return calculateBonus(inputs, true);
    }


    /**
     * 计算可以Claim的GAS奖励
     *
     * @param inputs        Claim指向的交易集合
     * @param ignoreClaimed 是否忽略已经Claims的input。当如果发现参数inputs指向的UTXO不存在或者已经被claim过了，<br> 这时如果
     *                      ignoreClaimed 指定为 true，则忽略这个错误继续计算剩下的部分，<br/> 如果 ignoreClaimed 指定为
     *                      false，则抛出异常。
     * @return 可以Claim的GAS数量
     * @throws IllegalArgumentException ignoreClaimed设置为false时，若出现以下一种情况：<br/> 1）claimable为null，或者其数量为零<br/>
     *                                  2）发现有已经claim的input时
     */
    public Fixed8 calculateBonus(Collection<CoinReference> inputs, boolean ignoreClaimed) {
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
                    return;
                }
                throw new IllegalArgumentException();
            }

            for (CoinReference claim : group) {
                if (!claimable.containsKey(claim.prevIndex)) {
                    if (ignoreClaimed) {
                        return;
                    }
                    throw new IllegalArgumentException();
                }
                SpentCoin claimed = claimable.get(claim.prevIndex);
                unclaimed.add(claimed);
            }
        });
        return calculateBonusInternal(unclaimed);
    }

    /**
     * 计算可以Claim到的GAS奖励
     *
     * @param inputs     Claim指向的交易集合
     * @param height_end 花费的高度
     * @return 可以Claim的GAS数量
     * @throws IllegalArgumentException 当指向的交易不存在，或者指向交易输出序号不合法，<br/> 或者指向交易输出不是NEO输出时，抛出异常
     */
    public Fixed8 calculateBonus(Collection<CoinReference> inputs, Uint height_end) {
        /*
            C# code
            foreach(var group in inputs.GroupBy(p = > p.PrevHash))
            {
                TransactionState tx_state = Transactions.TryGet(group.Key);
                if (tx_state == null) throw new ArgumentException();
                if (tx_state.BlockIndex == height_end) continue;
                foreach(CoinReference claim in group)
                {
                    if (claim.PrevIndex >= tx_state.Transaction.Outputs.Length || !tx_state.Transaction.Outputs[claim.PrevIndex].AssetId.Equals(Blockchain.GoverningToken.Hash))
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
        return calculateBonusInternal(unclaimed);
    }

    private Fixed8 calculateBonusInternal(Collection<SpentCoin> unclaimed) {
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
                amount += (uint)(this.GetSysFeeAmount(group.Key.EndHeight - 1) - (group.Key.StartHeight == 0 ? 0 : this.GetSysFeeAmount(group.Key.StartHeight - 1)));
                amount_claimed += group.Sum(p => p.Value) / 100000000 * amount;
            }
            return amount_claimed;
         */
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
            Fixed8 sub_amount_claimed = Fixed8.divide(Fixed8.multiply(coin.value(), amount), 100000000);
            amount_claimed = Fixed8.add(amount_claimed, sub_amount_claimed);
        }
        return amount_claimed;
    }

    private ECPoint[] validatorPubkeys = null;


    /**
     * 获取当前参与共识的验证人
     *
     * @return 参与共识的验证人列表
     */
    public ECPoint[] getValidatorPubkeys() {
        if (validatorPubkeys == null) {
            Collection<ECPoint> points = getValidators(Collections.emptyList());
            validatorPubkeys = new ECPoint[points.size()];
            points.toArray(validatorPubkeys);
        }
        return validatorPubkeys;
    }

    /**
     * 获取参与共识的验证人列表。
     *
     * @param others 打包的交易。验证人点票时，包含这些交易中的影响。
     * @return 参与共识的验证人列表
     */
    public Collection<ECPoint> getValidators(Collection<Transaction> others) {
        Snapshot snapshot = clone();

        // 计算因交易产生的票数变化
        for (Transaction tx : others) {
            for (TransactionOutput output : tx.outputs) {
                AccountState account = snapshot.getAccounts().getAndChange(output.scriptHash, () -> new AccountState(output.scriptHash));
                account.increaseBalance(output.assetId, output.value);

                if (output.assetId.equals(Blockchain.GoverningToken.hash()) && account.votes.length > 0) {
                    for (ECPoint pubkey : account.votes) {
                        ValidatorState validator = snapshot.getValidators().getAndChange(pubkey, () -> new ValidatorState(pubkey));
                        validator.votes = Fixed8.add(validator.votes, output.value);
                    }
                    ValidatorsCountState countState = snapshot.getValidatorsCount().getAndChange();
                    countState.votes[account.votes.length - 1] = Fixed8.subtract(countState.votes[account.votes.length - 1], output.value);
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
                            countState.votes[account.votes.length - 1] = Fixed8.subtract(countState.votes[account.votes.length - 1], output_prev.value);
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
                        .thenComparing(ValidatorState::getPublicKey)
                        .reversed())
                .map(p -> p.publicKey).limit(count).collect(Collectors.toList());

        if (results.size() == count) {
            return results;
        } else {
            HashSet<ECPoint> set = new HashSet<>(results);
            for (int i = 0; i < Blockchain.StandbyValidators.length && set.size() < count; i++) {
                set.add(Blockchain.StandbyValidators[i]);
            }
            results.clear();
            results.addAll(set);
        }
        Collections.sort(results);
        return results;
    }

}
