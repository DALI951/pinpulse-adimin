# PinPulse Admin (Fleet Command & SaaS Access Sheet)

Official dedicated Master Administration app and hardware-secured Web Console for the **PinPulse** ecosystem.

---

## 🛡️ Key Features

### 1. Hardware MAC Address Lock (PC Web Console)
- Locked to authorized physical machine MAC address: **`0A-E0-AF-A6-0E-2F`** (Ethernet) / **`90-DE-80-6C-F1-DD`** (Wi-Fi).
- On server boot (`server.js`), hardware network interfaces are inspected via `os.networkInterfaces()`. If executed on an unauthorized computer, access is immediately blocked and the process halts.
- Binds strictly to `127.0.0.1:8080` (localhost) so external LAN devices cannot intercept the admin controls.

### 2. Admin Android APK with Master Password Vault
- **First Launch Setup**: Prompts the administrator to create and encrypt a Master Admin Password.
- **Master Authentication**: On every subsequent launch, the master password must be entered to decrypt and access the Fleet Console.
- **Built-in Password Management**: Allows changing the master key safely within the console.
- **Embedded Offline & Online Runtime**: High-performance WebKit WebView with native hardware acceleration, vibration, and location access.

### 3. 👥 Live "Who Is Friends With Whom" Inspector
- Subscribes in real time to retained MQTT channels: `pinpulse/v1/registry/friends/#` and `pinpulse/v1/registry/user/+`.
- Visual directory displaying every registered user, their active friends count vs quota, and expandable list of current friends with names, friend codes, and emails.
- Instant search and filtering by user name, email, friend code, or connection status.

### 4. ⚡ Instant Force-Pairing Engine (Zero-Interaction Pairing)
- Pair any two users remotely with **zero interaction, no friend request, and no acceptance code required**.
- Dispatches retained `FORCE_ADD_FRIEND` administrative directives directly to both users' secure admin topics (`pinpulse/v1/admin/users/<id>`).
- Both mobile devices automatically store each other, subscribe to live GPS & status streams, and update their circles instantly.
- Also supports remote force-unpairing when necessary.

### 5. Full SaaS CRM & Subscriptions Management
- Plan tier controls: **DEMO** (1 Friend / 0 Groups / 7d), **FAMILY** (10 Friends / 2 Groups), **BUSINESS** (50 Friends / 10 Groups), and **CUSTOM**.
- Real-time account status switching: `ACTIVE`, `LOCKED_PAYMENT`, `BANNED`, `FORCE_UPDATE`, and `DELETED`.
- Rate limit tuning, live fleet GPS map, quarantine management, and audit logging.

---

## 🚀 How to Run the Hardware-Secured Web Console (PC)

### Prerequisites
- Node.js (v16+) installed.
- Must be executed on the authorized host workstation (`0A-E0-AF-A6-0E-2F`).

### 1-Click Launch
Double-click `start-admin.bat` in the root folder, or run in terminal:
```bash
node server.js
```
The console will verify host hardware and automatically become available at:
```
http://localhost:8080
```

---

## 📱 Android Admin APK Build & Release

The repository includes a complete GitHub Actions CI/CD pipeline (`.github/workflows/release.yml`) that automatically compiles, signs, and packages `PinPulseAdmin-vX.X.apk` on every push to `main`, publishing it directly to **GitHub Releases**.

To build locally:
```bash
./gradlew assembleRelease
```
The signed APK will be generated at `app/build/outputs/apk/release/app-release.apk`.
