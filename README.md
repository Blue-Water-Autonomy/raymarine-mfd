# Raymarine MFD apps

Small Android WebView wrappers for BWA web interfaces on Raymarine displays.
Tracked by [MAR-188](https://linear.app/blwai/issue/MAR-188/enable-rhib-controlblwai-access-from-mfd).

The first app opens **https://rhib-control.blw.ai/**. It uses the display's
installed WebView; Chrome, Google Play Services, and a Trusted Web Activity
provider are not required. No native libraries are bundled, so the same APK
contains architecture-independent Android bytecode for both ARM32 and ARM64.

**Status: development prototype, not Raymarine-approved.** The release output is
unsigned. Raymarine must confirm its submission process, sign an accepted build,
and the signed APK must be tested on the actual MFD before operational use.

The [latest validation record](docs/validation-0.1.1.md) includes build results, emulator
screenshots, and untested items. The [Raymarine email draft](docs/raymarine-questions.md)
collects the remaining integration and signing questions.

## Target hardware

RHIBerty reportedly has two Axiom 2 displays at the driver's console, described
internally as “Axiom 2 Pro 9-inch.” Exact product numbers and LightHouse builds
still need device confirmation. Raymarine's LightHouse 4 documentation lists:

| Family | Android | API | Architecture |
| --- | --- | --- | --- |
| Axiom-Series | 6.0 | 23 | ARMv7-A, 32-bit |
| Axiom 2-Series | 11.0 | 30 | ARM64, 64-bit |

The project compiles with SDK 35, requires API 23, and targets API 30 to match the
documented Axiom 2 platform. The target SDK is an initial OEM-distribution choice,
not a verified Raymarine requirement or a Play Store submission configuration.
Supporting these Android versions does not establish compatibility with every
MFD or every installed WebView. See [device acceptance](docs/device-acceptance.md).

## Build

Requirements: JDK 17, Android SDK Platform 35, Build Tools 35.0.0, and internet
access for the first Gradle build. Gradle 8.11.1 is pinned with a distribution
checksum; Android Gradle Plugin is pinned to 8.9.2.

Point `ANDROID_HOME` at your SDK, or put `sdk.dir=/absolute/path/to/sdk` in the
ignored `local.properties`. From the repository root:

```sh
./gradlew :app:testRhibDebugUnitTest :app:lintRhibRelease \
  :app:assembleRhibDebug :app:assembleRhibRelease
```

Outputs:

- `app/build/outputs/apk/rhib/debug/app-rhib-debug.apk`: debug-signed, package
  `ai.blw.mfd.rhibcontrol.debug`, for an emulator or normal Android test device.
- `app/build/outputs/apk/rhib/release/app-rhib-release-unsigned.apk`: unsigned,
  package `ai.blw.mfd.rhibcontrol`, candidate for Raymarine's signing process.

An unsigned APK cannot be installed directly. A debug signature does not grant
permission to install on a Raymarine MFD. No signing keys or credentials belong
in this repository. Increase `versionCode` before submitting an updated APK.

GitHub Actions builds the flavors, runs unit tests and lint, and uploads APKs and
reports. This repository has no native libraries, ROS dependencies, or runtime
dependency on power-module.

## App behavior

- JavaScript, DOM storage, first-party cookies, and HTTPS navigation support the
  hosted application and its sign-in flow.
- The initial public redirect was observed through
  `bluewater-ai.cloudflareaccess.com` to `login.microsoftonline.com`. The actual
  authenticated flow also reaches `rhib.blw.ai` and `rhib-grafana.blw.ai`; both
  observed destinations are explicitly allowed.
  The complete session still needs validation on the physical display.
- Back, Home, Reload, and Info remain available above the page. Info shows the
  Android API, WebView provider/version where available, and user agent.
- Native page-load, HTTP, TLS, and renderer-failure screens cover the web surface.
  Recovery loads the configured entry point; it does not replay a failed POST.
- Blocked navigation reports only its destination origin; popup restrictions use
  a separate message. Debug builds log this same sanitized diagnostic.
- Sessions persist in the app's private WebView storage. Android backup is disabled.
  Use the display's app-data controls to clear a session before transferring a device.
- The app preserves the system navigation controls and does not force orientation,
  kiosk mode, screen brightness, or changes to the vessel network.

## Navigation and limitations

`portals.gradle` defines each app's name, package ID, entry URL, and exact allowed
top-level HTTPS hosts. It includes the RHIB portal, the observed Cloudflare
tenant, and Microsoft's sign-in hosts. New sign-in destinations require an
explicit configuration change. HTTPS subresources are not restricted to those
hosts, allowing authentication assets and the web application's dependencies.
This policy is not a network firewall.

HTTP, mixed content, local file/content access, arbitrary intent launches, and
native JavaScript bridges are disabled. TLS errors are never bypassed. WebView
remote debugging is enabled only for debug builds. Auth URL query strings are
not displayed in the toolbar or included in application logs.

This first version is single-window: popup sign-in flows, downloads, file uploads,
camera/microphone/geolocation permissions, and external-app authentication are
not implemented. Microsoft tenant policies may reject embedded web views or
require a broker; a successful login page render is not proof of successful SSO.

The shell does not implement an offline copy of the site, a vessel-local URL
fallback, command retry, or telemetry freshness detection. The web application
owns live connection status, command authorization, and stale-data behavior.
After Android recreates the activity, it opens the entry point with the existing
cookies, rather than restoring an arbitrary authentication callback URL.

## Add another portal

Add an entry to `portals.gradle` with a unique application ID, display name,
confirmed HTTPS URL, and exact navigation hosts. Each entry becomes a Gradle
product flavor and separate launcher app. Do not add a guessed Dreadnought URL
or use an MFD's management IP as the web application's address.

Run `./gradlew test lint assembleDebug assembleRelease` to
check every configured portal. The navigation tests also verify that each
flavor permits its own entry point. Add the new site's actual sign-in flow to
device acceptance testing.

## References

- [Raymarine Android compatibility](https://docs.raymarine.com/81406/en-US/latest/ApkCompatibility-87AF8B2F.html)
- [Raymarine approved APKs](https://docs.raymarine.com/81406/en-US/latest/LightHouseApps-62C7CC8F.html)
- [Installing approved APKs](https://docs.raymarine.com/81406/en-US/latest/InstallLighthouseThird-partyApps-F7A4B553.html)
- [Android WebView documentation](https://developer.android.com/develop/ui/views/layout/webapps/webview)
