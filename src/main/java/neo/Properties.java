package neo;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * NEO 配置信息
 */
public class Properties {

    public final String version;

    public static Properties Default;

    static {
        Config config = ConfigFactory.load("neo4j.properties");
        Default = new Properties(config);
    }

    private Properties(Config config) {
        version = config.getString("neo.version");
    }

}
