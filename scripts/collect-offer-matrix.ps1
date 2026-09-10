# 8 test abonesinin (fix-subscriber-phones.ps1 sonrasi) gordugu teklif listelerini toplayip
# tek bir JSON dosyasina yazar - "kime ne oneriliyor" karsilastirmasini gorsellestirmek icin.
#
# On kosul: create-test-subscribers.ps1 ve fix-subscriber-phones.ps1 calismis olmali.

$ErrorActionPreference = "Stop"
$GATEWAY = "http://localhost:8080"

$subscribers = @(
    @{ phone = "5550000001"; label = "Yuksek Deger 1 (SUB-0006)" }
    @{ phone = "5550000002"; label = "Yuksek Deger 2 (SUB-0046)" }
    @{ phone = "5550000003"; label = "Riskli Kayip 1 (SUB-0010)" }
    @{ phone = "5550000004"; label = "Riskli Kayip 2 (SUB-0024)" }
    @{ phone = "5550000005"; label = "Yeni Abone 1 (SUB-0036)" }
    @{ phone = "5550000006"; label = "Yeni Abone 2 (SUB-0056)" }
    @{ phone = "5550000007"; label = "Pasif 1 (SUB-0043)" }
    @{ phone = "5550000008"; label = "Pasif 2 (SUB-0032)" }
)

$allResults = @()

foreach ($sub in $subscribers) {
    Write-Host "=== $($sub.label) ($($sub.phone)) ===" -ForegroundColor Cyan

    $otpBody = @{ authMode = "MOCK"; phone = $sub.phone } | ConvertTo-Json
    Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/otp-request" -Method Post -ContentType "application/json" -Body $otpBody | Out-Null
    Start-Sleep -Milliseconds 700

    $logs = docker compose logs identity-service --no-color --tail 300
    $match = $logs | Select-String "telefon: $($sub.phone), kod: (\d+)" | Select-Object -Last 1
    if (-not $match) {
        Write-Host "  OTP bulunamadi, atlaniyor." -ForegroundColor Red
        continue
    }
    $code = $match.Matches[0].Groups[1].Value

    $verifyBody = @{ authMode = "MOCK"; phone = $sub.phone; credential = $code } | ConvertTo-Json
    $auth = Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/otp-verify" -Method Post -ContentType "application/json" -Body $verifyBody
    $token = $auth.data.accessToken

    $offers = Invoke-RestMethod -Uri "$GATEWAY/api/v1/subscribers/me/offers" -Method Get -Headers @{ Authorization = "Bearer $token" }
    Write-Host "  $($offers.data.Count) teklif bulundu"

    foreach ($o in $offers.data) {
        $allResults += [PSCustomObject]@{
            subscriber  = $sub.label
            phone       = $sub.phone
            campaignNo  = $o.campaignNo
            title       = $o.title
            type        = $o.type
            score       = $o.score
            highlighted = $o.highlighted
            status      = $o.status
        }
    }

    Start-Sleep -Seconds 4
}

$outPath = "offer-matrix.json"
$allResults | ConvertTo-Json -Depth 5 | Out-File -Encoding utf8 $outPath
Write-Host "`nYazildi: $outPath ($($allResults.Count) satir)" -ForegroundColor Green
