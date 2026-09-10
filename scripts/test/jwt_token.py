"""Mints the JWTs the test scripts call the gateway with.

The gateway is the only thing that verifies a token, and it verifies it against
JWT_SECRET from the repo root .env. Signing here with that same secret gives a token
the gateway accepts without anyone having to log in first, which keeps the test
scripts independent of Identity Service being seeded.

Usage:  python jwt_token.py <ROLE> <USER_ID> [--expired | --refresh | --no-role]
"""

import base64
import hashlib
import hmac
import json
import os
import sys
import time

ENV_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".env")


def secret():
    with open(ENV_FILE, encoding="utf-8") as handle:
        for line in handle:
            if line.startswith("JWT_SECRET="):
                return base64.b64decode(line.split("=", 1)[1].strip())
    raise SystemExit("JWT_SECRET not found in " + os.path.normpath(ENV_FILE))


def b64(raw):
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()


def mint(subject, role, ttl=3600, token_type="access", issued_at=None):
    now = int(time.time()) if issued_at is None else issued_at
    header = b64(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
    claims = {"sub": subject, "type": token_type, "iat": now, "exp": now + ttl}
    # role omitted entirely when None: a correctly signed token can still be unusable, and
    # the gateway has to answer that with 401 rather than failing on a null.
    if role is not None:
        claims["role"] = role
    payload = b64(json.dumps(claims, separators=(",", ":")).encode())
    signing_input = f"{header}.{payload}".encode()
    signature = b64(hmac.new(secret(), signing_input, hashlib.sha256).digest())
    return f"{header}.{payload}.{signature}"


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)

    role, user_id = sys.argv[1], sys.argv[2]

    if "--no-role" in sys.argv:
        print(mint(user_id, None, ttl=900))
    elif "--refresh" in sys.argv:
        # Signed with the same secret as an access token, so only the type claim tells them
        # apart. The security script uses it to check the gateway looks at that claim.
        print(mint(user_id, role, ttl=604800, token_type="refresh"))
    elif "--expired" in sys.argv:
        # Issued two hours ago with the normal 15 minute life, so it is genuinely expired
        # rather than malformed. The security script uses it to check the gateway looks at
        # exp and not just at the signature.
        print(mint(user_id, role, ttl=900, issued_at=int(time.time()) - 7200))
    else:
        print(mint(user_id, role, ttl=3600))
