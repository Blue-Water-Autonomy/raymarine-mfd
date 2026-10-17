"""Package verified APKs and publish them without replacing a completed release."""

import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys


PORTALS = {"rhibControl": "rhib-control", "rhib": "rhib", "dreadnought": "dreadnought"}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def package(root, commit):
    output = root / "dist/release"
    assets = []
    versions = set()
    for flavor, slug in PORTALS.items():
        for build_type in ("release", "debug"):
            directory = root / "app/build/outputs/apk" / flavor / build_type
            metadata = json.loads((directory / "output-metadata.json").read_text())
            elements = metadata["elements"]
            if len(elements) != 1 or elements[0]["filters"]:
                raise ValueError("Expected one universal APK per variant")
            element = elements[0]
            version = element["versionName"]
            if build_type == "debug":
                if not version.endswith("-debug"):
                    raise ValueError("Debug version must carry its suffix")
                version = version.removesuffix("-debug")
            if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+\+build\.[1-9][0-9]*", version):
                raise ValueError("Release packaging requires a CI build version")
            versions.add((version, element["versionCode"]))
            source = directory / element["outputFile"]
            if not source.is_file():
                raise ValueError(f"Missing APK: {source}")
            suffix = "unsigned" if build_type == "release" else "debug"
            assets.append((source, f"{slug}-{version}-{suffix}.apk"))
    if len(versions) != 1:
        raise ValueError("All APKs must share a version and version code")
    version, code = versions.pop()
    if output.exists():
        shutil.rmtree(output)
    output.mkdir(parents=True)
    for source, name in assets:
        shutil.copyfile(source, output / name)
    info = {"version": version, "versionCode": code, "commit": commit,
            "assets": {name: digest(output / name) for _, name in assets}}
    (output / "release.json").write_text(json.dumps(info, indent=2) + "\n")
    files = sorted(output.iterdir())
    (output / "SHA256SUMS").write_text("".join(f"{digest(path)}  {path.name}\n" for path in files))
    print(f"Packaged {len(assets)} APKs: {version} (versionCode {code})")


def gh(*args, **kwargs):
    return subprocess.run(["gh", *args], check=True, text=True, **kwargs)


def publish(root, commit):
    output = root / "dist/release"
    info = json.loads((output / "release.json").read_text())
    if info["commit"] != commit:
        raise ValueError("Release bundle does not match this workflow commit")
    for name, expected in info["assets"].items():
        if digest(output / name) != expected:
            raise ValueError(f"APK checksum mismatch: {name}")
    for line in (output / "SHA256SUMS").read_text().splitlines():
        expected, name = line.split("  ", 1)
        if digest(output / name) != expected:
            raise ValueError(f"Bundle checksum mismatch: {name}")
    tag = "v" + info["version"]
    try:
        existing = json.loads(gh("release", "view", tag, "--json", "isDraft,targetCommitish",
                                 capture_output=True).stdout)
    except subprocess.CalledProcessError:
        existing = None
    if existing:
        if existing["targetCommitish"] != commit:
            raise ValueError("Existing release targets a different commit")
        if not existing["isDraft"]:
            print(f"{tag} is already published; preserving its assets")
            return
    else:
        notes = output / "notes.md"
        notes.write_text(
            f"Built from {commit}. Android versionCode: {info['versionCode']}.\n\n"
            "Unsigned APKs require Raymarine approval and signing before MFD installation. "
            "Debug APKs are for emulator/Android testing only; their signing key is ephemeral "
            "and may change between CI runs. SHA256SUMS covers the APKs and release.json.\n")
        gh("release", "create", tag, "--draft", "--target", commit,
           "--title", f"Raymarine MFD {info['version']}", "--generate-notes",
           "--notes-file", str(notes))
    files = [str(output / name) for name in sorted(info["assets"])]
    files += [str(output / "release.json"), str(output / "SHA256SUMS")]
    gh("release", "upload", tag, *files, "--clobber")
    gh("release", "edit", tag, "--draft=false")


if __name__ == "__main__":
    command = {"package": package, "publish": publish}[sys.argv[1]]
    command(Path(__file__).resolve().parents[1], os.environ["GITHUB_SHA"])
