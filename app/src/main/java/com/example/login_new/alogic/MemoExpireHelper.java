package com.example.login_new.alogic;
import java.util.Calendar;

public final class MemoExpireHelper {
    private MemoExpireHelper(){}

    /** baseMillis에서 days일 뒤의 23:59:59.999 millis를 반환 */
    public static long plusDaysEndOfDay(long baseMillis, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(baseMillis);
        cal.add(Calendar.DATE, days);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }
}
