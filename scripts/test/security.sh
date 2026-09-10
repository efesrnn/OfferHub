#!/usr/bin/env bash
# Guvenlik testleri. Case dokumaninin 11. bolumunde mentorlerin deneyecegi soylenen
# senaryolarin her biri burada. Sonuclarin yorumu docs/GUVENLIK-TESTLERI.md dosyasinda.
#
# Kullanim:  bash scripts/test/security.sh
set -u

. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
require_gateway

EXPERT=$(mint EXPERT "$EXPERT_ID")
SUPERVISOR=$(mint SUPERVISOR "$SUPERVISOR_ID")
SUBSCRIBER=$(mint SUBSCRIBER "$SUBSCRIBER_ID")
OTHER=$(mint SUBSCRIBER "$OTHER_SUBSCRIBER_ID")

VALID_UNTIL="2027-12-31T23:59:59Z"

# Tablonun sonunda hala ayakta oldugunu gosterebilmek icin once sayiyi aliyoruz.
count_campaigns() {
    docker exec offerhub-campaign-db-1 psql -U postgres -d campaign -t \
        -c 'select count(*) from campaigns;' 2>/dev/null | tr -d ' \r\n'
}
BEFORE=$(count_campaigns)

section "SQL enjeksiyonu"
# Beklenen 201: girdi veri olarak saklanmali, calistirilmamali. JPA parametreli sorgu
# kullandigi icin metin metin olarak kaliyor.
check "gövdede ' OR 1=1 --" "201" "$(status POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"' OR 1=1 --\",\"type\":\"EK_PAKET\",\"targetSegment\":\"PASIF\",\"discountRate\":10,\"validUntil\":\"$VALID_UNTIL\"}")"
check "gövdede DROP TABLE" "201" "$(status POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"x'); DROP TABLE campaigns;--\",\"type\":\"EK_PAKET\",\"targetSegment\":\"PASIF\",\"discountRate\":10,\"validUntil\":\"$VALID_UNTIL\"}")"
# Enum alanina enjeksiyon: deger enum'a cevrilemedigi icin sorguya hic ulasmiyor.
check "query parametresinde enjeksiyon" "400" "$(status GET \
    "/api/v1/campaigns?status=YENI%27%20OR%201=1--" "$EXPERT")"
check "assignedTo alanina enjeksiyon" "400" "$(status GET \
    "/api/v1/cases?assignedTo=1%27%20OR%20%271%27=%271" "$SUPERVISOR")"

if [ -n "$BEFORE" ]; then
    AFTER=$(count_campaigns)
    check "campaigns tablosu duruyor" "true" "$([ -n "$AFTER" ] && [ "$AFTER" -ge "$BEFORE" ] && echo true || echo false)"
else
    echo "  atlandi: campaigns tablosu sayimi (docker exec kullanilamadi)"
fi

section "XSS"
check "baslikta script etiketi" "400" "$(status POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"<script>alert(1)</script>\",\"type\":\"EK_PAKET\",\"targetSegment\":\"PASIF\",\"discountRate\":10,\"validUntil\":\"$VALID_UNTIL\"}")"
check "baslikta img onerror" "400" "$(status POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"<img src=x onerror=alert(1)>\",\"type\":\"EK_PAKET\",\"targetSegment\":\"PASIF\",\"discountRate\":10,\"validUntil\":\"$VALID_UNTIL\"}")"
check "optimizasyon notunda script" "400" "$(status PATCH "/api/v1/cases/00000000-0000-0000-0000-000000000000/status" "$EXPERT" \
    '{"targetStatus":"TAMAMLANDI","optimizationNote":"<script>x</script>"}')"

section "Token manipulasyonu"
check "imza bozulmus token" "401" "$(status GET /api/v1/campaigns "$(tamper "$EXPERT")")"
check "alg=none token" "401" "$(status GET /api/v1/campaigns \
    'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJhYWFhYWFhYS0xMTExLTExMTEtMTExMS1hYWFhYWFhYWFhYWEiLCJyb2xlIjoiQURNSU4ifQ.')"
check "suresi dolmus token" "401" "$(status GET /api/v1/campaigns "$(mint EXPERT "$EXPERT_ID" --expired)")"
check "bos token" "401" "$(status GET /api/v1/campaigns '')"
check "rastgele metin token" "401" "$(status GET /api/v1/campaigns 'not-a-jwt')"
# Refresh token ayni anahtarla imzali, onu access token'dan ayiran tek sey type alani.
check "refresh token API cagrisinda" "401" "$(status GET /api/v1/campaigns "$(mint EXPERT "$EXPERT_ID" --refresh)")"
# Imza dogru ama rol alani yok: filtre bunu 500 degil 401 olarak cevaplamali.
check "rol alani olmayan token" "401" "$(status GET /api/v1/campaigns "$(mint EXPERT "$EXPERT_ID" --no-role)")"

section "Yetkisiz erisim ve IDOR"
check "abone supervizor endpointi" "403" "$(status GET /api/v1/campaigns/dashboard "$SUBSCRIBER")"
check "uzman manuel atama yapamaz" "403" "$(status POST "/api/v1/cases/00000000-0000-0000-0000-000000000000/assign" "$EXPERT" \
    "{\"expertId\":\"$EXPERT_ID\"}")"
check "abone oyunlastirma profili goremez" "403" "$(status GET /api/v1/game/profile "$SUBSCRIBER")"
check "uzman liderlik tablosunu yeniden kuramaz" "403" "$(status POST /api/v1/game/leaderboard/rebuild "$EXPERT")"

# IDOR: baskasinin teklif id'sini ele gecirip okumaya calismak.
VICTIM_OFFER=$(json_field "$(body GET /api/v1/subscribers/me/offers "$SUBSCRIBER")" data.0.offerId)
if [ -n "$VICTIM_OFFER" ]; then
    check "baskasinin teklifini okuma" "403" "$(status GET "/api/v1/subscribers/me/offers/$VICTIM_OFFER" "$OTHER")"
    check "baskasinin teklifini kabul etme" "403" "$(status POST "/api/v1/subscribers/me/offers/$VICTIM_OFFER/accept" "$OTHER")"
    check "baskasinin teklifini puanlama" "403" "$(status POST "/api/v1/subscribers/me/offers/$VICTIM_OFFER/rating" "$OTHER" '{"rating":5}')"
else
    echo "  atlandi: IDOR testi icin teklif bulunamadi"
fi

section "Gateway atlatma"
# docker-compose artik bu servislerin host portlarini yayinlamiyor (kritik madde #1'in
# duzeltmesi), o yuzden burada beklenen sey 403 degil baglanti hic kurulamamasi: curl
# '%{http_code}' baglanamadiginda "000" yazar. Eskiden 403 donuyordu cunku header'siz istek
# CallerIdentityArgumentResolver tarafindan reddediliyordu - ama o kontrol imza dogrulamiyor,
# sahte X-User-Id/X-User-Role header'iyla gelen istegi kabul ederdi. Simdi o istek servise
# hic ulasamiyor, port disariya acik degil.
check "campaign servisine dogrudan (port kapali)" "000" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 $CAMPAIGN_DIRECT/api/v1/campaigns 2>/dev/null || echo 000)"
check "gamification servisine dogrudan (port kapali)" "000" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 $GAMIFICATION_DIRECT/api/v1/game/profile 2>/dev/null || echo 000)"
# Asil kritik olan senaryo buydu: sahte header'la dogrudan servise gitmek. Port kapali oldugu
# icin artik denenemiyor bile, ki tam olarak istenen sonuc bu.
check "campaign servisine sahte header ile dogrudan (port kapali)" "000" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 \
    -H "X-User-Id: $EXPERT_ID" -H 'X-User-Role: ADMIN' $CAMPAIGN_DIRECT/api/v1/campaigns/dashboard 2>/dev/null || echo 000)"
check "uydurma X-User-Id basligi" "401" "$(curl -s -o /dev/null -w '%{http_code}' \
    -H "X-User-Id: $EXPERT_ID" -H 'X-User-Role: ADMIN' $GATEWAY/api/v1/campaigns)"
# Token gecerli ama rol basligi elle ADMIN'e cekilmis: gateway kendi cozdugu rolu yaziyor,
# istemcinin gonderdigi baslik dikkate alinmiyor.
check "token uzerine sahte rol basligi" "403" "$(curl -s -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer $SUBSCRIBER" -H 'X-User-Role: ADMIN' $GATEWAY/api/v1/campaigns/dashboard)"

section "Girdi dogrulama ve kaynak tuketimi"
check "size=99999 kirpiliyor" "100" "$(json_field "$(body GET '/api/v1/campaigns?page=0&size=99999' "$EXPERT")" data.size)"
check "page=-5 sifira cekiliyor" "0" "$(json_field "$(body GET '/api/v1/campaigns?page=-5&size=5' "$EXPERT")" data.page)"
check "indirim orani 101 reddedilir" "400" "$(status POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"Sinir testi\",\"type\":\"EK_PAKET\",\"targetSegment\":\"PASIF\",\"discountRate\":101,\"validUntil\":\"$VALID_UNTIL\"}")"
check "negatif indirim reddedilir" "400" "$(status POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"Sinir testi\",\"type\":\"EK_PAKET\",\"targetSegment\":\"PASIF\",\"discountRate\":-1,\"validUntil\":\"$VALID_UNTIL\"}")"
check "gecmis tarih reddedilir" "400" "$(status POST /api/v1/campaigns "$EXPERT" \
    '{"title":"Gecmis tarih","type":"EK_PAKET","targetSegment":"PASIF","discountRate":10,"validUntil":"2020-01-01T00:00:00Z"}')"
check "bilinmeyen enum degeri reddedilir" "400" "$(status POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"Enum testi\",\"type\":\"HEDIYE_PAKET\",\"targetSegment\":\"PASIF\",\"discountRate\":10,\"validUntil\":\"$VALID_UNTIL\"}")"
check "201 karakterlik baslik reddedilir" "400" "$(status POST /api/v1/campaigns "$EXPERT" \
    "{\"title\":\"$(printf 'a%.0s' $(seq 1 201))\",\"type\":\"EK_PAKET\",\"targetSegment\":\"PASIF\",\"discountRate\":10,\"validUntil\":\"$VALID_UNTIL\"}")"
check "bos govde reddedilir" "400" "$(status POST /api/v1/campaigns "$EXPERT" '{}')"

section "Brute force"
# Gateway rate limit'i yalnizca token gerektirmeyen yollara uygular, cunku savundugu sey
# sifre ve OTP denemesi. Varsayilan 10 saniyede 10 istek.
LIMITED=0
for _ in $(seq 1 20); do
    code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GATEWAY/api/v1/auth/login" \
        -H 'Content-Type: application/json' -d '{"email":"deneme@offerhub.local","password":"yanlis"}')
    [ "$code" = "429" ] && LIMITED=$((LIMITED + 1))
done
check "ardisik giris denemesi 429 aliyor" "true" "$([ "$LIMITED" -gt 0 ] && echo true || echo false)"

summary "Guvenlik testi"
