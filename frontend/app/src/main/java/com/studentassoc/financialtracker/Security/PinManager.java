package com.studentassoc.financialtracker.Security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.util.Base64;


import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class PinManager {
    private static final String TAG = "PinManager";
    private static final  String PREFS_NAME = "security_prefs";
    private static final String KEYSTORE_ALIAS = "FinancialTrackerPinKey";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int    GCM_TAG_LENGTH_BITS     = 128;
    private static final int    GCM_IV_LENGTH_BYTES     = 12;

    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_PIN_ENABLED = "pin_enabled";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_TIME    = "lockout_time";

    public static final int MAX_FAILED_ATTEMPTS = 5;

    private static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000L;

    public static final int MIN_PIN_LENGTH = 4;

    private final SharedPreferences securePrefs;

    public PinManager(Context context) {
        securePrefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ensureKeystoreKey();
    }

  public boolean setupPin(String pin) {
        if (pin == null || pin.length() < MIN_PIN_LENGTH) return false;
        try {
            byte[] salt = generateSalt();
            byte[] pinHash = hashPin(pin, salt);

            String encHash = encryptToBase64(pinHash);
            String encSalt = encryptToBase64(salt);

            securePrefs.edit()
                    .putString(KEY_PIN_HASH, encHash)
                    .putString(KEY_PIN_SALT, encSalt)
                    .putBoolean(KEY_PIN_ENABLED, true)
                    .putInt(KEY_FAILED_ATTEMPTS, 0)
                    .putLong(KEY_LOCKOUT_TIME, 0)
                    .apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "setupPin failed", e);
            return false;
        }
  }

  public  boolean verifyPin(String pin) {
        if (isLockedOut()) return false;

        try {
            String encHash = securePrefs.getString(KEY_PIN_HASH, null);
            String encSalt = securePrefs.getString(KEY_PIN_SALT, null);
            if (encHash == null || encSalt == null) return false;

            byte[] storedSalt = decryptFromBase64(encSalt);
            byte[] storedHash = decryptFromBase64(encHash);
            byte[] enteredHash = hashPin(pin, storedSalt);

            boolean correct = Arrays.equals(enteredHash, storedHash);
            if (correct) {
                resetFailedAttempts();
            } else {
                recordFailedAttempt();
            }
            return correct;
        } catch (Exception e) {
            Log.e(TAG, "verifyPin failed", e);
            return false;
        }
  }

  public boolean isPinEnabled() {
        return securePrefs.getBoolean(KEY_PIN_ENABLED, false);
  }

  public boolean changePin(String oldPin, String newPin) {
        if (!verifyPin(oldPin)) return false;
        return setupPin(newPin);
  }

  public void removePin() {
        securePrefs.edit()
                .remove(KEY_PIN_HASH)
                .remove(KEY_PIN_SALT)
                .remove(KEY_PIN_ENABLED)
                .remove(KEY_FAILED_ATTEMPTS)
                .remove(KEY_LOCKOUT_TIME)
                .apply();
    }

    public boolean isLockedOut() {
        long lockout = securePrefs.getLong(KEY_LOCKOUT_TIME, 0);
        if (lockout == 0) return false;

        if (System.currentTimeMillis() < lockout) return true;

        securePrefs.edit()
                .putLong(KEY_LOCKOUT_TIME, 0)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .apply();
        return false;
    }

    public long getRemainingLockoutMs() {
        long lockout = securePrefs.getLong(KEY_LOCKOUT_TIME, 0);
        return Math.max(lockout - System.currentTimeMillis(), 0);
    }

    public int getFailedAttempts() {
        return securePrefs.getInt(KEY_FAILED_ATTEMPTS, 0);
    }

    private void ensureKeystoreKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) return;

            KeyGenerator keyGen = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
            keyGen.init(new KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setInvalidatedByBiometricEnrollment(false)
                    .build());
            keyGen.generateKey();
        } catch (Exception e) {
            Log.e(TAG, "ensureKeystoreKey failed", e);
        }
    }

    private SecretKey getKeystoreKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        return (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
    }

    // ─── AES-GCM encrypt / decrypt ───────────────────────────────────────────

    private String encryptToBase64(byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getKeystoreKey());

        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext);

        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    private byte[] decryptFromBase64(String base64) throws Exception {
        byte[] combined = Base64.decode(base64, Base64.NO_WRAP);

        byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH_BYTES, combined.length);

        Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getKeystoreKey(),
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(ciphertext);
    }

    // ─── PIN hashing ─────────────────────────────────────────────────────────

    private byte[] hashPin(String pin, byte[] salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        return digest.digest(pin.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    // ─── Failed attempt tracking ─────────────────────────────────────────────
    private void recordFailedAttempt() {
        int attempts = securePrefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        SharedPreferences.Editor editor =
                securePrefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            editor.putLong(KEY_LOCKOUT_TIME, System.currentTimeMillis() + LOCKOUT_DURATION_MS);
        }
        editor.apply();
    }

    private void resetFailedAttempts() {
        securePrefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_TIME,  0)
                .apply();
    }

}
