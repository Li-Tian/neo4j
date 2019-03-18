package neo.common;

import java.util.UUID;

import neo.log.notr.TR;

/**
 * GUID helper, generate a unique id
 */
public class GUID {

    /**
     * create a new guid string
     *
     * @return guid string
     */
    public static String newGuid() {
        TR.enter();
        UUID uuid = UUID.randomUUID();
        return TR.exit(uuid.toString());
    }
}
