# Mobil giris ekrani telefon alanini 10 haneli (basindaki 0 olmadan, +90 onekiyle) bekliyor,
# ama create-test-subscribers.ps1 hesaplari "0555..." (11 hane) formatinda actigi icin
# uygulamada giris yapilamiyordu. subscriberId ayni kaldigi icin campaign-db'deki AI profili
# baglantisina (external_ref) dokunmuyoruz, sadece telefonu duzeltiyoruz.

$ErrorActionPreference = "Stop"

$phones = 1..8 | ForEach-Object { "0555000000$_" }

foreach ($old in $phones) {
    $new = $old.Substring(1)  # basindaki 0'i at
    $sql = "UPDATE subscribers SET phone = '$new' WHERE phone = '$old';"
    docker compose exec -T identity-db psql -U postgres -d identity -c "$sql"
    Write-Host "$old -> $new"
}

Write-Host "`nSimdi mobil uygulamada giris ekranina basindaki 0 olmadan gir, ornegin:"
Write-Host "  5550000001, 5550000002, ... 5550000008"
