package com.studentassoc.financialtracker.Security;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BiometricAuthManager {

    private static final String TAG = "BiometricAuth";

    private final Context context;
    private BiometricPrompt biometricPrompt;

    public interface BiometricCallBack {
        void onAuthSucceeded();
        void onAuthFailed();
        void onAuthError(String errorMessage);
    }

    public BiometricAuthManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isBiometricAvailable() {
        BiometricManager bm = BiometricManager.from(context);

        int result = bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public String getBiometricStatus() {
        BiometricManager bm = BiometricManager.from(context);
        int result = bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK);

        switch (result) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return "Biometric available";
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return "No biometric hardware on this device";
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return "Biometric hardware is currently unavailable";
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return "No biometric credentials enrolled — please set up fingerprint or face unlock in device settings";
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                return "A security update is required to use biometrics";
            default:
                return "Biometric status unknown";
        }
    }

    /**
     * Show the system biometrics prompts.
     * Falls back to device credential (PIN / Password) if fails**/
    public void authenticate(FragmentActivity activity, BiometricCallBack callback) {
        Executor executor = ContextCompat.getMainExecutor(context);

        biometricPrompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Log.e(TAG, "Auth error " + errorCode + ": " + errString);
                        callback.onAuthError(errString.toString());
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Log.d(TAG, "Authentication succeeded");
                        callback.onAuthSucceeded();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Log.w(TAG, "Authentication failed (biometric not recognised)");
                        callback.onAuthFailed();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Financial Tracker")
                .setSubtitle("Verify your identity to access your financial data")
                .setAllowedAuthenticators(
                                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                BiometricManager.Authenticators.BIOMETRIC_WEAK |
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    public void cancelAuth() {
        if (biometricPrompt != null) {
            biometricPrompt.cancelAuthentication();
        }
    }
}
