package com.studentassoc.financialtracker.services;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GoogleDriveService {

    private static final String TAG = "GoogleDriveService";
    private static final String APP_FOLDER_NAME = "FinancialTracker";
    private static final String BACKUP_FOLDER_NAME = "backups";
    private static final String PREF_NAME = "DrivePrefs";
    private static final String KEY_ACCOUNT_EMAIL = "signed_in_email";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    private static final long TOKEN_REFRESH_MARGIN_MS = 5 * 60 * 1000L;

    private static final String WEB_CLIENT_ID = "506033137606-crlpo0aet0h9r40k529vgq7716pmgg40.apps.googleusercontent.com";

    private final Context context;
    private Drive driveService;
    private AuthorizationClient authorizationClient;
    private final Executor executors = Executors.newSingleThreadExecutor();
    private final SharedPreferences prefs;

    private AuthorizationResult pendingAuthorizationResult;
    private String currentEmail;


    public interface SignInCallback {
        void onSuccess(String email);

        void onFailure(String e);
    }

    public interface AuthorizationCallback {
        void onAuthorized(String email);
        void onFailed(String error);
    }

    public GoogleDriveService(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.authorizationClient = Identity.getAuthorizationClient(context);
    }

    public void signIn(Activity activity, SignInCallback callback) {
        CredentialManager credentialManager = CredentialManager.create(activity);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                activity,
                request,
                null,
                executors,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInResult(result, callback);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "Sign-in failed", e);
                        callback.onFailure(e.getMessage());
                    }
                }
        );
    }

    private void handleSignInResult(GetCredentialResponse result, SignInCallback callback) {
        if (!(result.getCredential() instanceof CustomCredential)) {
            callback.onFailure("Unexpected credential type");
            return;
        }

        CustomCredential credential = (CustomCredential) result.getCredential();

        if (!credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            callback.onFailure("Unexpected credential type");
            return;
        }

        try {
            GoogleIdTokenCredential googleIdToken = GoogleIdTokenCredential.createFrom(credential.getData());
            String email = googleIdToken.getId();

            if (email.isEmpty()) {
                callback.onFailure("Retrieved email is null or empty");
                return;
            }

            prefs.edit().putString(KEY_ACCOUNT_EMAIL, email).apply();

            requestDriveAuthorization(email, callback);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing credentials", e);
            callback.onFailure("Failed to parse credential: " + e.getMessage());
        }
    }

    private void requestDriveAuthorization(String email, SignInCallback callback) {
        List<Scope> scopes = Collections.singletonList(new Scope(DriveScopes.DRIVE_FILE));

        AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(scopes)
                .build();

        authorizationClient.authorize(authRequest)
                .addOnSuccessListener(executors, authResult -> {
                    if (authResult.hasResolution()) {
                        pendingAuthorizationResult = authResult;
                        currentEmail = email;
                        Log.w(TAG, "Drive authorization requires user interaction");
                        callback.onFailure("NEEDS_RESOLUTION:" + authResult.hasResolution());
                    } else {
                        String accessToken = authResult.getAccessToken();
                        if (accessToken == null || accessToken.isEmpty()) {
                            Log.e(TAG, "Auth succeeded but access token is null");
                            callback.onFailure("Access token is null after authorization");
                            return;
                        }
                        Log.d(TAG, "Drive authorization granted without consent screen");
                        pendingAuthorizationResult = null;
                        currentEmail = null;
                        persistAndBuildDriveService(email, accessToken);
                        callback.onSuccess(email);
                    }
                })
                .addOnFailureListener(executors, e -> {
                    Log.e(TAG, "Drive authorization failed", e);
                    callback.onFailure("Drive authorization failed: " + e.getMessage());
                });
    }

    private boolean silentlyRefreshToken() {
        String email = prefs.getString(KEY_ACCOUNT_EMAIL, null);

        if (email == null || email.isEmpty()) {
            Log.w(TAG, "Cannot silently refresh — no saved email");
            return false;
        }

        Log.d(TAG, "Attempting silent token refresh for: " + email);

        AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(DriveScopes.DRIVE_FILE)))
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        authorizationClient.authorize(authRequest)
                .addOnSuccessListener(executors, authResult -> {
                    if (!authResult.hasResolution()) {
                        String accessToken = authResult.getAccessToken();
                        if (accessToken != null && !accessToken.isEmpty()) {
                            persistAndBuildDriveService(email, accessToken);
                            success.set(true);
                            Log.d(TAG, "Silent token refresh succeeded for: " + email);
                        } else {
                            Log.e(TAG, "Silent refresh got null access token");
                        }
                    } else {
                        pendingAuthorizationResult = authResult;
                        currentEmail = email;
                        Log.w(TAG, "Silent refresh requires user consent — will need re-auth");
                    }
                    latch.countDown();
                })
                .addOnFailureListener(executors, e -> {
                    Log.e(TAG, "Silent token refresh failed", e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Silent token refresh interrupted");
        }

        return success.get();
    }

    @Nullable
    public AuthorizationResult getPendingAuthorizationResult() {
        return pendingAuthorizationResult;
    }

    @Nullable
    public String getCurrentEmail() {
        return currentEmail != null ? currentEmail : prefs.getString(KEY_ACCOUNT_EMAIL, null);
    }

    public void initializeDriveService(AuthorizationResult authResult,
                                       String email,
                                       AuthorizationCallback callback) {
        String accessToken = authResult.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "No access token in authorization result");
            callback.onFailed("Access token missing after user consent");
            return;
        }

        Log.d(TAG, "Initializing Drive service for: " + email);
        pendingAuthorizationResult = null;
        currentEmail = null;
        persistAndBuildDriveService(email, accessToken);
        callback.onAuthorized(email);
    }

    private void persistAndBuildDriveService(String email, String accessToken) {
        long expiryMs = System.currentTimeMillis() + (55 * 60 * 1000L);
        prefs.edit()
                .putString(KEY_ACCOUNT_EMAIL, email)
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putLong(KEY_TOKEN_EXPIRY, expiryMs)
                .apply();

        buildDriveServiceFromToken(accessToken);
    }

    private void buildDriveServiceFromToken(String accessToken) {
        AccessToken token = new AccessToken(accessToken, new Date(Long.MAX_VALUE));
        GoogleCredentials credentials = GoogleCredentials.create(token);

        driveService = new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Financial Tracker")
                .build();

        Log.d(TAG, "Drive service built from access token successfully");
    }

    public boolean isSignedIn() {
        String email = prefs.getString(KEY_ACCOUNT_EMAIL, null);
        return email != null && !email.isEmpty();
    }

    public boolean isReady() {
        if (driveService != null && !isTokenNearExpiry()) {
            return true;
        }

        String savedToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        if (savedToken != null && !savedToken.isEmpty() && !isTokenExpired()) {
            buildDriveServiceFromToken(savedToken);
            return true;
        }

        if (isSignedIn()) {
            Log.d(TAG, "Token expired — attempting silent refresh");
            return silentlyRefreshToken();
        }

        return false;
    }

    public boolean tryRestoreSignIn() {
       return isReady();
    }

    private boolean isTokenExpired() {
        long expiryMs = prefs.getLong(KEY_TOKEN_EXPIRY, 0L);
        return System.currentTimeMillis() >= expiryMs;
    }

    private boolean isTokenNearExpiry() {
        long expiryMs = prefs.getLong(KEY_TOKEN_EXPIRY, 0L);
        return System.currentTimeMillis() >= (expiryMs - TOKEN_REFRESH_MARGIN_MS);
    }

    public void signOut() {
        Log.d(TAG, "Signing out");
        prefs.edit()
                .remove(KEY_ACCOUNT_EMAIL)
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_TOKEN_EXPIRY)
                .apply();
        driveService = null;
        currentEmail = null;
    }

    public String getOrCreateAppFolder() throws IOException {
        ensureDriveReady();
        Log.d(TAG, "Getting or creating app folder: " + APP_FOLDER_NAME);

        String query = "name='" + APP_FOLDER_NAME + "' and mimeType='application/vnd.google-apps.folder' and trashed=false";

        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return files.get(0).getId();
        }

        File folderMetadata = new File();
        folderMetadata.setName(APP_FOLDER_NAME);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();
        return folder.getId();
    }

    public String getOrCreateBackupFolder(String parentFolderId) throws IOException {
        ensureDriveReady();
        String query = "name='" + BACKUP_FOLDER_NAME + "' and '" + parentFolderId +
                "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";

        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return files.get(0).getId();
        }

        File folderMetadata = new File();
        folderMetadata.setName(BACKUP_FOLDER_NAME);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setParents(Collections.singletonList(parentFolderId));

        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();
        return folder.getId();
    }

    public String uploadBackup(String fileName, String jsonContent, String folderId) throws IOException {
        ensureDriveReady();

        try {
            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(Collections.singletonList(folderId));

            ByteArrayContent content = new ByteArrayContent(
                    "application/json",
                    jsonContent.getBytes(StandardCharsets.UTF_8)
            );

            return driveService.files().create(fileMetadata, content)
                    .setFields("id")
                    .execute()
                    .getId();

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 401) {
                Log.e(TAG, "Authentication expired. Need to re-sign in.");
            } else if (e.getStatusCode() == 403) {
                Log.e(TAG, "Quota exceeded or insufficient permissions.");
            }
            throw e;
        }
    }

    public String downloadBackup(String fileId) throws IOException {
        ensureDriveReady();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream);
        return outputStream.toString("UTF-8");
    }

    public List<File> listBackups(String folderId) throws IOException {
        ensureDriveReady();

        String query = "'" + folderId + "' in parents and trashed=false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, createdTime, modifiedTime)")
                .setOrderBy("createdTime desc")
                .execute();
        return result.getFiles();
    }

    public void deleteOldBackups(String folderId, int keepCount) throws IOException {
        List<File> backups = listBackups(folderId);
        if (backups == null || backups.size() <= keepCount) return;

        for (int i = keepCount; i < backups.size(); i++) {
            driveService.files().delete(backups.get(i).getId()).execute();
        }
    }

    private void ensureDriveReady() throws IOException {
        if (driveService != null) return;;

        if (!tryRestoreSignIn()) {
            throw new IOException(
                    "Drive service is not initialized. "
                            + "The user may need to sign in again.");
        }
    }
}
