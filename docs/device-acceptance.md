# MAR-188 device acceptance

Run these checks for RHIB Control, RHIB, and Dreadnought. Each app has a separate
package ID and requires its own signed APK for physical display installation.
They are acceptance criteria, not a record of completed tests.

## Collect before submission

1. Record each display's exact product number and LightHouse version.
2. Photograph Settings → Network → the display's Product info.
3. Confirm the current IP of each display; one historical address does not identify both.
4. Record whether the display uses wired Ethernet, Wi-Fi, and an internet connection.
5. Ask Raymarine for its target SDK, manifest, package naming, and signing requirements.
6. Confirm whether a private partner deployment is supported without a public app listing.

## On an emulator or ordinary Android device

Install each debug APK with `adb install -r <apk-path>`. Confirm the three apps
coexist with distinct names and open their own start URLs. This establishes
ordinary Android behavior only. Match the MFD's WebView version where possible.

Check startup, top-level login redirects, Back/Home/Reload, orientation changes,
loss of internet, error recovery, and Info. Never use a successful emulator run
as evidence of Raymarine installation approval or vessel-control correctness.

## On both target MFDs

- Install the Raymarine-signed APK through Apps → Install from SD / USB.
- Confirm its launcher icon, name, version, and ability to exit to the MFD homescreen.
- Photograph Info to capture the actual Android and WebView versions.
- Complete Cloudflare/Microsoft sign-in, including MFA and any conditional-access policy.
- Verify session persistence after leaving/reopening the app and after a display reboot.
- Verify page layout, keyboard, touch targets, scrolling, charts, and live telemetry.
- Confirm each display's login/session is independent of the other display.
- Validate behavior when WAN access or the upstream web service is unavailable.
- Confirm a TLS error cannot be bypassed and page-load failures hide old controls.
- Check the web application's own stale-telemetry and disconnected-command handling
  under the team's vessel test procedure; the wrapper cannot establish these contracts.
- Check device alarm visibility, Back/Home behavior, and app pause/resume with Raymarine.
- Install a newer signed build with the same package ID and confirm the update path.

Capture the APK SHA-256, device model, LightHouse version, Android API, WebView
version, and observed results. Record failures instead of broad “works on MFD” claims.
