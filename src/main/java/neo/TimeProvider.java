package neo;

import java.util.Calendar;
import java.util.Date;

public class TimeProvider {
    private static final TimeProvider Default = new TimeProvider();
    private static TimeProvider current = Default;

    public static TimeProvider current() {
        return current;
    }

    public static void setCurrent(TimeProvider provider) {
        current = provider;
    }

    public Date utcNow() {
        Calendar cal = Calendar.getInstance();
        //获得时区和 GMT-0 的时间差,偏移量
        int offset = cal.get(Calendar.ZONE_OFFSET);
        //获得夏令时  时差
        int dstoff = cal.get(Calendar.DST_OFFSET);
        cal.add(Calendar.MILLISECOND, -(offset + dstoff));
        return cal.getTime();
    }

    public static void resetToDefault() {
        current = Default;
    }
}
