package com.studentassoc.financialtracker.View;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;
import com.google.gson.Gson;
import com.studentassoc.financialtracker.Backup.BackupScheduler;
import com.studentassoc.financialtracker.DTO.TransactionDao;
import com.studentassoc.financialtracker.Model.BackupStatus;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Repository.AppDatabase;
import com.studentassoc.financialtracker.Repository.TransactionRepository;
import com.studentassoc.financialtracker.services.BackupManager;
import com.studentassoc.financialtracker.services.GoogleDriveService;
import com.studentassoc.financialtracker.services.RestoreService;

import java.util.Collections;
import java.util.List;

public class BackupFragment extends Fragment {

    private static final String TAG = "BackupFragment";
    private static final String PREFS_NAME = "DrivePrefs";
    private static final String KEY_AUTO_BACKUP = "auto_backup_enabled";
    private static final String NEEDS_RESOLUTION_PREFIX = "NEEDS_RESOLUTION:";
    private String pendingAuthEmail = null;
    private TextView tvStatus;
    private TextView tvLastBackup;
    private Button btnSignIn;
    private Button btnBackupNow;
    private Button btnRestore;
    private SwitchCompat switchAutoBackup;
    private ProgressBar progressBar;

    private BackupManager backupManager;
    private RestoreService restoreService;
    private TransactionRepository transactionRepository;
    private SharedPreferences prefs;

    private ActivityResultLauncher<IntentSenderRequest> authorizationLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authorizationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                this::handleAuthorizationResult
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_backup, container, false);

        tvStatus = view.findViewById(R.id.statusText);
        tvLastBackup = view.findViewById(R.id.lastBackupText);
        btnSignIn = view.findViewById(R.id.signInButton);
        btnBackupNow = view.findViewById(R.id.backupNowButton);
        btnRestore = view.findViewById(R.id.restoreButton);
        switchAutoBackup = view.findViewById(R.id.autoBackupSwitch);
        progressBar = view.findViewById(R.id.progressBar);

        btnSignIn.setOnClickListener(v -> signInToGoogle());
        btnBackupNow.setOnClickListener(v -> performBackup());
        btnRestore.setOnClickListener(v -> performRestore());

        transactionRepository = new TransactionRepository(requireActivity().getApplication());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() == null) return;

        backupManager = new BackupManager(requireContext());
        if (backupManager.getDriveService().isSignedIn()) {
            boolean restored = backupManager.getDriveService().isReady();
            if (!restored) {
                Log.w(TAG, "Saved token expired — user will need to sign in again");
            }
        }

        restoreService = new RestoreService(requireContext(), backupManager.getDriveService());
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load auto-backup preference
        boolean autoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP, false);
        switchAutoBackup.setChecked(autoBackupEnabled);

        switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableAutoBackup();
            } else {
                disableAutoBackup();
            }
        });

        updateUI();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (backupManager != null) {
            backupManager.shutdown();
        }
    }

    private void signInToGoogle() {
        tvStatus.setText("Signing in...");
        setButtonsEnabled(false);

        GoogleDriveService driveService = backupManager.getDriveService();

        driveService.signIn((Activity) requireActivity(), new GoogleDriveService.SignInCallback() {
            @Override
            public void onSuccess(String email) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Signed in as " + email, Toast.LENGTH_SHORT).show();
                    updateUI();
                    setButtonsEnabled(true);
                });
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFailure(String error) {
                if (error != null && error.startsWith(NEEDS_RESOLUTION_PREFIX)) {
                    launchCachedAuthorizationResolution();
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Sign-in failed: " + error);
                        Toast.makeText(getContext(), "Sign-in failed: " + error, Toast.LENGTH_LONG).show();
                        tvStatus.setText("Sign-in failed");
                        setButtonsEnabled(true);
                    });
                }
            }
        });
    }

    private void launchCachedAuthorizationResolution() {
        AuthorizationResult cached = backupManager.getDriveService().getPendingAuthorizationResult();

        if (cached == null || !cached.hasResolution() || cached.getPendingIntent() == null) {
            Log.e(TAG, "No cached authorization result available — cannot launch consent screen");
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(),
                        "Could not open authorization screen. Please try signing in again.",
                        Toast.LENGTH_LONG).show();
                setButtonsEnabled(true);
            });
            return;
        }

        try {
            IntentSenderRequest request = new IntentSenderRequest.Builder(
                    cached.getPendingIntent().getIntentSender()).build();
            authorizationLauncher.launch(request);
            Log.d(TAG, "Launched Drive consent screen from cached PendingIntent");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch cached authorization PendingIntent", e);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(),
                        "Could not open authorization screen: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                setButtonsEnabled(true);
            });
        }
    }

    private void handleAuthorizationResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Log.w(TAG, "Authorization consent cancelled or denied (code="
                    + result.getResultCode() + ")");
            requireActivity().runOnUiThread(() -> {
                tvStatus.setText("Authorization cancelled");
                Toast.makeText(getContext(),
                        "Google Drive authorization was not granted",
                        Toast.LENGTH_LONG).show();
                setButtonsEnabled(true);
            });
            return;
        }

        Intent data = result.getData();
        if (data == null) {
            Log.e(TAG, "Authorization result Intent is null");
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(),
                        "Authorization result data is null",
                        Toast.LENGTH_LONG).show();
                setButtonsEnabled(true);
            });
            return;
        }

        try {
            AuthorizationResult authResult =
                    Identity.getAuthorizationClient(requireContext())
                            .getAuthorizationResultFromIntent(data);
            String email = backupManager.getDriveService().getCurrentEmail();

            backupManager.getDriveService().initializeDriveService(
                    authResult,
                    email,
                    new GoogleDriveService.AuthorizationCallback() {
                        @Override
                        public void onAuthorized(String authorizedEmail) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(),
                                        "Signed in as " + authorizedEmail,
                                        Toast.LENGTH_SHORT).show();
                                updateUI();
                                setButtonsEnabled(true);
                            });
                        }

                        @Override
                        public void onFailed(String error) {
                            requireActivity().runOnUiThread(() -> {
                                Log.e(TAG, "finalize authorization failed: " + error);
                                Toast.makeText(getContext(),
                                        "Sign-in failed: " + error, Toast.LENGTH_LONG).show();
                                tvStatus.setText("Sign-in failed");
                                setButtonsEnabled(true);
                            });
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse authorization result", e);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(),
                        "Failed to complete authorization: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                setButtonsEnabled(true);
            });
        }
    }

    private void performBackup() {
        if (!backupManager.getDriveService().isReady()) {
            Toast.makeText(getContext(),
                    "Google Drive is not ready. Please sign in again.",
                    Toast.LENGTH_LONG).show();
            updateUI();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        setButtonsEnabled(false);
        tvStatus.setText("Loading transactions...");

        new Thread(() -> {
            List<Transaction> transactions = getTransactionsFromDatabase();

            requireActivity().runOnUiThread(() -> {
                if (transactions.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "No transactions to backup", Toast.LENGTH_SHORT).show();
                    setButtonsEnabled(true);
                    return;
                }

                backupManager.backup(transactions, new BackupManager.BackupCallback() {
                    @Override
                    public void onSuccess(String message) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            updateUI();
                            setButtonsEnabled(true);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                            tvStatus.setText("Backup failed");
                            setButtonsEnabled(true);
                        });
                    }

                    @Override
                    public void onProgress(String status) {
                        requireActivity().runOnUiThread(() -> tvStatus.setText(status));
                    }
                });
            });
        }).start();
    }

    private void performRestore() {
        if (!backupManager.getDriveService().isSignedIn()) {
            Toast.makeText(getContext(), "Please sign in to Google Drive first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!backupManager.getDriveService().isReady()) {
            Toast.makeText(getContext(),
                    "Google Drive is not ready. Please sign in again.",
                    Toast.LENGTH_LONG).show();
            updateUI();
            return;
        }

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Restore Backup")
                .setMessage("This will replace your current transactions with the latest backup. Are you sure?")
                .setPositiveButton("Restore", (dialog, which) -> executeRestore())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeRestore() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        tvStatus.setText("Starting restore...");
        setButtonsEnabled(false);

        restoreService.restore(new RestoreService.RestoreCallback() {
            @Override
            public void onSuccess(List<Transaction> transactions, String message) {
                requireActivity().runOnUiThread(() -> {
                    // Save restored transactions to local database
                    saveRestoredTransactions(transactions);

                    // Update UI
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Restore complete");
                    tvLastBackup.setText("Restored " + transactions.size() + " transactions");
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    updateUI();
                    setButtonsEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Restore failed");
                    Toast.makeText(getContext(), "Restore error: " + error, Toast.LENGTH_LONG).show();
                    setButtonsEnabled(true);
                });
            }

            @Override
            public void onProgress(String status) {
                requireActivity().runOnUiThread(() -> {
                    tvStatus.setText(status);
                });
            }
        });
    }

    private void saveRestoredTransactions(List<Transaction> transactions) {
        transactionRepository.replaceAllTransactions(transactions);
        Toast.makeText(getContext(),
                "Restored " + transactions.size() + " transactions successfully",
                Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void enableAutoBackup() {
        if (!backupManager.getDriveService().isSignedIn()) {
            Toast.makeText(getContext(), "Please sign in to Google Drive first",
                    Toast.LENGTH_SHORT).show();
            switchAutoBackup.setChecked(false);
            return;
        }


        BackupScheduler.scheduleAutoBackup(requireContext(), 24);
        prefs.edit().putBoolean(KEY_AUTO_BACKUP, true).apply();
        Toast.makeText(getContext(),
                "Auto-backup enabled (daily)",
                Toast.LENGTH_SHORT).show();
    }

    private void disableAutoBackup() {
        BackupScheduler.cancelAutoBackup(requireContext());
        prefs.edit().putBoolean(KEY_AUTO_BACKUP, false).apply();
        Toast.makeText(getContext(), "Auto-backup disabled", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        BackupStatus status = backupManager.getStatus();

        if (status.isSignedIn()) {
            // Signed in - show backup controls
            btnSignIn.setVisibility(View.GONE);
            btnBackupNow.setVisibility(View.VISIBLE);
            btnRestore.setVisibility(View.VISIBLE);
            switchAutoBackup.setEnabled(true);

            // Update status text
            if (status.hasBackup()) {
                tvStatus.setText("Ready to backup/restore");
                tvLastBackup.setText("Last backup: Version " + status.getBackupVersion());
            } else {
                tvStatus.setText("No backups yet");
                tvLastBackup.setText("Tap 'Backup Now' to create first backup");
            }

        } else {
            // Not signed in - show sign-in button
            btnSignIn.setVisibility(View.VISIBLE);
            btnBackupNow.setVisibility(View.GONE);
            btnRestore.setVisibility(View.GONE);
            switchAutoBackup.setEnabled(false);

            tvStatus.setText("Not signed in to Google Drive");
            tvLastBackup.setText("Sign in to enable backup");
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        btnSignIn.setEnabled(enabled);
        btnBackupNow.setEnabled(enabled);
        btnRestore.setEnabled(enabled);
        switchAutoBackup.setEnabled(enabled && backupManager.getDriveService().isSignedIn());
    }

    private List<Transaction> getTransactionsFromDatabase() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        TransactionDao dao = db.transactionDao();

        return dao.getAllTransactionsSync();
    }
}
