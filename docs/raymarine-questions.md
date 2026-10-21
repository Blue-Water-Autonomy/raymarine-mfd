# MAR-188 — draft to Raymarine

Draft only; not sent.

**To:** Darren.Boden@teledyne.com

**Cc:** Andrew.Cox2@teledyne.com, Alex Fee

**Subject:** BWA web app on RHIBerty Axiom 2 displays — APK signing and integration

Hi Darren and Andrew,

We would like to display our RHIB control web interfaces on the two Raymarine
MFDs installed on RHIBerty, both Axiom 2 12 displays (E70724). MFD1, the data
master, runs LightHouse 5 (application 5.00.116, product bundle 5.0.127).

We have a small Android WebView shell that builds separate APKs for
`https://rhib-control.blw.ai/`, `https://rhib.blw.ai/`, and
`https://dreadnought.blw.ai/`, each with its own launcher and package ID.
It uses HTTPS, JavaScript, DOM storage, and cookies, with Cloudflare Access and
Microsoft sign-in. It contains no native libraries or direct vessel-network
protocol integration. An Android 11 emulator completes Microsoft sign-in and
renders the RHIB interface.

On the boat, Alex opened the browser view reachable from the approved Netflix
app, the route your rep showed him. Dreadnought and RHIB Control both worked
there over the vessel's Starlink connection. That view reports Android WebView
with Chrome 145 on Android 11, so we expect the same engine to run our apps.
Installing our APKs on the displays remains untested.

The three unsigned APKs (version 0.2.1+build.24, version code 1024) are here:
[link]. SHA-256:

- `rhib-control-0.2.1+build.24-unsigned.apk`:
  `1a85f4260dd6897a0eda12e50fd8a96e989d946bdafbabf827fe67446b27ca23`
- `rhib-0.2.1+build.24-unsigned.apk`:
  `b736721888a55300ec8e56a148216619dc5250a9c3eef5480d1d3deba452f2e2`
- `dreadnought-0.2.1+build.24-unsigned.apk`:
  `c96e18ab3247faf353fabbb050afde8c792b4727decb12ecfa67bbf91e2ba08e`

Your LightHouse 4 documentation describes Axiom 2 as Android 11/API 30/ARM64 and
requires Raymarine-approved, signed APKs. Could you help confirm the following?

1. What is the process for reviewing and signing a privately deployed partner
   APK? Can we submit these three apps together? Should builds be unsigned or
   signed with our own key?
2. Are there required target/minimum SDK levels, manifest entries, permissions,
   package-name conventions, launcher assets, or lifecycle behaviors? Does
   anything differ on LightHouse 5?
3. How is Android System WebView updated on Axiom 2: with LightHouse releases
   or separately? Our sites need a modern engine (an Android 11 emulator's stock
   WebView 91 cannot run Dreadnought), so we want to avoid an older one on a
   future display. Are embedded Cloudflare/Microsoft login, cookies, and
   JavaScript supported in partner apps?
4. Do partner apps have the same network access as the built-in apps, or are
   there proxy, DNS, or certificate-store differences we should account for?
5. How are signed-app updates distributed, and will the same signing identity
   preserve application data and login sessions?
6. Are there display-specific requirements for returning to the homescreen,
   alarm visibility, touch/keyboard input, app suspension, or screen layouts?

We also saw the documented HTML5 partner-integration route. If it is a better
fit for a hosted web interface, could you share its discovery/registration
requirements and whether it supports a remote HTTPS URL with our login flow?

Happy to provide the source and device information for your review.

Thanks,
Cody

## Before sending

- Replace `[link]` with a shared-file link. The GitHub repository is internal,
  so Teledyne cannot use its release page, and mail filters often block `.apk`
  attachments. The hashes above come from the build.24 release's `release.json`.
- Add Alex's email address to Cc.

Display details and the full user agent are in the
[display record](vessel-displays.md).

## Documentation behind these questions

- [APK compatibility](https://docs.raymarine.com/81406/en-US/latest/ApkCompatibility-87AF8B2F.html)
  lists Netflix among the Raymarine-signed apps for Axiom 2 and installation
  from SD card or USB.
- [LightHouse apps and signing](https://docs.raymarine.com/81406/en-US/latest/LightHouseApps-62C7CC8F.html)

The Netflix browser view is a workaround, not a supported installation path.
The APK compatibility page lists app versions for LightHouse 4; MFD1 runs LightHouse 5.
The Android target SDK in this
prototype is a provisional choice, not a documented Raymarine submission
requirement.
