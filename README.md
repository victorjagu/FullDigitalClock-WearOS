<h1 align="center">Full Digital Clock</h1>
<p align="center">
  <img src="app/src/main/res/mipmap/ic_launcher.png" width="256" alt="Full Digital Clock">
</p>

# Wear OS Digital Clock with Seconds

A high-performance, battery-optimized digital clock application designed specifically for Wear OS devices. Built entirely using **Kotlin** and **Jetpack Compose for Wear OS**, this standalone watch application provides a precise time display with seconds, featuring advanced lifecycle management and dynamic screen timeout configuration.

## 🚀 Key Features

* **OLED-Optimized Dark Mode**: Features a pure black `#000000` background that turns off physical pixels on OLED screens to drastically reduce power consumption.
* **Dynamic Screen-On Toggle**: Simple tap gestures allow the user to toggle between standard system timeout (White text) and persistent Ambient Screen-On mode (Dim Red text).
* **Zero-Lag Startup**: Uses primitive native variables instead of traditional Compose States for critical Window Flags, completely removing activity initialization delays.
* **Strict Battery Optimization**: Automatically halts UI recomposition and background coroutine timers the millisecond the application goes off-screen (`onPause`).
* **Auto-Scale Typography**: Dynamically calculates the available horizontal viewport (`BoxWithConstraints`) to scale the font to the absolute maximum size without clipping or text wrapping.
* **Aggressive Memory Management**: Clears residual process footprints and frees allocated RAM instantly upon exit.

## 📋 Requirements

* **Minimum SDK**: Android API 26 (Wear OS 2.0 and above)
* **Target SDK**: Android API 33+ (Wear OS 4.0+)
* **Language**: Kotlin 1.9+
* **UI Toolkit**: Jetpack Compose for Wear OS

## 🔌 Sideloading: Install APK via USB Cable

Follow these steps to manually install the compiled APK directly onto your Wear OS smartwatch using a physical USB cable and **Android Debug Bridge (ADB)**.

### Prerequisite: Install ADB on your Computer
You need the ADB tool installed on your system. If you don't have it yet, download the standalone platform-tools from the official developer site:
* 🪟 [Download SDK Platform-Tools for Windows](https://android.com "Android SDK Platform Tools Windows")
* 🍏 [Download SDK Platform-Tools for Mac](https://android.com "Android SDK Platform Tools Mac")
* 🐧 [Download SDK Platform-Tools for Linux](https://android.com "Android SDK Platform Tools Linux")

---

### Step 1: Enable Developer Options on your Watch
1. On your Wear OS watch, swipe down and go to **Settings** (gear icon).
2. Scroll to the bottom and select **System** (or **About watch** depending on your brand).
3. Tap on **About** / **Software info**.
4. Scroll down to **Build number** and tap it **7 times** until you see a notification saying *"You are now a developer!"*.

### Step 2: Enable ADB Debugging
1. Go back to the main **Settings** menu.
2. Scroll down and open the newly unlocked **Developer options**.
3. Toggle **ON** the option **Stay awake while charging** (this prevents the watch from sleeping/locking while connected to the PC, allowing a stable installation).
4. Locate **ADB debugging** and toggle it **ON**.
5. *(If prompted, confirm the action)*.

### Step 3: Connect and Authorize the Connection
1. Place your watch on its charging cradle/cable and connect the USB end to your computer.
2. Open your terminal (Linux/macOS) or Command Prompt/PowerShell (Windows).
3. Navigate to your platform-tools folder (if not added to your system PATH) and verify the connection by running:
   ```bash
   adb devices
   ```
4. **Look at your watch screen!** A prompt will appear asking to **Allow Debugging?**. Check the box *"Always allow from this computer"* and tap the green checkmark.
5. Run `adb devices` again. The status should change from `unauthorized` to `device`.

### Step 4: Install the APK
Run the following command in your terminal, replacing `app-release.apk` with the actual path to your downloaded file:

```bash
adb install app-release.apk
```

> 💡 **Tip:** If the installation fails with a `Targeting R+` or architecture mismatch error, ensure your build matches the specific chipset architecture of your watch (usually `armeabi-v7a` or `arm64-v8a`).

Once the terminal outputs `Success`, the application will be available in your watch's app drawer!

## 👤 Personal Project Policy

This app is a **Personal Project**.

This means that this is something I created in my free time because I needed it myself, and I
decided to share it with the world.

If you like the app I'm happy to hear that! And if you have suggestions or if you find any bugs
please do let me know!

However, please be aware that you're not entitled to:

- Receiving support
- Having any bugs fixed
- Having any features added

If you would like to make the changes yourself, you're very welcome to send me a pull request.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the issues page if you want to contribute.

## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the LICENSE file for details.
