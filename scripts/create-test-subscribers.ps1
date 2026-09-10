# Bagimsiz test abone hesaplari olusturur: gercek /register + /otp-verify akisiyla
# hesap acar, sonra campaign-service'in subscriber_projection tablosuna AI'nin
# tanidigi 220 profilden birini (externalRef) baglar - boylece hesap gercek/bagimsiz
# olur ama gercek, cesitlendirilmis AI skorlama/highlight davranisi gosterir.
#
# On kosul: docker compose ile tum servisler ayakta olmali, bu script repo kokunden
# (docker-compose.yml'in yaninda) calistirilmali.

$ErrorActionPreference = "Stop"
$GATEWAY = "http://localhost:8080"

# phone, firstName, lastName, email, baglanacak AI profili (subscriber_profiles.csv), segment (bilgi amacli)
$accounts = @(
    @{ phone = "05550000001"; first = "Test"; last = "Yuksek1";  email = "test.yuksek1@offerhub.dev";  sub = "SUB-0006"; segment = "YUKSEK_DEGER (yuksek kullanim/harcama, 0 sikayet)" }
    @{ phone = "05550000002"; first = "Test"; last = "Yuksek2";  email = "test.yuksek2@offerhub.dev";  sub = "SUB-0046"; segment = "YUKSEK_DEGER (en uzun tenure, 10 kabul edilmis teklif)" }
    @{ phone = "05550000003"; first = "Test"; last = "Riskli1";  email = "test.riskli1@offerhub.dev";  sub = "SUB-0010"; segment = "RISKLI_KAYIP (3 sikayet, negatif trend)" }
    @{ phone = "05550000004"; first = "Test"; last = "Riskli2";  email = "test.riskli2@offerhub.dev";  sub = "SUB-0024"; segment = "RISKLI_KAYIP (13 reddedilmis teklif, 2 sikayet)" }
    @{ phone = "05550000005"; first = "Test"; last = "Yeni1";    email = "test.yeni1@offerhub.dev";    sub = "SUB-0036"; segment = "YENI_ABONE (6 ay tenure)" }
    @{ phone = "05550000006"; first = "Test"; last = "Yeni2";    email = "test.yeni2@offerhub.dev";    sub = "SUB-0056"; segment = "YENI_ABONE (4 ay tenure, 2 sikayet)" }
    @{ phone = "05550000007"; first = "Test"; last = "Pasif1";   email = "test.pasif1@offerhub.dev";   sub = "SUB-0043"; segment = "PASIF (dusuk kullanim, ekonomik tarife)" }
    @{ phone = "05550000008"; first = "Test"; last = "Pasif2";   email = "test.pasif2@offerhub.dev";   sub = "SUB-0032"; segment = "PASIF/RISKLI karisik (dusuk harcama)" }
)

$results = @()

foreach ($acc in $accounts) {
    Write-Host "`n=== $($acc.first) $($acc.last) ($($acc.phone)) -> $($acc.sub) ===" -ForegroundColor Cyan

    $registerBody = @{
        firstName = $acc.first
        lastName  = $acc.last
        phone     = $acc.phone
        email     = $acc.email
        authMode  = "MOCK"
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $registerBody | Out-Null
    } catch {
        Write-Host "  register basarisiz (belki zaten kayitli, devam ediliyor): $($_.Exception.Message)" -ForegroundColor Yellow
    }

    # Gateway /api/v1/auth/ altinda IP basina 10 saniyede 10 istek siniri uyguluyor
    # (RateLimitFilter). Hesap basina 2 istek (register+otp-verify) var, araya bekleme
    # koymazsak 5-6 hesaptan sonra 429 yemeye baslariz.
    Start-Sleep -Seconds 4

    Start-Sleep -Milliseconds 700

    $logs = docker compose logs identity-service --no-color --tail 300
    $match = $logs | Select-String "telefon: $($acc.phone), kod: (\d+)" | Select-Object -Last 1
    if (-not $match) {
        Write-Host "  OTP logda bulunamadi, atlaniyor." -ForegroundColor Red
        continue
    }
    $code = $match.Matches[0].Groups[1].Value
    Write-Host "  OTP: $code"

    $verifyBody = @{
        authMode   = "MOCK"
        phone      = $acc.phone
        credential = $code
    } | ConvertTo-Json

    $auth = Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/otp-verify" -Method Post -ContentType "application/json" -Body $verifyBody
    $subscriberId = $auth.data.user.id
    Write-Host "  subscriberId: $subscriberId"

    $sql = "INSERT INTO subscriber_projection (subscriber_id, external_ref, synced_at) VALUES ('$subscriberId', '$($acc.sub)', now()) ON CONFLICT (subscriber_id) DO UPDATE SET external_ref = EXCLUDED.external_ref, synced_at = now();"
    docker compose exec -T campaign-db psql -U postgres -d campaign -c "$sql" | Out-Null
    Write-Host "  subscriber_projection baglandi -> $($acc.sub)" -ForegroundColor Green

    $results += [PSCustomObject]@{
        Telefon = $acc.phone
        Isim    = "$($acc.first) $($acc.last)"
        AiProfili = $acc.sub
        Segment = $acc.segment
        SubscriberId = $subscriberId
    }
}

Write-Host "`n=== OZET ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize
Write-Host "`nMobil uygulamada bu telefon numaralariyla giris yapabilirsin. OTP her seferinde"
Write-Host "yeniden istenir (Giris ekrani -> telefon gir -> kod gonder), kod docker compose logs"
Write-Host "identity-service icinde 'kod:' diye gecer."
