# MAR-188 prototype validation — 2026-10-16

This is a record of prototype checks, not MFD acceptance.

## Build and automated checks

Environment: macOS Apple Silicon, Temurin JDK 17, Android SDK 35, Build Tools
35.0.0, Gradle 8.11.1. No Dev Container was used.

| Command/check | Observed result |
| --- | --- |
| `./gradlew test lint assembleDebug assembleRelease --no-daemon` | Passed; debug and unsigned release APKs generated |
| Unit-test XML reports | 21 tests per variant, 42 total; zero failures, errors, or skips |
| Robolectric activity tests | Android API 23 and 30; API-specific cases run on 30 |
| `./gradlew :app:lintRhibRelease --no-daemon` | Passed; full release lint reported no issues |
| Debug lint report | No issues found |
| `apksigner verify --verbose` on debug APK | Signature verified, v1 and v2 |
| `apksigner verify` on release APK | Failed as expected: deliberately unsigned |
| `aapt dump badging` on release APK | Package `ai.blw.mfd.rhibcontrol`, min API 23, target API 30, INTERNET permission |
| Inspect both APK ZIPs | No bundled native libraries |

Lint excludes the Google Play target-SDK submission check because this is a
private OEM prototype. Target API 30 still requires Raymarine confirmation.
The build tools reported an SDK XML metadata-version warning; it did not prevent
compilation, lint, or testing.

## Android 11 emulator

Used the Google APIs ARM64 API 30 system image at 1280 × 720 and 160 dpi.
Its WebView was `com.google.android.webview` 91.0.4472.114. This is the emulator's
provider, not evidence of the provider installed on RHIBerty.

- `adb install -r` succeeded and the launcher activity opened.
- The configured RHIB URL redirected through Cloudflare to Blue Water's
  Microsoft sign-in page. No credentials were entered.
- Info showed Android, WebView, app version, and the configured start URL.
- Disabled Wi-Fi and mobile data in the emulator, then selected Home. The
  native “Page unavailable” screen covered the web surface.
- Restored connectivity and selected “Open RHIB Control.” Microsoft sign-in
  rendered again.

[Login screenshot](screenshots/android11-login.png) ·
[Info screenshot](screenshots/android11-info.png) ·
[Offline screenshot](screenshots/android11-offline.png) ·
[Recovered screenshot](screenshots/android11-recovered.png)

## Delivered build checksums

```text
aa911c2c4b8e2ae2b01f2a2ac89c35dc67a780cbe0b8009489ad0b824f30f41c  rhib-control-0.1.0-debug.apk
5fb954f3a0a10ea0432d7f67d09b588b29e2261b3fbcaa59d4182bca140ccdba  rhib-control-0.1.0-unsigned.apk
```

These describe the delivered binaries. A rebuild may have different hashes;
record new checksums for any artifact submitted for signing.

## RHIBerty network observations

Read-only checks over SSH to `blue@100.64.0.2` reached `rhib-ccs`. Its
`enp85s0` interface had `198.18.4.229/21` and a directly connected route to
the historical MFD address `198.18.5.163`.

That address did not answer ping or ARP; its neighbor entry became `FAILED`.
This does not establish whether a display is off, moved, disconnected, or
using a different address.

Two existing Raymarine-prefixed neighbors answered IPv6 link-local ping:

| MAC | IPv6 address, on `enp85s0` |
| --- | --- |
| `00:11:c7:16:d1:a7` | `fe80::211:c7ff:fe16:d1a7` |
| `00:11:c7:16:d3:d9` | `fe80::211:c7ff:fe16:d3d9` |

Neither matches the historical inventory MAC `00:11:c7:16:31:2c`. Their models
and current IPv4 addresses are unconfirmed. Targeted mDNS queries returned no
response, and a 20-second ARP capture did not identify their IPv4 addresses.
Compare these MACs with Product info on each physical display. No vessel
network settings were changed.

## Not tested

- Physical Raymarine installation, signing, upgrade, and alarm/app lifecycle.
- Full Microsoft authentication, MFA, conditional access, and session persistence.
- Authenticated RHIB UI, live telemetry, vessel commands, and stale-data behavior.
- Physical touch/keyboard behavior or the installed MFD WebView.
- Real Android 6 hardware; API 23 coverage here uses Robolectric.
- GitHub Actions execution; the workflow is prepared but has not run remotely.

Complete [device acceptance](device-acceptance.md) before treating this as an
approved MFD application.
