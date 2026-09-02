# Seed verisi

Abone projeksiyonu `backend/campaign-service/src/main/resources/seed/subscriber_profiles.csv`
dosyasindan yuklenir. Servis acilisinda `SubscriberSeeder` calisir; tablo bosssa doldurur,
doluysa hicbir sey yapmaz. Kapatmak icin `SEED_ENABLED=false`.

Dosya, AI Service'in egitim verisiyle **ayni kumedir**
(`backend/ai-service/data/subscriber_profiles.csv` kopyasi). Ikisi ayni aboneleri
kullanmali, yoksa demo sirasinda AI'in tanidigi abone ile Campaign'in tanidigi abone
farkli olur. Kopya olmasinin sebebi Docker build context'i: her servis yalnizca kendi
klasorunu gorur, kardes klasorden dosya kopyalayamaz.

CSV degisirse **iki dosyayi birden** guncelle.

## Abone id'si

CSV `SUB-0001` gibi okunabilir kodlar tutuyor, sistem ise UUID kullaniyor
(bkz. `docs/ORTAK-KARARLAR.md` C2). Kod, `UUID.nameUUIDFromBytes` ile deterministik bir
UUID'ye cevriliyor: ayni kod her zaman ayni UUID'yi verir, dolayisiyla Identity de ayni
kurali uygularsa iki servis ayni aboneyi ayni id ile tanir.
