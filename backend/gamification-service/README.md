# Gamification Service

Kampanya uzmanları için puan, rozet, seviye ve liderlik tablosu.

Port 8084, veritabanı `gamification` (PostgreSQL, 5435), sıralama için Redis (6379).
Sahibi Backend2.

## Sorumluluk

Bu servisin ayırt edici özelliği, **hiçbir şeyi çağrı ile yapmaması**. Puan yazan tek bir
endpoint yok. Servisi çalıştıran şey Campaign Service'in RabbitMQ'ya yayınladığı olaylar.
Dışarıya açtığı dört endpoint yalnızca o olayların ürettiği sonucu okuyor.

Bunun sonucu şu: bir istemci kendisine puan yazdıramaz, çünkü puan yazan bir yol yok.

Dinlediği olaylar:

| Olay | Ne olur |
|---|---|
| `campaign.optimized` | Tamamlama puanı, hız bonusu, dönüşüm bonusu ve KRITIK SLA bonusu değerlendirilir, sonra rozet koşulları kontrol edilir |
| `sla.breached` | 5 puan düşülür |
| `offer.rated` | Abone 1 veya 2 yıldız verdiyse puan düşülür |

Olayların içeriği kök dizindeki `EVENTS.md` dosyasında.

## Puan tablosu

Case dokümanının 7.1'i. Kodda `PointReason` enum'unda duruyor.

| Sebep | Puan | Koşul |
|---|---|---|
| `OPTIMIZATION_COMPLETED` | +10 | Her tamamlanan optimizasyon |
| `FAST_OPTIMIZATION` | +5 | Vaka açılışından tamamlanmasına kadar 2 saatten kısa |
| `CONVERSION_TARGET_EXCEEDED` | +15 | Ölçülen dönüşüm artışı 0.10'un üstünde |
| `CRITICAL_WITHIN_SLA` | +15 | Önceliği KRITIK olan vaka SLA içinde bitmiş |
| `SLA_BREACH` | -5 | Her SLA aşımı |
| `LOW_RATING` | -3 | Abone 1 veya 2 yıldız vermiş |

Aynı olay iki kez gelirse ikinci kez puan yazılmıyor. Kontrol `(sourceId, reason)` çiftinde,
yani kuyruğun aynı mesajı tekrar teslim etmesi puanı şişirmiyor.

## Rozetler

Case dokümanının 7.2'si. Kodda `BadgeService.evaluate` içinde.

| Rozet | Koşul |
|---|---|
| `ILK_KAMPANYA` | İlk tamamlanan optimizasyon |
| `HIZ_USTASI` | 2 saatin altında 10 optimizasyon |
| `DONUSUM_KRALI` | 10 kez dönüşüm hedefinin aşılması |
| `MARATONCU` | 24 saatlik pencerede 20 optimizasyon |
| `CHURN_AVCISI` | 10 tamamlanmış `RISKLI_KAYIP` vakası |
| `UZMAN` | Tek bir segmentte 50 tamamlanmış vaka |

Rozet kazanıldığında `badge.earned` yayınlanıyor, mobil uygulamanın bildirimi bundan
besleniyor.

## Seviyeler

Case dokümanının 7.3'ü. Kodda `Level` enum'unda.

| Seviye | Puan aralığı |
|---|---|
| `BRONZ` | 0 ile 499 |
| `GUMUS` | 500 ile 1499 |
| `ALTIN` | 1500 ile 2999 |
| `PLATIN` | 3000 ve üstü |

## Endpointler

Hepsi gateway üzerinden ve token ile çağrılıyor. Servis, gateway'in eklediği `X-User-Id` ve
`X-User-Role` başlıkları olmadan gelen isteğe 403 dönüyor.

Ayrıntılı ve denenebilir hali Swagger'da: `http://localhost:8084/swagger-ui.html`

| Metot | Yol | Roller | Ne yapar |
|---|---|---|---|
| GET | `/api/v1/game/profile` | EXPERT, SUPERVISOR | Çağıranın kendi profili |
| GET | `/api/v1/game/leaderboard?period=daily` | EXPERT, SUPERVISOR | İlk on, `daily` veya `weekly` |
| GET | `/api/v1/game/badges` | EXPERT, SUPERVISOR | Altı rozetin hepsi, kazanılmış olanlar işaretli |
| POST | `/api/v1/game/leaderboard/rebuild` | SUPERVISOR, ADMIN | Sıralamayı puan defterinden yeniden kurar |

Profil ve rozet yollarında uzman kimliği geçmiyor. Cevaplanan profil her zaman `X-User-Id`
başlığındaki kişinin profili, o yüzden başkasının kaydını isteyecek bir yol yok.

`period` alanına `daily` ve `weekly` dışında bir şey gelirse 400 dönüyor. Sessizce `daily`
kabul etmiyoruz, çünkü o zaman yazım hatası boş bir tablo gibi görünürdü.

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
| `DB_URL` | `jdbc:postgresql://localhost:5435/gamification` | Veritabanı adresi |
| `DB_USERNAME` | `postgres` | Veritabanı kullanıcısı |
| `DB_PASSWORD` | `postgres` | Veritabanı parolası |
| `REDIS_HOST` | `localhost` | Liderlik sıralamasının tutulduğu Redis |
| `REDIS_PORT` | `6379` | Redis portu |
| `RABBITMQ_HOST` | `localhost` | Olay kuyruğu adresi |
| `RABBITMQ_PORT` | `5672` | Olay kuyruğu portu |
| `RABBITMQ_USERNAME` | `guest` | Kuyruk kullanıcısı |
| `RABBITMQ_PASSWORD` | `guest` | Kuyruk parolası |

Servisin dinlediği port kodda sabit 8080. Dışarı hangi porttan çıktığını compose belirliyor,
o yüzden bir port değişkeni yok.

## Çalıştırma

```bash
docker compose up -d gamification-service
```

Tek başına çalıştıracaksan veritabanı, Redis ve RabbitMQ'nun ayakta olması gerekiyor.
`JAVA_HOME` bu makinede varsayılan olarak JDK 23'ü gösteriyor ve derleme patlıyor:

```bash
JAVA_HOME="/c/Users/Sergen/.jdks/openjdk-25.0.1" ./mvnw spring-boot:run
```

## Bilinmesi gereken tasarım kararları

**Redis türetilmiş bir veri, doğruluk kaynağı değil.** Sıralama Redis'te, ama her puanın
sebebi `point_entries` tablosunda duruyor. Redis silinse veya yeniden başlasa hiçbir veri
kaybolmuyor, `POST /api/v1/game/leaderboard/rebuild` ile defterden yeniden kuruluyor. Bu
yüzden Redis'in kalıcı olması gerekmiyor.

**Puan Postgres'e yazıldıktan sonra Redis'e gidiyor.** Redis'in geri alması mümkün olmadığı
için sıralama güncellemesi işlem tamamlandıktan sonra uygulanıyor. İşlem geri alınırsa
Redis'e hiçbir şey gitmemiş oluyor.

**Kuyruk üç deneme sonrası mesajı düşürüyor, tekrar sıraya koymuyor.** Varsayılan davranış
sonsuza kadar yeniden sıraya koymak. Bu durumda ayrıştırılamayan tek bir mesaj tüketiciyi
döndürüp arkasındaki bütün olayları kilitlerdi.

**Liderlik tablosundaki `name` alanı boş.** Bu servis yalnızca uzman kimliklerini tutuyor.
İsimler Identity Service'in verisi ve buraya kopyalamak, başkasının verisinin ikinci ve
bayatlamış bir kopyasını tutmak olurdu. İsmi mobil uygulama
`GET /api/v1/users/staff` üzerinden kendisi çözebilir.

**Rozetler her olayda yeniden değerlendiriliyor.** Rozetlerin kendi tekillik kısıtı olduğu
için tekrar çalıştırmak zararsız, ve daha önce bir hata yüzünden atlanmış bir rozet böylece
kendiliğinden telafi ediliyor.
