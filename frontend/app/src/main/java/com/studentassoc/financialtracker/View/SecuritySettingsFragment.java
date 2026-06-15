package com.studentassoc.financialtracker.View;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Security.PinManager;
import com.studentassoc.financialtracker.Security.SecurityManager;
import com.studentassoc.financialtracker.Security.SessionManager;

public class SecuritySettingsFragment extends Fragment {

    private SecurityManager securityManager;
    private PinManager pinManager;

    private SwitchMaterial switchAppLock;
    private SwitchMaterial switchBiometric;
    private View layoutSetupPin;
    private View layoutChangePin;
    private View layoutRemovePin;
    private View layoutAutoLock;
    private Spinner spinnerAutoLock;
    private TextView tvBiometricStatus;

    private final ActivityResultLauncher<Intent> setupPinLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == getActivity().RESULT_OK) {
                            // PIN was created — now we can enable app lock
                            securityManager.setAppLockEnabled(true);
                        }
                        refreshUI();
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_security_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        securityManager = SecurityManager.getInstance(requireContext());
        pinManager = securityManager.getPinManager();

        bindViews(view);
        buildSpinner();
        setupListeners();
        refreshUI();
    }

    private void bindViews(View root) {
        switchAppLock = root.findViewById(R.id.switchAppLock);
        switchBiometric = root.findViewById(R.id.switchBiometric);
        layoutSetupPin = root.findViewById(R.id.layoutSetupPin);
        layoutChangePin = root.findViewById(R.id.layoutChangePin);
        layoutRemovePin = root.findViewById(R.id.layoutRemovePin);
        layoutAutoLock = root.findViewById(R.id.layoutAutoLock);
        spinnerAutoLock = root.findViewById(R.id.spinnerAutoLock);
        tvBiometricStatus = root.findViewById(R.id.tvBiometricStatus);
    }

    private void buildSpinner() {
        SessionManager.TimeoutOption[] options = SessionManager.getTimeoutOption();
        String[] labels = new String[options.length];
        for (int i = 0; i < options.length; i++) labels[i] = options[i].label;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAutoLock.setAdapter(adapter);

    }

    private void setupListeners() {
        switchAppLock.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                if (!pinManager.isPinEnabled()) {
                    btn.setChecked(false);
                    launchSetupPin(false);
                } else {
                    securityManager.setAppLockEnabled(true);
                    refreshUI();
                }
            } else {
                securityManager.setAppLockEnabled(false);
                refreshUI();
            }
        });

        switchBiometric.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked && !securityManager.getBiometricManager().isBiometricAvailable()) {
                btn.setChecked(false);
                String status = securityManager.getBiometricManager().getBiometricStatus();
                Toast.makeText(requireContext(), status, Toast.LENGTH_LONG).show();
            } else {
                securityManager.setBiometricEnabled(isChecked);
            }
        });

        layoutSetupPin.setOnClickListener(v -> launchSetupPin(false));
        layoutChangePin.setOnClickListener(v -> launchSetupPin(true));
        layoutRemovePin.setOnClickListener(v -> confirmRemovePin());

        spinnerAutoLock.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                long duration = SessionManager.getTimeoutOption()[pos].durationMs;
                securityManager.getSessionManager().setAutoLockTimeout(duration);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshUI() {
        boolean appLockOn  = securityManager.isAppLockEnabled();
        boolean pinEnabled = pinManager.isPinEnabled();
        boolean bioAvail   = securityManager.getBiometricManager().isBiometricAvailable();

        switchAppLock.setOnCheckedChangeListener(null);
        switchBiometric.setOnCheckedChangeListener(null);

        switchAppLock.setChecked(appLockOn);
        switchBiometric.setChecked(securityManager.isBiometricEnabled());

        setupListeners();

        layoutSetupPin.setVisibility(pinEnabled ? View.GONE : View.VISIBLE);
        layoutChangePin.setVisibility(pinEnabled ? View.VISIBLE : View.GONE);
        layoutRemovePin.setVisibility(pinEnabled ? View.VISIBLE : View.GONE);

        switchBiometric.setEnabled(appLockOn && pinEnabled && bioAvail);
        if (tvBiometricStatus != null) {
            tvBiometricStatus.setText(securityManager.getBiometricManager().getBiometricStatus());
        }

        layoutAutoLock.setEnabled(appLockOn);
        spinnerAutoLock.setEnabled(appLockOn);

        long current = securityManager.getSessionManager().getAutoLockTimeout();
        SessionManager.TimeoutOption[] opts = SessionManager.getTimeoutOption();
        for (int i = 0; i < opts.length; i++) {
            if (opts[i].durationMs == current) {
                spinnerAutoLock.setSelection(i, false);
                break;
            }
        }
    }

    private void launchSetupPin(boolean changeMode) {
        Intent intent = new Intent(requireContext(), SetupPinActivity.class);
        intent.putExtra(SetupPinActivity.EXTRA_CHANGE_MODE, changeMode);
        setupPinLauncher.launch(intent);
    }

    private void confirmRemovePin() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.remove_pin_title)
                .setMessage(R.string.remove_pin_message)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    pinManager.removePin();
                    securityManager.setAppLockEnabled(false);
                    securityManager.setBiometricEnabled(false);
                    refreshUI();
                    Toast.makeText(requireContext(), R.string.pin_removed, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

}