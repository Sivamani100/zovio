Play Store release checklist and signing instructions

1) App identifier
- ApplicationId: `com.zovio.announcer` (already set in `app/build.gradle.kts`).
- Source packages were refactored to `com.zovio.announcer.*`.

2) Keystore (upload key)
- Recommended: create an upload key and keep it private. Example command (requires Java keytool):

```bash
keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

- The project expects these environment variables during release build (preferred) or a file at `${rootDir}/my-upload-key.jks`:
  - `KEYSTORE_PATH` -> absolute path to `my-upload-key.jks` (optional)
  - `STORE_PASSWORD` -> keystore password
  - `KEY_PASSWORD` -> key alias password

- You can also keep a `.env` file (the Secrets Gradle Plugin is configured) with these keys (do NOT commit `.env`):

```
KEYSTORE_PATH=/full/path/to/my-upload-key.jks
STORE_PASSWORD=your_store_password
KEY_PASSWORD=your_key_password
```

3) Build Play-ready AAB (recommended)
- Assemble a release Android App Bundle (AAB):

```bash
./gradlew clean bundleRelease
```

- The generated bundle will be under `app/build/outputs/bundle/release/app-release.aab`.

4) ProGuard / R8
- Release build already has `isMinifyEnabled = true` and `isShrinkResources = true` in `app/build.gradle.kts` and uses `proguard-rules.pro`.
- Review `proguard-rules.pro` if you rely on reflection or codegen libraries.

5) Permissions and Play Store policy notes
- The app requests `RECEIVE_SMS`, `READ_SMS`, and `BIND_NOTIFICATION_LISTENER_SERVICE` plus `POST_NOTIFICATIONS`.
- Because the app reads SMS and notifications, Play Store will require a privacy policy and justification for sensitive permissions during review. Prepare a hosted privacy policy URL describing: what is read, why, retention, and opt-out.
- `QUERY_ALL_PACKAGES` has been removed to reduce Play Store review risk.

6) Notification behavior (as requested)
- The app is implemented to *listen* to other apps' notifications and speak announcements; it does NOT cancel or hide other apps' notifications.
- The service posts its own silent notification (low importance) for user confirmation; it does not block or suppress other app notifications.

7) Size and optimization
- Review `build.gradle.kts` dependencies and remove unused commented libraries to reduce method count and APK size.
- R8/shrinker is enabled for release; after building, analyze the generated AAB for large assets (PNG, audio). Remove unused drawables in `app/src/main/res` and large assets in `files/` if present.

8) Play Console assets
- Prepare: app icon (512x512), feature graphic (1024x500), screenshots (phone/tablet), a short and full description, and a privacy policy URL.

9) Upload flow
- Create an app entry in Play Console and follow the guided production/beta track upload.
- Upload the `app-release.aab` and fill required information (targetSdk, content rating questionnaire, privacy policy, contact email).

10) Next steps I can take for you
- Generate a keystore (if you want) and add `.env.example` (no secret values).
- Remove unused dependencies and run `./gradlew bundleRelease` to verify output locally.
- Create a minimal `PRIVACY_POLICY.md` draft to host or paste into your website.

If you want, I can now:
- Generate `.env.example` and `PLAYSTORE_UPLOAD.md` with exact commands.
- Remove unused/commented dependencies from `app/build.gradle.kts` and run a local build (if you give permission to run Gradle here).

