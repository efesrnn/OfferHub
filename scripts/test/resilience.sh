#!/usr/bin/env bash
# Servis kapatma testi. Case dokumaninin 12.3 adim 7'si, projenin en onemli anı:
# bir servis coktugunde geri kalanin calismaya devam ettigini kanitlamak.
#
# Sirayla ai-service, gamification-service ve rabbitmq durdurulur, her seferinde kampanya
# akisi denenir, sonra servis geri baslatilir.
#
# Kullanim:  bash scripts/test/resilience.sh
set -u

. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
require_gateway

if ! docker info >/dev/null 2>&1; then
    echo "Docker motoru cevap vermiyor, bu test docker olmadan calismaz."
    exit 1
fi

EXPERT=$(mint EXPERT "$EXPERT_ID")
SUPERVISOR=$(mint SUPERVISOR "$SUPERVISOR_ID")
VALID_UNTIL="2027-12-31T23:59:59Z"

STOPPED=""

# Script yarida kesilse bile durdurulan servis ayakta birakilmali, yoksa sistem bozuk kalir.
restore() {
    if [ -n "$STOPPED" ]; then
        echo
        echo "  $STOPPED geri baslatiliyor"
        docker start "$STOPPED" >/dev/null 2>&1
        STOPPED=""
    fi
}
trap restore EXIT INT TERM

wait_for_gateway() {
    local i=0
    while [ $i -lt 20 ]; do
        [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$GATEWAY/api/v1/campaigns" \
            -H "Authorization: Bearer $EXPERT")" = "200" ] && return 0
        i=$((i + 1))
        sleep 2
    done
    return 1
}

# Durdurulup baslatilan bir servisin actuator'unun yesile donmesini bekler.
#
# docker-compose artik bu servislerin host portlarini yayinlamiyor (gateway-bypass acigini
# kapatmanin bir parcasi), o yuzden actuator'a curl ile disaridan ulasilamiyor. Bunun yerine
# compose'un zaten takip ettigi container healthcheck durumuna bakiyoruz.
wait_for_service() {
    local container="$1" i=0
    while [ $i -lt 30 ]; do
        [ "$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null)" = "healthy" ] && return 0
        i=$((i + 1))
        sleep 2
    done
    return 1
}

# probe <etiket> : ilgili servis kapaliyken kampanya akisinin ayakta oldugunu dogrular
probe() {
    local label="$1"
    check "$label POST /campaigns" "201" "$(status POST /api/v1/campaigns "$EXPERT" \
        "{\"title\":\"Dayaniklilik $label\",\"type\":\"EK_PAKET\",\"targetSegment\":\"PASIF\",\"discountRate\":10,\"validUntil\":\"$VALID_UNTIL\"}")"
    check "$label GET /campaigns" "200" "$(status GET /api/v1/campaigns "$EXPERT")"
    check "$label GET /cases" "200" "$(status GET /api/v1/cases "$EXPERT")"
    check "$label GET /dashboard" "200" "$(status GET /api/v1/campaigns/dashboard "$SUPERVISOR")"
}

stop_service() {
    STOPPED="$1"
    docker stop "$1" >/dev/null
    sleep 3
}

start_service() {
    docker start "$1" >/dev/null
    STOPPED=""
    sleep 5
}

section "ai-service kapali"
stop_service offerhub-ai-service-1
probe "ai kapali"
# Case 5.1: AI'a ulasilamadiginda kampanya yine olusmali, BELIRSIZ ve ORTA olarak isaretlenip
# manuel kuyruga dusmeli. Kampanyayi burada ayrica olusturup alanlarini kontrol ediyoruz.
FALLBACK=$(body POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"AI kapaliyken fallback\",\"type\":\"EK_PAKET\",\"targetSegment\":\"YUKSEK_DEGER\",\"discountRate\":15,\"validUntil\":\"$VALID_UNTIL\"}")
check "AI yokken segment BELIRSIZ" "BELIRSIZ" "$(json_field "$FALLBACK" data.segment)"
check "AI yokken oncelik ORTA" "ORTA" "$(json_field "$FALLBACK" data.priority)"
check "AI yokken skor bos" "" "$(json_field "$FALLBACK" data.recommendationScore)"
check "AI yokken kampanya YENI kaliyor" "YENI" "$(json_field "$FALLBACK" data.status)"
start_service offerhub-ai-service-1

section "gamification-service kapali"
stop_service offerhub-gamification-service-1
probe "gamification kapali"
# Kendi endpoint'i cevap veremiyor ama 500 degil 503 donmeli: kapali bir bilesen ile bozuk
# bir sistem farkli iki cevaptir ve dayaniklilik gereksiniminin butun anlami bu ayrimda.
#
# BILINEN ACIK (Backend1): su an 500 donuyor. GatewayErrorHandler.isUnreachable sebep
# zincirinde ConnectException ariyor. Netty durdurulmus container icin bazen
# AnnotatedConnectException (ConnectException'in alt sinifi, yakalaniyor) bazen
# AnnotatedNoRouteToHostException (NoRouteToHostException'dan turuyor, yakalanmiyor)
# firlatiyor. Ikisinin ortak atasi java.net.SocketException, kontrol ona cevrilirse
# ikisi de kapsanir. Test bilerek 503 bekliyor, gecmesi duzeltmenin geldigini gosterir.
check "gamification endpointi 503" "503" "$(status GET /api/v1/game/profile "$EXPERT")"
start_service offerhub-gamification-service-1

section "rabbitmq kapali"
stop_service offerhub-rabbitmq-1
probe "rabbitmq kapali"
start_service offerhub-rabbitmq-1

section "Toparlanma"
# Durdurulan servislerin gercekten hazir olmasi beklenir. Aksi halde bu scriptten hemen
# sonra calistirilan bir paket, henuz acilmakta olan bir servise denk gelip yaniltici
# sekilde kaliyor.
if wait_for_gateway; then
    check "sistem geri geldi" "200" "$(status GET /api/v1/campaigns "$EXPERT")"
else
    check "sistem geri geldi" "200" "zaman asimi"
fi

if wait_for_service offerhub-gamification-service-1; then
    check "gamification tekrar hazir" "200" "$(status GET /api/v1/game/profile "$EXPERT")"
else
    check "gamification tekrar hazir" "200" "zaman asimi"
fi

# AI Service'te actuator yok, o yuzden saglik yerine davranisa bakiliyor: yeni bir kampanya
# BELIRSIZ disinda bir segment aliyorsa AI yeniden cevap veriyor demektir.
AI_BACK=$(json_field "$(body POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"Toparlanma kontrolu\",\"type\":\"EK_PAKET\",\"targetSegment\":\"PASIF\",\"discountRate\":10,\"validUntil\":\"$VALID_UNTIL\"}")" data.segment)
check "ai tekrar siniflandiriyor" "true" "$([ -n "$AI_BACK" ] && [ "$AI_BACK" != "BELIRSIZ" ] && echo true || echo "segment=$AI_BACK")"

summary "Dayaniklilik testi"
