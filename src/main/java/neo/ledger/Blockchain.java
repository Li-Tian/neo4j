package neo.ledger;


import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;

import akka.actor.UntypedActor;
import neo.Fixed8;
import neo.ProtocolSettings;
import neo.UInt160;
import neo.network.p2p.payloads.AssetType;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.RegisterTransaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.cryptography.ECC.ECPoint;
import neo.vm.OpCode;

/**
 * 区块链核心Actor
 */
public final class Blockchain extends UntypedActor {

    /**
     * 出块时间
     */
    public static final int SecondsPerBlock = ProtocolSettings.Default.secondsPerBlock;

    /**
     * 区块奖励
     */
    public static final int[] GenerationAmount = {8, 7, 6, 5, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    /**
     * 块奖励调整周期。衰减周期
     */
    public static final int DecrementInterval = 2000000;

    /**
     * 最大验证人个数
     */
    public static final int MaxValidators = 1024;

    /**
     * 区块间隔时间(Duration类型)
     */
    public static final Duration TimePerBlock = Duration.ofSeconds(SecondsPerBlock);

//    public static final ECPoint[] StandbyValidators = ProtocolSettings.Default.standbyValidators.stream().map(p -> PublicKeyIm.);

    public static final RegisterTransaction GoverningToken = new RegisterTransaction() {
        {
            assetType = AssetType.GoverningToken;
            name = "[{\"lang\":\"zh-CN\",\"name\":\"小蚁股\"},{\"lang\":\"en\",\"name\":\"AntShare\"}]";
            amount = Fixed8.fromDecimal(new BigDecimal(100000000));
            precision = 0;
            owner = ECPoint.secp256r1.getCurve().getInfinity();
            admin = UInt160.parseToScriptHash(new byte[]{OpCode.PUSHT.getCode()});
            attributes = new TransactionAttribute[0];
            inputs = new CoinReference[0];
            outputs = new TransactionOutput[0];
            witnesses = new Witness[0];
        }
    };

    public static final RegisterTransaction UtilityToken = new RegisterTransaction() {
        {
            assetType = AssetType.UtilityToken;
            name = "[{\"lang\":\"zh-CN\",\"name\":\"小蚁币\"},{\"lang\":\"en\",\"name\":\"AntCoin\"}]";
            amount = Fixed8.fromDecimal(BigDecimal.valueOf(Arrays.stream(GenerationAmount).mapToLong(p -> p * DecrementInterval).sum()));
            precision = 8;
            owner = ECPoint.secp256r1.getCurve().getInfinity();
            admin = UInt160.parseToScriptHash(new byte[]{OpCode.PUSHT.getCode()});
            attributes = new TransactionAttribute[0];
            inputs = new CoinReference[0];
            outputs = new TransactionOutput[0];
            witnesses = new Witness[0];
        }
    };


    @Override
    public void onReceive(Object message) throws Throwable {

    }
}
