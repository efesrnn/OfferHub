# OfferHub — API Sözleşmesi

Bu doküman, `ORTAK-KARARLAR.md`'de kesinleşen kararlar (**camelCase**, ISO 8601 UTC, karma ID formatı, page+size pagination, sabit hata kod kataloğu, mobil: **Kotlin/native Android**) uygulanarak hazırlanmış uçtan uca endpoint sözleşmesidir. Gün 2 toplantısında bunun üzerinden geçip netleştirin, sonra doğrudan bir **Postman Collection**'a aktarın — Kotlin tarafı Retrofit `baseUrl`'ini o mock sunucuya çevirip Hafta 1'i onunla geçirir.

**Not:** Buradaki alan adları ve yollar başlangıç önerisidir, case dokümanı zorunlu kılmadığı her yerde ekip kararıyla değiştirilebilir. Değiştirdiğiniz her şeyi bu dosyaya geri yazın ki tek doğru kaynak burası olsun. Hata kodlarının tam listesi ve açıklamaları için: `docs/ERROR-CODES.md`.

---

## 0. Genel Kurallar (Özet)

- Base URL (lokal): `http://localhost:8080` (Gateway üzerinden)
- Tüm cevaplar şu zarfta döner:
```json
{ "success": true, "data": { }, "error": null }
{ "success": false, "data": null, "error": { "code": "STRING", "message": "..." } }
```
- **Alan adı stili: camelCase** (`subscriberId`, `createdAt`, `campaignNo`). İstisna: `error.code` değeri ve enum değerleri (`status`, `role`, `segment` gibi alanların içeriği) UPPER_SNAKE_CASE kalır — bunlar sabit değerlerdir, alan adı değil. Örnek: `{ "campaignNo": "CMP-2026-000123", "status": "YENI" }` — `campaignNo` camelCase anahtar, `"YENI"` sabit değer.
- Kimlik doğrulama gereken her istekte header: `Authorization: Bearer <accessToken>`
- Tarih/saat: ISO 8601 UTC — `2026-08-17T14:22:10Z` (Kotlin'de `java.time.Instant.parse(...)` ile doğrudan okunur)
- Liste endpoint'leri: `?page=0&size=20` query param, cevapta:
```json
{ "items": [ ], "total": 142, "page": 0, "size": 20 }
```
- ID formatı: iç kayıtlarda UUID (`subscriberId`, `expertId`, `caseId`, `offerId`), dışa dönük kampanya numarasında okunabilir format (`CMP-2026-000123`)

### Kotlin Entegrasyon Notu

Retrofit interface'i tüm servisleri tek bir base URL üzerinden çağırır (Gateway), örnek:

```kotlin
interface OfferHubApi {
    @POST("api/v1/auth/otp-verify")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): ApiResponse<AuthData>

    @GET("api/v1/campaigns")
    suspend fun getCampaigns(
        @Query("status") status: String?,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<PagedResult<Campaign>>
}

data class ApiResponse<T>(val success: Boolean, val data: T?, val error: ApiError?)
data class ApiError(val code: String, val message: String)
```

Şeffaf token yenileme için bir OkHttp `Authenticator` (401 geldiğinde otomatik `/auth/refresh` çağırıp isteği tekrar dener) veya `Interceptor` kullanılır — access token her isteğe otomatik eklenir, kullanıcı hiçbir şey fark etmez.

---

## 1. Identity Service — `/api/v1/auth`, `/api/v1/users`, `/api/v1/admin`

### 1.1 Abone Kaydı
`POST /api/v1/auth/register` — **Yetki:** açık (girişsiz)
```json
// Request
{ "firstName": "Ayşe", "lastName": "Yılmaz", "phone": "+905551112233", "email": null }
// Response 201
{ "success": true, "data": { "subscriberId": "b7e1...", "otpSent": true }, "error": null }
```
Hatalar: `VALIDATION_ERROR`, `DUPLICATE_RESOURCE` (telefon zaten kayıtlı)

### 1.2 OTP Doğrulama (Abone Girişi)
`POST /api/v1/auth/otp-verify` — **Yetki:** açık
```json
// Request
{ "phone": "+905551112233", "otpCode": "1234" }
// Response 200
{ "success": true, "data": {
    "accessToken": "eyJ...", "refreshToken": "eyJ...", "expiresIn": 900,
    "user": { "id": "b7e1...", "role": "SUBSCRIBER", "firstName": "Ayşe" }
  }, "error": null }
```
Hatalar: `INVALID_CREDENTIALS`, `NOT_FOUND`

### 1.3 Personel Girişi
`POST /api/v1/auth/login` — **Yetki:** açık
```json
// Request
{ "email": "uzman@offerhub.com", "password": "Sifre123!" }
// Response 200
{ "success": true, "data": {
    "accessToken": "eyJ...", "refreshToken": "eyJ...", "expiresIn": 900,
    "user": { "id": "a7f3...", "role": "EXPERT", "specialties": ["CHURN_ONLEME"] }
  }, "error": null }
```
Hatalar: `INVALID_CREDENTIALS`, `ACCOUNT_LOCKED` (mesajda kalan dakika)

### 1.4 Token Yenileme
`POST /api/v1/auth/refresh` — **Yetki:** geçerli refreshToken
```json
// Request
{ "refreshToken": "eyJ..." }
// Response 200
{ "success": true, "data": { "accessToken": "eyJ...", "refreshToken": "eyJ_yeni...", "expiresIn": 900 }, "error": null }
```
Hatalar: `TOKEN_INVALID` / `TOKEN_REVOKED` (replay tespit edilirse tüm oturumlar sonlandırılır)

### 1.5 Çıkış
`POST /api/v1/auth/logout` — **Yetki:** giriş yapmış herkes
```json
// Request
{ "refreshToken": "eyJ..." }
// Response 200
{ "success": true, "data": null, "error": null }
```

### 1.6 Personel Hesabı Oluşturma
`POST /api/v1/admin/staff` — **Yetki:** ADMIN
```json
// Request
{ "firstName": "Can", "lastName": "Demir", "email": "can@offerhub.com",
  "role": "EXPERT", "specialties": ["CHURN_ONLEME", "YUKSEK_DEGER"], "regions": ["ISTANBUL"] }
// Response 201
{ "success": true, "data": { "staffId": "c9d2...", "tempPasswordSent": true }, "error": null }
```
Hatalar: `VALIDATION_ERROR`, `FORBIDDEN`

### 1.7 Rol Güncelleme
`PATCH /api/v1/admin/staff/{staffId}/role` — **Yetki:** ADMIN
```json
{ "role": "SUPERVISOR" }
```

### 1.8 Audit Log Listesi
`GET /api/v1/admin/audit-logs?action=LOGIN_FAILED&from=2026-08-01&page=0&size=20` — **Yetki:** ADMIN
```json
{ "success": true, "data": { "items": [
    { "id": "...", "userId": "...", "action": "LOGIN_FAILED", "timestamp": "2026-08-17T10:00:00Z",
      "ip": "192.168.1.5", "result": "FAILED", "detail": "..." }
  ], "total": 12, "page": 0, "size": 20 }, "error": null }
```

### 1.9 Kendi Profilim
`GET /api/v1/users/me` — **Yetki:** giriş yapmış herkes

---

## 2. Campaign Service — `/api/v1/campaigns`, `/api/v1/cases`, `/api/v1/offers`

### 2.1 Kampanya Oluşturma
`POST /api/v1/campaigns` — **Yetki:** EXPERT, SUPERVISOR
```json
// Request
{ "title": "Yaz Ek Paket Kampanyası", "type": "EK_PAKET", "targetSegment": "YUKSEK_DEGER",
  "discountRate": 20, "validUntil": "2026-09-30T23:59:59Z" }
// Response 201
{ "success": true, "data": {
    "campaignNo": "CMP-2026-000123", "status": "YENI", "priority": "ORTA",
    "aiSegment": "YUKSEK_DEGER", "createdAt": "2026-08-17T14:00:00Z"
  }, "error": null }
```
Not: Bu endpoint arka planda AI Service'e senkron çağrı yapar; AI cevap vermezse `aiSegment` alanı `"BELIRSIZ"`, `priority` `"ORTA"` döner ama işlem yine 201 ile başarılı kabul edilir.

### 2.2 Kampanya Listesi
`GET /api/v1/campaigns?status=YENI&segment=RISKLI_KAYIP&page=0&size=20` — **Yetki:** EXPERT (atananlar), SUPERVISOR/ADMIN (tümü)

### 2.3 Kampanya Detayı
`GET /api/v1/campaigns/{campaignNo}` — **Yetki:** EXPERT, SUPERVISOR, ADMIN

### 2.4 Segment/Öncelik Override
`PATCH /api/v1/campaigns/{campaignNo}/segment` — **Yetki:** EXPERT, SUPERVISOR
```json
// Request
{ "segment": "PASIF", "reason": "Kullanım verisi güncel değildi" }
// Response 200 — bu işlem arka planda AI Service'e "yanlış sınıflandırma" bildirimi (event) gönderir
```

### 2.5 Optimizasyon Vakaları Listesi
`GET /api/v1/cases?assignedTo=me&sort=priority&page=0&size=20` — **Yetki:** EXPERT (atananlar), SUPERVISOR (tümü)
```json
{ "success": true, "data": { "items": [
    { "caseId": "d4e5...", "campaignNo": "CMP-2026-000123", "segment": "RISKLI_KAYIP",
      "priority": "YUKSEK", "status": "ATANDI", "slaDeadline": "2026-08-17T22:00:00Z",
      "slaRemainingSeconds": 27600 }
  ], "total": 8, "page": 0, "size": 20 }, "error": null }
```

### 2.6 Vaka Detayı
`GET /api/v1/cases/{caseId}` — **Yetki:** EXPERT (kendi atanan), SUPERVISOR, ADMIN

### 2.7 Vaka Durum Geçişi
`PATCH /api/v1/cases/{caseId}/status` — **Yetki:** EXPERT, SUPERVISOR (case'in rol matrisine göre değişir)
```json
// Request (OPTIMIZE_EDILIYOR -> TAMAMLANDI geçişinde optimizasyon notu zorunlu)
{ "targetStatus": "TAMAMLANDI", "optimizationNote": "A/B testi ile indirim oranı %15'e çekildi" }
// Response 200
// Kural dışı geçiş denemesi:
{ "success": false, "data": null, "error": { "code": "INVALID_STATE_TRANSITION",
  "message": "YENI durumundan doğrudan TAMAMLANDI'ya geçilemez" } }
```

### 2.8 Manuel Atama
`POST /api/v1/cases/{caseId}/assign` — **Yetki:** SUPERVISOR
```json
{ "expertId": "a7f3..." }
```

### 2.9 Süpervizör Dashboard Verisi
`GET /api/v1/campaigns/dashboard` — **Yetki:** SUPERVISOR, ADMIN
```json
{ "success": true, "data": {
    "segmentDistribution": { "YUKSEK_DEGER": 42, "RISKLI_KAYIP": 18, "YENI_ABONE": 30, "PASIF": 10 },
    "conversionRate": 0.34,
    "slaComplianceRate": 0.91,
    "slaBreachedActiveCases": 3,
    "pendingQueueCount": 5
  }, "error": null }
```

### 2.10 Aboneye Özel Teklifler
`GET /api/v1/offers` — **Yetki:** SUBSCRIBER (kendi teklifleri)
```json
{ "success": true, "data": { "items": [
    { "offerId": "f1a2...", "campaignNo": "CMP-2026-000123", "title": "Yaz Ek Paket Kampanyası",
      "score": 0.83, "highlighted": true, "status": "PENDING" }
  ], "total": 3, "page": 0, "size": 20 }, "error": null }
```
Not: `score < 0.60` olan kampanyalar bu listede hiç görünmez (AI Service filtresi).

### 2.11 Teklif Yanıtlama
`POST /api/v1/offers/{offerId}/respond` — **Yetki:** SUBSCRIBER (sadece kendi teklifi)
```json
{ "response": "ACCEPTED" }   // veya "DECLINED"
```
Hatalar: `FORBIDDEN` (başkasının offerId'sini denerse — IDOR koruması)

### 2.12 Memnuniyet Puanlama
`POST /api/v1/offers/{offerId}/rate` — **Yetki:** SUBSCRIBER, tek seferlik
```json
{ "stars": 4 }
```

---

## 3. AI Service — `/api/v1/ai`

Bu servisin çoğu endpoint'i **dahili** (servisler arası), mobil doğrudan çağırmaz — Gateway route etse de gerçek çağıran Campaign Service'tir.

### 3.1 Öneri Skorlama (dahili, Campaign → AI)
`POST /api/v1/ai/recommend`
```json
// Request
{ "subscriberId": "b7e1...", "campaignType": "EK_PAKET" }
// Response 200
{ "success": true, "data": { "score": 0.83, "conversionProbability": 0.61, "segment": "YUKSEK_DEGER" }, "error": null }
```

### 3.2 Akıllı Uzman Ataması (dahili, Campaign → AI)
`POST /api/v1/ai/assign-expert`
```json
// Request
{ "caseId": "d4e5...", "segment": "RISKLI_KAYIP" }
// Response 200
{ "success": true, "data": { "expertId": "a7f3...", "matchScore": 0.87 }, "error": null }
// Kapasite yoksa:
{ "success": true, "data": { "expertId": null, "queued": true }, "error": null }
```

### 3.3 Yanlış Sınıflandırma Bildirimi (dahili, Campaign → AI)
`POST /api/v1/ai/misclassification`
```json
{ "campaignNo": "CMP-2026-000123", "originalSegment": "YUKSEK_DEGER", "correctedSegment": "PASIF" }
```

### 3.4 Doğruluk Oranı (Süpervizör dashboard için)
`GET /api/v1/ai/accuracy` — **Yetki:** SUPERVISOR, ADMIN
```json
{ "success": true, "data": { "overallAccuracy": 0.78, "bySegment": {
    "YUKSEK_DEGER": 0.82, "RISKLI_KAYIP": 0.71, "YENI_ABONE": 0.85, "PASIF": 0.69
  } }, "error": null }
```

---

## 4. Gamification Service — `/api/v1/game`

Bu servise dışarıdan sadece **okuma** istekleri gelir; puan/rozet değişimleri tamamen event dinleyerek gerçekleşir (bkz. Bölüm 5).

### 4.1 Kendi Profilim
`GET /api/v1/game/profile` — **Yetki:** EXPERT, SUPERVISOR
```json
{ "success": true, "data": {
    "totalPoints": 1240, "level": "GUMUS", "badges": ["ILK_KAMPANYA", "HIZ_USTASI"],
    "dailyRank": 3, "weeklyRank": 7, "casesResolved": 34, "avgPointsPerCase": 11.2
  }, "error": null }
```

### 4.2 Liderlik Tablosu
`GET /api/v1/game/leaderboard?period=daily` — **Yetki:** EXPERT, SUPERVISOR
```json
{ "success": true, "data": { "period": "daily", "items": [
    { "rank": 1, "expertId": "a7f3...", "name": "Can Demir", "points": 210 }
  ] }, "error": null }
```

### 4.3 Rozet Listesi
`GET /api/v1/game/badges` — **Yetki:** EXPERT, SUPERVISOR

---

## 5. Event Şemaları

Mesaj kuyruğu (RabbitMQ, topic exchange `offerhub.events`, routing key = `eventType` değeri) üzerinden yayınlanan tüm event'ler. Zarf sabit: `eventType`, `timestamp`, `payload`. Tam liste ve routing key detayları için: `docs/EVENTS.md`.

### 5.1 `campaign.created`
```json
{ "eventType": "campaign.created", "timestamp": "2026-08-17T14:00:00Z",
  "payload": { "campaignNo": "CMP-2026-000123", "type": "EK_PAKET", "targetSegment": "YUKSEK_DEGER" } }
```

### 5.2 `campaign.optimized`
```json
{ "eventType": "campaign.optimized", "timestamp": "2026-08-17T14:22:10Z",
  "payload": { "caseId": "d4e5...", "expertId": "a7f3...", "segment": "RISKLI_KAYIP",
    "priority": "YUKSEK", "conversionLift": 0.18,
    "createdAt": "2026-08-17T13:40:02Z", "completedAt": "2026-08-17T14:22:10Z" } }
```

(Diğer event'ler için bkz. `docs/EVENTS.md` — orada tüm 6 event, publisher/consumer bilgisiyle birlikte tam olarak listeleniyor.)

---

## 6. Sonraki Adım

Bu taslağı Gün 2 toplantısında satır satır gözden geçirin: eksik endpoint bulursanız ekleyin, alan adını değiştirirseniz burayı güncelleyin. Netleştikten sonra bu dosyayı temel alıp bir **Postman Collection** oluşturun — her endpoint için örnek request/response'ları buradan kopyalayabilirsiniz. Kotlin tarafı o collection'dan Mock Server açtığında, Retrofit `baseUrl`'ini mock URL'ine çevirip buradaki şemalarla birebir aynı sahte API'yi kullanmış olur.
