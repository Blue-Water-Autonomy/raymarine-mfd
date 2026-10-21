# MAR-188 — RHIBerty displays

Reported by Alex Fee from the vessel. These are observations, not installation
results.

## Product information (2026-10-21)

Photographed from Settings → Network → Product info. Each photo showed only part
of the page, so some fields are missing.

| | MFD1 (data master) | MFD2 |
| --- | --- | --- |
| Product name | not captured | Raymarine AXIOM 2 12 |
| Product ID | not captured | E70724 |
| Application version | 5.00.116 | not captured |
| Platform version | 5.0.21 | not captured |
| Product bundle version | 5.0.127 | not captured |
| Kernel | 4.19.193-ab5.0.21_B5.0.127 | not captured |
| CAN address | 07 | 08 |
| IP address / mask | 198.18.1.186 / 255.255.248.0 | 198.18.0.55 / 255.255.248.0 |
| Link | 1000 Mbps full-duplex | 1000 Mbps full-duplex |

E70724 is the 12-inch Axiom 2, not an Axiom 2 Pro. MFD1's software is
LightHouse 5. Both displays are on wired RayNet and, per Alex, reach the
internet through the vessel's Starlink connection (2026-10-19).

## Browser engine (2026-10-21)

Checked with whatismybrowser.com in the browser view reachable from Raymarine's
approved Netflix app:

```text
Mozilla/5.0 (Linux; Android 11; endeavour Build/RQ3A.210705.001) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/145.0.7632.45 Safari/537.36
```

- `Version/4.0` with a full Chrome version identifies Android WebView. Chrome
  itself omits that token and reports a reduced version. The usual `; wv` token
  is absent, so the hosting app probably sets its own user agent.
- Android 11 matches Raymarine's documented Axiom 2 platform and this project's
  target API 30.
- Chromium 145 is far newer than the emulator's WebView 91, which cannot run
  Dreadnought. It is also newer than Tailwind CSS 4's Chrome 111 baseline.
- The report also showed a 1280 × 800 screen, 6 CPU cores, 4 GB RAM, and a
  Mali-T860 GPU.

On 2026-10-19 Dreadnought and RHIB Control both worked in that view; Dreadnought
rendered its chart. `rhib.blw.ai` has not been checked there.

After a signed APK is installed, confirm that its Info screen reports the same
WebView version.
