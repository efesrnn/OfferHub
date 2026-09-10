#!/usr/bin/env bash
# Uc test paketini sirayla calistirir ve toplu sonuc verir.
#
# Kullanim:  bash scripts/test/run-all.sh
#            bash scripts/test/run-all.sh --skip-resilience
#
# Dayaniklilik testi container durdurup baslattigi icin en uzun suren bolum. Demo
# provasindan hemen once calistirmak istemezsen --skip-resilience ile atlayabilirsin.
set -u

TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKIP_RESILIENCE=0
[ "${1:-}" = "--skip-resilience" ] && SKIP_RESILIENCE=1

FAILED=0

run_suite() {
    echo
    echo "============================================================="
    echo "  $1"
    echo "============================================================="
    bash "$TEST_DIR/$2" || FAILED=$((FAILED + 1))
}

run_suite "Islevsel testler" smoke.sh
run_suite "Guvenlik testleri" security.sh

if [ "$SKIP_RESILIENCE" -eq 1 ]; then
    echo
    echo "  Dayaniklilik testi atlandi (--skip-resilience)"
else
    run_suite "Dayaniklilik testleri" resilience.sh
fi

echo
echo "============================================================="
if [ "$FAILED" -eq 0 ]; then
    echo "  Butun paketler gecti"
else
    echo "  $FAILED pakette kalan test var, yukaridaki dokume bakin"
fi
echo "============================================================="
exit "$FAILED"
