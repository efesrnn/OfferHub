# Sunum icin: deneme1@offerhub.com (SUPERVISOR) + deneme2@offerhub.com (EXPERT) hesaplarini
# admin@offerhub.com ile olusturur, sifrelerini bilinen bir degere sabitler, sonra her 4
# segment x cesitli kampanya turlerinde 12 kampanya acar (bazisi dogrudan yayina girer,
# bazisi dusuk skorla optimizasyon kuyruguna duser - AI'nin gercek kararina gore).
#
# On kosul: repo kokunden, tum servisler ayakta, admin sifresi degismemisse varsayilan
# Admin123!. Degistiyse asagidaki $adminPassword degiskenini guncelle.

$ErrorActionPreference = "Stop"
$GATEWAY = "http://localhost:8080"
$demoPassword = "Admin123!"
$adminPassword = "Admin123!"

function Login-Staff($email, $password) {
    $body = @{ email = $email; password = $password } | ConvertTo-Json
    return Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $body
}

Write-Host "=== Admin girisi ===" -ForegroundColor Cyan
$adminAuth = Login-Staff "admin@offerhub.com" $adminPassword
$adminToken = $adminAuth.data.accessToken
Write-Host "  admin OK, mustChangePassword=$($adminAuth.data.user.mustChangePassword)"

function New-DemoStaff($email, $firstName, $lastName, $role, $specialties, $regions) {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $body = @{
        firstName   = $firstName
        lastName    = $lastName
        email       = $email
        role        = $role
        specialties = $specialties
        regions     = $regions
    } | ConvertTo-Json

    try {
        $resp = Invoke-RestMethod -Uri "$GATEWAY/api/v1/admin/staff" -Method Post -ContentType "application/json" -Headers $headers -Body $body
        $tempPassword = $resp.data.tempPassword
        Write-Host "  $email olusturuldu, gecici sifre aliniyor..."
    } catch {
        Write-Host "  $email zaten var olabilir, dogrudan demo sifresiyle giris denenecek." -ForegroundColor Yellow
        return
    }

    # Gecici sifreyle giris yapip sabit demo sifresine cevir
    $tempAuth = Login-Staff $email $tempPassword
    $tempToken = $tempAuth.data.accessToken
    $changeBody = @{ currentPassword = $tempPassword; newPassword = $demoPassword } | ConvertTo-Json
    Invoke-RestMethod -Uri "$GATEWAY/api/v1/auth/change-password" -Method Post -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer $tempToken" } -Body $changeBody | Out-Null
    Write-Host "  $email sifresi '$demoPassword' olarak ayarlandi" -ForegroundColor Green
}

Write-Host "`n=== Personel hesaplari ===" -ForegroundColor Cyan
New-DemoStaff "deneme1@offerhub.com" "Deneme" "Supervisor" "SUPERVISOR" @() @("Istanbul")
# Not: specialties AI Service'in ExpertAssignmentService'inde campaign.segment (Segment enum:
# YUKSEK_DEGER/RISKLI_KAYIP/YENI_ABONE/PASIF) ile birebir karsilastiriliyor - kampanya turu
# (TARIFE_YUKSELTME, CIHAZ_FIRSATI vb.) degil. Deneme2 genel/en cok atanan uzman oldugu icin
# tum segmentleri kapsiyor.
New-DemoStaff "deneme2@offerhub.com" "Deneme" "Expert" "EXPERT" @("YUKSEK_DEGER", "RISKLI_KAYIP", "YENI_ABONE", "PASIF") @("Istanbul")

Write-Host "`n=== Supervisor/Expert girisi (demo sifresiyle) ===" -ForegroundColor Cyan
$supAuth = Login-Staff "deneme1@offerhub.com" $demoPassword
$supToken = $supAuth.data.accessToken
$supId = $supAuth.data.user.id
$expAuth = Login-Staff "deneme2@offerhub.com" $demoPassword
$expToken = $expAuth.data.accessToken
$expId = $expAuth.data.user.id
Write-Host "  supervisor id: $supId"
Write-Host "  expert id: $expId"

function New-Campaign($token, $title, $type, $segment, $discount, $daysValid) {
    $headers = @{ Authorization = "Bearer $token" }
    $body = @{
        title        = $title
        type         = $type
        targetSegment = $segment
        discountRate = $discount
        validUntil   = (Get-Date).ToUniversalTime().AddDays($daysValid).ToString("yyyy-MM-ddTHH:mm:ss'Z'")
    } | ConvertTo-Json
    return Invoke-RestMethod -Uri "$GATEWAY/api/v1/campaigns" -Method Post -ContentType "application/json" -Headers $headers -Body $body
}

# title, type, segment, discount, gecerlilik(gun), olusturan (sup/exp)
$campaigns = @(
    @{ title = "Yüksek Değer - Sadakat Bonusu";           type = "SADAKAT";          segment = "YUKSEK_DEGER"; discount = 15; days = 45; by = "sup" }
    @{ title = "Yüksek Değer - Cihaz Yenileme";           type = "CIHAZ_FIRSATI";    segment = "YUKSEK_DEGER"; discount = 25; days = 60; by = "exp" }
    @{ title = "Yüksek Değer - Ek Paket Fırsatı";         type = "EK_PAKET";         segment = "YUKSEK_DEGER"; discount = 10; days = 30; by = "sup" }
    @{ title = "Riskli Kayıp - Özel Ek Paket";            type = "EK_PAKET";         segment = "RISKLI_KAYIP"; discount = 30; days = 30; by = "exp" }
    @{ title = "Riskli Kayıp - Tarife Yükseltme Teklifi"; type = "TARIFE_YUKSELTME"; segment = "RISKLI_KAYIP"; discount = 20; days = 30; by = "sup" }
    @{ title = "Riskli Kayıp - Cihaz Fırsatı";            type = "CIHAZ_FIRSATI";    segment = "RISKLI_KAYIP"; discount = 35; days = 30; by = "exp" }
    @{ title = "Yeni Abone - Hoşgeldin Ek Paketi";        type = "EK_PAKET";         segment = "YENI_ABONE";   discount = 20; days = 45; by = "sup" }
    @{ title = "Yeni Abone - Cihaz Taksit Fırsatı";       type = "CIHAZ_FIRSATI";    segment = "YENI_ABONE";   discount = 15; days = 60; by = "exp" }
    @{ title = "Yeni Abone - Sadakat Programı Girişi";    type = "SADAKAT";          segment = "YENI_ABONE";   discount = 10; days = 45; by = "sup" }
    @{ title = "Pasif - Geri Kazanım Sadakat";            type = "SADAKAT";          segment = "PASIF";        discount = 25; days = 30; by = "exp" }
    @{ title = "Pasif - Tarife Yükseltme Denemesi";       type = "TARIFE_YUKSELTME"; segment = "PASIF";        discount = 15; days = 30; by = "sup" }
    @{ title = "Pasif - Ek Paket Hatırlatma";             type = "EK_PAKET";         segment = "PASIF";        discount = 20; days = 30; by = "exp" }
)

Write-Host "`n=== Kampanyalar olusturuluyor ===" -ForegroundColor Cyan
$results = @()
foreach ($c in $campaigns) {
    $token = if ($c.by -eq "sup") { $supToken } else { $expToken }
    $resp = New-Campaign $token $c.title $c.type $c.segment $c.discount $c.days
    $d = $resp.data
    Write-Host "  $($d.campaignNo) [$($c.by)] $($d.title) -> status=$($d.status) prob=$($d.conversionProbability) priority=$($d.priority) aiSegment=$($d.aiSegment)"
    $results += [PSCustomObject]@{
        CampaignNo = $d.campaignNo
        Title      = $d.title
        Type       = $d.type
        Segment    = $d.targetSegment
        Status     = $d.status
        Prob       = $d.conversionProbability
        Priority   = $d.priority
        HasCase    = ($d.status -eq "YENI")
    }
    Start-Sleep -Milliseconds 400
}

Write-Host "`n=== OZET ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize

Write-Host "`n=== Acik case kuyrugu (YENI) ===" -ForegroundColor Cyan
$openCases = Invoke-RestMethod -Uri "$GATEWAY/api/v1/cases?status=YENI&size=100" -Method Get -Headers @{ Authorization = "Bearer $supToken" }
$openCases.data.content | Select-Object caseId, campaignNo, title, priority, status | Format-Table -AutoSize

Write-Host "`nsupervisorToken, expertToken ve id'ler bu oturumda degiskenlerde tutuluyor:"
Write-Host "  supId=$supId  expId=$expId"
Write-Host "Devam scripti icin caseId listesini yukaridaki tablodan kullan."
