# Faz 3: deneme2 tek basina her seyi tasimasin diye 2 uzman daha ekler (deneme3: EK_PAKET/
# SADAKAT uzmani, deneme4: CIHAZ_FIRSATI/TARIFE_YUKSELTME uzmani), ikisine de kendi
# uzmanliklarinda kampanya actirip TAMAMLANDI'ya kadar goturur - bazisinda AI'nin
# siniflandirmasini duzeltip olculebilir bir conversion lift birakiyor, bazisinda notla
# yetiniyor. Boylece supervisor dashboard'undaki "expert performance" karsilastirmasi
# (kim ne kadar tamamlamis, ortalama lift ne) tek kisilik degil, gercek bir karsilastirma
# olsun diye. Ayrica devam eden 2 case'i (000016, 000017) deneme2'den bu iki yeni uzmana
# devrediyor, boylece "in progress" kuyruklar da tek kisiye yigilmasin.
#
# On kosul: create-demo-staff-and-campaigns.ps1 ve progress-demo-cases.ps1 daha once
# calismis olmali (CMP-2026-000016/000017 acik durumda olmali).

$ErrorActionPreference = "Stop"
$GATEWAY = "http://localhost:8080"
$password = "Admin123!"

function Login-Staff($email, $password) {
    $body = @{ email = $email; password = $password } | ConvertTo-Json
    return Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $body
}

Write-Host "=== Admin girisi ===" -ForegroundColor Cyan
$adminAuth = Login-Staff "admin@offerhub.com" $password
$adminToken = $adminAuth.data.accessToken

function New-DemoStaff($email, $firstName, $lastName, $role, $specialties, $regions) {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $body = @{ firstName = $firstName; lastName = $lastName; email = $email; role = $role; specialties = $specialties; regions = $regions } | ConvertTo-Json
    try {
        $resp = Invoke-RestMethod -Uri "$GATEWAY/api/v1/admin/staff" -Method Post -ContentType "application/json" -Headers $headers -Body $body
        $tempPassword = $resp.data.tempPassword
        $tempAuth = Login-Staff $email $tempPassword
        $changeBody = @{ currentPassword = $tempPassword; newPassword = $password } | ConvertTo-Json
        Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/change-password" -Method Post -ContentType "application/json" `
            -Headers @{ Authorization = "Bearer $($tempAuth.data.accessToken)" } -Body $changeBody | Out-Null
        Write-Host "  $email olusturuldu, sifre $password" -ForegroundColor Green
    } catch {
        Write-Host "  $email zaten var olabilir, atlaniyor." -ForegroundColor Yellow
    }
}

Write-Host "`n=== Yeni uzmanlar ===" -ForegroundColor Cyan
New-DemoStaff "deneme3@offerhub.com" "Deneme" "Uzman3" "EXPERT" @("EK_PAKET","SADAKAT") @("Ankara")
New-DemoStaff "deneme4@offerhub.com" "Deneme" "Uzman4" "EXPERT" @("CIHAZ_FIRSATI","TARIFE_YUKSELTME") @("Izmir")

Write-Host "`n=== Girisler ===" -ForegroundColor Cyan
$supToken = (Login-Staff "deneme1@offerhub.com" $password).data.accessToken
$e3Auth = Login-Staff "deneme3@offerhub.com" $password
$e3Token = $e3Auth.data.accessToken; $e3Id = $e3Auth.data.user.id
$e4Auth = Login-Staff "deneme4@offerhub.com" $password
$e4Token = $e4Auth.data.accessToken; $e4Id = $e4Auth.data.user.id
Write-Host "  deneme3 id: $e3Id"
Write-Host "  deneme4 id: $e4Id"

function Assign-Case($caseId, $expertId, $token) {
    $body = @{ expertId = $expertId } | ConvertTo-Json
    Invoke-RestMethod -Uri "$GATEWAY/api/v1/cases/$caseId/assign" -Method Post -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer $token" } -Body $body | Out-Null
}
function Move-Case($caseId, $status, $token, $note = $null) {
    $body = @{ targetStatus = $status; optimizationNote = $note } | ConvertTo-Json
    Invoke-RestMethod -Uri "$GATEWAY/api/v1/cases/$caseId/status" -Method Patch -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer $token" } -Body $body | Out-Null
}
function Reclassify($campaignNo, $token, $segment = $null, $type = $null, $reason) {
    $obj = @{ reason = $reason }
    if ($segment) { $obj.segment = $segment }
    if ($type) { $obj.type = $type }
    $body = $obj | ConvertTo-Json
    Invoke-RestMethod -Uri "$GATEWAY/api/v1/campaigns/$campaignNo/classification" -Method Patch -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer $token" } -Body $body | Out-Null
}
function New-Campaign($token, $title, $type, $segment, $discount, $daysValid) {
    $headers = @{ Authorization = "Bearer $token" }
    $body = @{ title = $title; type = $type; targetSegment = $segment; discountRate = $discount
        validUntil = (Get-Date).ToUniversalTime().AddDays($daysValid).ToString("yyyy-MM-ddTHH:mm:ss'Z'") } | ConvertTo-Json
    return Invoke-RestMethod -Uri "$GATEWAY/api/v1/campaigns" -Method Post -ContentType "application/json" -Headers $headers -Body $body
}

Write-Host "`n=== deneme3'un kendi kampanyalari (EK_PAKET / SADAKAT) ===" -ForegroundColor Cyan
$c1 = (New-Campaign $e3Token "Riskli Kayıp - Ek Paket Fırsatı" "EK_PAKET" "RISKLI_KAYIP" 20 30).data
$c2 = (New-Campaign $e3Token "Pasif - Ek Paket Denemesi" "EK_PAKET" "PASIF" 15 30).data
$c3 = (New-Campaign $e3Token "Yeni Abone - Sadakat Başlangıcı" "SADAKAT" "YENI_ABONE" 10 45).data
Write-Host "  $($c1.campaignNo) status=$($c1.status) prob=$($c1.conversionProbability)"
Write-Host "  $($c2.campaignNo) status=$($c2.status) prob=$($c2.conversionProbability)"
Write-Host "  $($c3.campaignNo) status=$($c3.status) prob=$($c3.conversionProbability)"

Write-Host "`n=== deneme4'un kendi kampanyalari (CIHAZ_FIRSATI / TARIFE_YUKSELTME) ===" -ForegroundColor Cyan
$c4 = (New-Campaign $e4Token "Riskli Kayıp - Tarife Teklifi" "TARIFE_YUKSELTME" "RISKLI_KAYIP" 20 30).data
$c5 = (New-Campaign $e4Token "Riskli Kayıp - Cihaz Fırsatı" "CIHAZ_FIRSATI" "RISKLI_KAYIP" 30 30).data
$c6 = (New-Campaign $e4Token "Pasif - Tarife Yükseltme Fırsatı" "TARIFE_YUKSELTME" "PASIF" 15 30).data
Write-Host "  $($c4.campaignNo) status=$($c4.status) prob=$($c4.conversionProbability)"
Write-Host "  $($c5.campaignNo) status=$($c5.status) prob=$($c5.conversionProbability)"
Write-Host "  $($c6.campaignNo) status=$($c6.status) prob=$($c6.conversionProbability)"

Start-Sleep -Milliseconds 500
$allCases = (Invoke-RestMethod -Uri "$GATEWAY/api/v1/cases?size=100" -Method Get -Headers @{ Authorization = "Bearer $supToken" }).data.items
function CaseFor($campaignNo) { ($allCases | Where-Object { $_.campaignNo -eq $campaignNo }).caseId }

Write-Host "`n=== deneme3 case'lerini tamamliyor ===" -ForegroundColor Cyan
if ($c1.status -eq "YENI") {
    $id = CaseFor $c1.campaignNo
    Assign-Case $id $e3Id $supToken
    Move-Case $id "OPTIMIZE_EDILIYOR" $e3Token
    Move-Case $id "TAMAMLANDI" $e3Token "Şikayet geçmişi incelendi, mevcut sınıflandırma doğru bulundu, indirim oranı revize edildi."
    Write-Host "  $($c1.campaignNo) -> TAMAMLANDI (duzeltmesiz, lift 0 beklenir)"
}
if ($c2.status -eq "YENI") {
    $id = CaseFor $c2.campaignNo
    Assign-Case $id $e3Id $supToken
    Reclassify $c2.campaignNo $e3Token "YENI_ABONE" $null "Abone 5 aydır sistemde, AI'nin PASIF etiketi kullanım geçmişiyle uyuşmuyor."
    Move-Case $id "OPTIMIZE_EDILIYOR" $e3Token
    Move-Case $id "TAMAMLANDI" $e3Token "Segment düzeltmesi sonrası teklif yeniden hesaplandı."
    Write-Host "  $($c2.campaignNo) -> TAMAMLANDI (segment PASIF->YENI_ABONE duzeltildi, olculebilir lift beklenir)"
}
if ($c3.status -eq "YENI") {
    $id = CaseFor $c3.campaignNo
    Assign-Case $id $e3Id $supToken
    Reclassify $c3.campaignNo $e3Token $null "EK_PAKET" "Yeni abonede sadakat programından önce ek paket denemesi daha yüksek dönüşüm getiriyor."
    Move-Case $id "OPTIMIZE_EDILIYOR" $e3Token
    Move-Case $id "TAMAMLANDI" $e3Token "Kampanya türü EK_PAKET'e çevrildi, dönüşüm tahmini yükseldi."
    Write-Host "  $($c3.campaignNo) -> TAMAMLANDI (tur SADAKAT->EK_PAKET duzeltildi, olculebilir lift beklenir)"
}

Write-Host "`n=== deneme4 case'lerini tamamliyor ===" -ForegroundColor Cyan
if ($c4.status -eq "YENI") {
    $id = CaseFor $c4.campaignNo
    Assign-Case $id $e4Id $supToken
    Move-Case $id "OPTIMIZE_EDILIYOR" $e4Token
    Move-Case $id "TAMAMLANDI" $e4Token "Teklif metni netleştirildi, sınıflandırma değiştirilmedi."
    Write-Host "  $($c4.campaignNo) -> TAMAMLANDI (duzeltmesiz, lift 0 beklenir)"
}
if ($c5.status -eq "YENI") {
    $id = CaseFor $c5.campaignNo
    Assign-Case $id $e4Id $supToken
    Reclassify $c5.campaignNo $e4Token "PASIF" $null "Abone görüşmesinde kayıp riski değil düşük kullanım tespit edildi, segment PASIF'e çekildi."
    Move-Case $id "OPTIMIZE_EDILIYOR" $e4Token
    Move-Case $id "TAMAMLANDI" $e4Token "Segment düzeltmesi sonrası cihaz fırsatı yeniden fiyatlandı."
    Write-Host "  $($c5.campaignNo) -> TAMAMLANDI (segment RISKLI_KAYIP->PASIF duzeltildi, olculebilir lift beklenir)"
}
if ($c6.status -eq "YENI") {
    $id = CaseFor $c6.campaignNo
    Assign-Case $id $e4Id $supToken
    Reclassify $c6.campaignNo $e4Token $null "EK_PAKET" "Pasif abonede tarife yükseltmek yerine küçük ek paket denemek daha gerçekçi."
    Move-Case $id "OPTIMIZE_EDILIYOR" $e4Token
    Move-Case $id "TAMAMLANDI" $e4Token "Kampanya türü EK_PAKET'e çevrildi."
    Write-Host "  $($c6.campaignNo) -> TAMAMLANDI (tur TARIFE_YUKSELTME->EK_PAKET duzeltildi, olculebilir lift beklenir)"
}

Write-Host "`n=== 000016 ve 000017 devrediliyor ===" -ForegroundColor Cyan
$id16 = CaseFor "CMP-2026-000016"
Assign-Case $id16 $e3Id $supToken
Write-Host "  CMP-2026-000016 -> deneme3'e devredildi (ATANDI olarak kalir)"
$id17 = CaseFor "CMP-2026-000017"
Assign-Case $id17 $e4Id $supToken
Write-Host "  CMP-2026-000017 -> deneme4'e devredildi (OPTIMIZE_EDILIYOR olarak kalir)"

Write-Host "`n=== Supervisor dashboard: expert performance ===" -ForegroundColor Cyan
$dash = Invoke-RestMethod -Uri "$GATEWAY/api/v1/campaigns/dashboard" -Method Get -Headers @{ Authorization = "Bearer $supToken" }
$dash.data.expertPerformance | Format-Table -AutoSize
