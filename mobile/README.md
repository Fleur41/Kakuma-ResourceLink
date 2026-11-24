# Kakuma ResourceLink — Android

Native Android client: Kotlin, Jetpack Compose (Material 3), MVVM + Clean Architecture, Hilt, Room, Retrofit, Paging 3, WorkManager. It uses the same Django backend and PostgreSQL database as the web app. There is no Firebase, and the Claude API is only reached through the backend.

| | |
|---|---|
| minSdk / target / compile | 26 / 37 / 37.1 |
| Toolchain | Gradle 9.6.1, AGP 9.3.3 (built-in Kotlin), Kotlin 2.4.20, KSP, JDK 21 (Android Studio JBR) |
| Languages | English, Kiswahili, Español, Italiano, Deutsch (per-app language on Android 13+, AppCompat backport below) |

## Run it locally

1. Start the backend (see the root README) on port 8000 with `python manage.py seed_demo` data.
2. Forward the backend port to the device or emulator:

   ```bash
   adb reverse tcp:8000 tcp:8000
   ```

   Debug builds call `http://127.0.0.1:8000/api/v1/`. Android 17 (API 37) blocks apps from private-LAN addresses such as the emulator's `10.0.2.2` unless they hold `ACCESS_LOCAL_NETWORK`. Loopback over `adb reverse` works on emulators and USB devices without that permission. Run `adb reverse` again after every emulator or device restart.
3. Build and install:

   ```bash
   ./gradlew :app:installDebug
   ```

   To point at another server, pass a URL: `./gradlew :app:installDebug -Pkakuma.apiUrl=https://your-host/api/v1/`.

Demo accounts are listed in the root README.

## Architecture

```
app/src/main/java/com/kakuma/resourcelink/
├── core/        common (AppResult/AppError/UiText), network (Retrofit, token refresh), security (Keystore tokens),
│                database (Room), datastore (settings), designsystem (theme + components), ui (UiEvent, toasts), locale
├── feature/     auth · home · opportunities · stories · services · alumni · mentorship · projects · assistant ·
│                notifications · profile — each split into data / domain / presentation
├── sync/        WorkManager: pending offline actions, periodic refresh, notification polling
└── navigation/  string routes + NavHost
```

- **Offline first.** Opportunities and stories are paged from Room through a `RemoteMediator`. Services are cached in full. Stories you open are kept in the Offline library.
- **Offline actions.** Bookmarks and likes update the UI at once. When offline they are queued in `pending_operations` and replayed by WorkManager. If you change your mind while offline, only your last choice is sent.
- **Story drafts.** Drafts autosave to Room. A draft written offline uploads when the connection returns, cover photo included.
- **Auth.** JWTs are encrypted with an Android Keystore key. `TokenAuthenticator` runs one refresh at a time; refresh tokens rotate. When the refresh token is rejected, the app clears local data, shows a short toast and returns to login.
- **Feedback.** Quick confirmations use short localized Toasts. A Snackbar appears only when there is an action (Retry). Field errors show inline, and destructive actions ask for confirmation in a dialog.
- **Notifications.** There is no push service. `NotificationPoller` checks the API while the device is online and posts to per-topic channels.

## Tests

```bash
./gradlew :app:testDebugUnitTest          # JVM + Robolectric: 61 tests
./gradlew :app:connectedDebugAndroidTest  # emulator/device, Hilt: 6 navigation tests
./gradlew :app:lintDebug
```

| Area | Tests |
|---|---|
| Use cases / domain rules | `AuthUseCasesTest`, `ToggleOpportunityBookmarkUseCaseTest`, `CredentialsValidatorTest`, `DomainRulesTest`, `MarkdownParserTest` |
| ViewModels + Flow (Turbine) | `LoginViewModelTest` (state, process recreation, toast/snackbar events, retry), `MainViewModelTest` (session expiry) |
| Network | `SafeApiCallTest` (error envelope mapping), `TokenAuthenticatorTest` (refresh, rotation, expiry; MockWebServer) |
| Room DAO | `DaoTest` (ordering, converters, observable flows, offline-queue cancel-out) |
| Repository | `OpportunityRepositoryImplTest` (Room + Retrofit + MockWebServer: synced, queued offline, reverted) |
| UI | `ScreenStatesTest` (error/empty states, login errors and loading, welcome) |
| Navigation | `AppNavigationTest` (instrumented: auth flow, guest guards for mentorship and the assistant) |
