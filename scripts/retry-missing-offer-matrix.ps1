# collect-offer-matrix.ps1'de OTP loga geç dusup atlanan hesaplari tekrar dener (birkaç
# kez, artan bekleme ile) ve sonuclari mevcut offer-matrix.json'a ekler.

$ErrorActionPreference = "Stop"
$GATEWAY = "http://localhost:8080"

$subscribers = @(
    @{ phone = "5550000003"; label = "Riskli Kayip 1 (SUB-0010)" }
    @{ phone = "5550000004"; label = "Riskli Kayip 2 (SUB-0024)" }
    @{ phone = "5550000005"; label = "Yeni Abone 1 (SUB-0036)" }
    @{ phone = "5550000006"; label = "Yeni Abone 2 (SUB-0056)" }
)

$newResults = @()

foreach ($sub in $subscribers) {
    Write-Host "=== $($sub.label) ($($sub.phone)) ===" -ForegroundColor Cyan

    $otpBody = @{ authMode = "MOCK"; phone = $sub.phone } | ConvertTo-Json
    Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/otp-request" -Method Post -ContentType "application/json" -Body $otpBody | Out-Null

    $code = $null
    foreach ($attempt in 1..5) {
        Start-Sleep -Milliseconds (700 * $attempt)
        $logs = docker compose logs identity-service --no-color --tail 500
        $match = $logs | Select-String "telefon: $($sub.phone), kod: (\d+)" | Select-Object -Last 1
        if ($match) {
            $code = $match.Matches[0].Groups[1].Value
            break
        }
        Write-Host "  deneme $attempt bulamadi, tekrar deniyor..." -ForegroundColor Yellow
    }
    if (-not $code) {
        Write-Host "  OTP bir turlu bulunamadi, atlaniyor." -ForegroundColor Red
        continue
    }
    Write-Host "  OTP: $code"

    $verifyBody = @{ authMode = "MOCK"; phone = $sub.phone; credential = $code } | ConvertTo-Json
    $auth = Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/otp-verify" -Method Post -ContentType "application/json" -Body $verifyBody
    $token = $auth.data.accessToken

    $offers = Invoke-RestMethod -Uri "$GATEWAY/api/v1/subscribers/me/offers" -Method Get -Headers @{ Authorization = "Bearer $token" }
    Write-Host "  $($offers.data.Count) teklif bulundu" -ForegroundColor Green

    foreach ($o in $offers.data) {
        $newResults += [PSCustomObject]@{
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

    Start-Sleep -Seconds 5
}

$existing = Get-Content "offer-matrix.json" -Raw | ConvertFrom-Json
$combined = @($existing) + $newResults
$combined | ConvertTo-Json -Depth 5 | Out-File -Encoding utf8 "offer-matrix.json"
Write-Host "`noffer-matrix.json guncellendi, toplam $($combined.Count) satir" -ForegroundColor Green
