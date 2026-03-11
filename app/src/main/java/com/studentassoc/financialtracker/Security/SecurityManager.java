package com.studentassoc.financialtracker.Security;

import android.content.Context;
import android.content.SharedPreferences;

public class SecurityManager {

    private static final String PREFS_NAME = "security_settings";
    private static final String KEY_APP_LOCK_ENABLED = "app_lock_enabled";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    private static volatile SecurityManager instance;

    private final SharedPreferences prefs;
    private final BiometricAuthManager biometricManager;
    private final PinManager pinManager;
    private final SessionManager sessionManager;

    private SecurityManager(Context context) {
        Context appCtx   = context.getApplicationContext();
        prefs            = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        biometricManager = new BiometricAuthManager(appCtx);
        pinManager       = new PinManager(appCtx);
        sessionManager   = new SessionManager(appCtx);
    }

    public static SecurityManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SecurityManager.class) {
                if (instance == null) {
                    instance = new SecurityManager(context);
                }
            }
        }
        return instance;
    }

    public boolean isAppLockEnabled() {
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false);
    }

    public void setAppLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply();
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
                && biometricManager.isBiometricAvailable();
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean shouldLock() {
        if (!isAppLockEnabled()) return false;
        return !sessionManager.isSessionActive() || sessionManager.hasSessionTimedOut();
    }

    public void unlock() {
        sessionManager.startSession();
    }
    public void lock() {
        sessionManager.endSession();
    }

    public void updateActivity() {
        sessionManager.updateActivity();
    }

    public enum AuthMethod { NONE, PIN, BIOMETRIC }

    public AuthMethod getAuthMethod() {
        if (isBiometricEnabled()) return AuthMethod.BIOMETRIC;
        if (pinManager.isPinEnabled()) return AuthMethod.PIN;
        return AuthMethod.NONE;
    }

    public BiometricAuthManager getBiometricManager() { return biometricManager; }
    public PinManager            getPinManager()       { return pinManager;       }
    public SessionManager        getSessionManager()   { return sessionManager;   }
}
