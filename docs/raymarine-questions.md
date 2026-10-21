# MAR-188 — draft to Raymarine

Draft only; not sent.

**To:** Darren.Boden@teledyne.com

**Cc:** Andrew.Cox2@teledyne.com, Alex Fee

**Subject:** Signing request: three web-app APKs for RHIBerty's Axiom 2 displays

Hi Darren and Andrew,

Alex Fee suggested I contact you about getting three Android apps approved and
signed for the two Raymarine displays on our vessel, RHIBerty.

**The apps.** Each is a small Android WebView wrapper for one of our web
interfaces: https://rhib-control.blw.ai/, https://rhib.blw.ai/, and
https://dreadnought.blw.ai/. Each has its own package ID and launcher icon. They
load the site over HTTPS with JavaScript, DOM storage, and cookies, and sign in
through Cloudflare Access and Microsoft. The only permission is INTERNET. There
are no native libraries and no direct integration with the vessel network.

**The displays.** Both are Axiom 2 12 (E70724). MFD1, the data master, runs
LightHouse 5 (application 5.00.116, product bundle 5.0.127). Both are on wired
RayNet with internet access through Starlink.

**What we've tested.** On an Android 11 emulator, the apps complete Microsoft
sign-in and render our interfaces. On the boat, Alex used the browser view in
the Netflix app, the route your rep showed him. Dreadnought and RHIB Control
both work there, and that view reports Android WebView with Chrome 145 on
Android 11. We can't install our own APKs on the displays without your
signature, so that step is untested.

**The APKs.** Unsigned release builds, version 0.2.1+build.24 (version code
1024), are here: [link]

SHA-256:

- rhib-control-0.2.1+build.24-unsigned.apk:
  1a85f4260dd6897a0eda12e50fd8a96e989d946bdafbabf827fe67446b27ca23
- rhib-0.2.1+build.24-unsigned.apk:
  b736721888a55300ec8e56a148216619dc5250a9c3eef5480d1d3deba452f2e2
- dreadnought-0.2.1+build.24-unsigned.apk:
  c96e18ab3247faf353fabbb050afde8c792b4727decb12ecfa67bbf91e2ba08e

Could you help with the following?

1. What is your process for reviewing and signing a privately deployed partner
   app? Can we submit all three together, and do you want unsigned builds like
   these or builds signed with our own key?
2. Do you have requirements for target or minimum SDK, manifest entries,
   permissions, package names, launcher icons, or app lifecycle? Your APK
   compatibility page lists Axiom 2 as Android 11, API 30, ARM64 under
   LightHouse 4. Does anything differ on LightHouse 5? Our apps target API 30
   and require API 23.
3. How is Android System WebView updated on Axiom 2: with LightHouse releases
   or separately? Our sites need a modern engine; an emulator with the stock
   WebView 91 can't run Dreadnought.
4. Is Cloudflare and Microsoft sign-in inside an embedded WebView supported?
   Do partner apps get the same network access as built-in apps, or are there
   proxy, DNS, or certificate-store differences?
5. How are updates to signed apps distributed? Will a new version signed with
   the same identity keep app data and login sessions?
6. Are there display-specific requirements for returning to the home screen,
   alarm visibility, touch or keyboard input, app suspension, or screen layout?

We also saw your HTML5 partner-integration option. If that's a better fit for a
hosted web interface, could you share its requirements, and whether it supports
a remote HTTPS URL with our sign-in flow?

I'm happy to share the source or anything else that helps your review.

Thanks,
Cody Brookshear
Blue Water Autonomy

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
The Android target SDK in this prototype is a provisional choice, not a
documented Raymarine submission requirement.
