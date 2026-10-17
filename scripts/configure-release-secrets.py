#!/usr/bin/env python3
"""Interactively validate and upload signing inputs without echoing passwords."""
import base64
import getpass
import os
from pathlib import Path
import subprocess
import sys

REPOSITORY = "JayDev-Lee/step-weather-locker"


def main():
    if not sys.stdin.isatty():
        sys.exit("Run this script in an interactive terminal; passwords are entered with echo disabled.")
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "~/jaydev-keystore.jks").expanduser().resolve()
    if not path.is_file():
        sys.exit(f"Keystore not found: {path}")
    env = os.environ.copy()
    # Scope authentication to this process without changing the globally active gh account.
    token = subprocess.run(
        ["gh", "auth", "token", "--hostname", "github.com", "--user", "JayDev-Lee"],
        capture_output=True, text=True, check=True,
    ).stdout.strip()
    env["GH_TOKEN"] = token
    password = getpass.getpass("Keystore password: ")
    if not password:
        sys.exit("Keystore password must not be empty.")
    env["HARURING_STORE_PASSWORD"] = password
    listing = subprocess.run(
        ["keytool", "-list", "-keystore", str(path), "-storepass:env", "HARURING_STORE_PASSWORD"],
        env=env, capture_output=True, text=True,
    )
    if listing.returncode:
        sys.exit("Could not open the keystore. Check the password and installed JDK.")
    print(listing.stdout)  # Public certificate metadata and aliases only.
    alias = input("Signing key alias (listed above): ").strip()
    if not alias:
        sys.exit("Alias must not be empty.")
    key_password = getpass.getpass("Key password (Enter = keystore password): ") or password
    env["HARURING_KEY_PASSWORD"] = key_password
    # A CSR proves the selected alias is a private key and validates its password.
    validation = subprocess.run(
        ["keytool", "-certreq", "-alias", alias, "-keystore", str(path),
         "-storepass:env", "HARURING_STORE_PASSWORD", "-keypass:env", "HARURING_KEY_PASSWORD"],
        env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    if validation.returncode:
        sys.exit("Could not use this signing key. Check alias and key password. No secrets were uploaded.")
    secrets = {
        "ANDROID_KEYSTORE_BASE64": base64.b64encode(path.read_bytes()).decode("ascii"),
        "ANDROID_KEYSTORE_PASSWORD": password,
        "ANDROID_KEY_ALIAS": alias,
        "ANDROID_KEY_PASSWORD": key_password,
    }
    for name, value in secrets.items():
        subprocess.run(["gh", "secret", "set", name, "--repo", REPOSITORY],
                       input=value, text=True, env=env, check=True)
        print(f"Registered {name}")
    print("Signing secrets registered. Push a vMAJOR.MINOR.PATCH tag to publish an APK.")


if __name__ == "__main__":
    main()
