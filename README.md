<div align="center">

# 🎛️ Volume Assistant

### *Floating System Volume Control — Like AssistiveTouch for Android*

<br>

![Kotlin](https://img.shields.io/badge/Kotlin-2.2+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%20Design%203-0066FF?style=for-the-badge&logo=materialdesign&logoColor=white)
![API](https://img.shields.io/badge/Min%20API-26-4CAF50?style=for-the-badge&logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-FF5722?style=for-the-badge)

<br>

```
╔══════════════════════════════════════════════════╗
║                                                  ║
║   ██╗   ██╗ ██████╗ ██╗     ██╗   ██╗███╗   ███╗║
║   ██║   ██║██╔═══██╗██║     ██║   ██║████╗ ████║║
║   ██║   ██║██║   ██║██║     ██║   ██║██╔████╔██║║
║   ╚██╗ ██╔╝██║   ██║██║     ██║   ██║██║╚██╔╝██║║
║    ╚════╝  ╚██████╔╝███████╗╚██████╔╝██║ ╚═╝ ██║║
║     ╚═══╝   ╚═════╝ ╚══════╝ ╚═════╝ ╚═╝     ╚═╝║
║                                                  ║
║   ▄▄█▀▀▀▄█▄                                      ║
║   ▐▌▐▌  ▐▌ ▀▄   ▄█▀▀▀▄▄   ▄▀█▄  ▄▀█▄  ▄█▀▀▀▄▄   ║
║   ▐▌▐▌  ▐▌  ▀▄ ▐▌    ▀▀ ▐▌  ▐▌▐▌  ▐▌▐▌    ▀▀   ║
║   ▐▌▐▌  ▐▌▄▄▄▀▀ ▀█▄▄▄▄▀  ▀▄▄▀▀ ▀▄▄▀▀ ▀█▄▄▄▄▀   ║
║                                                  ║
╚══════════════════════════════════════════════════╝
```

<br>

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🟢 Floating Overlay
- Draggable circular button overlay
- Smart edge-snap (left/right)
- Double-tap & long-press gestures
- Remembers position via DataStore

</td>
<td width="50%">

### 🔊 Volume Popup
- Compact popup near the button
- Switch between Media / Ring / Alarm / Call
- Slider + increment / decrement buttons
- Auto-dismiss timer

</td>
</tr>
<tr>
<td width="50%">

### 🎮 Custom Gestures
| Gesture | Action |
|---------|--------|
| Single Tap | Volume Popup / Up / Down |
| Double Tap | Mute / Settings / etc. |
| Long Press | Customizable |

</td>
<td width="50%">

### ⚙️ Dashboard & Settings
- Live service status
- Permission check
- Battery optimization guide
- Theme (System / Light / Dark)
- Size, opacity, haptic feedback

</td>
</tr>
</table>

---

## 🛠️ Tech Stack

```
┌─────────────────────────────────────────────┐
│  Kotlin 2.2+  │  Jetpack Compose  │  MD3   │
│  MVVM + Coroutines + StateFlow              │
│  DataStore Preferences                      │
│  AudioManager + ContentObserver             │
│  WindowManager + ComposeView                │
│  Room  │  Retrofit  │  Moshi                │
│  Firebase AI  │  Roborazzi  │  KSP         │
└─────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

```bash
# Clone
git clone https://github.com/rakibshorkar2/assistivetouch.git

# Open in Android Studio Ladybug+
# Sync Gradle → Run on device (API 26+)
```

| Step | Description |
|------|-------------|
| 1️⃣ | Open project in Android Studio |
| 2️⃣ | Let Gradle sync dependencies |
| 3️⃣ | Run on emulator / device (API 26+) |
| 4️⃣ | Grant *Display over other apps* permission |

---

## 📱 APK Downloads

> Auto-built releases are available on the [Releases](https://github.com/rakibshorkar2/assistivetouch/releases) page.

---

<div align="center">

```
   ██████╗  █████╗ ██╗  ██╗██╗██████╗
   ██╔══██╗██╔══██╗██║ ██╔╝██║██╔══██╗
   ██████╔╝███████║█████╔╝ ██║██████╔╝
   ██╔══██╗██╔══██║██╔═██╗ ██║██╔══██╗
   ██║  ██║██║  ██║██║  ██╗██║██████╔╝
   ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝╚═════╝
```

**Developer — RAKIB**

[![GitHub](https://img.shields.io/badge/GitHub-rakibshorkar2-181717?style=flat-square&logo=github)](https://github.com/rakibshorkar2)
[![Release](https://img.shields.io/github/v/release/rakibshorkar2/assistivetouch?style=flat-square&logo=githubactions)](https://github.com/rakibshorkar2/assistivetouch/releases)

*Built with ❤️ using Kotlin & Jetpack Compose*

</div>
