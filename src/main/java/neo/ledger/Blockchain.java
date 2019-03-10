package neo.ledger;


import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;

import akka.actor.Props;
import akka.actor.UntypedActor;
import neo.Fixed8;
import neo.NeoSystem;
import neo.ProtocolSettings;
import neo.UInt160;
import neo.UInt256;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.io.SerializeHelper;
import neo.log.tr.TR;
import neo.network.p2p.payloads.AssetType;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.RegisterTransaction;
import neo.network.p2p.payloads.StateDescriptor;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.Snapshot;
import neo.persistence.Store;
import neo.vm.OpCode;

/**
 * The core Actor of blockChain
 */
public class Blockchain extends UntypedActor {

    /**
     * The time for each block produce
     */
    public static final int SecondsPerBlock = ProtocolSettings.Default.secondsPerBlock;

    /**
     * The decrement interval of reward for each block m
     */
    public static final int[] GenerationAmount = {8, 7, 6, 5, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    /**
     * The decrement interval of reward for each block m
     */
    public static final int DecrementInterval = 2000000;

    /**
     * Maximum number of validators
     */
    public static final int MaxValidators = 1024;

    /**
     * The time for each block produce (TimeSpan)
     */
    public static final Duration TimePerBlock = Duration.ofSeconds(SecondsPerBlock);

    /**
     * The list of standby validators
     */
    public static final ECPoint[] StandbyValidators = ProtocolSettings.Default.standbyValidators.stream().map(p -> ECC.parseFromHexString(p)).toArray(ECPoint[]::new);

    /**
     * The definition of NEO token
     */
    public static final RegisterTransaction GoverningToken = new RegisterTransaction() {
        {
            assetType = AssetType.GoverningToken;
            name = "[{\"lang\":\"zh-CN\",\"name\":\"小蚁股\"},{\"lang\":\"en\",\"name\":\"AntShare\"}]";
            amount = Fixed8.fromDecimal(new BigDecimal(100000000));
            precision = 0;
            owner = new neo.cryptography.ecc.ECPoint(ECC.Secp256r1.getCurve().getInfinity());
            admin = UInt160.parseToScriptHash(new byte[]{OpCode.PUSHT.getCode()});
            attributes = new TransactionAttribute[0];
            inputs = new CoinReference[0];
            outputs = new TransactionOutput[0];
            witnesses = new Witness[0];
        }
    };

    /**
     * The definication of GAS token
     */
    public static final RegisterTransaction UtilityToken = new RegisterTransaction() {
        {
            assetType = AssetType.UtilityToken;
            name = "[{\"lang\":\"zh-CN\",\"name\":\"小蚁币\"},{\"lang\":\"en\",\"name\":\"AntCoin\"}]";
            amount = Fixed8.fromDecimal(BigDecimal.valueOf(Arrays.stream(GenerationAmount).mapToLong(p -> p * DecrementInterval).sum()));
            precision = 8;
            owner = new ECPoint(ECC.Secp256r1.getCurve().getInfinity());
            admin = UInt160.parseToScriptHash(new byte[]{OpCode.PUSHT.getCode()});
            attributes = new TransactionAttribute[0];
            inputs = new CoinReference[0];
            outputs = new TransactionOutput[0];
            witnesses = new Witness[0];
        }
    };

    /**
     * Constructor which create a core blockchain
     *
     * @param system NEO actor system
     * @param store  The storage for persistence
     */
    public Blockchain(NeoSystem system, Store store) {
        singleton = this;
    }

    /**
     * Create the blockchain Actor Ref
     *
     * @param system NEO actor system
     * @param store  The storage for persistence
     * @return return a actorRef which is immutable and thread safe
     */
    public static Props props(NeoSystem system, Store store) {
        return Props.create(Blockchain.class, system, store).withMailbox("blockchain-mailbox");
    }


    private static Blockchain singleton;

    public static Blockchain singleton() {
        // TODO 有待改进
        while (singleton == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                TR.error(e);
            }
        }
        return singleton;
    }


    private ArrayList<UInt256> headerIndex = new ArrayList<>();


    /**
     * Query block hash by block index
     *
     * @param index block index
     * @return block hash
     */
    public UInt256 getBlockHash(Uint index) {
        if (headerIndex.size() <= index.intValue()) {
            return null;
        }
        return headerIndex.get(index.intValue());
    }


    public static void processAccountStateDescriptor(StateDescriptor descriptor, Snapshot snapshot) {
        UInt160 hash = new UInt160(descriptor.key);
        AccountState account = snapshot.getAccounts().getAndChange(hash, () -> new AccountState(hash));
        switch (descriptor.field) {
            case "Votes":
                Fixed8 balance = account.getBalance(GoverningToken.hash());
                for (ECPoint pubkey : account.votes) {
                    ValidatorState validator = snapshot.getValidators().getAndChange(pubkey);
                    validator.votes = Fixed8.subtract(validator.votes, balance);
                    if (!validator.registered && validator.votes.equals(Fixed8.ZERO)) {
                        snapshot.getValidators().delete(pubkey);
                    }
                }

                ECPoint[] votes = SerializeHelper.asAsSerializableArray(descriptor.value, ECPoint[]::new, ECPoint::new);
                votes = Arrays.stream(votes).distinct().toArray(ECPoint[]::new);
                //   ECPoint[] votes = descriptor.value.AsSerializableArray < ECPoint > ().Distinct().ToArray();

                if (votes.length != account.votes.length) {
                    ValidatorsCountState count_state = snapshot.getValidatorsCount().getAndChange();
                    if (account.votes.length > 0) {
                        count_state.votes[account.votes.length - 1] = Fixed8.subtract(count_state.votes[account.votes.length - 1], balance);
                        // count_state.votes[account.votes.length - 1] -= balance;
                    }
                    if (votes.length > 0) {
                        count_state.votes[votes.length - 1] = Fixed8.add(count_state.votes[votes.length - 1], balance);
                        // count_state.votes[votes.length - 1] += balance;
                    }
                }
                account.votes = votes;
                for (ECPoint pubkey : account.votes) {
                    ValidatorState state = snapshot.getValidators().getAndChange(pubkey, () -> new ValidatorState(pubkey));
                    state.votes = Fixed8.add(state.votes, balance);
                }
                break;
        }
    }

    public static void processValidatorStateDescriptor(StateDescriptor descriptor, Snapshot snapshot) {
        ECPoint pubkey = ECPoint.fromBytes(descriptor.key, ECC.Secp256r1.getCurve());
        ValidatorState validator = snapshot.getValidators().getAndChange(pubkey, () -> new ValidatorState(pubkey));
        switch (descriptor.field) {
            case "Registered":
                validator.registered = BitConverter.toBoolean(descriptor.value, 0);
                break;
            default:
                break;
        }
    }


    @Override
    public void onReceive(Object message) throws Throwable {

    }
}
