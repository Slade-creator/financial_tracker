package com.studentassoc.financialtracker.View;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Security.BiometricAuthManager;
import com.studentassoc.financialtracker.Security.PinManager;
import com.studentassoc.financialtracker.Security.SecurityManager;

public class LockScreenActivity extends AppCompatActivity {

    private SecurityManager securityManager;
    private PinManager pinManager;
    private BiometricAuthManager biometricAuthManager;

    private TextView tvLockMessage;
    private TextInputLayout tilPin;
    private TextInputEditText etPin;
    private Button btnUnlock;
    private Button btnUseBiometric;
    private TextView tvFailedAttempts;
    private TextView tvLockoutTimer;

    private CountDownTimer lockoutCountDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        securityManager = SecurityManager.getInstance(this);
        pinManager = securityManager.getPinManager();
        biometricAuthManager = securityManager.getBiometricManager();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });

        bindViews();
        setupUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lockoutCountDown != null) lockoutCountDown.cancel();
    }

    private void bindViews() {
        tvLockMessage = findViewById(R.id.tvLockMessage);
        tilPin = findViewById(R.id.tilPin);
        etPin = findViewById(R.id.etPin);
        btnUnlock = findViewById(R.id.btnUnlock);
        btnUseBiometric = findViewById(R.id.btnUseBiometric);
        tvFailedAttempts = findViewById(R.id.tvFailedAttempts);
        tvLockoutTimer = findViewById(R.id.tvLockoutTimer);
    }

    private void setupUI() {
        if (pinManager.isLockedOut()) {
            showLockout();
            return;
        }

        switch (securityManager.getAuthMethod()) {
            case BIOMETRIC:
                showBiometricUI();
                showBiometricPrompt();
                break;
            case PIN:
                showPinUI();
                break;
            default:
                unlockApp();
                break;
        }

        updateFailedAttemptsDisplay();
    }

    private void showBiometricUI() {
        tvLockMessage.setText(R.string.lock_message_biometric);
        tilPin.setVisibility(View.GONE);
        btnUnlock.setVisibility(View.GONE);
        btnUseBiometric.setVisibility(View.VISIBLE);

        btnUseBiometric.setOnClickListener(v -> showBiometricPrompt());
    }

    private void showPinUI() {
        tvLockMessage.setText(R.string.lock_message_pin);
        tilPin.setVisibility(View.VISIBLE);
        btnUnlock.setVisibility(View.VISIBLE);
        btnUseBiometric.setVisibility(View.GONE);

        btnUnlock.setOnClickListener(v -> attemptPinUnlock());

        // Also allow submitting with the keyboard's Done/Enter action
        etPin.setOnEditorActionListener((v, actionId, event) -> {
            attemptPinUnlock();
            return true;
        });

        // Clears error when the user starts typing again
        etPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilPin.setError(null);
            }
        });
    }

    private void showLockout() {
        tvLockMessage.setVisibility(View.GONE);
        tilPin.setVisibility(View.GONE);
        btnUnlock.setVisibility(View.GONE);
        btnUseBiometric.setVisibility(View.GONE);
        tvFailedAttempts.setVisibility(View.GONE);
        tvLockoutTimer.setVisibility(View.VISIBLE);

        long remaining = pinManager.getRemainingLockoutMs();

        lockoutCountDown = new CountDownTimer(remaining, 1000) {
            @Override
            public void onFinish() {
                tvLockoutTimer.setVisibility(View.GONE);
                tvLockMessage.setVisibility(View.VISIBLE);
                setupUI();
            }

            @Override
            public void onTick(long millisUntilFinished) {
                long totalSeconds = millisUntilFinished / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                tvLockoutTimer.setText(getString(
                        R.string.lockout_message, minutes, seconds));
            }
        }.start();
    }

    private void showBiometricPrompt() {
        biometricAuthManager.authenticate(this, new BiometricAuthManager.BiometricCallBack() {
            @Override
            public void onAuthSucceeded() {
                unlockApp();
            }

            @Override
            public void onAuthFailed() {
                Toast.makeText(LockScreenActivity.this,
                        R.string.biometric_failed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthError(String errorMessage) {
                if (pinManager.isPinEnabled()) {
                    showPinUI();
                } else {
                    Toast.makeText(LockScreenActivity.this,
                            errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void attemptPinUnlock() {
        Editable text = etPin.getText();
        String pin = text != null ? text.toString().trim() : "";

        if (pin.isEmpty()) {
            tilPin.setError(getString(R.string.error_enter_pin));
            return;
        }

        if (pinManager.verifyPin(pin)) {
            unlockApp();
        } else {
            etPin.setText("");
            if (pinManager.isLockedOut()) {
                showLockout();
            } else {
                int remaining = PinManager.MAX_FAILED_ATTEMPTS - pinManager.getFailedAttempts();
                tilPin.setError(getString(R.string.error_wrong_pin, remaining));
                updateFailedAttemptsDisplay();
            }
        }
    }

    private void unlockApp() {
        securityManager.unlock();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateFailedAttemptsDisplay() {
        int attempts = pinManager.getFailedAttempts();
        if (attempts > 0) {
            tvFailedAttempts.setVisibility(View.VISIBLE);
            tvFailedAttempts.setText(getString(
                    R.string.failed_attempts_label,
                    attempts,
                    PinManager.MAX_FAILED_ATTEMPTS));
        } else {
            tvFailedAttempts.setVisibility(View.GONE);
        }
    }

}
