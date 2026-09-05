package com.studentassoc.financialtracker.Utils;

import com.studentassoc.financialtracker.Model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class Utils {

    public static int toNgwee(String amount) {
        try {
            double kwacha = Double.parseDouble(amount);
            return (int) Math.round(kwacha * 100);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String toKwacha(int ngwee) {
        double kwacha = ngwee / 100.0;
        Locale zambia = new Locale("en", "ZM");
        NumberFormat format = NumberFormat.getCurrencyInstance(zambia);

        return format.format(kwacha);
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * UTC timestamp for backup metadata, e.g. 2026-09-05T10:30:00Z.
     */
    public static String nowIsoUtc() {
        return getCurrentTimestamp();
    }

    /**
     * Timestamp for backup/export file names, e.g. 2026-09-05_143012.
     */
    public static String fileTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.ROOT);
        return sdf.format(new Date());
    }

    /**
     * Returns the lexicographically smallest and largest transaction dates
     * as {min, max}, or null if the list is null or empty.
     */
    public static String[] minMaxTransactionDate(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return null;
        }
        String min = transactions.get(0).getTransactionDate();
        String max = min;
        for (Transaction tx : transactions) {
            String date = tx.getTransactionDate();
            if (date.compareTo(min) < 0) {
                min = date;
            }
            if (date.compareTo(max) > 0) {
                max = date;
            }
        }
        return new String[]{min, max};
    }

    public static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    public static String toISO8601(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    public static Date fromISO8601(String iso8601String) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(iso8601String);
        } catch (Exception e) {
            return new Date();
        }
    }

    public static String formatDateForDisplay(String iso8601String) {
        try {
            Date date = fromISO8601(iso8601String);
            Locale zambia = new Locale("en", "ZM");

            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", zambia);

            return displayFormat.format(date);
        } catch (Exception e) {
            return iso8601String;
        }
    }

    public static boolean isValidAmount(int amount) {
        return amount > 0;
    }

    public static boolean isValidTransactionType(String type) {
        return "INCOME".equals(type) || "EXPENSE".equals(type);
    }

    public static boolean isValidPaymentMethod(String method) {
        return "CASH".equals(method) || "MOBILE_MONEY".equals(method);
    }

    public static boolean isDateInFuture(Date date) {
        return date.after(new Date());
    }
}
