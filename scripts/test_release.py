import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import release


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.commit = "a" * 40
        for flavor in release.PORTALS:
            for kind in ("release", "debug"):
                directory = self.root / "app/build/outputs/apk" / flavor / kind
                directory.mkdir(parents=True)
                (directory / "app.apk").write_bytes(f"{flavor}-{kind}".encode())
                version = "0.2.1+build.42" + ("-debug" if kind == "debug" else "")
                (directory / "output-metadata.json").write_text(json.dumps({"elements": [{
                    "filters": [], "versionName": version, "versionCode": 1042,
                    "outputFile": "app.apk"}]}))

    def bundle(self):
        release.package(self.root, self.commit)
        return self.root / "dist/release"

    def test_packages_all_six_variants_and_checksums(self):
        output = self.bundle()
        self.assertEqual(len(list(output.glob("*.apk"))), 6)
        self.assertTrue((output / "rhib-control-0.2.1+build.42-unsigned.apk").is_file())
        for line in (output / "SHA256SUMS").read_text().splitlines():
            digest, name = line.split("  ")
            self.assertEqual(release.digest(output / name), digest)
        self.assertEqual(json.loads((output / "release.json").read_text())["commit"], self.commit)

    def test_missing_apk_prevents_packaging(self):
        (self.root / "app/build/outputs/apk/rhib/debug/app.apk").unlink()
        with self.assertRaisesRegex(ValueError, "Missing APK"):
            self.bundle()
        self.assertFalse((self.root / "dist/release").exists())

    def test_inconsistent_version_prevents_packaging(self):
        path = self.root / "app/build/outputs/apk/rhib/debug/output-metadata.json"
        path.write_text(path.read_text().replace("1042", "1043"))
        with self.assertRaisesRegex(ValueError, "share a version"):
            self.bundle()

    @patch("release.gh")
    def test_publish_waits_for_upload(self, gh):
        self.bundle()
        gh.side_effect = [subprocess.CalledProcessError(1, "gh"), None, None, None]
        release.publish(self.root, self.commit)
        self.assertEqual([call.args[1] for call in gh.call_args_list],
                         ["view", "create", "upload", "edit"])
        self.assertIn("--draft", gh.call_args_list[1].args)
        self.assertIn(self.commit, gh.call_args_list[1].args)
        self.assertEqual(sum(arg.endswith(".apk") for arg in gh.call_args_list[2].args), 6)

    @patch("release.gh")
    def test_retry_finishes_existing_draft(self, gh):
        self.bundle()
        gh.return_value = subprocess.CompletedProcess([], 0, json.dumps({
            "isDraft": True, "targetCommitish": self.commit}))
        release.publish(self.root, self.commit)
        self.assertEqual([call.args[1] for call in gh.call_args_list], ["view", "upload", "edit"])

    @patch("release.gh")
    def test_upload_failure_keeps_draft(self, gh):
        self.bundle()
        gh.return_value = subprocess.CompletedProcess([], 0, json.dumps({
            "isDraft": True, "targetCommitish": self.commit}))
        gh.side_effect = [gh.return_value, subprocess.CalledProcessError(1, "gh")]
        with self.assertRaises(subprocess.CalledProcessError):
            release.publish(self.root, self.commit)
        self.assertEqual([call.args[1] for call in gh.call_args_list], ["view", "upload"])

    @patch("release.gh")
    def test_published_release_is_not_replaced(self, gh):
        self.bundle()
        gh.return_value = subprocess.CompletedProcess([], 0, json.dumps({
            "isDraft": False, "targetCommitish": self.commit}))
        release.publish(self.root, self.commit)
        self.assertEqual(gh.call_count, 1)

    @patch("release.gh")
    def test_existing_release_cannot_change_commit(self, gh):
        self.bundle()
        gh.return_value = subprocess.CompletedProcess([], 0, json.dumps({
            "isDraft": True, "targetCommitish": "b" * 40}))
        with self.assertRaisesRegex(ValueError, "different commit"):
            release.publish(self.root, self.commit)
        self.assertEqual(gh.call_count, 1)

    @patch("release.gh")
    def test_wrong_commit_or_corrupt_bundle_never_publishes(self, gh):
        output = self.bundle()
        with self.assertRaisesRegex(ValueError, "workflow commit"):
            release.publish(self.root, "b" * 40)
        next(output.glob("*.apk")).write_bytes(b"corrupt")
        with self.assertRaisesRegex(ValueError, "checksum mismatch"):
            release.publish(self.root, self.commit)
        gh.assert_not_called()
