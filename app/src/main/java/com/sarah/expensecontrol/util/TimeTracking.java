package com.sarah.expensecontrol.util;

import android.util.Log;

import java.util.Calendar;

/**
 * Hjälpfunktioner för att hantera tid i applikationen.
 */

public class TimeTracking {

    /**
     * Kontrollerar om input datumer är inom den nuvarande månaden.
     * @param calendar input datum
     * @return true om input var inom den nuvarande månaden
     */

    public static boolean isThisMonth(Calendar calendar) {
        Calendar today = Calendar.getInstance();
        Log.d("STATISTICS", "Is current month? >" + ((today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) &&
                (today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH))));
        return (today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) &&
                (today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH));
    }

    /**
     * Returnerar den nuvarande månaden som int (0 indexerat), 0 för januari.
     * @return int, nuvarande månadens index
     */

    public static int getCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.MONTH);
    }

    /**
     * Hämtar den absoluta början av input datumet.
     * @param calendar input datum
     * @return calendar, den absoluta början av input månaden
     */

    public static Calendar getStartOfMonth(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    /**
     * Hämtar det absoluta slutet av input datumet.
     * @param calendar input datum
     * @return calendar, det absoluta slutet av input månaden
     */

    public static Calendar getEndOfMonth(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);

        return calendar;
    }

    /**
     * Lägg till en månad till input datumet.
     * @param calendar datumet att öka
     */

    public static void addOneMonth(Calendar calendar) {
        calendar.add(Calendar.MONTH, 1);
    }

    /**
     * Ta borten månad från input datumet.
     * @param calendar datumet att minska
     */

    public static void removeOneMonth(Calendar calendar) {
        calendar.add(Calendar.MONTH, -1);
    }
}
