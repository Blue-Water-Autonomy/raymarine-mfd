# MAR-188 — draft to Raymarine

Draft only; not sent.

**To:** Darren.Boden@teledyne.com

**Cc:** Andrew.Cox2@teledyne.com, Alex Fee

**Subject:** BWA web app on RHIBerty Axiom 2 displays — APK signing and integration

Hi Darren and Andrew,

We would like to display our RHIB control web interfaces on the two Raymarine
MFDs installed on RHIBerty, both Axiom 2 Pro 12-inch displays. We will follow up
with their exact product numbers and LightHouse software versions.

We have a small Android WebView shell that builds separate APKs for
`https://rhib-control.blw.ai/`, `https://rhib.blw.ai/`, and
`https://dreadnought.blw.ai/`, each with its own launcher and package ID.
It uses HTTPS, JavaScript, DOM storage, and cookies, with Cloudflare Access and
Microsoft sign-in. It contains no native libraries or direct vessel-network
protocol integration. An Android 11 emulator completes Microsoft sign-in and
renders the RHIB interface.

On the boat, Alex opened the browser view reachable from the approved Netflix
app, the route your rep showed him. Dreadnought and RHIB Control both worked
there over the vessel's Starlink connection. Installing our APKs on the
displays remains untested.

The three unsigned APKs (version 0.2.1+build.24, version code 1024) are here:
[link]. SHA-256:

- `rhib-control-0.2.1+build.24-unsigned.apk`:
  `1a85f4260dd6897a0eda12e50fd8a96e989d946bdafbabf827fe67446b27ca23`
- `rhib-0.2.1+build.24-unsigned.apk`:
  `b736721888a55300ec8e56a148216619dc5250a9c3eef5480d1d3deba452f2e2`
- `dreadnought-0.2.1+build.24-unsigned.apk`:
  `c96e18ab3247faf353fabbb050afde8c792b4727decb12ecfa67bbf91e2ba08e`

Your LightHouse documentation describes Axiom 2 as Android 11/API 30/ARM64 and
requires Raymarine-approved, signed APKs. Could you help confirm the following?

1. What is the process for reviewing and signing a privately deployed partner
   APK? Can we submit these three apps together? Should builds be unsigned or
   signed with our own key?
2. Are there required target/minimum SDK levels, manifest entries, permissions,
   package-name conventions, launcher assets, or lifecycle behaviors?
3. Which Android System WebView version ships with the current LightHouse
   release on Axiom 2, and how is it updated? Our sites need a modern engine:
   an Android 11 emulator's stock WebView 91 cannot run Dreadnought. Are
   embedded Cloudflare/Microsoft login, cookies, and JavaScript supported in
   partner apps?
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
- If Alex has captured the browser view's user agent by then, state the
  WebView version in question 3 instead of only asking for it.

## Documentation behind these questions

- [APK compatibility](https://docs.raymarine.com/81406/en-US/latest/ApkCompatibility-87AF8B2F.html)
  lists Netflix among the Raymarine-signed apps for Axiom 2 and installation
  from SD card or USB.
- [LightHouse apps and signing](https://docs.raymarine.com/81406/en-US/latest/LightHouseApps-62C7CC8F.html)

The Netflix browser view is a workaround, not a supported installation path,
and its browser engine has not been identified. The Android target SDK in this
prototype is a provisional choice, not a documented Raymarine submission
requirement.
