# Volume Assistant — Complete Android Kotlin Floating Volume Control App

A modern, high-performance Android application built with Kotlin, Jetpack Compose, and Material Design 3 that provides a floating system volume control assistant (similar to AssistiveTouch).

## Key Features

* **Persistent Floating Overlay Button**:
  * Draggable circular button (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`) that stays visible over other apps.
  * Drag gesture vs. tap/double-tap/long-press gesture detection.
  * Configurable edge-snap behavior (automatically snaps to left/right screen edge on release).
  * Remembers last screen position using Jetpack DataStore.

* **Compact Volume Control Popup Overlay**:
  * Opens intelligently near the floating button (auto-detects left/right/top/bottom placement to prevent offscreen clipping).
  * Audio Stream Switcher: Instant switching between **Media**, **Ring**, **Notification**, **Alarm**, and **Call** streams.
  * Real-time volume percentage indicator synchronized with physical device volume buttons.
  * Volume decrement (`-`), volume slider, volume increment (`+`), and mute/unmute controls.
  * Auto-close popup timer (3s, 5s, 10s, or disabled) and tap-outside-to-dismiss.

* **Customizable Gestures**:
  * Single Tap, Double Tap, and Long Press actions:
    * Open Volume Popup
    * Volume Up (+1)
    * Volume Down (-1)
    * Mute / Unmute
    * Open Settings
    * Disabled

* **Foreground Service & Boot Restoration**:
  * `FloatingVolumeService`: Persistent foreground service with low battery impact.
  * `BootReceiver`: Optionally restores floating assistant after device restarts (`RECEIVE_BOOT_COMPLETED`).
  * Notification Channel with quick control and stop actions.

* **Modern Dashboard & Settings Screen**:
  * Live status overview (Assistant state, SYSTEM_ALERT_WINDOW permission status, Notification permission status).
  * Interactive live stream test sliders.
  * Device-specific battery optimization guide (for Samsung OneUI, Xiaomi MIUI, Pixel, etc.).
  * Comprehensive Settings screen for sizes, opacity, theme (System, Light, Dark), gestures, and haptic feedback.

---

## Tech Stack & Architecture

* **Language**: Kotlin 2.2+
* **UI**: Jetpack Compose, Material Design 3
* **Architecture**: MVVM with Coroutines & StateFlow
* **Data Persistence**: Jetpack DataStore Preferences
* **Audio Control**: Android `AudioManager` with `Settings.System.CONTENT_URI` ContentObserver
* **Overlay Engine**: Android `WindowManager` with ComposeView hosting

---

## Build & Run Instructions

### Requirements
* Android Studio Ladybug or newer
* Gradle 8.x / Android Gradle Plugin 9.x
* Minimum SDK: Android 8.0 (API 26)
* Target SDK: Android 15 / 16 (API 36)

### Running in Android Studio
1. Open the project folder in Android Studio.
2. Let Gradle sync and download dependencies.
3. Select an emulator or physical device running Android 8.0+.
4. Click **Run** (`Shift + F10`).
5. Grant the **"Display over other apps"** permission when prompted.
