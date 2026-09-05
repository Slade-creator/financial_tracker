package com.studentassoc.financialtracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.studentassoc.financialtracker.Utils.Utils;

import org.junit.Test;

import java.util.Date;

/**
 * Plain JVM unit tests for {@link Utils} — no Robolectric, no Android Context.
 *
 * These tests document the CURRENT behavior of the utility methods, including
 * quirks (e.g. toNgwee(null) throws, fromISO8601 falls back to "now").
 */
public class UtilsTest {

    // ------------------------------------------------------------------
    // toNgwee — parse a kwacha string into whole ngwee (x100, rounded)
    // ------------------------------------------------------------------

    @Test
    public void toNgwee_simpleAmount() {
        assertEquals(1250, Utils.toNgwee("12.50"));
    }

    @Test
    public void toNgwee_wholeAmount() {
        assertEquals(10000, Utils.toNgwee("100"));
    }

    @Test
    public void toNgwee_zero() {
        assertEquals(0, Utils.toNgwee("0"));
        assertEquals(0, Utils.toNgwee("0.00"));
    }

    @Test
    public void toNgwee_negativeAmount() {
        assertEquals(-550, Utils.toNgwee("-5.50"));
    }

    @Test
    public void toNgwee_roundsHalfUp() {
        // Math.round(1234.5) == 1235
        assertEquals(1235, Utils.toNgwee("12.345"));
    }

    @Test
    public void toNgwee_scientificNotationIsParsedByDouble() {
        assertEquals(10000, Utils.toNgwee("1e2"));
    }

    @Test
    public void toNgwee_leadingAndTrailingWhitespaceAreTrimmedByParseDouble() {
        assertEquals(1500, Utils.toNgwee(" 15.00 "));
    }

    @Test
    public void toNgwee_malformedInputReturnsZero() {
        assertEquals(0, Utils.toNgwee("abc"));
        assertEquals(0, Utils.toNgwee("12.34.56"));
        assertEquals(0, Utils.toNgwee(""));
    }

    @Test
    public void toNgwee_nullThrowsNullPointerException() {
        // Current behavior: Double.parseDouble(null) throws NPE, which is NOT
        // caught by the NumberFormatException handler.
        try {
            Utils.toNgwee(null);
            fail("Expected NullPointerException for null input");
        } catch (NullPointerException expected) {
            // documented behavior
        }
    }

    // ------------------------------------------------------------------
    // toKwacha — format whole ngwee as a ZMW currency string
    // ------------------------------------------------------------------

    // Note: the currency *symbol* for en_ZM comes from CLDR locale data and
    // differs between runtimes ("K" on modern desktop JDKs, "ZMW" on some
    // Android ICU versions), so these tests assert the numeric formatting
    // (two decimals, grouping, sign) rather than the exact symbol.

    @Test
    public void toKwacha_formatsTwoDecimals() {
        String formatted = Utils.toKwacha(1250);
        assertTrue("Expected 12.50 in: " + formatted, formatted.contains("12.50"));
    }

    @Test
    public void toKwacha_groupsThousands() {
        String formatted = Utils.toKwacha(123456);
        assertTrue("Expected grouped 1,234.56 in: " + formatted,
                formatted.contains("1,234.56"));
    }

    @Test
    public void toKwacha_zero() {
        assertTrue(Utils.toKwacha(0).contains("0.00"));
    }

    @Test
    public void toKwacha_negative() {
        // -550 ngwee = -5.50 kwacha, formatted with a minus sign
        String formatted = Utils.toKwacha(-550);
        assertTrue("Expected 5.50 in: " + formatted, formatted.contains("5.50"));
        assertTrue("Expected a minus sign in: " + formatted, formatted.contains("-"));
    }

    // ------------------------------------------------------------------
    // fromISO8601 / toISO8601
    // ------------------------------------------------------------------

    @Test
    public void fromISO8601_parsesUtcTimestamp() {
        Date date = Utils.fromISO8601("2024-01-15T10:30:00Z");
        // 2024-01-15T10:30:00Z == 1705314600000 ms
        assertEquals(1705314600000L, date.getTime());
    }

    @Test
    public void fromISO8601_malformedInputFallsBackToNow() {
        long before = System.currentTimeMillis() - 60_000;
        Date date = Utils.fromISO8601("not-a-date");
        long after = System.currentTimeMillis() + 60_000;
        assertNotNull(date);
        assertTrue("Malformed input should yield current time",
                date.getTime() >= before && date.getTime() <= after);
    }

    @Test
    public void fromISO8601_nullFallsBackToNow() {
        long before = System.currentTimeMillis() - 60_000;
        Date date = Utils.fromISO8601(null);
        long after = System.currentTimeMillis() + 60_000;
        assertNotNull(date);
        assertTrue("Null input should yield current time",
                date.getTime() >= before && date.getTime() <= after);
    }

    @Test
    public void toISO8601_fromIso8601_roundTrip() {
        String iso = "2026-03-15T08:45:30Z";
        Date parsed = Utils.fromISO8601(iso);
        assertEquals(iso, Utils.toISO8601(parsed));
    }

    // ------------------------------------------------------------------
    // Small validators (behavior lock-in)
    // ------------------------------------------------------------------

    @Test
    public void isValidAmount_requiresStrictlyPositive() {
        assertTrue(Utils.isValidAmount(1));
        assertTrue(!Utils.isValidAmount(0));
        assertTrue(!Utils.isValidAmount(-100));
    }

    @Test
    public void isValidTransactionType() {
        assertTrue(Utils.isValidTransactionType("INCOME"));
        assertTrue(Utils.isValidTransactionType("EXPENSE"));
        assertTrue(!Utils.isValidTransactionType("income"));
        assertTrue(!Utils.isValidTransactionType(null));
    }

    @Test
    public void isValidPaymentMethod() {
        assertTrue(Utils.isValidPaymentMethod("CASH"));
        assertTrue(Utils.isValidPaymentMethod("MOBILE_MONEY"));
        assertTrue(!Utils.isValidPaymentMethod("CARD"));
    }
}
