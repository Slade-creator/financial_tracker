package com.studentassoc.financialtracker.Utils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
