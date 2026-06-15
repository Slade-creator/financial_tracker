package com.studentassoc.financialtracker.View;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Security.PinManager;
import com.studentassoc.financialtracker.Security.SecurityManager;

public class SetupPinActivity extends AppCompatActivity {

    public static final String EXTRA_CHANGE_MODE = "change_mode";

    private SecurityManager securityManager;
    private PinManager pinManager;

    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextInputLayout tilPin;
    private TextInputEditText etPin;
    private TextInputLayout   tilConfirm;
    private TextInputEditText etConfirm;
    private Button btnNext;
    private Button btnCancel;

    private boolean isChangeMode = false;
    private String  oldPin       = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_pin);

        securityManager = SecurityManager.getInstance(this);
        pinManager      = securityManager.getPinManager();
        isChangeMode    = getIntent().getBooleanExtra(EXTRA_CHANGE_MODE, false);

        bindViews();
        configureForMode();
        setupListeners();
    }

    private void bindViews() {
        tvTitle      = findViewById(R.id.tvSetupPinTitle);
        tvSubtitle   = findViewById(R.id.tvSetupPinSubtitle);
        tilPin       = findViewById(R.id.tilNewPin);
        etPin        = findViewById(R.id.etNewPin);
        tilConfirm   = findViewById(R.id.tilConfirmPin);
        etConfirm    = findViewById(R.id.etConfirmPin);
        btnNext      = findViewById(R.id.btnSetupNext);
        btnCancel    = findViewById(R.id.btnSetupCancel);
    }

    private void configureForMode() {
        if (isChangeMode) {
            tvTitle.setText(R.string.setup_pin_title_change);
            tvSubtitle.setText(R.string.setup_pin_subtitle_change);
        } else {
            tvTitle.setText(R.string.setup_pin_title_new);
            tvSubtitle.setText(R.string.setup_pin_subtitle_new);
        }
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnNext.setOnClickListener(v -> onNextClicked());

        // Clear errors when user types
        etPin.addTextChangedListener(clearError(tilPin));
        etConfirm.addTextChangedListener(clearError(tilConfirm));
    }

    private void onNextClicked() {
        String pin = text(etPin);
        String confirm = text(etConfirm);

        if (!validatePin(pin)) return;
        if (!validateConfirm(pin, confirm)) return;

        boolean success;

        if (isChangeMode) {
            success = pinManager.setupPin(pin);
        } else {
            success = pinManager.setupPin(pin);
        }

        if (success) {
            Toast.makeText(this, R.string.pin_setup_success, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, R.string.pin_setup_error, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validatePin(String pin) {
        if (pin.isEmpty()) {
            tilPin.setError(getString(R.string.error_enter_pin));
            return false;
        }
        if (pin.length() < PinManager.MIN_PIN_LENGTH) {
            tilPin.setError(getString(R.string.error_pin_too_short, PinManager.MIN_PIN_LENGTH));
            return false;
        }
        return true;
    }

    private boolean validateConfirm(String pin, String confirm) {
        if (confirm.isEmpty()) {
            tilConfirm.setError(getString(R.string.error_confirm_pin));
            return false;
        }
        if (!pin.equals(confirm)) {
            tilConfirm.setError(getString(R.string.error_pin_mismatch));
            etConfirm.setText("");
            return false;
        }
        return true;
    }

    private String text(TextInputEditText et) {
        Editable e = et.getText();
        return e != null ? e.toString().trim() : "";
    }

    private TextWatcher clearError(TextInputLayout til) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                til.setError(null);
            }
        };
    }
}
