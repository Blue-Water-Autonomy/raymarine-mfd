# MAR-188 — draft to Raymarine

Draft only; not sent.

**To:** Darren.Boden@teledyne.com

**Cc:** Andrew.Cox2@teledyne.com

**Subject:** BWA web app on RHIBerty Axiom 2 displays — APK signing and integration

Hi Darren and Andrew,

We would like to display our RHIB control web interface on the two Raymarine
MFDs installed on RHIBerty. Our inventory describes them as Axiom 2 Pro 9-inch;
we are collecting exact product numbers and LightHouse software versions.

We have a small Android WebView shell that builds separate APKs for
`https://rhib-control.blw.ai/`, `https://rhib.blw.ai/`, and
`https://dreadnought.blw.ai/`, each with its own launcher and package ID.
It uses HTTPS, JavaScript, DOM storage, and cookies, with Cloudflare Access and
Microsoft sign-in. It contains no native libraries or direct vessel-network
protocol integration. An Android 11 emulator completes Microsoft sign-in and renders the RHIB
interface. MFD installation remains untested.

Your LightHouse documentation describes Axiom 2 as Android 11/API 30/ARM64 and
requires Raymarine-approved, signed APKs. Could you help confirm the following?

1. What is the process for reviewing and signing a privately deployed partner
   APK? Can we submit these three apps together? Should builds be unsigned or
   signed with our own key?
2. Are there required target/minimum SDK levels, manifest entries, permissions,
   package-name conventions, launcher assets, or lifecycle behaviors?
3. Which Android System WebView version ships with these displays, and how is
   it updated? Are embedded Cloudflare/Microsoft login, cookies, and JavaScript
   supported? Is there a recommended alternative for authentication?
   Our Android 11 test image shipped WebView 91, which fails Dreadnought startup
   and cannot apply its modern CSS. Does your current LightHouse release provide
   a newer engine with CSS layers, modern JavaScript APIs, and WebGL 2 support?
   Can partners request a WebView update independently of a LightHouse release?
4. Can the app use wired Ethernet for HTTPS internet access, and are there
   routing, DNS, proxy, or certificate-store restrictions we should account for?
5. How are signed-app updates distributed, and will the same signing identity
   preserve application data and login sessions? Is USB supported as well as SD?
6. Are there display-specific requirements for returning to the homescreen,
   alarm visibility, touch/keyboard input, app suspension, or screen layouts?
7. Alex mentioned a short-term custom-content method demonstrated by your rep.
   Was this the supported HTML5 browser integration, a browser shortcut, or a
   separate development feature? What setup and limitations apply?

We also saw the documented HTML5 partner-integration route. If it is a better
fit for a hosted web interface, could you share its discovery/registration
requirements and whether it supports a remote HTTPS URL with our login flow?

Happy to provide the APK, source, and device information for your review.

Thanks,
Cody

## Documentation behind these questions

- [APK compatibility](https://docs.raymarine.com/81406/en-US/latest/ApkCompatibility-87AF8B2F.html)
- [LightHouse apps and signing](https://docs.raymarine.com/81406/en-US/latest/LightHouseApps-62C7CC8F.html)

The “short-term method” is Alex's report, not a confirmed installation path.
The Android target SDK in this prototype is a provisional choice, not a
documented Raymarine submission requirement.
