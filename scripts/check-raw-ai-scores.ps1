# Offer listesinde hic teklif gormeyen 4 test abonesinin baglandigi AI profillerine,
# 4 kampanya turunun hepsi icin dogrudan ai-service'ten skor sorar - gercekten hepsi
# 0.60 esiginin altinda mi, yoksa baska bir sorun mu var gorelim.

$ErrorActionPreference = "Stop"
$GATEWAY = "http://localhost:8080"

$loginBody = @{ email = "deneme1@offerhub.com"; password = "Admin123!" } | ConvertTo-Json
$auth = Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$headers = @{ Authorization = "Bearer $($auth.data.accessToken)" }

$profiles = @("SUB-0010", "SUB-0024", "SUB-0036", "SUB-0056")
$types = @("EK_PAKET", "TARIFE_YUKSELTME", "CIHAZ_FIRSATI", "SADAKAT")

foreach ($p in $profiles) {
    Write-Host "=== $p ===" -ForegroundColor Cyan
    foreach ($t in $types) {
        $body = @{ subscriberId = $p; campaignType = $t } | ConvertTo-Json
        try {
            $resp = Invoke-RestMethod -Uri "$GATEWAY/api/v1/ai/recommend" -Method Post -ContentType "application/json" -Headers $headers -Body $body
            $d = $resp.data
            Write-Host "  $t -> segment=$($d.segment) conversion=$($d.conversionProbability) score=$($d.score)"
        } catch {
            Write-Host "  $t -> HATA: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}
