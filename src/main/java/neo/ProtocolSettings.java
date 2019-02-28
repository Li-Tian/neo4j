package neo;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import neo.network.p2p.payloads.TransactionType;

/**
 * NEO 网络协议配置
 */
public class ProtocolSettings {

    public final int magic;
    public final byte addressVersion;
    public final List<String> standbyValidators;
    public final List<String> seedList;
    public final HashMap<TransactionType, Fixed8> systemFee;
    public final Fixed8 lowPriorityThreshold;
    public final int secondsPerBlock;

    public static ProtocolSettings Default;

    static {
        Config config = ConfigFactory.load("protocol.json");
        Default = new ProtocolSettings(config);
    }

    private ProtocolSettings(Config config) {
        Config protolConfig = config.getConfig("ProtocolConfiguration");
        magic = protolConfig.getInt("Magic");
        addressVersion = Byte.parseByte(protolConfig.getString("AddressVersion"));
        standbyValidators = protolConfig.getStringList("Magic");
        seedList = protolConfig.getStringList("Magic");

        lowPriorityThreshold = protolConfig.hasPathOrNull("SecondsPerBlock") ?
                Fixed8.fromDecimal(BigDecimal.valueOf(0.001)) :
                Fixed8.fromDecimal(BigDecimal.valueOf(protolConfig.getDouble("LowPriorityThreshold")));
        secondsPerBlock = protolConfig.hasPathOrNull("SecondsPerBlock") ?
                15 :
                protolConfig.getInt("SecondsPerBlock");

        systemFee = new HashMap<>();
        Config feeConfig = config.getConfig("SystemFee");
        for (Map.Entry<String, ConfigValue> entry : feeConfig.entrySet()) {
            String key = entry.getKey();
            TransactionType transactionType = TransactionType.valueOf(key);
            Fixed8 fee = Fixed8.fromDecimal(BigDecimal.valueOf(protolConfig.getDouble(key)));
            systemFee.put(transactionType, fee);
        }
    }


}
