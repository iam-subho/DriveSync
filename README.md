# Drive Sync

A native Android app that synchronizes files between local folders and Google Drive.
Supports **multiple Google accounts** and **multiple independent sync jobs**, each pairing one
local folder (chosen via Storage Access Framework) with one Drive folder.

Built with Kotlin, Jetpack Compose (Material 3), MVVM + Clean Architecture, Hilt, Room,
WorkManager, and the Google Drive API v3.

---

## Features

- **Multiple Google Drive accounts** — sign in with Google, per-account storage quota display,
  add/remove accounts, silent token refresh in the background. Sign-in uses Credential Manager
  and **falls back automatically to the classic Google account chooser** on devices where
  Credential Manager self-cancels (common on some OEM skins).
- **Sync jobs** — created through a 9-step wizard:
  1. Choose Drive account
  2. Choose (or create) a Drive folder
  3. Choose a local folder (SAF; permission persisted for background sync)
  4. Battery optimization exemption (required for reliable background sync)
  5. File-type filters — All / Images / Videos / Documents / Audio / Archives / custom extensions
  6. Size limits per direction — *No limit* or *Custom MB*, with a mode:
     **Skip files above this size** or **Only sync files above this size**
  7. Sync direction — Two-way / Upload only / Download only
  8. Schedule & cleanup — automatic re-run interval (2h/4h/8h/12h/24h presets or custom
     hours, default **every 24 hours**) and an optional **delete local files after upload**
     toggle (files already on Drive are skipped as always; with delete ON, remote-only files
     are never downloaded back, preventing ping-pong)
  9. Review and create (job name + full summary)
- **Sync engine** — snapshot → plan → durable queue → streaming transfers:
  - Conflict resolution: **Newest Wins** (strategy interface ready for more modes)
  - Duplicate detection: name + size + modified time, MD5 hash check when needed
  - Resumable after interruption, reboot, or pause (queue persisted in Room)
  - Retry with exponential backoff (30s → 30min, max 5 attempts per file)
  - Foreground notification with progress, speed, and current file (degrades gracefully when
    Android denies foreground promotion in the background)
  - Network-constrained WorkManager execution; scheduled runs skip paused jobs; a per-job
    lock prevents manual and scheduled runs from overlapping
  - Any failure surfaces as a visible **Error** status with the reason in Job Details —
    never a silently stuck "Syncing"
- **Job details** — Overview tab (live progress, speed, ETA, stat tiles, full configuration
  including the complete local path and next-run schedule) and Logs tab (paginated transfer
  history with success/failed/skipped reasons, clear logs).
- **See Files browser** — each account card has a "See Files" button listing the Drive folders
  synced by the app. Opening a folder shows all its files (recursive) with:
  - name search and a **date filter** on the file's original device date (stored in Drive
    metadata `appProperties.deviceCreatedAt` + `createdTime` at upload)
  - **multi-select checkboxes with bulk delete from Drive** (confirmation required)
  - tap a file to open it in the **Google Drive app** (previews stay out of this app);
    if Drive isn't installed, a dialog explains how to install and sign in with the same account
- **Settings** — manage accounts (add / remove / refresh storage) and edit the extension lists
  for Images, Videos, and Documents (add, remove, restore defaults; persisted in DataStore).

## Architecture

```
com.iamsubho.drivesync
├── di/             Hilt modules (database, repositories, Drive)
├── domain/         Pure models, repository interfaces, CloudStorageProvider abstraction
├── data/
│   ├── local/      Room (accounts, jobs, transfer logs, transfer queue) + DataStore
│   ├── remote/     Credential Manager + classic sign-in fallback, AuthorizationClient,
│   │               Drive v3 client with per-account silent token refresh
│   ├── sync/       SyncEngine, SyncPlanner (pure, unit-tested), FileFilter, backoff, job locks
│   └── repository/ Repository implementations
├── worker/         WorkManager foreground SyncWorker (manual + periodic), notifications,
│                   boot receiver
└── presentation/   Compose screens (Home, Wizard, Details, Settings, See Files browser)
                    + ViewModels
```

- **Domain layer is Android-free where possible** — sync planning, filtering, and backoff are
  pure Kotlin covered by unit tests.
- **`CloudStorageProvider` interface** — Google Drive is the v1 implementation; OneDrive /
  Dropbox / Box can be added without touching the sync engine.
- **`ConflictResolver` seam** — v1 ships Newest Wins; Drive-wins / Local-wins / Ask-user can
  slot in later.

Project documents: design spec in `docs/superpowers/specs/`, implementation plan in
`docs/superpowers/plans/`. Static site (homepage, privacy policy, terms of service — needed
for OAuth verification) in `website/`.

## Google Cloud Console setup (required)

The app talks to the real Drive API, so you need OAuth credentials:

1. **Create a project** at <https://console.cloud.google.com/>.
2. **Enable the Google Drive API**: *APIs & Services → Library → Google Drive API → Enable*.
3. **Configure the OAuth consent screen** (*APIs & Services → OAuth consent screen*):
   - User type: **External**, Publishing status: **Testing**
   - Add your Google account(s) under **Test users**
   - Scopes: add `https://www.googleapis.com/auth/drive`
     (full Drive scope — required because the wizard browses existing Drive folders)
4. **Create credentials** (*APIs & Services → Credentials → Create credentials → OAuth client ID*):
   - **Android client(s)**: package name `com.iamsubho.drivesync` plus a SHA-1 **for every
     signing key you use** — one client for the debug keystore, and another for the release
     keystore if you ship signed builds. Get the fingerprints with:
     ```
     gradlew signingReport
     keytool -list -v -keystore <your-release.jks> -alias <alias>
     ```
   - **Web application client**: no redirect URIs needed. Copy its **client ID** — the app
     needs it for sign-in (do NOT use the Android client's ID here).
5. **Provide the Web client ID to the build** — add this line to `local.properties`
   (or `gradle.properties`):
   ```
   DRIVE_SYNC_WEB_CLIENT_ID=1234567890-abcdefg.apps.googleusercontent.com
   ```

> Sign-in fails with a placeholder or wrong-type client ID, for signing keys whose SHA-1 is
> not registered, and for accounts missing from the consent screen's test-user list. Console
> changes can take a few minutes to propagate.

## Build & run

Requirements: Android Studio (Ladybug or newer), JDK 11+, Android SDK 36.

```
gradlew :app:assembleDebug        # build APK
gradlew :app:testDebugUnitTest    # run unit tests
```

Or open the project in Android Studio and press Run. Minimum Android version: 7.0 (API 24).

First launch flow:
1. **Add Google Drive** → pick an account → grant Drive access.
2. **Create Sync Job** → walk the 9-step wizard.
3. **Start** the job from its card (grant the notification permission when asked on
   Android 13+).

## Permissions

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Drive API calls; network-aware retries |
| `POST_NOTIFICATIONS` (API 33+) | Foreground sync progress + completion notifications |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` | Reliable long-running sync |
| `RECEIVE_BOOT_COMPLETED` | Resume interrupted jobs after reboot |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Wizard step 4 — exempt the app so background sync isn't killed |
| SAF persisted URI grants | Access to the chosen local folder (no broad storage permission) |

## Troubleshooting

The app logs its auth and sync pipelines under two logcat tags:

```
adb logcat -s DriveSyncAuth:* DriveSyncEngine:*
```

| Symptom | Likely cause / fix |
|---|---|
| Sign-in sheet closes instantly | Handled automatically — the app falls back to the classic account chooser. If that also fails, read the snackbar/logcat code below. |
| "Google rejected the app's configuration" or code 10 / 12500 | The SHA-1 of the APK's signing key isn't registered (debug and release keys differ!), or `DRIVE_SYNC_WEB_CLIENT_ID` is the Android client's ID instead of the Web client's. |
| Job shows **Error: Account needs re-authorization** | Drive access was revoked/expired — remove and re-add the account. |
| Job shows an Error status | Open the job → Overview shows the exact reason; the log tab records per-file failures. |
| Sync runs but plans 0 transfers | Check the job's file-type filter — the wizard defaults to Images + Videos; other types are skipped by design. `DriveSyncEngine` logs "N local files scanned / N transfers planned". |
| Scheduled runs don't fire on time | Android batches periodic work; complete wizard step 4 (battery exemption) and avoid force-stopping the app. |

## Known limits

- Conflicts always resolve as Newest Wins.
- Deletions and renames made on one side do not propagate to the other (the Drive provider
  already implements `delete`/`rename` for when deletion sync ships). Files deleted via the
  See Files browser are removed from Drive only.
- Google-native files (Docs/Sheets/Slides) are ignored — they have no binary content to sync.
- "Device created date" is the file's last-modified timestamp at upload (Android's SAF does
  not expose true creation time).

## License

Copyright 2026 **Subhojit Kundu ([iam-subho](https://github.com/iam-subho))**.

Licensed under the [Apache License 2.0](LICENSE). You may use, modify, and redistribute this
project, provided you keep the copyright and [NOTICE](NOTICE) attribution and prominently mark
any files you changed.

## Tests

Pure-logic unit tests live in `app/src/test/` (49 tests):

- `SyncPlannerTest` — direction matrix, Newest Wins conflicts, hash-identical skips,
  modified-time tolerance, size-limit interactions, delete-after-upload safeguards
- `FileFilterTest` — type/extension filtering and both size-limit modes (skip-above / only-above)
- `FileSearchFilterTest` — See Files name + date-range search
- `BackoffPolicyTest`, `SpeedTrackerTest`, `FormattersTest`, `ExtensionDefaultsTest`
