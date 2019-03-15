package neo.common;

import java.util.UUID;

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
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}
