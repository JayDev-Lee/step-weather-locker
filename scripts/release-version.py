#!/usr/bin/env python3
"""Convert a stable vMAJOR.MINOR.PATCH tag into Android version metadata."""
import re
import sys


def versions(tag: str) -> tuple[str, int]:
    match = re.fullmatch(r"v(0|[1-9][0-9]*)\.(0|[1-9][0-9]{0,2})\.(0|[1-9][0-9]{0,2})", tag)
    if not match:
        raise ValueError("Use a stable vMAJOR.MINOR.PATCH tag; minor and patch must be 0–999.")
    major, minor, patch = map(int, match.groups())
    code = major * 1_000_000 + minor * 1_000 + patch
    if not 1 <= code <= 2_100_000_000:
        raise ValueError("Tag produces a versionCode outside Android's supported range.")
    return tag[1:], code


if __name__ == "__main__":
    try:
        name, code = versions(sys.argv[1])
    except (ValueError, IndexError) as error:
        sys.exit(str(error))
    print(f"RELEASE_VERSION_NAME={name}\nRELEASE_VERSION_CODE={code}")
