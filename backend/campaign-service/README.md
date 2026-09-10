# Campaign Service

Kampanya yaşam döngüsü, optimizasyon vakaları, SLA takibi ve abone teklifleri.

Port 8082, veritabanı `campaign` (PostgreSQL, 5433). Sahibi Backend2.

## Sorumluluk

Bu servis dört işi yapıyor.

**Kampanya yaşam döngüsü.** Kampanya oluşturulduğunda AI Service'e gönderilip dönüşüm
tahmini, segment ve öncelik alıyor. AI'a ulaşılamazsa kampanya yine oluşuyor, segment
`BELIRSIZ` ve öncelik `ORTA` olarak işaretlenip manuel kuyruğa düşüyor. Kampanya numarası
`CMP-2026-000123` biçiminde, yıl bazlı bir sayaçtan üretiliyor.

**Optimizasyon vakaları.** AI'ın dönüşüm tahmini 0.60'ın altındaysa kampanyaya bir vaka
açılıyor ve uzmana atanıyor. Vakanın durum makinesi case dokümanının 5.2'sindeki tabloyla
birebir aynı, tablo dışındaki her geçiş 422 dönüyor.

**SLA takibi.** Vaka açıldığı anda önceliğe göre bir son tarih damgalanıyor. Bir zamanlayıcı
düzenli olarak süresi geçmiş vakaları tarayıp `sla.breached` yayınlıyor.

**Abone teklifleri.** Aboneye gösterilecek teklifler, kabul ve ret cevapları, bir defalık
memnuniyet puanı.

Kampanya durumunu Gamification'a haber vermek de buradan çıkıyor, ama doğrudan çağrı ile
değil RabbitMQ üzerinden. Hangi olayın ne taşıdığı kök dizindeki `EVENTS.md` dosyasında.

## Endpointler

Hepsi gateway üzerinden ve token ile çağrılıyor. Servis, gateway'in eklediği `X-User-Id` ve
`X-User-Role` başlıkları olmadan gelen isteğe 403 dönüyor.

Ayrıntılı ve denenebilir hali Swagger'da: `http://localhost:8082/swagger-ui.html`

### Kampanyalar

| Metot | Yol | Roller | Ne yapar |
|---|---|---|---|
| POST | `/api/v1/campaigns` | EXPERT, SUPERVISOR | Kampanya oluşturur, AI analizini tetikler |
| GET | `/api/v1/campaigns` | EXPERT, SUPERVISOR, ADMIN | Listeler. Uzman yalnızca kendi kampanyalarını görür |
| GET | `/api/v1/campaigns/{campaignNo}` | EXPERT, SUPERVISOR, ADMIN | Tek kampanya |
| PATCH | `/api/v1/campaigns/{campaignNo}/classification` | EXPERT, SUPERVISOR | Segment, tür veya öncelik düzeltmesi |
| GET | `/api/v1/campaigns/dashboard` | SUPERVISOR, ADMIN | Süpervizör paneli |

### Vakalar

| Metot | Yol | Roller | Ne yapar |
|---|---|---|---|
| GET | `/api/v1/cases` | EXPERT, SUPERVISOR, ADMIN | Listeler. `sort=sla` ile SLA sırasına geçer |
| GET | `/api/v1/cases/{caseId}` | EXPERT, SUPERVISOR, ADMIN | Tek vaka |
| POST | `/api/v1/cases/{caseId}/assign` | SUPERVISOR | Manuel atama |
| PATCH | `/api/v1/cases/{caseId}/status` | EXPERT, SUPERVISOR | Durum geçişi |

### Teklifler

İki ayrı yol aynı servise bakıyor. `/api/v1/offers` ekibin API sözleşmesindeki biçim,
`/api/v1/subscribers/me/offers` mobil uygulamanın çağırdığı biçim. İkisi de aynı
`OfferService`'i ve aynı tabloyu kullanıyor, ayrım yalnızca URL ve gövde şeklinde.

| Metot | Yol | Roller |
|---|---|---|
| GET | `/api/v1/offers` | SUBSCRIBER |
| POST | `/api/v1/offers/{offerId}/respond` | SUBSCRIBER |
| POST | `/api/v1/offers/{offerId}/rate` | SUBSCRIBER |
| GET | `/api/v1/subscribers/me/offers` | SUBSCRIBER |
| GET | `/api/v1/subscribers/me/offers/{offerId}` | SUBSCRIBER |
| POST | `/api/v1/subscribers/me/offers/{offerId}/accept` | SUBSCRIBER |
| POST | `/api/v1/subscribers/me/offers/{offerId}/decline` | SUBSCRIBER |
| POST | `/api/v1/subscribers/me/offers/{offerId}/rating` | SUBSCRIBER |

Abone kimliği hiçbir yolda geçmiyor. `me` sabit, gerçek kimlik token'dan geliyor, o yüzden
değiştirilecek bir id yok.

### İşletim

| Metot | Yol | Ne yapar |
|---|---|---|
| GET | `/actuator/health` | Sağlık kontrolü, compose bunu kullanıyor |
| GET | `/v3/api-docs` | OpenAPI tanımı |
| GET | `/swagger-ui.html` | Swagger arayüzü |

## Ortam değişkenleri

Tam liste ve açıklamaları `.env.example` dosyasında. Özet:

| Değişken | Varsayılan | Ne işe yarar |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/campaign` | Veritabanı adresi |
| `DB_USERNAME` | `postgres` | Veritabanı kullanıcısı |
| `DB_PASSWORD` | `postgres` | Veritabanı parolası |
| `RABBITMQ_HOST` | `localhost` | Olay kuyruğu adresi |
| `RABBITMQ_PORT` | `5672` | Olay kuyruğu portu |
| `RABBITMQ_USERNAME` | `guest` | Kuyruk kullanıcısı |
| `RABBITMQ_PASSWORD` | `guest` | Kuyruk parolası |
| `AI_SERVICE_URL` | `http://localhost:8083` | AI Service adresi |
| `AI_AUTO_ASSIGN` | `true` | Otomatik uzman ataması. `false` ise her vaka süpervizörü bekler |
| `AI_EXPERT_MAPPING` | boş | `EXP-001=<uuid>` biçiminde eşleme. Boşsa otomatik atama yapılmaz |
| `SLA_TIME_UNIT` | `hours` | `minutes` yapılırsa demoda SLA aşımı gösterilebilir |
| `SLA_SCAN_INTERVAL_MS` | `30000` | SLA aşım taramasının sıklığı |
| `CAMPAIGN_ARCHIVE_INTERVAL_MS` | `60000` | Süresi dolmuş kampanya taramasının sıklığı |
| `SEED_ENABLED` | `true` | Abone projeksiyonunun CSV'den doldurulması |

Servisin dinlediği port kodda sabit 8080. Dışarı hangi porttan çıktığını compose belirliyor,
o yüzden bir port değişkeni yok.

## Çalıştırma

Normal yol compose:

```bash
docker compose up -d campaign-service
```

Tek başına çalıştırmak istersen veritabanı ve RabbitMQ'nun ayakta olması gerekiyor.
`JAVA_HOME` bu makinede varsayılan olarak JDK 23'ü gösteriyor ve derleme patlıyor,
doğrusu şu:

```bash
JAVA_HOME="/c/Users/Sergen/.jdks/openjdk-25.0.1" ./mvnw spring-boot:run
```

`-o` (çevrimdışı) yalnızca `compile` için çalışıyor. `spring-boot:run` eklentinin
indirilmemiş jar'larını istediği için orada `-o` kullanma.

## Testler

Birim testleri:

```bash
JAVA_HOME="/c/Users/Sergen/.jdks/openjdk-25.0.1" ./mvnw test
```

`CaseStateMachineTest` durum makinesinin sözleşmedeki yedi geçişe uyduğunu ve dışındaki her
şeyi reddettiğini doğruluyor. `CampaignLifecycleTest` kampanyanın kendi durumunu, yani vaka
açılmayan kampanyanın doğrudan yayına geçmesini ve vakasız kampanyanın süresi dolunca
arşivlenmesini kapsıyor.

Uçtan uca testler ayakta duran sisteme istek atıyor, repo kökünden:

```bash
bash scripts/test/run-all.sh
```

## Bilinmesi gereken tasarım kararları

**`SecurityConfig` içindeki `permitAll` kasıtlı.** Kimlik doğrulama gateway'de yapılıyor,
yetkilendirme burada. Servisin kendi zincirinde ikinci bir kimlik doğrulaması olması, iki
ayrı yerde tutulan iki ayrı doğruluk kaynağı demek olurdu. Ayrıntısı
`docs/GUVENLIK-TESTLERI.md` beşinci bölümde.

**`aiSegment` hiçbir zaman üzerine yazılmıyor.** Uzman segmenti düzelttiğinde yalnızca
`segment` değişiyor. `aiSegment` AI'ın doğruluğunun ölçüldüğü referans, onu ezmek tam da
kaydetmeye çalıştığımız hatayı silmek olurdu.

**SLA ihlali geri alınmıyor.** Öncelik düşürülünce son tarih ileri gidiyor ama
`slaBreachedAt` damgası kalıyor ve kesilen puan iade edilmiyor. İhlal olmuş bir şeydir.

**Vaka açılmayan kampanya doğrudan `YAYINDA` başlıyor.** `YENI` "bir uzman buna bakacak"
demek. AI eşiğin üstünde puanladıysa bekleyecek kimse yok, o yüzden bekletmenin anlamı da
yok. Bu kampanyaların arşivlenmesi de kampanya seviyesinde yapılıyor, vakası olanlara
dokunulmuyor, çünkü açık bir vakayı arkadan arşivlemek durum makinesiyle çelişirdi.
