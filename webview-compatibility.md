# MAR-188 — Dreadnought browser diagnosis

Observed on 2026-10-16 with wrapper 0.2.1, Android 11/API 30, ARM64,
and emulator `mar188-api30` (`emulator-5556`).

## Confirmed failure

After successful authentication, the deployed Dreadnought page appears unstyled.
Both “GPU problem” and “GPU has recovered” messages show together.
Remote WebView inspection establishes:

- Provider: `com.google.android.webview`, version `91.0.4472.114`.
- JavaScript startup throws `TypeError: Object.hasOwn is not a function`.
- CSS layers, `oklch()` colors, and `:has()` selectors are unsupported.
- The status text that carries the `hidden` class still has computed display `inline`.
- A new, independent WebGL 2 context is created successfully. This does not test
  the map's rendering load, but rules out total absence of WebGL 2 in this emulator.

The warning is not sufficient evidence of a GPU crash. The browser cannot run
the frontend or apply its visibility rules. No navigation allowlist change or
wrapper GPU flag resolves these missing web-platform features.

[Tailwind CSS 4 documents Chrome 111 as its baseline](https://tailwindcss.com/docs/compatibility).
Other application features can require a newer browser. Do not treat 111 as a
validated minimum for Dreadnought or imply that Android API 30 guarantees it.

## Emulator comparison

Installed official [AOSP WebView 128.0.6613.88, ARM64](https://android.googlesource.com/platform/external/chromium-webview/+/refs/heads/main/128.0.6613.88/arm64/)
alongside the original provider and selected `com.android.webview`.
The emulator is a `userdebug` image, which permits this provider. Production
Raymarine firmware may have different provider/signature restrictions.

- Git blob: `258dc814a9dc02ce953889440255d6787fca7b57` (verified after download).
- APK SHA-256: `492c3d054e4123ae0c1e6f3541e5ab515633e1fb4006e33bf343749ef1b66ae2`.
- On the new engine, CSS layers, `oklch()`, `:has()`, `Array.toSorted`, and
  `structuredClone` probes succeed; an independent WebGL 2 context also succeeds.
- Switching providers returned the app to Cloudflare sign-in. Authenticated
  Dreadnought rendering on the new provider remains pending user sign-in.

This pinned historical AOSP build is a compatibility test, not a current
production browser recommendation. It is not bundled with any of our APKs.
No Dreadnought server code, vessel settings, or controls were changed.

Select the test provider with:

```sh
adb -s emulator-5556 shell cmd webviewupdate set-webview-implementation com.android.webview
```

Return to the emulator's original provider with:

```sh
adb -s emulator-5556 shell cmd webviewupdate set-webview-implementation com.google.android.webview
```

[Chromium's provider documentation](https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/docs/webview-providers.md)
explains the image-specific allowlist and signature requirements.

## Physical-device follow-up

Ask Alex for the LightHouse version and the wrapper's Info screen, which includes
the installed WebView version. If the app is not installable yet, ask Raymarine
for this information against the exact model and firmware release.

Ask Raymarine whether a maintained, newer WebView is available and supported.
If the engine cannot be updated, supporting that browser requires deliberate
frontend compatibility work across CSS, JavaScript, and chart rendering.

Not tested: authenticated map rendering with WebView 128, long-running graphics
stability, physical MFD compatibility, telemetry correctness, or vessel commands.
