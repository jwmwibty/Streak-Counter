# Daylight — streak counter

A native Android app for building habits, made to open in Android Studio. All habits and check-ins are stored locally on the device; no account or network connection is needed.

## Run it

1. Open this folder in a recent Android Studio (with JDK 17 and Android SDK 35 installed).
2. Let Gradle sync, then run the `app` configuration on an Android 8.0 (API 26) or newer device or emulator.
3. Create a habit and select the weekdays you want to do it. For example, choose Monday–Friday for five days a week. Tap **Check in** once on a scheduled day. Use the ⋯ button to delete a habit.
4. Enable notifications from the reminder prompt in the app (and in Android settings if prompted).

## How streaks work

- A check-in counts once per local calendar day, and only on a selected weekday. You cannot check in on a rest day or check in twice.
- A streak is the number of consecutive **scheduled days** completed. Rest days are skipped. Today stays open until midnight, so an unchecked habit does not lose its streak early. Once a scheduled day is missed, the streak resets to zero; the next check-in starts it again at one.
- Weekday selection is fixed when a habit is created so changing a schedule cannot rewrite its past. To change it, delete and recreate the habit (this clears its history).
- Daylight schedules an inexact reminder around 8 PM local time when an unchecked habit has an existing streak at risk on a scheduled day. Android may delay delivery due to battery restrictions, and notifications must be enabled. Reminders are rescheduled after reboot and time-zone changes. They are not a guarantee of delivery before midnight.

## Build from the command line

With JDK 17 and Android SDK 35 installed, set `ANDROID_HOME` or create `local.properties` with `sdk.dir`, then run `./gradlew assembleDebug` (or `gradlew.bat assembleDebug` on Windows). The debug APK is written to `app/build/outputs/apk/debug/`.

## Download an APK from GitHub

The **Build and release APK** GitHub Actions workflow runs on branch pushes and can also be started manually from the repository's **Actions** tab. After the tests and build pass, it creates a GitHub Release and attaches `Daylight-debug.apk` directly to that release (not as an Actions artifact). Each run gets its own build tag. Find the APK under **Releases** on the repository page and download it to your Android phone. You may need to allow your browser or file manager to install apps from unknown sources.

The workflow needs GitHub Actions enabled and permission to write repository contents. It produces a debug-signed APK, not a Play Store release. Because GitHub's runners can generate a different debug signing key for each build, a later APK may not install over an earlier one. If that happens, uninstalling the old app will also delete its locally stored habits and streaks. For reliable updates, configure a persistent private signing key as GitHub Actions secrets before distributing production builds.
