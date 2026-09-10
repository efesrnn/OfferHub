# OfferHub

Bir telekom operatörünün abonelerine doğru zamanda doğru teklifi sunan, yapay zeka destekli
kampanya ve öneri platformu. Dört mikroservis, bir API Gateway ve Kotlin ile yazılmış native
bir Android uygulaması.

Üç kişilik staj projesi, 20 iş günü.

## Ne yapıyor

Bir kampanya uzmanı kampanya oluşturuyor ve bir abone segmentine hedefliyor. AI Service her
abone için dönüşüm olasılığı ve öneri skoru hesaplayıp kampanyayı sınıflandırıyor. Dönüşüm
tahmini düşük kalan kampanyalar bir optimizasyon vakasına dönüşüp uygun uzmana atanıyor.
Uzman kampanyayı optimize ettikçe puan ve rozet kazanıyor. Süpervizör bütün performansı ve
modelin isabetini tek ekrandan izliyor.

## Mimari

```
                      ┌──────────────────┐
      Mobil App ─────▶│   API GATEWAY    │  :8080
       (Kotlin)       │  JWT + rate limit│
                      └────────┬─────────┘
             ┌────────────┬────┴──────┬──────────────┐
             ▼            ▼           ▼              ▼
       ┌──────────┐ ┌──────────┐ ┌─────────┐ ┌──────────────┐
       │ Identity │ │ Campaign │ │   AI    │ │ Gamification │
       │  :8081   │ │  :8082   │ │  :8083  │ │    :8084     │
       └────┬─────┘ └────┬─────┘ └────┬────┘ └──────┬───────┘
            ▼            ▼            ▼             ▼
        [Postgres]   [Postgres]   [Postgres]   [Postgres]
          :5432        :5433        :5434        :5435
                                                     +
                                                  [Redis]
                                                   :6379

                    ┌──────────────────────────┐
                    │  RabbitMQ  :5672 / 15672 │
                    └──────────────────────────┘
                     Campaign ──▶ AI, Gamification
```

Her servisin kendi veritabanı var ve hiçbiri başkasının veritabanına dokunmuyor. Servisler
arası iletişim iki şekilde: kampanya oluşturulurken Campaign'den AI'a bir REST çağrısı, geri
kalan her şey RabbitMQ üzerinden olay olarak.

Mobil uygulama yalnızca gateway ile konuşuyor, servislere doğrudan istek atmıyor. Zaten
atamaz, servisler gateway'in eklediği kimlik başlıkları olmadan gelen isteğe 403 dönüyor.

Ayrıntılar için `docs/architecture.md`.

## Servisler

| Servis               | Port | Veritabanı                       | Sorumluluk                                                | README                                   |
| -------------------- | ---- | -------------------------------- | --------------------------------------------------------- | ---------------------------------------- |
| api-gateway          | 8080 | yok                              | Tek giriş noktası, yönlendirme, JWT doğrulama, rate limit | `backend/api-gateway/README.md`          |
| identity-service     | 8081 | identity, 5432                   | Kayıt, giriş, token, rol, audit log, hesap kilitleme      | `backend/identity-service/README.md`     |
| campaign-service     | 8082 | campaign, 5433                   | Kampanya yaşam döngüsü, vakalar, SLA, teklifler           | `backend/campaign-service/README.md`     |
| ai-service           | 8083 | ai, 5434                         | Öneri skorlama, segment sınıflandırma, uzman ataması      | `backend/ai-service/README.md`           |
| gamification-service | 8084 | gamification, 5435 ve Redis 6379 | Puan, rozet, seviye, liderlik                             | `backend/gamification-service/README.md` |
| mobile               | yok  | yok                              | Kotlin native Android, dört rolün de arayüzü              | `README.md`                              |

Yönetim arayüzleri: RabbitMQ `http://localhost:15672` (guest / guest).

## Kurulum

Gereken tek şey Docker. Repo kökünde:

```bash
docker compose up -d --build
```

Beş servis, dört Postgres, bir Redis ve bir RabbitMQ ayağa kalkıyor. Servisler
veritabanlarının sağlıklı olmasını bekliyor, ilk açılış birkaç dakika sürebilir.

Durumu görmek için:

```bash
docker compose ps
```

Sıfırdan temiz bir kurulum istersen (veritabanları dahil her şey silinir):

```bash
docker compose down -v && docker compose up -d --build
```

### Seed verisi

Ayrı bir komut çalıştırmaya gerek yok. Campaign Service açılışta 220 aboneyi
`subscriber_profiles.csv` dosyasından yüklüyor, tablo doluysa hiçbir şey yapmıyor. Identity
Service de açılışta admin hesabını oluşturuyor.

Kapatmak istersen `SEED_ENABLED=false`. Ayrıntısı `scripts/seed/README.md` dosyasında.

## Demo kullanıcıları

**Admin**

```
E-posta: admin@offerhub.com
Parola:  Admin123!
```

Identity Service açılışta bu hesabı oluşturuyor. `ADMIN_EMAIL` ve `ADMIN_PASSWORD` ile
değiştirilebilir.

**Demoda dikkat:** bu hesap `must_change_password` işaretli geliyor. İlk girişten sonra
uygulama doğrudan "Create New Password" ekranına düşüyor ve devam etmek için yeni bir parola
belirlemek gerekiyor. Bu bir hata değil, case 4.1'in istediği davranış. Ama parolayı
değiştirdikten sonra yukarıdaki `Admin123!` artık çalışmaz, o yüzden demoda hangi parolanın
kullanılacağına önceden karar verin. Temiz kurulumda (`docker compose down -v`) hesap
yeniden oluşuyor ve parola tekrar `Admin123!` oluyor.

**Uzman ve süpervizör**

Hazır hesap gelmiyor, bunları admin oluşturuyor. Demoda önce admin ile giriş yapıp personel
hesabı açmak gerekiyor, bu zaten case dokümanının 4.1'inde tanımlı akış.

**Abone**

Kayıt GSM numarası ve OTP ile. OTP kodu sabit değil, rastgele üretilip loga yazılıyor.
Demoda koda şuradan bakılır:

```bash
docker compose logs identity-service | grep "OTP GONDERILDI"
```

Demoda abone seçerken dikkat: bazı aboneler hiç teklif görmüyor, çünkü onlara uygun bütün
kampanyalar 0.60 eşiğinin altında puanlanıyor. Bu kuralın doğru çalıştığının işareti ama
demoda boş ekran gösterir. **SUB-0001** ve **SUB-0002** güvenli seçimler.

## Uçtan uca demo akışı

Case dokümanının 12.3'teki zorunlu senaryosu:

1. `docker compose up -d --build`
2. Uzman olarak kampanya oluştur ve bir segmente hedefle
3. AI'ın verdiği öneri skoru, segment ve dönüşüm tahminini göster
4. Düşük performanslı segmentin uzmana atandığını göster
5. Uzman olarak optimizasyonu notla tamamla
6. Puanın liderlik tablosuna yansıdığını göster
7. `docker stop offerhub-ai-service-1` ile bir servisi kapat, sistemin geri kalanının
   çalıştığını göster
8. Güvenlik senaryolarını dene

Yedinci ve sekizinci adımlar script ile de gösterilebilir:

```bash
bash scripts/test/resilience.sh
bash scripts/test/security.sh
```

## Testler

Uçtan uca test paketi ayakta duran sisteme gerçek istek atıyor. 100'ün üzerinde kontrol:

```bash
bash scripts/test/run-all.sh
```

Ayrıntısı ve tek tek çalıştırma `scripts/test/README.md` dosyasında.

Birim testleri için servis klasöründe:

```bash
JAVA_HOME="/c/Users/Sergen/.jdks/openjdk-25.0.1" ./mvnw test
```

## API dokümantasyonu

Swagger arayüzleri doğrudan servis portlarında:

- Campaign: `http://localhost:8082/swagger-ui.html`
- Gamification: `http://localhost:8084/swagger-ui.html`
- Identity: `http://localhost:8081/swagger-ui.html`

Gateway yalnızca `/api/v1/**` yollarını yönlendiriyor, Swagger oradan erişilmiyor. Bu
bilinçli, Swagger bir geliştirici aracı ve dışa açılan gateway üzerinden yayınlanması
gereksiz bir yüzey açar.

## Dokümanlar

| Dosya                       | İçerik                                               |
| --------------------------- | ---------------------------------------------------- |
| `offerhub-staj-projesi.md`  | Case dokümanının kendisi, bütün zorunluluklar burada |
| `docs/architecture.md`      | Mimari kararlar                                      |
| `docs/API-CONTRACT.md`      | Ortak API sözleşmesi                                 |
| `docs/CAMPAIGN-API.md`      | Campaign Service endpoint referansı                  |
| `docs/GAMIFICATION-API.md`  | Gamification Service endpoint referansı              |
| `docs/ERROR-CODES.md`       | Hata kodu kataloğu                                   |
| `docs/ORTAK-KARARLAR.md`    | Ekipçe alınan kararlar                               |
| `docs/ai-approach.md`       | AI yaklaşımı, model ve eğitim süreci                 |
| `docs/GUVENLIK-TESTLERI.md` | Güvenlik testleri, alınan önlemler ve gerekçeleri    |
| `EVENTS.md`                 | Bütün olaylar ve payload'ları                        |
| `scripts/test/README.md`    | Uçtan uca test paketi                                |
| `scripts/seed/README.md`    | Seed verisi                                          |

## Teknoloji

Java 25, Spring Boot 4.0.7, PostgreSQL 16, Redis 7, RabbitMQ 3, Docker Compose. Mobil
tarafta Kotlin ve Jetpack Compose. AI Service kural tabanlı ve ML hibrit bir yaklaşım
kullanıyor, eğitim verisi ve süreci `docs/ai-approach.md` dosyasında.

Yanıt biçimi bütün servislerde aynı:

```json
{ "success": true, "data": {}, "error": null }
```

Hata durumunda `error.code` sabit bir metin taşıyor, kataloğu `docs/ERROR-CODES.md`
dosyasında.

## Ekip

| Kişi              | Sorumluluk                                                             |
| ----------------- | ---------------------------------------------------------------------- |
| Efe Serin         | Backend1: Gateway, Identity, AI, güvenlik                              |
| Sergen Yalçın     | Backend2: Campaign, Gamification, olay altyapısı, Docker Compose, seed |
| Serranur Türkoğlu | Mobil: Kotlin native Android, dört rolün de arayüzü                    |

## Bilinen açıklar

Dürüstlük açısından, teslimde bilinen ve kapanmamış maddeler:

- Servis kapalıyken gateway 503 yerine 500 dönüyor. Kök sebep ve tek satırlık düzeltme
  `docs/GUVENLIK-TESTLERI.md` sekizinci bölümde.
- Identity Service'te üç endpoint beklenmeyen girdiye 500 dönüyor.
- Gateway, Identity ve AI Service'te actuator health check yok. Campaign ve Gamification'da
  var ve compose bunları kullanıyor.
- Liderlik tablosunda uzman ismi `null` geliyor, yalnızca kimlik dönüyor.
- Servis README'lerinden üçü hâlâ iskelet halinde: api-gateway, identity-service ve
  ai-service. Campaign ve Gamification yazıldı. Mobil README hiç yok.
- AI Service'te Swagger yok. Case en az Campaign ve AI istiyor, Campaign'de var.
- Dal stratejisi son haftada fiilen bırakıldı. Başta kişi başına dal ve PR review olarak
  kurgulanmıştı, son günlerde hıza öncelik verilip doğrudan push'a dönüldü.
