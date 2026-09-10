#!/usr/bin/env bash
# Uctan uca islevsel test. Case dokumaninin 15. bolumundeki kabul kriterlerini sirayla
# dogrular. Her istek gateway uzerinden ve gercek bir token ile gider.
#
# Kullanim:  bash scripts/test/smoke.sh
set -u

. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
require_gateway

EXPERT=$(mint EXPERT "$EXPERT_ID")
SUPERVISOR=$(mint SUPERVISOR "$SUPERVISOR_ID")
ADMIN=$(mint ADMIN "$ADMIN_ID")
SUBSCRIBER=$(mint SUBSCRIBER "$SUBSCRIBER_ID")

VALID_UNTIL="2027-12-31T23:59:59Z"
STAMP=$(date +%H%M%S)

# docker CLI her bash ortamindan gorunmeyebilir (ornegin Docker Desktop'in WSL entegrasyonu
# o dagitimda acik degilse) - boyle durumlarda ona bagli kontrolleri sert FAIL yerine
# nazikce atliyoruz, bu ortam eksikligi kodun bozuk oldugu anlamina gelmiyor.
HAS_DOCKER=0
command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1 && HAS_DOCKER=1

section "Saglik"
# Servis portlari artik host'a acik degil (gateway-bypass duzeltmesi), o yuzden
# actuator/health'e curl atamiyoruz - container healthcheck'ine bakiyoruz, resilience.sh'deki
# ile ayni yontem.
if [ "$HAS_DOCKER" = "1" ]; then
    check "campaign health" "healthy" "$(docker inspect --format='{{.State.Health.Status}}' offerhub-campaign-service-1 2>/dev/null)"
    check "gamification health" "healthy" "$(docker inspect --format='{{.State.Health.Status}}' offerhub-gamification-service-1 2>/dev/null)"
else
    echo "  atlandi: campaign/gamification health (docker CLI bu kabuktan gorunmuyor)"
fi

section "Kimlik ve yetki sinirlari"
check "token yok" "401" "$(curl -s -o /dev/null -w '%{http_code}' $GATEWAY/api/v1/campaigns)"
# Once servisin sahte basliklari reddettigini (403) dogruluyordu. Artik port disariya hic
# acik degil, o yuzden beklenen sonuc da "hicbir HTTP cevabi alinamadi" (000), 403 degil.
check "servise dogrudan istek engelli (port kapali)" "000" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 $CAMPAIGN_DIRECT/api/v1/campaigns)"
check "sahte X-User-Id basligi" "401" "$(curl -s -o /dev/null -w '%{http_code}' \
    -H "X-User-Id: $EXPERT_ID" -H 'X-User-Role: EXPERT' $GATEWAY/api/v1/campaigns)"
check "gecerli uzman token" "200" "$(status GET /api/v1/campaigns "$EXPERT")"

section "Kampanya olusturma ve AI siniflandirmasi"
LOW=$(body POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"Smoke churn $STAMP\",\"type\":\"SADAKAT\",\"targetSegment\":\"RISKLI_KAYIP\",\"discountRate\":35,\"validUntil\":\"$VALID_UNTIL\"}")
LOW_NO=$(json_field "$LOW" data.campaignNo)
LOW_SCORE=$(json_field "$LOW" data.recommendationScore)
check "kampanya olusturuldu" "true" "$(json_field "$LOW" success)"
check "RISKLI_KAYIP otomatik YUKSEK oncelik" "YUKSEK" "$(json_field "$LOW" data.priority)"
check "dusuk skor YENI kaliyor" "YENI" "$(json_field "$LOW" data.status)"

HIGH=$(body POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"Smoke cihaz $STAMP\",\"type\":\"CIHAZ_FIRSATI\",\"targetSegment\":\"PASIF\",\"discountRate\":10,\"validUntil\":\"$VALID_UNTIL\"}")
HIGH_SCORE=$(json_field "$HIGH" data.recommendationScore)
check "yuksek skor dogrudan YAYINDA" "YAYINDA" "$(json_field "$HIGH" data.status)"

# Case 15: girdi degisince cikti da degismeli, sabit cevap donen bir AI kabul edilmiyor.
if [ "$LOW_SCORE" = "$HIGH_SCORE" ]; then
    check "AI girdiye gore farkli skor uretiyor" "farkli" "ayni ($LOW_SCORE)"
else
    check "AI girdiye gore farkli skor uretiyor" "farkli" "farkli"
fi

section "Rol matrisi"
check "abone kampanya olusturamaz" "403" "$(status POST /api/v1/campaigns "$SUBSCRIBER" \
    "{\"title\":\"Yetkisiz\",\"type\":\"EK_PAKET\",\"targetSegment\":\"PASIF\",\"discountRate\":10,\"validUntil\":\"$VALID_UNTIL\"}")"
check "abone vaka listeleyemez" "403" "$(status GET /api/v1/cases "$SUBSCRIBER")"
check "uzman dashboard goremez" "403" "$(status GET /api/v1/campaigns/dashboard "$EXPERT")"
check "supervizor dashboard gorebilir" "200" "$(status GET /api/v1/campaigns/dashboard "$SUPERVISOR")"
check "admin kampanya listeleyebilir" "200" "$(status GET /api/v1/campaigns "$ADMIN")"

section "Vaka durum makinesi"
CASE_ID=$(json_field "$(body GET "/api/v1/cases?status=YENI&size=1&sort=sla" "$SUPERVISOR")" data.items.0.caseId)
if [ -z "$CASE_ID" ]; then
    echo "  atlandi: YENI durumda vaka yok"
else
    # Atama once yapiliyor: bir uzman kendisine ait olmayan vakada zaten 403 alir, o yuzden
    # gecersiz gecisi denemek icin vakanin bu uzmanin olmasi gerekiyor.
    check "supervizor vakayi atar" "200" "$(status POST "/api/v1/cases/$CASE_ID/assign" "$SUPERVISOR" \
        "{\"expertId\":\"$EXPERT_ID\"}")"
    check "gecersiz gecis 422 doner" "422" "$(status PATCH "/api/v1/cases/$CASE_ID/status" "$EXPERT" \
        '{"targetStatus":"TAMAMLANDI","optimizationNote":"atlama denemesi"}')"
    check "ATANDI -> OPTIMIZE_EDILIYOR" "200" "$(status PATCH "/api/v1/cases/$CASE_ID/status" "$EXPERT" \
        '{"targetStatus":"OPTIMIZE_EDILIYOR"}')"
    check "notsuz tamamlama reddedilir" "400" "$(status PATCH "/api/v1/cases/$CASE_ID/status" "$EXPERT" \
        '{"targetStatus":"TAMAMLANDI"}')"
    check "abone durum degistiremez" "403" "$(status PATCH "/api/v1/cases/$CASE_ID/status" "$SUBSCRIBER" \
        '{"targetStatus":"TEST_EDILIYOR"}')"
    check "notlu tamamlama" "200" "$(status PATCH "/api/v1/cases/$CASE_ID/status" "$EXPERT" \
        '{"targetStatus":"TAMAMLANDI","optimizationNote":"Indirim yukseltildi ve baslik yenilendi"}')"
fi

section "Olay tabanli puanlama"
# campaign.optimized RabbitMQ uzerinden gidiyor, tuketiciye ulasmasi icin kisa bir an gerek.
sleep 3
PROFILE=$(body GET /api/v1/game/profile "$EXPERT")
check "profil okunuyor" "true" "$(json_field "$PROFILE" success)"
POINTS=$(json_field "$PROFILE" data.totalPoints)
if [ "${POINTS:-0}" -gt 0 ] 2>/dev/null; then
    check "uzmanin puani var" "puanli" "puanli"
else
    check "uzmanin puani var" "puanli" "puan yok ($POINTS)"
fi
check "seviye hesaplaniyor" "true" "$([ -n "$(json_field "$PROFILE" data.level)" ] && echo true || echo false)"
check "gunluk liderlik" "200" "$(status GET '/api/v1/game/leaderboard?period=daily' "$EXPERT")"
check "haftalik liderlik" "200" "$(status GET '/api/v1/game/leaderboard?period=weekly' "$EXPERT")"
check "gecersiz period reddedilir" "400" "$(status GET '/api/v1/game/leaderboard?period=aylik' "$EXPERT")"
check "rozet listesi" "200" "$(status GET /api/v1/game/badges "$EXPERT")"

section "Abone teklif akisi"
OFFERS=$(body GET /api/v1/subscribers/me/offers "$SUBSCRIBER")
check "teklif listesi" "true" "$(json_field "$OFFERS" success)"
MIN_OK=$(printf '%s' "$OFFERS" | "$PYTHON" -c '
import json, sys
data = json.load(sys.stdin)["data"]
scores = [o["score"] for o in data if o.get("score") is not None]
print("true" if scores and min(scores) >= 0.60 else ("bos" if not scores else "false"))')
check "0.60 altindaki teklif gosterilmiyor" "true" "$MIN_OK"

HIGHLIGHT_OK=$(printf '%s' "$OFFERS" | "$PYTHON" -c '
import json, sys
data = json.load(sys.stdin)["data"]
bad = [o for o in data if o.get("score") is not None and o["highlighted"] != (o["score"] > 0.80)]
print("true" if not bad else "false")')
check "0.80 ustu vurgulaniyor" "true" "$HIGHLIGHT_OK"

PENDING=$(printf '%s' "$OFFERS" | "$PYTHON" -c '
import json, sys
data = json.load(sys.stdin)["data"]
pending = [o["offerId"] for o in data if o["status"] == "PENDING"]
print(pending[0] if pending else "")')

if [ -z "$PENDING" ]; then
    echo "  atlandi: bu abonenin cevaplanmamis teklifi kalmamis"
else
    OTHER=$(mint SUBSCRIBER "$OTHER_SUBSCRIBER_ID")
    check "IDOR, baskasinin teklifi" "403" "$(status GET "/api/v1/subscribers/me/offers/$PENDING" "$OTHER")"
    check "teklif kabul" "200" "$(status POST "/api/v1/subscribers/me/offers/$PENDING/accept" "$SUBSCRIBER")"
    check "ikinci kez cevaplanamaz" "409" "$(status POST "/api/v1/subscribers/me/offers/$PENDING/decline" "$SUBSCRIBER")"
    check "gecersiz puan reddedilir" "400" "$(status POST "/api/v1/subscribers/me/offers/$PENDING/rating" "$SUBSCRIBER" '{"rating":6}')"
    check "puanlama" "200" "$(status POST "/api/v1/subscribers/me/offers/$PENDING/rating" "$SUBSCRIBER" '{"rating":2}')"
    check "puanlama tek seferlik" "409" "$(status POST "/api/v1/subscribers/me/offers/$PENDING/rating" "$SUBSCRIBER" '{"rating":5}')"
fi

section "Supervizor dashboard"
DASH=$(body GET /api/v1/campaigns/dashboard "$SUPERVISOR")
for field in segmentDistribution conversionRate slaComplianceRate slaBreachedActiveCases \
             pendingQueueCount aiAccuracyRate aiClassifiedCampaigns conversionTrend expertPerformance; do
    check "dashboard alani $field" "var" \
        "$([ -n "$(json_field "$DASH" data.$field)" ] && echo var || echo yok)"
done

section "Swagger"
# Port artik host'a acik degil, o yuzden container'in kendi ici uzerinden (localhost:8080,
# icerideki app'in kendi baglandigi adres) kontrol ediyoruz - lib.sh'nin ustundeki notta
# anlatilan yontem.
if [ "$HAS_DOCKER" = "1" ]; then
    check "campaign api-docs" "200" "$(docker compose exec -T campaign-service curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/v3/api-docs 2>/dev/null)"
    check "campaign swagger-ui" "200" "$(docker compose exec -T campaign-service curl -s -o /dev/null -w '%{http_code}' -L http://localhost:8080/swagger-ui.html 2>/dev/null)"
    check "gamification api-docs" "200" "$(docker compose exec -T gamification-service curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/v3/api-docs 2>/dev/null)"
    check "gamification swagger-ui" "200" "$(docker compose exec -T gamification-service curl -s -o /dev/null -w '%{http_code}' -L http://localhost:8080/swagger-ui.html 2>/dev/null)"
else
    echo "  atlandi: Swagger kontrolleri (docker CLI bu kabuktan gorunmuyor)"
fi

summary "Islevsel test"
