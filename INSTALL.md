# Fluval Discovery

For the Pixel 6 Pro or another Android 12+ phone. Keep FluvalSmart and your existing backup in place. Discovery reads Bluetooth advertisements and never connects, pairs, or sends fixture commands. Scan results stay in memory and the app has no Internet permission.

1. Sign in to GitHub on your phone and open the repository's Actions tab.
2. Open the latest successful **Build Android debug APK** run and download **fluval-discovery-debug** under Artifacts.
3. Extract the ZIP in Files and open **app-debug.apk**. If Android prompts you, temporarily allow installation from the app opening the APK, then turn that setting off after installing.
4. Turn on Bluetooth, open **Fluval Discovery**, tap **Start scan**, and allow **Nearby devices**. Keep the app open during the 15-second scan.

Look for the light's advertised name. Unnamed devices can still appear. Android can filter some advertisements when location access is disabled for a scanner, and a fixture that is already connected might not advertise. This version does not identify a fixture from signal strength alone.

The workflow starts on push and also supports **Run workflow**. For a local build, use JDK 17, Gradle 8.9, Android SDK platform 35 and build-tools 34.0.0, then run `gradle :app:assembleDebug :app:lintDebug`. GitHub-hosted runs can generate different debug signing keys. If an update reports a signature mismatch, uninstall **Fluval Discovery** and install the new APK. Leave **FluvalSmart** installed.
