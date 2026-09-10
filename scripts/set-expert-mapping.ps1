# AI Service'in kendi sentetik uzman kodlarini (EXP-001..EXP-005, bkz. ai-service DataSeeder)
# gercek staff hesaplarina (deneme2/3/4) baglar, boylece otomatik atama gercek hesaplara duser.
# Eslestirme, DataSeeder'daki her EXP-XXX'in specialty listesiyle en cok ortusen gercek uzmana
# gore secildi:
#   EXP-001 (RISKLI_KAYIP)              -> deneme4
#   EXP-002 (YUKSEK_DEGER)              -> deneme2 (tek YUKSEK_DEGER kapsayan)
#   EXP-003 (YENI_ABONE, PASIF)         -> deneme3
#   EXP-004 (RISKLI_KAYIP, YUKSEK_DEGER)-> deneme2
#   EXP-005 (PASIF)                     -> deneme4
#
# On kosul: repo kokunden calistir, tum servisler ayakta, deneme2/3/4 hesaplari zaten olusturulmus
# olmali (create-demo-staff-and-campaigns.ps1 + add-more-experts.ps1).

$ErrorActionPreference = "Stop"
$GATEWAY = "http://localhost:8080"
$adminPassword = "Admin123!"

function Login-Staff($email, $password) {
    $body = @{ email = $email; password = $password } | ConvertTo-Json
    return Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $body
}

Write-Host "=== Admin girisi ===" -ForegroundColor Cyan
$adminAuth = Login-Staff "admin@offerhub.com" $adminPassword
$adminToken = $adminAuth.data.accessToken

Write-Host "=== Personel listesi cekiliyor ===" -ForegroundColor Cyan
$staffList = Invoke-RestMethod -Uri "$GATEWAY/api/v1/admin/staff" -Method Get -Headers @{ Authorization = "Bearer $adminToken" }

function Find-StaffId($email) {
    $match = $staffList.data | Where-Object { $_.email -eq $email }
    if (-not $match) { throw "Personel bulunamadi: $email - once create-demo-staff-and-campaigns.ps1 / add-more-experts.ps1 calistirilmali" }
    return $match.id
}

$deneme2Id = Find-StaffId "deneme2@offerhub.com"
$deneme3Id = Find-StaffId "deneme3@offerhub.com"
$deneme4Id = Find-StaffId "deneme4@offerhub.com"

Write-Host "  deneme2 -> $deneme2Id"
Write-Host "  deneme3 -> $deneme3Id"
Write-Host "  deneme4 -> $deneme4Id"

$mapping = "EXP-001=$deneme4Id,EXP-002=$deneme2Id,EXP-003=$deneme3Id,EXP-004=$deneme2Id,EXP-005=$deneme4Id"
Write-Host "`n=== Olusturulan mapping ===" -ForegroundColor Cyan
Write-Host "  $mapping"

$envPath = Join-Path (Split-Path -Parent $PSScriptRoot) ".env"
if (-not (Test-Path $envPath)) { $envPath = ".\.env" }

$lines = @()
if (Test-Path $envPath) { $lines = Get-Content $envPath }
$newLine = "AI_EXPERT_MAPPING=$mapping"
if ($lines -match "^AI_EXPERT_MAPPING=") {
    $lines = $lines -replace "^AI_EXPERT_MAPPING=.*", $newLine
} else {
    $lines += $newLine
}
Set-Content -Path $envPath -Value $lines

Write-Host "`n=== .env guncellendi: $envPath ===" -ForegroundColor Green
Write-Host "Devam etmek icin campaign-service'i yeniden baslat:"
Write-Host "  docker compose up -d campaign-service"
