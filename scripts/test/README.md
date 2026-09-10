# Uçtan uca test paketi

Bu klasördeki scriptler çalışan sisteme gerçek istek atar. Birim testi değiller, JUnit
testlerinin yerini almazlar. Amaçları case dokümanının kabul kriterlerini ayağa kalkmış
sistem üzerinde tek komutla gösterebilmek.

Her istek API Gateway üzerinden ve gerçek bir JWT ile gider. Servisler gateway'in eklediği
başlıklar olmadan gelen isteği reddettiği için tek geçerli yol bu.

## Çalıştırma

Önce sistemin ayakta olması gerekiyor:

```bash
docker compose up -d
```

Sonra repo kökünden:

```bash
bash scripts/test/run-all.sh
```

Tek tek çalıştırmak istersen:

```bash
bash scripts/test/smoke.sh        # işlevsel akış, 51 kontrol
bash scripts/test/security.sh     # güvenlik senaryoları, 35 kontrol
bash scripts/test/resilience.sh   # servis kapatma, 20 kontrol
```

Dayanıklılık testi container durdurup başlattığı için en uzun süren bölüm. Atlamak için:

```bash
bash scripts/test/run-all.sh --skip-resilience
```

## Ne gerekiyor

`bash`, `curl`, `python` ve `docker`. Windows'ta Git Bash içinde çalışır. `jq` gerekmiyor,
JSON okuma işini python yapıyor, çünkü bu makinelerde `jq` kurulu olduğunu varsayamayız ama
python zaten AI Service için kurulu.

## Dosyalar

| Dosya | Ne yapar |
|---|---|
| `run-all.sh` | Üç paketi sırayla çalıştırır, toplu sonuç verir |
| `smoke.sh` | Sağlık, yetki sınırları, kampanya oluşturma, rol matrisi, durum makinesi, olay tabanlı puanlama, abone teklif akışı, dashboard, Swagger |
| `security.sh` | SQL enjeksiyonu, XSS, token manipülasyonu, yetkisiz erişim, IDOR, gateway atlatma, girdi doğrulama, brute force |
| `resilience.sh` | ai-service, gamification-service ve rabbitmq'yu sırayla durdurup sistemin ayakta kaldığını gösterir |
| `lib.sh` | Ortak yardımcılar. Doğrudan çalıştırılmaz, diğerleri bunu `source` eder |
| `jwt_token.py` | Test JWT'si üretir |

## Token üretimi

`jwt_token.py`, repo kökündeki `.env` dosyasından `JWT_SECRET` okuyup gateway'in kabul edeceği
bir token imzalar. Böylece testler Identity Service'in seed edilmiş olmasına bağlı kalmıyor.

```bash
python scripts/test/jwt_token.py EXPERT aaaaaaaa-1111-1111-1111-111111111111
python scripts/test/jwt_token.py EXPERT aaaaaaaa-1111-1111-1111-111111111111 --expired
```

`--expired`, iki saat önce üretilmiş ve ömrü dolmuş bir token verir. Güvenlik testi bununla
gateway'in imzaya ek olarak `exp` alanına da baktığını doğruluyor.

## Testlerin bıraktığı iz

`smoke.sh` ve `security.sh` her koşuda kampanya oluşturur ve bir teklifi kabul edip
puanlar. Bunlar veritabanında kalır. Sunum öncesi temiz bir tablo istiyorsan:

```bash
docker compose down -v && docker compose up -d --build
```

Güvenlik testi bilerek SQL enjeksiyonu içeren başlıklar yazıyor. Veritabanında
`' OR 1=1 --` gibi kampanyalar görürsen sebebi bu, ve zaten testin göstermek istediği şey
tam olarak bunların veri olarak durup çalışmamış olması.

## Kararsız çıkabilen bir kontrol

`resilience.sh` içinde gamification kapalıyken 503 bekleyen bir kontrol var. Bu kontrol
bazı koşularda geçiyor, bazılarında 500 alıp kalıyor.

Sebebi API Gateway'de. `GatewayErrorHandler.isUnreachable` sebep zincirinde
`ConnectException` arıyor. Netty durdurulmuş bir container için her zaman aynı istisnayı
fırlatmıyor: bazen `AnnotatedConnectException` geliyor, o `ConnectException`'ın alt sınıfı
olduğu için yakalanıyor ve 503 dönüyor. Bazen `AnnotatedNoRouteToHostException` geliyor, o
ise `NoRouteToHostException`'dan türüyor ve kontrolden kaçıp 500'e düşüyor. Hangisinin
geleceği container'ın ne kadar süredir kapalı olduğuna ve ağın durumuna göre değişiyor.

İkisinin ortak atası `java.net.SocketException`, kontrol ona çevrilirse ikisi de kapsanır ve
sonuç kararlı hale gelir.

Gateway Backend1'in sorumluluğunda, düzeltme oraya iletildi. Test bilerek 503 beklemeye
devam ediyor.
