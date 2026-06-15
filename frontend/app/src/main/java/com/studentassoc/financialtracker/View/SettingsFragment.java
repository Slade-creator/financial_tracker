package com.studentassoc.financialtracker.View;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Security.PinManager;
import com.studentassoc.financialtracker.Security.SecurityManager;
import com.studentassoc.financialtracker.Security.SessionManager;
import com.studentassoc.financialtracker.ViewModel.TransactionViewModel;

import java.util.Objects;

public class SettingsFragment extends Fragment {

    private static final String DEVELOPER_EMAIL = "eltonchiwala@gmail.com";

    private TransactionViewModel transactionViewModel;
    private SecurityManager securityManager;
    private PinManager pinManager;

    // General prefs
    private TextView tvCurrentCurrency;
    private TextView tvCurrentDateFormat;

    // Security views
    private SwitchMaterial switchAppLock;
    private SwitchMaterial switchBiometric;
    private TextView tvBiometricStatus;
    private View layoutAutoLock;
    private Spinner spinnerAutoLock;
    private View layoutSetupPin;
    private View layoutChangePin;
    private View layoutRemovePin;

    // About
    private TextView tvAppVersion;

    private static final String[] CURRENCY_LABELS = {
            "K (Zambian Kwacha)", "$ (US Dollar)", "€ (Euro)",
            "£ (British Pound)", "R (South African Rand)"
    };

    private static final String[] DATE_FORMAT_LABELS = {
            "YYYY-MM-DD", "DD/MM/YYYY", "MM/DD/YYYY", "DD MMM YYYY"
    };

    private static final String PREFS_GENERAL        = "general_prefs";
    private static final String KEY_CURRENCY_INDEX   = "currency_index";
    private static final String KEY_DATE_FORMAT_INDEX = "date_format_index";

    private final ActivityResultLauncher<Intent> setupPinLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        requireActivity();
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            securityManager.setAppLockEnabled(true);
                        }
                        refreshSecurityUI();
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        transactionViewModel = new ViewModelProvider(requireActivity())
                .get(TransactionViewModel.class);

        securityManager = SecurityManager.getInstance(requireContext());
        pinManager = securityManager.getPinManager();

        bindViews(view);
        buildAutoLockSpinner();
        wireSecurityListeners();
        refreshSecurityUI();
        wireDataManagement(view);
        wireAbout(view);
        wireGeneral();

        // ── Backup & Sync → navigate to BackupFragment ───────────────
        view.findViewById(R.id.settingBackup).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_backup));
    }

    private void bindViews(View root) {
        // General prefs
        tvCurrentCurrency   = root.findViewById(R.id.tvCurrentCurrency);
        tvCurrentDateFormat = root.findViewById(R.id.tvCurrentDateFormat);

        // Security
        switchAppLock   = root.findViewById(R.id.switchAppLock);
        switchBiometric = root.findViewById(R.id.switchBiometric);
        tvBiometricStatus = root.findViewById(R.id.tvBiometricStatus);
        layoutAutoLock  = root.findViewById(R.id.layoutAutoLock);
        spinnerAutoLock = root.findViewById(R.id.spinnerAutoLock);
        layoutSetupPin  = root.findViewById(R.id.layoutSetupPin);
        layoutChangePin = root.findViewById(R.id.layoutChangePin);
        layoutRemovePin = root.findViewById(R.id.layoutRemovePin);

        // About
        tvAppVersion = root.findViewById(R.id.tvAppVersion);
    }

    private void wireGeneral() {
        int savedCurrency   = loadPref(KEY_CURRENCY_INDEX, 0);
        int savedDateFormat = loadPref(KEY_DATE_FORMAT_INDEX, 0);
        tvCurrentCurrency.setText(CURRENCY_LABELS[savedCurrency]);
        tvCurrentDateFormat.setText(DATE_FORMAT_LABELS[savedDateFormat]);

        requireView().findViewById(R.id.settingCurrency).setOnClickListener(v ->
                showPickerDialog("Currency Symbol", CURRENCY_LABELS,
                        loadPref(KEY_CURRENCY_INDEX, 0), index -> {
                            savePref(KEY_CURRENCY_INDEX, index);
                            tvCurrentCurrency.setText(CURRENCY_LABELS[index]);
                            Toast.makeText(requireContext(),
                                    "Currency set to " + CURRENCY_LABELS[index],
                                    Toast.LENGTH_SHORT).show();
                        }));

        requireView().findViewById(R.id.settingDateFormat).setOnClickListener(v ->
                showPickerDialog("Date Format", DATE_FORMAT_LABELS,
                        loadPref(KEY_DATE_FORMAT_INDEX, 0), index -> {
                            savePref(KEY_DATE_FORMAT_INDEX, index);
                            tvCurrentDateFormat.setText(DATE_FORMAT_LABELS[index]);
                            Toast.makeText(requireContext(),
                                    "Date format set to " + DATE_FORMAT_LABELS[index],
                                    Toast.LENGTH_SHORT).show();
                        }));
    }

    private void buildAutoLockSpinner() {
        SessionManager.TimeoutOption[] options = SessionManager.getTimeoutOption();
        String[] labels = new String[options.length];
        for (int i = 0; i < options.length; i++) labels[i] = options[i].label;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAutoLock.setAdapter(adapter);
    }

    private void wireSecurityListeners() {
        switchAppLock.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                if (!pinManager.isPinEnabled()) {
                    btn.setChecked(false);
                    launchSetupPin(false);
                } else {
                    securityManager.setAppLockEnabled(true);
                    refreshSecurityUI();
                }
            } else {
                securityManager.setAppLockEnabled(false);
                refreshSecurityUI();
            }
        });

        switchBiometric.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked && !securityManager.getBiometricManager().isBiometricAvailable()) {
                btn.setChecked(false);
                Toast.makeText(requireContext(),
                        securityManager.getBiometricManager().getBiometricStatus(),
                        Toast.LENGTH_LONG).show();
            } else {
                securityManager.setBiometricEnabled(isChecked);
            }
        });

        spinnerAutoLock.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                long duration = SessionManager.getTimeoutOption()[pos].durationMs;
                securityManager.getSessionManager().setAutoLockTimeout(duration);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        layoutSetupPin.setOnClickListener(v  -> launchSetupPin(false));
        layoutChangePin.setOnClickListener(v -> launchSetupPin(true));
        layoutRemovePin.setOnClickListener(v -> confirmRemovePin());
    }

    private void refreshSecurityUI() {
        boolean appLockOn  = securityManager.isAppLockEnabled();
        boolean pinEnabled = pinManager.isPinEnabled();
        boolean bioAvail   = securityManager.getBiometricManager().isBiometricAvailable();

        // Detach listeners before updating state to avoid recursive triggers
        switchAppLock.setOnCheckedChangeListener(null);
        switchBiometric.setOnCheckedChangeListener(null);

        switchAppLock.setChecked(appLockOn);
        switchBiometric.setChecked(securityManager.isBiometricEnabled());

        wireSecurityListeners();

        if (tvBiometricStatus != null) {
            tvBiometricStatus.setText(
                    securityManager.getBiometricManager().getBiometricStatus());
        }

        layoutSetupPin.setVisibility(pinEnabled ? View.GONE    : View.VISIBLE);
        layoutChangePin.setVisibility(pinEnabled ? View.VISIBLE : View.GONE);
        layoutRemovePin.setVisibility(pinEnabled ? View.VISIBLE : View.GONE);

        switchBiometric.setEnabled(appLockOn && pinEnabled && bioAvail);
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
                .setTitle("Remove PIN")
                .setMessage("Removing your PIN will also disable app lock and biometric unlock. Continue?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Remove", (dialog, which) -> {
                    pinManager.removePin();
                    securityManager.setAppLockEnabled(false);
                    securityManager.setBiometricEnabled(false);
                    refreshSecurityUI();
                    Toast.makeText(requireContext(), "PIN removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void wireDataManagement(View root) {
        root.findViewById(R.id.settingClearCache).setOnClickListener(v -> clearCache());

        root.findViewById(R.id.settingExportAllData).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Export coming soon", Toast.LENGTH_SHORT).show());

        // Single place for delete — removed the duplicate wireDeleteAllData call
        root.findViewById(R.id.settingDeleteAllData).setOnClickListener(v ->
                showDeleteConfirmationDialog());
    }

    private void clearCache() {
        try {
            deleteDir(requireContext().getCacheDir());
            Toast.makeText(requireContext(), "Cache cleared", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Nothing to clear", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            for (java.io.File child : Objects.requireNonNull(dir.listFiles())) deleteDir(child);
        }
        if (dir != null) dir.delete();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete All Data")
                .setMessage("This will permanently delete every transaction record. "
                        + "This action cannot be undone.\n\nAre you sure you want to continue?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete Everything", (dialog, which) ->
                        showFinalConfirmationDialog())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFinalConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Are you absolutely sure?")
                .setMessage("All transactions will be permanently removed. "
                        + "There is no way to recover this data.")
                .setPositiveButton("Yes, delete all", (dialog, which) ->
                        deleteAllTransactions())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllTransactions() {
        transactionViewModel.deleteAllTransactions();
        Toast.makeText(requireContext(),
                "All transactions deleted", Toast.LENGTH_SHORT).show();
    }

    private void wireAbout(View root) {
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            tvAppVersion.setText(versionName);
        } catch (PackageManager.NameNotFoundException ignored) {
            tvAppVersion.setText("—");
        }

        root.findViewById(R.id.settingContactDeveloper).setOnClickListener(v ->
                showContactDialog());

        root.findViewById(R.id.settingPrivacyPolicy).setOnClickListener(v ->
                showInfoDialog("Privacy Policy", PRIVACY_POLICY_TEXT));

        root.findViewById(R.id.settingTermsOfService).setOnClickListener(v ->
                showInfoDialog("Terms of Service", TERMS_OF_SERVICE_TEXT));
    }

    private void showContactDialog() {
        String[] reasons = {
                "Report a bug",
                "Request backup access",
                "Request a new feature",
                "Other enquiry"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Contact Developer")
                .setItems(reasons, (dialog, which) ->
                        sendEmail(reasons[which] + " — Financial Tracker"))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendEmail(String subject) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL,   new String[]{DEVELOPER_EMAIL});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            startActivity(Intent.createChooser(intent, "Send email via…"));
        }
    }

    private void showInfoDialog(String title, String content) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("Close", null)
                .show();
    }

    private interface OnPickedListener { void onPicked(int index); }

    private void showPickerDialog(String title, String[] items,
                                  int current, OnPickedListener listener) {
        final int[] selected = {current};
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setSingleChoiceItems(items, current, (d, which) -> selected[0] = which)
                .setPositiveButton("OK", (d, w) -> listener.onPicked(selected[0]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void savePref(String key, int value) {
        requireContext()
                .getSharedPreferences(PREFS_GENERAL, 0)
                .edit().putInt(key, value).apply();
    }

    private int loadPref(String key, int defaultValue) {
        return requireContext()
                .getSharedPreferences(PREFS_GENERAL, 0)
                .getInt(key, defaultValue);
    }

    private static final String PRIVACY_POLICY_TEXT =
            "Your financial data is stored locally on this device only. We do not collect, " +
                    "transmit, or share any personal or financial information with third parties.\n\n" +
                    "Backups (if enabled) are stored in your personal Google Drive account and are " +
                    "accessible only by you.\n\n" +
                    "For any privacy concerns, use the Contact Developer option.";

    private static final String TERMS_OF_SERVICE_TEXT =
            "This app is provided for personal financial tracking purposes. " +
                    "By using this app you agree that:\n\n" +
                    "• All data is your responsibility to back up.\n" +
                    "• The developer is not liable for any data loss.\n" +
                    "• The app is provided as-is without warranty.\n\n" +
                    "For questions or issues, use the Contact Developer option.";
}