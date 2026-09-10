#!/usr/bin/env bash
# Shared helpers for the OfferHub test scripts. Sourced, not executed.
#
# Everything goes through the gateway on 8080, because that is the only way a client can
# reach the system: the services refuse a request that arrives without the headers the
# gateway adds.

GATEWAY="${GATEWAY:-http://localhost:8080}"
CAMPAIGN_DIRECT="${CAMPAIGN_DIRECT:-http://localhost:8082}"
GAMIFICATION_DIRECT="${GAMIFICATION_DIRECT:-http://localhost:8084}"

TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$TEST_DIR/../.." && pwd)"

PASS_COUNT=0
FAIL_COUNT=0
FAILED_NAMES=""

# Demo identities. The uuids are arbitrary but fixed, so a rerun scores the same expert
# and the leaderboard stays comparable between runs.
EXPERT_ID="aaaaaaaa-1111-1111-1111-111111111111"
SUPERVISOR_ID="bbbbbbbb-2222-2222-2222-222222222222"
ADMIN_ID="eeeeeeee-5555-5555-5555-555555555555"
# SUB-0002 maps to this uuid through the deterministic rule in scripts/seed/README.md.
# It is one of the subscribers that actually has offers above the 0.60 threshold.
SUBSCRIBER_ID="c4c65416-1458-3cdd-85c9-23904a5b7fd0"
OTHER_SUBSCRIBER_ID="02d005a4-c563-3ba7-92f2-9b6de66b535c"

python_bin() {
    if command -v python >/dev/null 2>&1; then echo python; else echo python3; fi
}
PYTHON="$(python_bin)"

mint() { "$PYTHON" "$TEST_DIR/jwt_token.py" "$1" "$2" ${3:-}; }

# Bir token'in imzasini bozar.
#
# Imzanin ILK karakteri degistiriliyor, sonuncusu degil. Sebebi base64url dolgusu: 32
# baytlik bir HMAC 43 karaktere sigiyor ve son karakter yalnizca 4 anlamli bit tasiyor,
# kalan 2 bit dolgu. Yani U, V, W ve X ayni bayta cozuluyor. Son karakteri X yapan bir test,
# token zaten U ile bitiyorsa metni degistirir ama imzayi degistirmez ve gecerli bir token
# uretir. Ilk karakterde alti bitin hepsi anlamli, degistirmek imzayi her zaman bozar.
tamper() {
    local token="$1"
    local head="${token%.*}"
    local sig="${token##*.}"
    local first="${sig:0:1}"
    local replacement="A"
    [ "$first" = "A" ] && replacement="B"
    printf '%s.%s%s' "$head" "$replacement" "${sig:1}"
}

section() {
    echo
    echo "== $1"
}

# check <name> <expected> <actual>
check() {
    if [ "$2" = "$3" ]; then
        printf "  ok    %-58s %s\n" "$1" "$3"
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        printf "  FAIL  %-58s beklenen %s, alinan %s\n" "$1" "$2" "$3"
        FAIL_COUNT=$((FAIL_COUNT + 1))
        FAILED_NAMES="$FAILED_NAMES\n    $1 (beklenen $2, alinan $3)"
    fi
}

# status <method> <path> <token> [body]
status() {
    local method="$1" path="$2" token="$3" body="${4:-}"
    if [ -n "$body" ]; then
        curl -s -o /dev/null -w '%{http_code}' -X "$method" "$GATEWAY$path" \
            -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "$body"
    else
        curl -s -o /dev/null -w '%{http_code}' -X "$method" "$GATEWAY$path" \
            -H "Authorization: Bearer $token"
    fi
}

# body <method> <path> <token> [body]
body() {
    local method="$1" path="$2" token="$3" payload="${4:-}"
    if [ -n "$payload" ]; then
        curl -s -X "$method" "$GATEWAY$path" \
            -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "$payload"
    else
        curl -s -X "$method" "$GATEWAY$path" -H "Authorization: Bearer $token"
    fi
}

# Reads one field out of a JSON response. Written with python because jq is not something
# we can assume on a Windows machine, and every developer here already has python for the
# AI service.
# json_field <json> <expression on the parsed object, e.g. data.status>
json_field() {
    printf '%s' "$1" | "$PYTHON" -c '
import json, sys
doc = json.load(sys.stdin)
for part in sys.argv[1].split("."):
    if part.isdigit():
        doc = doc[int(part)]
    else:
        doc = doc.get(part) if isinstance(doc, dict) else None
    if doc is None:
        break
# Booleans are printed the way JSON spells them, so a check can compare against "true"
# rather than against Python-s True.
print("" if doc is None else json.dumps(doc) if isinstance(doc, bool) else doc)
' "$2"
}

require_gateway() {
    local code
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$GATEWAY/api/v1/campaigns" || echo 000)
    if [ "$code" = "000" ]; then
        echo "Gateway $GATEWAY cevap vermiyor."
        echo "Once 'docker compose up -d' calistirin, sonra 'docker info' ile motorun ayakta oldugunu dogrulayin."
        exit 1
    fi
}

summary() {
    echo
    echo "-------------------------------------------------------------"
    printf "  %s: %d gecti, %d kaldi\n" "$1" "$PASS_COUNT" "$FAIL_COUNT"
    if [ "$FAIL_COUNT" -gt 0 ]; then
        printf "  Kalanlar:%b\n" "$FAILED_NAMES"
        return 1
    fi
    return 0
}
