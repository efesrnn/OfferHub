# Faz 2: acilan 8 case'i her CaseStatus'u (YENI haric zaten var) gosterecek sekilde
# ilerletir, hepsini deneme2 (expert)'e atar (biri haric, kuyrukta unassigned kalsin diye),
# ayrica bir segment override (madde 5 ile ayni ozellik) ve bir supervisor KRITIK priority
# ornegi ekler.
#
# On kosul: create-demo-staff-and-campaigns.ps1 calisip CMP-2026-000013..024 arasi
# kampanyalari acmis olmali.

$ErrorActionPreference = "Stop"
$GATEWAY = "http://localhost:8080"
$password = "Admin123!"

function Login-Staff($email, $password) {
    $body = @{ email = $email; password = $password } | ConvertTo-Json
    return Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $body
}

Write-Host "=== Giris ===" -ForegroundColor Cyan
$supAuth = Login-Staff "deneme1@offerhub.com" $password
$supToken = $supAuth.data.accessToken
$expAuth = Login-Staff "deneme2@offerhub.com" $password
$expToken = $expAuth.data.accessToken
$expId = $expAuth.data.user.id
Write-Host "  supervisor ve expert token alindi, expId=$expId"

function Get-CaseIdFor($campaignNo, $token) {
    $resp = Invoke-RestMethod -Uri "$GATEWAY/api/v1/cases?size=100" -Method Get -Headers @{ Authorization = "Bearer $token" }
    $match = $resp.data.items | Where-Object { $_.campaignNo -eq $campaignNo }
    if (-not $match) { throw "Case bulunamadi: $campaignNo" }
    return $match.caseId
}

function Assign-Case($caseId) {
    $body = @{ expertId = $expId } | ConvertTo-Json
    Invoke-RestMethod -Uri "$GATEWAY/api/v1/cases/$caseId/assign" -Method Post -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer $supToken" } -Body $body | Out-Null
}

function Move-Case($caseId, $status, $token, $note = $null) {
    $body = @{ targetStatus = $status; optimizationNote = $note } | ConvertTo-Json
    Invoke-RestMethod -Uri "$GATEWAY/api/v1/cases/$caseId/status" -Method Patch -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer $token" } -Body $body | Out-Null
}

Write-Host "`n=== Case ID'leri cekiliyor ===" -ForegroundColor Cyan
$campaignNos = @("CMP-2026-000015","CMP-2026-000016","CMP-2026-000017","CMP-2026-000018",
                 "CMP-2026-000019","CMP-2026-000021","CMP-2026-000023","CMP-2026-000024")
$caseIds = @{}
foreach ($no in $campaignNos) {
    $caseIds[$no] = Get-CaseIdFor $no $supToken
    Write-Host "  $no -> $($caseIds[$no])"
}

Write-Host "`n=== 000015: YENI'de birakiliyor (atanmamis kuyruk ornegi) ===" -ForegroundColor Cyan
# hicbir islem yok

Write-Host "=== 000016: ATANDI ===" -ForegroundColor Cyan
Assign-Case $caseIds["CMP-2026-000016"]

Write-Host "=== 000017: ATANDI -> OPTIMIZE_EDILIYOR ===" -ForegroundColor Cyan
Assign-Case $caseIds["CMP-2026-000017"]
Move-Case $caseIds["CMP-2026-000017"] "OPTIMIZE_EDILIYOR" $expToken

Write-Host "=== 000018: ATANDI -> OPTIMIZE_EDILIYOR -> TEST_EDILIYOR ===" -ForegroundColor Cyan
Assign-Case $caseIds["CMP-2026-000018"]
Move-Case $caseIds["CMP-2026-000018"] "OPTIMIZE_EDILIYOR" $expToken
Move-Case $caseIds["CMP-2026-000018"] "TEST_EDILIYOR" $expToken

Write-Host "=== 000019: ... -> TAMAMLANDI ===" -ForegroundColor Cyan
Assign-Case $caseIds["CMP-2026-000019"]
Move-Case $caseIds["CMP-2026-000019"] "OPTIMIZE_EDILIYOR" $expToken
Move-Case $caseIds["CMP-2026-000019"] "TAMAMLANDI" $expToken "Ek paket limiti artirildi, hedef kitleye ozel indirim yeniden hesaplandi."

Write-Host "=== 000021: ... -> TAMAMLANDI -> YAYINDA ===" -ForegroundColor Cyan
Assign-Case $caseIds["CMP-2026-000021"]
Move-Case $caseIds["CMP-2026-000021"] "OPTIMIZE_EDILIYOR" $expToken
Move-Case $caseIds["CMP-2026-000021"] "TAMAMLANDI" $expToken "Sadakat programi kosullari netlestirildi, indirim orani optimize edildi."
Move-Case $caseIds["CMP-2026-000021"] "YAYINDA" $supToken

Write-Host "=== 000023: ... -> TAMAMLANDI -> YAYINDA -> ARSIVLENDI (tam yasam dongusu) ===" -ForegroundColor Cyan
Assign-Case $caseIds["CMP-2026-000023"]
Move-Case $caseIds["CMP-2026-000023"] "OPTIMIZE_EDILIYOR" $expToken
Move-Case $caseIds["CMP-2026-000023"] "TAMAMLANDI" $expToken "Tarife yukseltme teklifi pasif abonelere gore yeniden kurgulandi."
Move-Case $caseIds["CMP-2026-000023"] "YAYINDA" $supToken
Move-Case $caseIds["CMP-2026-000023"] "ARSIVLENDI" $supToken

Write-Host "=== 000024: ATANDI + expert segment override ornegi (madde 5 ile ayni ozellik) ===" -ForegroundColor Cyan
Assign-Case $caseIds["CMP-2026-000024"]
$reclassifyBody = @{ segment = "RISKLI_KAYIP"; reason = "Abone gorusmelerinde kayip riski isaretleri tespit edildi, AI siniflandirmasi PASIF olarak kalmisti." } | ConvertTo-Json
Invoke-RestMethod -Uri "$GATEWAY/api/v1/campaigns/CMP-2026-000024/classification" -Method Patch -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $expToken" } -Body $reclassifyBody | Out-Null
Write-Host "  segment PASIF -> RISKLI_KAYIP olarak duzeltildi, priority floor devreye girmis olmali"

Write-Host "`n=== 000013: dogrudan yayindaki kampanyada supervisor KRITIK oncelik ornegi ===" -ForegroundColor Cyan
$priorityBody = @{ priority = "KRITIK"; reason = "Yonetim bu kampanyayi bu ceyrekte oncelikli ilan etti." } | ConvertTo-Json
Invoke-RestMethod -Uri "$GATEWAY/api/v1/campaigns/CMP-2026-000013/classification" -Method Patch -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $supToken" } -Body $priorityBody | Out-Null

Write-Host "`n=== SON DURUM ===" -ForegroundColor Cyan
$final = Invoke-RestMethod -Uri "$GATEWAY/api/v1/cases?size=100" -Method Get -Headers @{ Authorization = "Bearer $supToken" }
$final.data.items | Select-Object campaignNo, status, priority, segment, assignedExpertId | Format-Table -AutoSize

Write-Host "`nSunum icin ozet:"
Write-Host "  CMP-2026-000015 -> YENI (atanmamis kuyruk)"
Write-Host "  CMP-2026-000016 -> ATANDI"
Write-Host "  CMP-2026-000017 -> OPTIMIZE_EDILIYOR"
Write-Host "  CMP-2026-000018 -> TEST_EDILIYOR"
Write-Host "  CMP-2026-000019 -> TAMAMLANDI"
Write-Host "  CMP-2026-000021 -> YAYINDA (case uzerinden yayinlandi)"
Write-Host "  CMP-2026-000023 -> ARSIVLENDI (tam yasam dongusu)"
Write-Host "  CMP-2026-000024 -> ATANDI + segment override ornegi (PASIF -> RISKLI_KAYIP)"
Write-Host "  CMP-2026-000013 -> case'siz yayinda kampanya + supervisor'in KRITIK oncelik duzeltmesi"
Write-Host "  CMP-2026-000014/020/022 -> case'siz, dogrudan yayinda ornekleri"
