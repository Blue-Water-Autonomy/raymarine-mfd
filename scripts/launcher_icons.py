"""Generate the launcher icon VectorDrawables from the Blue Water mark.

The mark was traced from blw.ai's 256 px app icon. Coordinates below use that
image's pixel grid. Run from the repository root:

    python3 scripts/launcher_icons.py [--preview preview.svg]

Colors come from each flavor's launcher_background/launcher_foreground values.
"""

import argparse
import math
from pathlib import Path


# Three stripes, each stepping up through the same S-curve. Stripe k is stripe 0
# moved down 54 px and left 14.55 px; only the lower bar's left end differs.
RIGHT = 227.5
LEFTS = (40.0, 25.6, 59.0)
HANDLE = 18.5
MARK_BOX = (25.6, 36.0, 227.5, 220.0)


def _solve(fraction):
    """Return t where a horizontal-handle cubic covers this fraction of its height."""
    lo, hi = 0.0, 1.0
    for _ in range(60):
        mid = (lo + hi) / 2
        if 3 * mid * mid - 2 * mid ** 3 < fraction:
            lo = mid
        else:
            hi = mid
    return (lo + hi) / 2


def _lerp(a, b, t):
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)


def _split(curve, t):
    a, b, c = (_lerp(curve[i], curve[i + 1], t) for i in range(3))
    d, e = _lerp(a, b, t), _lerp(b, c, t)
    f = _lerp(d, e, t)
    return [curve[0], a, d, f], [f, e, c, curve[3]]


def _stripe(k):
    top = 36 + 54 * k
    bottom = top + 76
    x0 = 166.4 - 14.55 * k
    x1 = 118.2 - 14.55 * k
    curve = [(x0, top), (x0 - HANDLE, top), (x1 + HANDLE, bottom), (x1, bottom)]
    # The upper bar spans top..top+39.5 and the lower bar top+36..bottom.
    upper, _ = _split(curve, _solve(36 / 76))
    _, lower = _split(curve, _solve(39.5 / 76))
    return [
        ("M", [curve[0]]), ("H", RIGHT), ("V", top + 39.5), ("H", lower[0][0]),
        ("C", lower[1:]), ("H", LEFTS[k]), ("V", top + 36), ("H", upper[3][0]),
        ("C", [upper[2], upper[1], upper[0]]), ("Z", None),
    ]


MARK = [_stripe(k) for k in range(3)]


# Geometric capitals with cap height 10 and 2.4 stems. Holes wind opposite to
# their outlines because android:fillType needs API 24 and minSdk is 23.
def _rect(x0, y0, x1, y1):
    return [("M", [(x0, y0)]), ("H", x1), ("V", y1), ("H", x0), ("Z", None)]


def _polygon(points):
    return [("M", [points[0]])] + [("L", [p]) for p in points[1:]] + [("Z", None)]


def _arc(radius, large, sweep, end):
    return ("A", (radius, large, sweep, end))


def _glyphs():
    cos, sin = math.cos(math.radians(42)), math.sin(math.radians(42))
    return {
        "D": (8.2, [("M", [(0, 0)]), ("H", 3.2), _arc(5, 0, 1, (3.2, 10)), ("H", 0), ("Z", None),
                    ("M", [(2.4, 2.2)]), ("V", 7.8), ("H", 3.2), _arc(2.8, 0, 0, (3.2, 2.2)),
                    ("Z", None)]),
        "O": (10.0, [("M", [(5, 0)]), _arc(5, 1, 1, (5, 10)), _arc(5, 1, 1, (5, 0)), ("Z", None),
                     ("M", [(5, 2.3)]), _arc(2.7, 1, 0, (5, 7.7)), _arc(2.7, 1, 0, (5, 2.3)),
                     ("Z", None)]),
        "M": (10.6, _polygon([(0, 10), (0, 0), (2.7, 0), (5.3, 6.0), (7.9, 0), (10.6, 0),
                              (10.6, 10), (8.2, 10), (8.2, 4.59), (5.856, 10), (4.744, 10),
                              (2.4, 4.59), (2.4, 10)])),
        "E": (7.0, _rect(0, 0, 2.4, 10) + _rect(0, 0, 7, 2.2) + _rect(0, 3.95, 6.4, 6.05)
              + _rect(0, 7.8, 7, 10)),
        "T": (8.0, _rect(0, 0, 8, 2.2) + _rect(2.8, 0, 5.2, 10)),
        "I": (2.4, _rect(0, 0, 2.4, 10)),
        "C": (5 + 5 * cos, [("M", [(5 + 5 * cos, 5 - 5 * sin)]), _arc(5, 1, 0, (5 + 5 * cos, 5 + 5 * sin)),
                            ("L", [(5 + 2.7 * cos, 5 + 2.7 * sin)]),
                            _arc(2.7, 1, 1, (5 + 2.7 * cos, 5 - 2.7 * sin)), ("Z", None)]),
    }


# Tighter spacing beside T, whose crossbar leaves open space below.
GAPS = {"DO": 1.5, "OM": 1.5, "ME": 1.5, "ET": 1.0, "TI": 1.0, "IC": 1.5}


def _word(text):
    glyphs = _glyphs()
    x = 0.0
    placed = []
    for i, letter in enumerate(text):
        width, commands = glyphs[letter]
        placed.append((x, commands))
        x += width
        if i + 1 < len(text):
            x += GAPS[text[i:i + 2]]
    return placed, x


def _number(value):
    text = ("%.2f" % value).rstrip("0").rstrip(".")
    return "0" if text == "-0" else text


def _path_data(commands, scale, dx, dy):
    x = lambda v: _number(v * scale + dx)
    y = lambda v: _number(v * scale + dy)
    out = []
    for command, args in commands:
        if command in "ML":
            out.append(command + x(args[0][0]) + "," + y(args[0][1]))
        elif command == "H":
            out.append("H" + x(args))
        elif command == "V":
            out.append("V" + y(args))
        elif command == "C":
            out.append("C" + " ".join(x(p[0]) + "," + y(p[1]) for p in args))
        elif command == "A":
            radius, large, sweep, end = args
            r = _number(radius * scale)
            out.append(f"A{r},{r} 0 {large},{sweep} {x(end[0])},{y(end[1])}")
        else:
            out.append("Z")
    return "".join(out)


def icon_paths(label=None):
    """Return foreground path data for a 108 x 108 viewport, centered."""
    x0, y0, x1, y1 = MARK_BOX
    width, height = x1 - x0, y1 - y0
    if label is None:
        scale = 64 / width
        top = 54 - height * scale / 2
    else:
        scale = 52 / width
        cap, gap = 9.0, 6.5
        top = (108 - (height * scale + gap + cap)) / 2
    dx = 54 - (x0 + width / 2) * scale
    paths = [_path_data(stripe, scale, dx, top - y0 * scale) for stripe in MARK]
    if label is not None:
        letters, text_width = _word(label)
        size = cap / 10
        left = 54 - text_width * size / 2
        baseline_top = top + height * scale + gap
        paths += [_path_data(commands, size, left + gx * size, baseline_top)
                  for gx, commands in letters]
    return paths


def vector_drawable(paths):
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!-- Generated by scripts/launcher_icons.py; edit that script instead. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="108dp" android:height="108dp"',
        '    android:viewportWidth="108" android:viewportHeight="108">',
        '    <path android:fillColor="@color/launcher_background" android:pathData="M0,0h108v108h-108z" />',
    ]
    lines += [f'    <path android:fillColor="@color/launcher_foreground"\n        android:pathData="{d}" />'
              for d in paths]
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


ICONS = {
    # Dreadnought and RHIB share the plain mark; their colors differ by flavor.
    Path("app/src/main/res/drawable/ic_launcher.xml"): None,
    Path("app/src/rhibControl/res/drawable/ic_launcher.xml"): "DOMETIC",
}


def preview(path):
    tiles = [("Dreadnought", "#3B72DF", None), ("RHIB", "#F9003C", None),
             ("RHIB Control", "#F9003C", "DOMETIC")]
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" width="696" height="250" '
             'font-family="Helvetica, Arial, sans-serif">', '<rect width="696" height="250" fill="#1b1d20"/>']
    for i, (name, background, label) in enumerate(tiles):
        x = 24 + i * 224
        body = f'<rect width="108" height="108" fill="{background}"/>' + "".join(
            f'<path fill="#FFFFFF" d="{d}"/>' for d in icon_paths(label))
        parts.append(f'<svg x="{x}" y="24" width="192" height="192" viewBox="0 0 108 108">{body}</svg>')
        parts.append(f'<text x="{x + 96}" y="238" fill="#ffffff" font-size="16" text-anchor="middle">{name}</text>')
    parts.append("</svg>")
    Path(path).write_text("".join(parts) + "\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--preview", help="also write an SVG preview of all three icons")
    args = parser.parse_args()
    for target, label in ICONS.items():
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(vector_drawable(icon_paths(label)))
    if args.preview:
        preview(args.preview)


if __name__ == "__main__":
    main()
