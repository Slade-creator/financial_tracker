package com.studentassoc.financialtracker.Security;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME = "session_prefs";
    private static final String KEY_SESSION_ACTIVE = "session_active";
    private static final String KEY_LAST_ACTIVITY = "last_activity_time";
    private static final String KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout";

    private static final long DEFAULT_TIMEOUT_MS = 5 * 60 * 1000L;

    private final SharedPreferences prefs;
    private long autoLockTimeout;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        autoLockTimeout = prefs.getLong(KEY_AUTO_LOCK_TIMEOUT, DEFAULT_TIMEOUT_MS);
    }

    public void startSession() {
        prefs.edit()
                .putBoolean(KEY_SESSION_ACTIVE, true)
                .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
                .apply();
    }

    public void endSession() {
        prefs.edit()
                .putBoolean(KEY_SESSION_ACTIVE , false)
                .putLong(KEY_LAST_ACTIVITY, 0)
                .apply();
    }

    public void updateActivity() {
        if (isSessionActive()) {
            prefs.edit()
                    .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
                    .apply();
        }
    }

    public boolean isSessionActive() {
        return prefs.getBoolean(KEY_SESSION_ACTIVE , false);
    }

    public boolean hasSessionTimedOut() {
        if(!isSessionActive()) return true;
        if (autoLockTimeout == Long.MAX_VALUE) return false;

        long elapsed = System.currentTimeMillis() - prefs.getLong(KEY_LAST_ACTIVITY, 0);
        return elapsed > autoLockTimeout;
    }

    public void setAutoLockTimeout(long timeoutMs) {
        autoLockTimeout = timeoutMs;
        prefs.edit().putLong(KEY_AUTO_LOCK_TIMEOUT, timeoutMs).apply();
    }

    public long getAutoLockTimeout() {
        return autoLockTimeout;
    }

    public static class TimeoutOption {
        public final String label;
        public final long durationMs;

        public TimeoutOption(String label, long durationMs) {
            this.label = label;
            this.durationMs = durationMs;
        }
    }

    public static TimeoutOption[] getTimeoutOption() {
        return new TimeoutOption[] {
                new TimeoutOption("Immediately",  0L),
                new TimeoutOption("30 seconds",   30_000L),
                new TimeoutOption("1 minute",     60_000L),
                new TimeoutOption("5 minutes",    5  * 60_000L),
                new TimeoutOption("10 minutes",   10 * 60_000L),
                new TimeoutOption("30 minutes",   30 * 60_000L),
                new TimeoutOption("Never",        Long.MAX_VALUE)
        };
    }

}
