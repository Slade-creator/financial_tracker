# Security

How the app protects the treasurer's financial data on-device: app lock (PIN + biometric), session timeout, and what is (and isn't) protected elsewhere in the stack.

All security code lives in `frontend/app/src/main/java/com/studentassoc/financialtracker/Security/`.

## Components

| Class | Responsibility | Persistence |
|-------|----------------|-------------|
| `SecurityManager` | Facade/singleton — decides *whether* to lock and *which* auth method to use | `security_settings` prefs (`app_lock_enabled`, `biometric_enabled`) |
| `PinManager` | PIN setup/verify/change, hashing, keystore encryption, failed-attempt lockout | `security_prefs` + **Android Keystore** |
| `SessionManager` | Session activity tracking and auto-lock timeout | `session_prefs` |
| `BiometricAuthManager` | Wraps `BiometricPrompt`, checks hardware/credential availability | — |

## App Lock Flow

Every activity extends `BaseActivity`, which enforces the lock on every resume and on every user interaction:

```mermaid
flowchart TD
    RESUME["BaseActivity.onResume()"] --> CHK{"shouldLock()?"}
    CHK -- "app lock disabled" --> ACT["updateActivity()<br/>(refresh last-activity time)"]
    CHK -- "no active session OR<br/>session timed out" --> LOCK["LockScreenActivity<br/>(FLAG_NEW_TASK + CLEAR_TASK,<br/>finish() current)"]

    INTERACT["onUserInteraction()"] --> ACT2["securityManager.updateActivity()"]

    ACT --> NORMAL["App usable"]
    ACT2 --> NORMAL
```

`shouldLock()` logic:

```java
if (!isAppLockEnabled()) return false;
return !sessionManager.isSessionActive() || sessionManager.hasSessionTimedOut();
```

## Authentication Method Selection

`SecurityManager.getAuthMethod()` resolves the unlock method:

```mermaid
flowchart TD
    A{"Biometric enabled<br/>AND hardware available?"} -- yes --> BIO["AuthMethod.BIOMETRIC"]
    A -- no --> B{"PIN set?"}
    B -- yes --> PIN["AuthMethod.PIN"]
    B -- no --> NONE["AuthMethod.NONE<br/>(app lock effectively off)"]
```

- Biometric only appears as an option when the user enabled it **and** `BiometricAuthManager.isBiometricAvailable()` passes (enrolled biometric or device credential).
- PIN is the always-available fallback.

## PIN Storage & Verification

PINs are **never stored in plaintext**. `PinManager` combines a salted SHA-256 hash with AES-GCM encryption bound to a hardware-backed Keystore key:

```mermaid
flowchart TD
    subgraph setup["setupPin()"]
        P1["PIN (min 4 digits)"] --> P2["Generate 16-byte<br/>SecureRandom salt"]
        P2 --> P3["hashPin:<br/>SHA-256(salt ‖ PIN)"]
        P3 --> P4["Encrypt hash & salt<br/>AES/GCM/NoPadding<br/>Keystore key 'FinancialTrackerPinKey'"]
        P4 --> P5["Store Base64(IV ‖ ciphertext)<br/>in security_prefs"]
    end

    subgraph verify["verifyPin()"]
        V1["Entered PIN"] --> V2["Decrypt stored hash & salt<br/>(Keystore)"]
        V2 --> V3["SHA-256(stored salt ‖ entered PIN)"]
        V3 --> V4{"constant-time-ish<br/>Arrays.equals?"}
        V4 -- match --> V5["Reset failed attempts"]
        V4 -- mismatch --> V6["recordFailedAttempt()"]
    end
```

Key parameters:

| Parameter | Value |
|-----------|-------|
| Keystore alias | `FinancialTrackerPinKey` (AES-256, GCM, in **AndroidKeyStore**) |
| Cipher | `AES/GCM/NoPadding`, 12-byte IV prepended to ciphertext, 128-bit GCM tag |
| Hash | SHA-256 with 16-byte random salt |
| PIN length | 4–6 digits (`MIN_PIN_LENGTH = 4`) |

### Brute-Force Lockout

| Rule | Value |
|------|-------|
| Max failed attempts | `MAX_FAILED_ATTEMPTS = 5` |
| Lockout duration | 5 minutes (`LOCKOUT_DURATION_MS`) |
| Behavior | After the 5th failure, `LOCKOUT_TIME` is set; `verifyPin()` refuses (returns `false`) until it expires. The countdown is surfaced via `getRemainingLockoutMs()` |
| Reset | Successful verification clears attempts **and** lockout |

The lockout deadline is stored, not computed — so it survives process death.

## Session Timeout

`SessionManager` tracks the session in `session_prefs`:

- `startSession()` on successful unlock (marks active, stamps last activity)
- `updateActivity()` on every user interaction (via `BaseActivity.onUserInteraction()`)
- `hasSessionTimedOut()` compares elapsed time against the configured timeout; `Long.MAX_VALUE` means "Never"

Configurable options (Settings → Security):

| Option | Duration |
|--------|----------|
| Immediately | 0 ms |
| 30 seconds | 30 s |
| 1 minute | 60 s |
| 5 minutes *(default)* | 5 min |
| 10 minutes | 10 min |
| 30 minutes | 30 min |
| Never | `Long.MAX_VALUE` |

## Unlock Sequence (End to End)

```mermaid
sequenceDiagram
    participant U as User
    participant BA as BaseActivity
    participant SM as SecurityManager
    participant SS as SessionManager
    participant LS as LockScreenActivity
    participant PM as PinManager / Biometric

    U->>BA: Return to app (onResume)
    BA->>SM: shouldLock()?
    SM->>SS: session active? timed out?
    SS-->>SM: yes/no
    SM-->>BA: lock needed
    BA->>LS: startActivity (clear task)
    LS->>PM: getAuthMethod()
    alt BIOMETRIC
        PM-->>LS: show BiometricPrompt
        U->>LS: authenticate (fingerprint/face)
    else PIN
        U->>LS: enter PIN
        LS->>PM: verifyPin()
        PM-->>LS: ok? (lockout enforced after 5 fails)
    end
    LS->>SM: unlock() → startSession()
    LS-->>U: back to MainActivity
```

## What Is *Not* Protected (Known Gaps)

Be aware of these when touching related code — they are deliberate trade-offs of an educational/offline-first project, not invitations to rely on them:

1. **Transaction database is not encrypted at rest.** Room stores `financial_tracker_database` unencrypted; the app lock is a UI-level gate, not disk encryption. (SQLCipher/`SupportFactory` would be the upgrade path.)
2. **PIN hash uses a single SHA-256 round.** Salted, but not a memory-hard KDF (PBKDF2/scrypt/Argon2 would be stronger). The Keystore encryption of the hash compensates somewhat by requiring the hardware key to even read it.
3. **Backup files in Drive are plaintext JSON.** Any full transaction history is readable by anything with access to that Drive folder.
4. **Security prefs are `SharedPreferences`, not `EncryptedSharedPreferences`.** The PIN hash/salt *values* are themselves AES-GCM encrypted via Keystore, but the flags (`pin_enabled`, `app_lock_enabled`, session timestamps) are plaintext.
5. **Backend has no auth.** Anyone who knows the URL can POST transactions to `/api/generate-report`. Acceptable because the backend holds no data — see [API Reference](./API.md).

## Testing

`UtilsTest`, `BackupLogicTest` cover non-Android logic. PIN/Keystore/session behavior requires a device or emulator (Keystore is hardware/virtual-hardware backed) — verify manually via Settings → Security when changing `Security/` classes.
