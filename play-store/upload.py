#!/usr/bin/env python3
"""Upload a signed AAB to Google Play Store.

Usage:
    python3 play-store/upload.py --key /path/to/service-account.json

Requirements (install once):
    pip3 install google-api-python-client google-auth
"""

import argparse
import sys
from pathlib import Path

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]
PACKAGE_NAME = "com.autorelay.app"
DEFAULT_AAB = "app/build/outputs/bundle/release/app-release.aab"
DEFAULT_KEY = "../keystore/play-store-key.json"


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Upload a signed AAB to Google Play."
    )
    parser.add_argument(
        "--key",
        default=DEFAULT_KEY,
        help=(
            "Path to service account JSON key "
            f"(default: {DEFAULT_KEY})"
        ),
    )
    parser.add_argument(
        "--aab",
        default=DEFAULT_AAB,
        help=f"Path to the signed AAB (default: {DEFAULT_AAB})",
    )
    parser.add_argument(
        "--track",
        default="internal",
        choices=["internal", "alpha", "beta", "production"],
        help="Release track (default: internal)",
    )
    parser.add_argument(
        "--notes",
        metavar="TEXT",
        help="Release notes / What's new text (en-US)",
    )
    args = parser.parse_args()

    aab_path = Path(args.aab)
    key_path = Path(args.key)

    if not aab_path.exists():
        sys.exit(f"error: AAB not found at {aab_path}")
    if not key_path.exists():
        sys.exit(
            f"error: service account key not found at {key_path}\n"
            "See play-store/release-runbook.md § 0 for setup instructions."
        )

    credentials = service_account.Credentials.from_service_account_file(
        str(key_path), scopes=SCOPES
    )
    service = build(
        "androidpublisher", "v3", credentials=credentials,
        cache_discovery=False,
    )
    edits = service.edits()

    # Open a new edit session
    edit_id = edits.insert(packageName=PACKAGE_NAME, body={}).execute()["id"]
    print(f"Edit opened: {edit_id}")

    # Upload the bundle
    print(f"Uploading {aab_path} …")
    media = MediaFileUpload(
        str(aab_path),
        mimetype="application/octet-stream",
        resumable=True,
    )
    bundle = edits.bundles().upload(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        media_body=media,
    ).execute()
    version_code = bundle["versionCode"]
    print(f"Bundle uploaded — versionCode: {version_code}")

    # Build the release object
    release: dict = {
        "versionCodes": [str(version_code)],
        "status": "draft",
    }
    if args.notes:
        release["releaseNotes"] = [
            {"language": "en-US", "text": args.notes}
        ]

    # Assign to track as a draft
    edits.tracks().update(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        track=args.track,
        body={"releases": [release]},
    ).execute()
    print(f"Assigned to track '{args.track}' as draft")

    # Commit the edit
    edits.commit(packageName=PACKAGE_NAME, editId=edit_id).execute()
    print(
        "\nDone — the release is saved as a draft in Play Console.\n"
        "Open Play Console to add release notes and start the rollout."
    )


if __name__ == "__main__":
    main()