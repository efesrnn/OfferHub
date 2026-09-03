# OfferHub — Hata Kod Kataloğu

Tüm servisler, hata döndürürken standart zarfı kullanır:

```json
{ "success": false, "data": null, "error": { "code": "VALIDATION_ERROR", "message": "..." } }
```

`error.code` ve `error.message` anahtarları camelCase kuralına tabidir (zaten tek kelime oldukları için görünmüyor), ama `code` değerinin kendisi (`VALIDATION_ERROR` gibi) **UPPER_SNAKE_CASE sabit bir enum değeridir** — bu, `docs/ORTAK-KARARLAR.md`'de camelCase kararının kapsamı dışında tutulan tek istisna. Kotlin tarafında bu kodlar bir `enum class ErrorCode` olarak tanımlanıp switch/when ile kullanıcıya özel mesaja çevrilmeli — asla `error.message` alanı doğrudan kullanıcıya gösterilmemeli (o alan geliştirici/log amaçlıdır, Türkçe kullanıcı dostu metin mobilde `code`'a göre ayrıca yazılır).

Yeni bir hata durumu bulduğunuzda önce bu listeye bakın, burada karşılığı yoksa ekleyip PR ile geçin — "serbest string" hata mesajı yazmayın (bkz. Ortak Kararlar C5).

---

## Genel (Tüm Servisler)

| Kod | HTTP Status | Ne zaman | Örnek mesaj |
|---|---|---|---|
| `VALIDATION_ERROR` | 400 | İstek gövdesinde eksik/hatalı alan | "title alanı boş olamaz" |
| `NOT_FOUND` | 404 | İstenen kayıt yok | "Bu kampanya numarasına ait kayıt bulunamadı" |
| `FORBIDDEN` | 403 | Kullanıcının bu işlem için rol yetkisi yok | "Bu işlem için süpervizör yetkisi gereklidir" |
| `RATE_LIMITED` | 429 | Kısa sürede çok fazla istek (brute-force koruması) | "Çok fazla deneme yaptınız, lütfen birkaç dakika sonra tekrar deneyin" |
| `INTERNAL_ERROR` | 500 | Beklenmeyen sunucu hatası | "Bir şeyler ters gitti, lütfen tekrar deneyin" |

---

## Identity Service

| Kod | HTTP Status | Ne zaman | Örnek mesaj |
|---|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Personel girişinde yanlış e-posta/şifre | "E-posta veya şifre hatalı" |
| `INVALID_OTP` | 401 | Abone OTP doğrulamasında yanlış/süresi dolmuş kod, veya telefon numarası bulunamadı | "Doğrulama kodu geçersiz veya süresi dolmuş" |
| `WEAK_PASSWORD` | 400 | Şifre politikası ihlali — case, **hangi kuralın** ihlal edildiğini belirten mesaj istiyor | "Şifre en az 1 büyük harf içermelidir" |
| `ACCOUNT_LOCKED` | 401 | 5 başarısız girişten sonra hesap 15 dakika kilitli | "Hesabınız kilitlendi, kalan süre: 12 dakika" |
| `TOKEN_EXPIRED` | 401 | Access token'ın 15 dakikalık ömrü doldu | "Oturum süresi doldu" (mobil bunu görmeden refresh akışı devreye girmeli) |
| `TOKEN_INVALID` | 401 | JWT imzası geçersiz veya format bozuk | "Geçersiz kimlik doğrulama bilgisi" |
| `TOKEN_REVOKED` | 401 | Rotation sonrası geçersiz kılınmış bir refresh token tekrar kullanılmaya çalışıldı (token theft belirtisi) — bu durumda kullanıcının **tüm oturumları** sonlandırılır | "Güvenlik nedeniyle tüm oturumlarınız kapatıldı, lütfen tekrar giriş yapın" |
| `DUPLICATE_RESOURCE` | 409 | Aynı telefon/e-posta ile tekrar kayıt denemesi | "Bu telefon numarası zaten kayıtlı" |

---

## Campaign Service

| Kod | HTTP Status | Ne zaman | Örnek mesaj |
|---|---|---|---|
| `INVALID_STATE_TRANSITION` | 422 | State machine kural dışı geçiş denemesi | "YENI durumundan doğrudan TAMAMLANDI'ya geçilemez" |
| `OPTIMIZATION_NOTE_REQUIRED` | 400 | `TAMAMLANDI`'ya geçerken optimizasyon notu boş bırakıldı | "Tamamlama için optimizasyon notu zorunludur" |
| `OFFER_ALREADY_RATED` | 409 | Abone aynı teklife ikinci kez puan vermeye çalıştı (puanlama tek seferlik) | "Bu teklife zaten puan verdiniz" |
| `OFFER_ALREADY_RESPONDED` | 409 | Abone aynı teklife ikinci kez kabul/ret yanıtı vermeye çalıştı | "Bu teklife zaten yanıt verdiniz" |
| `OFFER_NOT_FOUND` | 404 | Teklif id'si bu aboneye ait değil veya yok | "Teklif bulunamadı" |
| `OFFER_NOT_ACCEPTED` | 409 | Kabul edilmemiş bir teklife puan verilmeye çalışıldı | "Sadece kabul edilen tekliflere puan verilebilir" |
| `INVALID_RATING` | 400 | Puan 1-5 aralığı dışında | "Puan 1 ile 5 arasında olmalıdır" |

---

## AI Service

| Kod | HTTP Status | Ne zaman | Örnek mesaj |
|---|---|---|---|
| `MODEL_INPUT_INVALID` | 400 | Skorlama/segment isteğinde eksik abone verisi | "Abone profili eksik, skorlama yapılamıyor" |

**Not:** `AI_SERVICE_UNAVAILABLE` diye bir kod **yok** — çünkü Campaign Service, AI'a ulaşamadığında bunu kullanıcıya hata olarak döndürmez; kampanyayı `BELIRSIZ` segmentle yine de 201 ile kaydeder (bkz. Rehber Bölüm 7.1, fallback davranışı). Bu, case'in "AI kapalıyken de kampanya oluşturulabilmeli" kuralının doğrudan sonucu.

---

## Gamification Service

Bu serviste dışa dönük yazma endpoint'i olmadığı için (tamamen event dinleyerek çalışıyor) client-facing hata kodu neredeyse yok — sadece okuma endpoint'lerinde genel `NOT_FOUND`/`FORBIDDEN` kullanılır.

---

## Güvenlik Testi Senaryolarıyla Eşleşme

Case'in Bölüm 11'inde listelenen saldırı senaryolarının her biri, aslında yukarıdaki kodlardan biriyle sonuçlanmalı — mentörler deneme yaptığında sisteminizin **doğru kodla** yanıt vermesi bekleniyor:

| Saldırı senaryosu | Beklenen kod |
|---|---|
| SQL injection (`' OR 1=1 --`) | `VALIDATION_ERROR` (girdi ORM/prepared statement katmanında reddedilir, hiçbir zaman SQL olarak yorumlanmamalı) |
| Abone token'ıyla süpervizör endpoint'i çağırma | `FORBIDDEN` (403, audit log'a yazılır) |
| IDOR (başkasının `offerId`/`caseId`'sini deneme) | `FORBIDDEN` |
| Süresi dolmuş/manipüle JWT | `TOKEN_EXPIRED` / `TOKEN_INVALID` |
| Geçersiz kılınmış refresh token'ın tekrar kullanımı | `TOKEN_REVOKED` |
| XSS (metin alanına script enjeksiyonu) | `VALIDATION_ERROR` (input sanitization aşamasında reddedilmeli, ya da temizlenmiş haliyle kabul edilmeli) |
| Ardışık hızlı giriş denemesi | `RATE_LIMITED`, ardından `ACCOUNT_LOCKED` |

Kapanış demosunda bu tabloyu göstererek "hangi saldırıya hangi kodla, neden böyle yanıt verdiğinizi" anlatmanız beklenen sonuçtan daha değerli — case bunu özellikle vurguluyor.
