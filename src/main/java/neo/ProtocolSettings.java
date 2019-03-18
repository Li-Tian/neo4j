package neo;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import neo.csharp.Uint;
import neo.log.tr.TR;
import neo.network.p2p.payloads.TransactionType;

/**
 * NEO network protocol configuration
 */
public class ProtocolSettings {

    /**
     * magic number,
     */
    public final Uint magic;

    /**
     * address version
     */
    public final byte addressVersion;

    /**
     * standby validators
     */
    public final List<String> standbyValidators;

    /**
     * seed node list
     */
    public final List<String> seedList;

    /**
     * transaction's system fee
     */
    public final HashMap<TransactionType, Fixed8> systemFee;

    /**
     * the low priority threshold of transaction
     */
    public final Fixed8 lowPriorityThreshold;

    /**
     * block internal
     */
    public final int secondsPerBlock;

    /**
     * default protocol
     */
    public static ProtocolSettings Default;

    static {
        Config config = ConfigFactory.load("protocol.json");
        Default = new ProtocolSettings(config);
    }

    private ProtocolSettings(Config config) {
        TR.enter();
        Config protocolConfig = config.getConfig("ProtocolConfiguration");
        magic = new Uint(protocolConfig.getInt("Magic"));
        addressVersion = Byte.parseByte(protocolConfig.getString("AddressVersion"));
        standbyValidators = protocolConfig.getStringList("StandbyValidators");
        seedList = protocolConfig.getStringList("SeedList");

        lowPriorityThreshold = protocolConfig.hasPathOrNull("SecondsPerBlock") ?
                Fixed8.fromDecimal(BigDecimal.valueOf(0.001)) :
                Fixed8.fromDecimal(BigDecimal.valueOf(protocolConfig.getDouble("LowPriorityThreshold")));
        secondsPerBlock = protocolConfig.hasPathOrNull("SecondsPerBlock") ?
                15 :
                protocolConfig.getInt("SecondsPerBlock");

        systemFee = new HashMap<>();
        Config feeConfig = protocolConfig.getConfig("SystemFee");
        for (Map.Entry<String, ConfigValue> entry : feeConfig.entrySet()) {
            String key = entry.getKey();
            TransactionType transactionType = TransactionType.valueOf(key);
            Fixed8 fee = Fixed8.fromDecimal(BigDecimal.valueOf(feeConfig.getDouble(key)));
            systemFee.put(transactionType, fee);
        }
        TR.exit();
    }


}
