# Backend'in gateway'ine (localhost:8080) bagli tum Android cihazlar/emulatorler icin
# `adb reverse` kurar. Birden fazla emulator ayni anda acikken duz `adb reverse` komutu
# "more than one device/emulator" hatasi verir - bu script her cihaza kendi seri
# numarasiyla (-s) tek tek uygular.
#
# Kullanim: bu dosyayi calistir (PowerShell'de: .\scripts\adb-reverse-all.ps1)
# adb PATH'te degilse asagidaki $adb satirini kendi yoluna gore duzelt.

$adb = "adb"
if (-not (Get-Command $adb -ErrorAction SilentlyContinue)) {
    $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
}

$devices = & $adb devices | Select-String "device$" | ForEach-Object {
    ($_.Line -split "\s+")[0]
}

if (-not $devices) {
    Write-Host "Bagli cihaz/emulator bulunamadi. Emulatoru actigindan emin ol."
    exit 1
}

foreach ($serial in $devices) {
    Write-Host "-> $serial icin adb reverse kuruluyor..."
    & $adb -s $serial reverse tcp:8080 tcp:8080
}

Write-Host "Tamam. $($devices.Count) cihaz icin localhost:8080 -> backend yonlendirildi."
