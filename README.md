# 🛡️ SmartLogin

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://adoptium.net/)
[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Spigot-brightgreen.svg?style=flat-square)](https://papermc.io/)
[![Version](https://img.shields.io/badge/Version-1.21.x-blue.svg?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-purple.svg?style=flat-square)](LICENSE)
[![Zero DRM](https://img.shields.io/badge/DRM-Zero%20%2F%20Free-red.svg?style=flat-square)]()

**SmartLogin** is the next-generation, high-performance, non-destructive authentication and identity suite for Minecraft Paper/Purpur/Spigot 1.21.x servers. Designed from the ground up to outperform traditional and proprietary alternatives with zero DRM, zero telemetry, and maximum security.

---

## ✨ Features

- ⚡ **Interactive Setup Wizard (`/smartlogin setup`):** First-time in-game visual click-to-configure wizard. Toggle security modes, encryption speeds, PIN pad, 2FA enforcement, and auto-login with clickable chat actions—no manual YAML editing required!
- 🔐 **Military-Grade Password Security:** PBKDF2 with HMAC-SHA512 and 32-byte cryptographic salt per user. Timing-attack resistant hash comparison.
- 📱 **Real In-Game QR Code 2FA:** Generates dynamic, custom ZXing QR codes rendered directly onto in-game maps for instantaneous pairing with Google Authenticator, Aegis, Authy, or 1Password.
- 🔢 **Virtual PIN Pad GUI:** Anti-streamer & anti-keylogger 9-digit randomized virtual inventory keypad for password entry.
- 💎 **Tri-Mode Auto-Login Detection:**
  - **Bedrock / Floodgate:** Instant auto-login for verified Geyser/Floodgate bedrock players.
  - **Java Premium (Mojang):** Automatic Mojang account verification with cryptographic public key matching.
  - **SessionShield:** IP-based reconnect session caching with configurable expiration windows.
- 🗄️ **Dual Ultra-Fast Storage:**
  - SQLite in Write-Ahead Logging (WAL) mode for lightweight single-server setups.
  - HikariCP connection-pooled MySQL / MariaDB for high-throughput networks and proxies.
- 🌐 **Multi-Language Architecture:** Pre-bundled with English (`en`), Spanish (`es`), French (`fr`), and Portuguese (`pt`).
- 🛡️ **Total Zero-Bypass Lockdown:** Restricts movement, inventory interaction, item drops, block interaction, entity targeting, command execution, and chat leakage before authentication.

---

## 🚀 Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/register <password> <confirm>` | Default | Register a new account |
| `/login <password>` | Default | Log into an existing account |
| `/changepassword <old> <new>` | Default | Update your account password |
| `/2fa setup` | Default | Receive an in-game QR code map to configure 2FA |
| `/2fa verify <code>` | Default | Authenticate 2FA session code |
| `/2fa disable <code>` | Default | Turn off 2FA protection |
| `/premium` | Default | Toggle Mojang Java premium auto-login verification |
| `/smartlogin setup` | `smartlogin.admin` | Open interactive setup wizard in chat |
| `/smartlogin reload` | `smartlogin.admin` | Reload configuration and localization files |
| `/smartlogin unregister <player>` | `smartlogin.admin` | Wipe player authentication data |
| `/smartlogin reset2fa <player>` | `smartlogin.admin` | Disable and reset 2FA secret for player |
| `/smartlogin pinpad <player>` | `smartlogin.admin` | Force open virtual PIN pad for a player |
| `/smartlogin toggle <feature>` | `smartlogin.admin` | Instantly toggle feature flags |

---

## 🛠️ Compilation & Installation

### Requirements
- **JDK 21+**
- **Maven 3.8+**
- **Paper / Purpur / Spigot 1.21.x**

```bash
# Clone repository
git clone https://github.com/dr8553097-sudo/SmartLogin.git
cd SmartLogin

# Compile JAR package
mvn clean package
```

The compiled plugin will be located in `target/SmartLogin-1.0.0.jar`.

---

## 📄 License
SmartLogin is released under the **MIT License**. Free and open-source forever.
