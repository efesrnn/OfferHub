# OfferHub — Ortak Kararlar (Kesinleşmiş)

Bu doküman artık bir seçenek listesi değil, **verilmiş kararların kaydı**. Her başlıkta ne seçildiğini, bunun pratikte ne anlama geldiğini ve nasıl uygulanacağını bulacaksın. Mobil tarafın **Kotlin (native Android)** olarak netleşmesiyle birlikte birkaç karar (alan adı stili, mock API aracı) buna göre güncellendi — eski Flutter varsayımlarına dayanan notlar burada geçersizdir, referans alma.

Bir karar ileride değişirse bu dosya güncellenip PR ile geçilir; sessizce değiştirilmez.

---

## Karar Özeti

| # | Karar | Seçilen |
|---|---|---|
| A1 | Repo yapısı | Monorepo |
| A2 | Branch stratejisi | Trunk-based / GitHub Flow |
| B1 | API Gateway | Spring Cloud Gateway |
| B2 | Event altyapısı | RabbitMQ |
| B3 | Veritabanları | PostgreSQL (Identity/Campaign/AI) + Redis & PostgreSQL (Gamification) |
| C1 | Alan adı stili | camelCase |
| C2 | ID formatı | Karma (UUID + okunabilir kampanya numarası) |
| C3 | Tarih-saat formatı | ISO 8601, UTC |
| C4 | Pagination | page + size |
| C5 | Hata kod kataloğu | Sabit enum/liste (ayrı `ERROR-CODES.md`) |
| C6 | Mobil mock API aracı | Postman Mock Server |
| D1 | Swagger üretimi | Otomatik (springdoc-openapi) |
| E1 | AI yaklaşımı | Klasik ML modeli (scikit-learn) |
| — | Mobil platform | **Kotlin, native Android** |

---

## Bölüm A — Proje İskeleti

### A1. Repo Yapısı — Monorepo

Tek repo, `backend/identity-service`, `backend/campaign-service`, `backend/ai-service`, `backend/gamification-service`, `backend/api-gateway` klasörleri + artık `mobile/` altında Kotlin/Android projesi. Ortak `docker-compose.yml`, ortak `docs/` klasörü tek yerde yaşıyor. Değişen bir şey yok, mevcut iskelet zaten bu şekilde kuruldu.

### A2. Branch Stratejisi — Trunk-based / GitHub Flow

Uygulama şekli:
- `main` her zaman deploy edilebilir/çalışır durumda kalır.
- Her iş için kısa ömürlü bir dal: `feature/identity-jwt`, `feature/campaign-state-machine`, `feature/gamification-leaderboard`, `feature/android-auth-flow` gibi.
- Dal ömrü mümkünse 1-2 gün — uzun yaşayan dallar merge çakışmasını büyütür.
- Her PR'da en az bir takım arkadaşının review'ı zorunlu (case'in kendi kuralı da bu).
- `main`'e doğrudan push kapalı olmalı (repo ayarlarından branch protection açılabilir).

---

## Bölüm B — Altyapı Teknolojileri

### B1. API Gateway — Spring Cloud Gateway

Uygulama notu: `spring-cloud-starter-gateway` bağımlılığı eklenir, route tanımları `application.yml`'de predicate/filter olarak yazılır:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: identity-service
          uri: http://identity-service:8080
          predicates: [Path=/api/v1/auth/**]
        - id: campaign-service
          uri: http://campaign-service:8080
          predicates: [Path=/api/v1/campaigns/**, Path=/api/v1/cases/**, Path=/api/v1/offers/**]
```

JWT doğrulama, bir `GlobalFilter` (`AuthenticationFilter implements GlobalFilter, Ordered`) olarak yazılır ve `Authorization` header'ını her istekte kontrol eder. Rate limiting isterseniz Spring Cloud Gateway'in yerleşik `RequestRateLimiter` filtresi + Redis ile (zaten Gamification için Redis kuracaksınız, aynı instance kullanılabilir) kolayca eklenir.

### B2. Event Altyapısı — RabbitMQ

Uygulama notu: `docker-compose.yml`'e `rabbitmq:3-management` image'ı eklenir (management UI `15672` portunda debug için çok işe yarar — kuyrukta bekleyen mesajları görsel olarak izlersiniz). Spring tarafında `spring-boot-starter-amqp` bağımlılığı yeterli.

Önerilen exchange/queue tasarımı:
- Tek bir **topic exchange**: `offerhub.events`
- Her event, kendi `eventType` değerini **routing key** olarak kullanır (örn. `campaign.optimized`, `sla.breached`)
- Gamification Service, ilgilendiği routing key'lere kendi kuyruğunu bind eder (`campaign.optimized`, `sla.breached`); AI Service `segment.changed` ve `offer.responded`'a bind eder.
- Bu sayede yeni bir dinleyici eklemek istediğinizde (örn. ileride bildirim servisi), var olan publisher'lara hiç dokunmadan yeni bir kuyruk bind edilir.

### B3. Veritabanları — PostgreSQL + Redis (karma)

| Servis | Veritabanı |
|---|---|
| Identity Service | PostgreSQL (kendi container'ı, `identity-db`) |
| Campaign Service | PostgreSQL (kendi container'ı, `campaign-db`) |
| AI Service | PostgreSQL (kendi container'ı, `ai-db`) + eğitim verisi repo'da CSV/dosya olarak |
| Gamification Service | Redis (anlık liderlik tablosu, `gamification-cache`) + PostgreSQL (kalıcı puan/rozet geçmişi, `gamification-db`) |

Her Postgres, `docker-compose.yml`'de ayrı bir container ve ayrı bir volume ile tanımlanır — motor aynı (Postgres) olsa da instance'lar birbirinden tamamen izole, database-per-service kuralı korunur.

---

## Bölüm C — API Sözleşmesi Formatı

### C1. Alan Adı Stili — camelCase

`subscriberId`, `createdAt`, `campaignNo` gibi. Bu, önceki taslaktaki snake_case önerisinin tersi — bilinçli olarak değiştirildi, gerekçesi:

- Kotlin'in kendi konvansiyonu zaten camelCase (`data class Campaign(val campaignNo: String, val createdAt: Instant)`), yani Kotlin tarafında **hiç mapping/dönüşüm kodu yazmanıza gerek kalmıyor** — Retrofit + Moshi/kotlinx.serialization, JSON alanlarını doğrudan data class alanlarına eşler.
- Java/Spring tarafında da Jackson'ın **varsayılan** davranışı camelCase'dir — `@JsonNaming` gibi ekstra bir anotasyon eklemenize gerek yok, sıfır ek konfigürasyon.
- Yani her iki taraf da (Kotlin ve Java) camelCase'i zaten "doğal dili" olarak konuştuğu için, bu seçim entegrasyonu snake_case'e göre daha az sürtünmeli hale getiriyor.

**Not:** Case dokümanının kendi örnek payload'ları (`user_id`, `case_id`, `conversion_lift`) snake_case yazılmıştı — bunlar case'in sadece örnek gösterimiydi, sizi bağlamıyor. Kendi API'nizde camelCase kullanmanız case'in hiçbir kuralını ihlal etmez, tutarlı olmanız yeterli.

### C2. ID Formatı — Karma (değişmedi)

İç kayıtlarda UUID (`subscriberId`, `expertId`, `caseId`, `offerId`), dışa dönük kampanya numarasında okunabilir format: `CMP-2026-000123`.

### C3. Tarih-Saat Formatı — ISO 8601, UTC (değişmedi)

`2026-08-17T14:22:10Z`. Kotlin tarafında `java.time.Instant` ile doğrudan parse edilir (`Instant.parse(...)`), ekstra kütüphane gerekmez.

### C4. Pagination — page + size (değişmedi)

`GET /api/v1/campaigns?page=0&size=20`, cevap zarfı:
```json
{ "items": [ ], "total": 142, "page": 0, "size": 20 }
```

### C5. Hata Kod Kataloğu — Sabit enum/liste

Tüm kodlar artık ayrı bir dosyada: **`docs/ERROR-CODES.md`**. Hata kodlarının kendisi (`VALIDATION_ERROR`, `INVALID_CREDENTIALS` gibi) camelCase kuralına tabi **değildir** — bunlar JSON alan adı değil, sabit bir enum değeridir, UPPER_SNAKE_CASE olarak kalır. Karışmaması için: `{"error": {"code": "INVALID_STATE_TRANSITION", "message": "..."}}` — `error` ve `code` anahtarları camelCase, `INVALID_STATE_TRANSITION` değeri UPPER_SNAKE.

### C6. Mobil Mock API Aracı — Postman Mock Server (Kotlin'e göre güncellendi)

Mobil Kotlin/native Android olarak netleştiği için önceki değerlendirme gözden geçirildi:

- **MSW** zaten elenmişti (JS/web ekosistemine özgü, Kotlin'le hiç ilgisi yok) — bu değişmedi.
- **json-server** hâlâ dil bağımsız bir seçenek ama state/durum bazlı senaryoları (örn. 422 dönen kural dışı state geçişi) simüle etmesi zayıf.
- **OkHttp MockWebServer** (Square'in kütüphanesi) Kotlin'e özgü, kod içinde çalışan bir mock sunucu — ama sadece o an testi çalıştıran kişinin local process'inde yaşar, takım arkadaşlarınızla paylaşılan tek bir "sahte backend" olamaz. Birim/entegrasyon testleri için ayrıca faydalı olabilir ama Gün 2'nin ihtiyacı olan **paylaşılan** mock için uygun değil.
- **Postman Mock Server** dil bağımsızdır — Retrofit sadece bir `baseUrl`'e bakar, o URL'in arkasında gerçek servis mi Postman mock'u mu olduğunu bilmez/bilmesi gerekmez. `API-CONTRACT.md`'deki şemalardan üretilen collection, tek bir paylaşılan mock URL'i verir, herkes aynı sahte backend'e bağlanır.

**Sonuç: Postman Mock Server aynı kalıyor**, kararı değiştiren bir sebep yok — dil bağımsız olması onu Kotlin geçişinden etkilenmez kılıyor. Ekstra olarak: OkHttp MockWebServer'ı ileride (Hafta 4, test yazarken) birim testlerde ayrıca kullanmayı düşünebilirsiniz, ama o Gün 2'nin konusu değil.

---

## Bölüm D — Dokümantasyon

### D1. Swagger/OpenAPI Üretimi — Otomatik (springdoc-openapi), değişmedi

`springdoc-openapi-starter-webmvc-ui` bağımlılığı + `@Operation`/`@Schema` anotasyonları. En az Campaign ve AI'da zorunlu.

---

## Bölüm E — AI Yaklaşımı

### E1. AI Yaklaşımı — Klasik ML Modeli

Önceki öneri (kural+ML hibrit) yerine **saf klasik ML** seçildi — scikit-learn ile eğitilmiş bir model (Random Forest / Logistic Regression / Gradient Boosting gibi). Pratik sonuçları:

- **Eğitim verisi zorunlu:** Minimum 100 örnek, gerçekçi Türkçe abone profili + kampanya kabul/ret geçmişi. Repo'da `backend/ai-service/data/` altında CSV olarak saklanır (klasör zaten iskelette hazır).
- **README'de eğitim süreci anlatılmalı:** Hangi model, hangi özellikler (features), nasıl train/test split yapıldığı, doğruluk metrikleri — case bunu zorunlu tutuyor.
- **Kritik teknik alt-karar (henüz netleşmedi, ayrıca konuşulmalı):** AI Service'in kendisi Java Spring mi olacak yoksa model eğitimi + servis Python (FastAPI/Flask) ile mi yazılacak? İki yol var:
  1. Modeli Python'da (scikit-learn) eğitip **PMML veya ONNX** formatına export edip Java içinde (`jpmml-evaluator` veya ONNX Runtime for Java) çalıştırmak — AI Service diğer 3 servisle aynı Java/Spring ekosisteminde kalır.
  2. AI Service'in tamamını Python/FastAPI olarak yazmak — case "backend serbest dil/framework" dediği için bu tamamen kabul edilebilir, diğer servisler Java kalırken sadece AI Service Python olur; Gateway ve diğer servisler ona yine REST üzerinden bağlanır, hangi dilde yazıldığı dışarıdan görünmez.
  
  Bu ikisinden hangisini seçeceğiniz AI Service'i kimin yazacağına ve ekibin Python'a ne kadar hakim olduğuna bağlı — henüz "seçilen" olarak işaretlenmedi, B1 ile ayrıca netleştirilmeli.

---

## Mobil Platform Notu (yeni)

Mobil taraf **Kotlin, native Android** olarak netleşti (önceki Flutter/cross-platform varsayımı geçersiz). Bunun diğer kararlara etkisi:
- Auth token saklama: `EncryptedSharedPreferences` ya da Android Keystore tabanlı bir çözüm (flutter_secure_storage değil).
- HTTP client: Retrofit + OkHttp. Şeffaf token yenileme, bir OkHttp `Authenticator` veya `Interceptor` ile yapılır (401 geldiğinde otomatik `/auth/refresh` çağrısı, orijinal isteği tekrar dener).
- JSON parsing: Moshi veya kotlinx.serialization — camelCase alan adları data class'larla birebir örtüştüğü için ekstra `@SerializedName` anotasyonuna genelde gerek kalmaz.
- Yerel ağ bağlantısı (emulator/fiziksel cihaz) kuralları aynı kalıyor: Android emulator'da `10.0.2.2`, fiziksel cihazda laptop'un LAN IP'si.

---

## Sonraki Adım

Bu kararlar netleştiğine göre güncel `API-CONTRACT.md`, `EVENTS.md` ve `ERROR-CODES.md` dosyaları da bu kararlara (özellikle camelCase ve Kotlin) göre güncellendi — bkz. `docs/` klasörü.
