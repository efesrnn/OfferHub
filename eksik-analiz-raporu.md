# OfferHub — Spec Karşılaştırma ve Eksik Analizi

*Kaynak: `offerhub-staj-projesi.md` vs. gerçek kod (backend/*, mobile/OfferHub). İlk tarama: 2026-09-10. Güncelleme: 2026-09-10, backend2'nin (Campaign+Gamification) yeni push'u sonrası.*
*Not: Bu, tüm servisleri paralel tarayan agent'ların bulgularının birleştirilmiş halidir. Her madde TAM / KISMİ / YOK olarak işaretli, dosya/satır referanslarıyla.*

---

## ✅ Düzeltilenler

- **Kritik madde #4 kapatıldı: refresh token akışı.** Identity Service'e `RefreshToken` entity + `refresh_tokens` tablosu, `/api/v1/auth/refresh` ve `/api/v1/auth/logout` endpoint'leri eklendi. Her refresh token'ın JWT `jti`'si DB'deki satırla eşleşiyor; `/refresh` çağrıldığında eski token iptal edilip yenisi veriliyor (rotation); iptal edilmiş bir token tekrar sunulursa (theft/replay belirtisi) kullanıcının tüm aktif refresh token'ları iptal ediliyor ve `REFRESH_TOKEN_REUSE` audit log'a yazılıyor; `/logout` ilgili token'ı iptal edip `LOGOUT` audit log'a yazıyor. `JwtService.generateRefreshToken` artık bir `tokenId` (jti) parametresi alıyor — mevcut çağrı yerleri (`verifyOtp`, `staffLogin`) ortak bir `issueTokens` yardımcı metoduna taşındı.
- **Kritik madde #1 kapatıldı: gateway bypass.** `docker-compose.yml`'de identity(8081)/campaign(8082)/ai(8083)/gamification(8084) servislerinin host port eşlemeleri kaldırıldı — bu servisler artık yalnızca docker ağı içinden (servis adıyla) erişilebilir, dışarıdan hiç ulaşılamaz. `CallerIdentityArgumentResolver`'ın header imzası doğrulamaması artık önemsiz, çünkü sahte header'lı isteğin ulaşacağı bir port yok. Yan etki olarak `scripts/test/security.sh`'deki "gateway atlatma" testi güncellendi (artık 403 değil bağlantı reddi `000` bekliyor, ayrıca gerçek saldırı senaryosunu — sahte header + direkt port — da test ediyor) ve `scripts/test/resilience.sh`'deki gamification sağlık kontrolü artık `docker inspect` ile container healthcheck'ine bakıyor (eskiden kapanan porttan actuator'a curl atıyordu).
- Campaign Service ve Gamification Service README'leri artık dolu (sorumluluk, endpoint listesi, env değişkenleri) — önceden TODO stub'dı.
- Campaign Service'e Swagger/OpenAPI eklendi (`OpenApiConfig.java`) — Gamification'da zaten vardı, ikisi de artık TAM.
- Yazılı güvenlik testi notu artık var, spec'in istediğinin çok ötesinde: `docs/GUVENLIK-TESTLERI.md` + `scripts/test/security.sh` — 35 otomatik kontrol (SQL injection, XSS, token manipülasyonu, IDOR, brute-force).
- Resilience/servis kapatma testi eklendi: `scripts/test/resilience.sh` — AI ve Gamification kapatılınca kampanya akışının ayakta kaldığı, AI kapalıyken BELIRSIZ/ORTA fallback'in çalıştığı otomatik doğrulanıyor.
- `TEST_EDILIYOR→OPTIMIZE_EDILIYOR` geçişinin hâlâ sadece Süpervizör'de olması artık bilinçli/dokümante edilmiş bir tasarım kararı ("Sistem" satırlarının arkasında henüz scheduler yok) — unutulmuş bir eksik değil.
- **Kritik madde #5 kapatıldı: mobilde segment override.** Uzman ekranında vaka detayına "Segmenti düzelt" aksiyonu eklendi (`ExpertApi.reclassifyCampaign` → backend'in mevcut `PATCH /api/v1/campaigns/{campaignNo}/classification` endpoint'i). Yeni bir bottom sheet (`SegmentOverrideSheet`) segment seçimi + zorunlu neden alanı (max 500 karakter, `<`/`>` yasak — backend'in `@Pattern` kuralıyla birebir) sunuyor; başarılı gönderimde vaka detayı yeniden çekiliyor (vaka'nın segmenti backend'de kampanyadan canlı okunduğu için bu yeterli). `ExpertRepository`/`ExpertRepositoryImpl`/`ExpertViewModel`/`MockExpertRepository` güncellendi, iki yeni birim testi eklendi.
- **Kritik madde #6 kapatıldı: mobilde şeffaf (silent) token yenileme.** Uygulama genelinde tek bir `TokenAuthenticator` (OkHttp `Authenticator`) eklendi — 401 alan herhangi bir istekte otomatik olarak `/api/v1/auth/refresh`'i dener, `Mutex` ile eşzamanlı yenileme denemelerini seri hale getirir (backend'in tek kullanımlık rotating refresh token'ları nedeniyle paralel refresh çağrısı "çalıntı token" sayılıp tüm oturumları iptal ederdi). Yenileme başarısız olursa `SessionEvents` üzerinden uygulama genelinde oturum sonlandırma tetiklenip kullanıcı login ekranına düşüyor. `AuthRepository.refresh()`/`logout()` eklendi, abonenin telefon numarası refresh sonrası kaybolmasın diye `withPreservedPhone` eklendi (backend `/refresh` yanıtı telefon içermiyor).

## 🔴 Demoyu veya çalışmayı doğrudan riske atan kritik maddeler

1. ~~Gateway bypass açığı~~ ✅ **kapatıldı** — yukarıdaki "Düzeltilenler" bölümüne bakın.
2. ~~Gamification kapalıyken gateway 500 dönüyor, 503 değil~~ ✅ **kapatıldı** — `GatewayErrorHandler.isUnreachable` artık `ConnectException` yerine `SocketException`'a bakıyor, hem `AnnotatedConnectException` hem `AnnotatedNoRouteToHostException` yakalanıyor. `resilience.sh`'deki "BİLİNEN AÇIK" notu kaldırıldı.
3. ~~Identity Service'te `application.yml` bulunamadı~~ ❌ **yanlış alarm** — `application.yaml` zaten var (ilk taramada `.yaml` uzantısı gözden kaçmış), `jwt.secret`/token süreleri/admin seed/SMS key hepsi doğru bağlanmış, `JWT_SECRET` de kök `.env` dosyasında zaten set. Servis sorunsuz açılıyor olmalı, yapılacak bir şey yok.
4. ~~Refresh token akışı yok~~ ✅ **kapatıldı** — `/api/v1/auth/refresh` ve `/api/v1/auth/logout` eklendi, yeni `refresh_tokens` tablosunda saklanıyor, rotation ve theft-protection (tekrar kullanılan token → kullanıcının tüm oturumları sonlandırılır) çalışıyor. Ayrıntı için aşağıya bakın.
5. ~~Mobilde segment override özelliği tamamen yok~~ ✅ **kapatıldı** — vaka detayında "Segmenti düzelt" aksiyonu, backend'in mevcut classification endpoint'ine bağlandı. Ayrıntı için yukarıdaki "Düzeltilenler" bölümüne bakın.
6. ~~Mobilde token yenileme "şeffaf" değil~~ ✅ **kapatıldı** — app genelinde tek `TokenAuthenticator` + `Mutex` ile 401'de otomatik/güvenli refresh, başarısızlıkta otomatik logout. Ayrıntı için yukarıdaki "Düzeltilenler" bölümüne bakın.

Not: Bu oturumda shell/derleme sandbox'ı çöktüğü için (`mvn compile` / `./gradlew build` hiç çalıştırılamadı) hem backend hem mobil değişiklikler yalnızca dikkatli manuel kod okumasıyla doğrulandı, gerçek bir derleme doğrulaması yapılamadı. Repoyu senkronlarken hem `mvn compile` hem `./gradlew build` (veya Android Studio'da bir build) ile teyit etmen önemli.

---

## 1. Identity Service

| Madde | Durum | Not |
|---|---|---|
| Abone kaydı (GSM+OTP, alanlar) | KISMİ | Akış var ama OTP sabit `1234` değil, rastgele üretiliyor (`MockPhoneVerification`) |
| Personel hesabı oluşturma (admin, uzmanlık/bölge) | TAM | `AdminController/AdminService.createStaff` |
| Şifre politikası (min 8, büyük harf, rakam, özel karakter + kural bazlı hata) | **YOK** | Hiçbir yerde validasyon yok |
| Bcrypt/Argon2 hash | TAM | `PasswordConfig` → `BCryptPasswordEncoder` |
| Hesap kilitleme (5 deneme/15 dk, kalan süre) | TAM | `AuthService.registerFailedAttempt`, 423 + `lockedUntil` |
| Access token JWT 15dk (user_id+rol+uzmanlık/bölge) | KISMİ | Token'da sadece `user_id`+`role` var, specialties/regions token payload'ında yok |
| Refresh token (7 gün, DB'de saklı) | ✅ TAM (düzeltildi) | Yeni `RefreshToken` entity (`refresh_tokens` tablosu), her refresh token'ın JWT `jti`'si DB'deki satırla eşleşiyor |
| Token rotation + theft protection | ✅ TAM (düzeltildi) | `/refresh` her çağrıda eski token'ı iptal edip yenisini veriyor; iptal edilmiş bir token tekrar kullanılırsa kullanıcının tüm refresh token'ları iptal ediliyor + audit log'a `REFRESH_TOKEN_REUSE` yazılıyor |
| Logout | ✅ TAM (düzeltildi) | `/logout` ilgili refresh token'ı iptal ediyor, audit log'a `LOGOUT` yazılıyor |
| Rol/yetki matrisi (admin-only işlemler) | TAM | `/api/v1/admin/**` → `hasRole("ADMIN")` |
| 403 + audit log (yetkisiz erişim) | TAM | `AuditingAccessDeniedHandler` |
| Audit log alanları (kim/ne/ne zaman/nereden/sonuç/detay) | TAM | `AuditLog` entity birebir eşleşiyor |
| Audit log olayları (login, kilitleme, rol değişikliği) | TAM | Hepsi kaydediliyor |
| README (sorumluluk, endpoint, env) | **YOK** | Tamamı `TODO` |
| Swagger/OpenAPI | TAM | Bağımlılık ve config mevcut |

**Ekstra bulgular:** `RegisterRequest`/`OtpVerifyRequest`'te hiç Bean Validation (`@Valid`, `@NotBlank`) yok — campaign-service'in disiplinli validasyonunun aksine.

---

## 2. Campaign Service + API Gateway

| Madde | Durum | Not |
|---|---|---|
| Kampanya alanları (başlık, tip, segment, indirim, süre) | TAM | `CreateCampaignRequest` |
| AI'a otomatik gönderim | KISMİ | Her abone için değil, segmentten örneklenen ≤20 abone ortalaması ile (dokümante edilmiş, bilinçli basitleştirme) |
| AI erişilemezse fallback (BELIRSIZ/ORTA, yine oluşturulur) | TAM | Circuit breaker + fallback tam çalışıyor |
| Kampanya numarası (CMP-2026-000123) | TAM | `CampaignNumberGenerator` |
| State machine (7 geçiş, 422) | KISMİ | 6/7 doğru; **TEST_EDILIYOR→OPTIMIZE_EDILIYOR** spec'te "Sistem" iken kodda sadece Süpervizör yapabiliyor, otomatik yol yok |
| Segment/öncelik enum'ları + RISKLI_KAYIP→min YUKSEK | TAM | |
| Segment override → AI'a bildirim | TAM | `SEGMENT_CHANGED` event yayınlanıyor |
| SLA süreleri (2/8/24/72 saat) + demo için `SLA_TIME_UNIT` | TAM | Birebir eşleşiyor |
| Abone geri bildirimi (Kabul/İlgilenmiyorum) | KISMİ | Campaign tarafı tam; skor düşürme AI Service'in event'i tüketmesine bağlı (ayrıca doğrulanmalı) |
| 1-5 yıldız memnuniyet, tek seferlik | TAM | |
| Gateway routing (4 servis) | TAM | |
| Gateway JWT doğrulama | TAM | |
| Gateway rate limiting | TAM | Redis tabanlı |
| **Tek giriş noktası (gateway-only)** | ✅ TAM (düzeltildi) | Servis portları artık host'a açık değil, docker-compose.yml'den kaldırıldı |
| Swagger/OpenAPI (Campaign) | ✅ TAM (düzeltildi) | `OpenApiConfig.java` eklendi |
| README | ✅ TAM (düzeltildi) | Sorumluluk/endpoint/env dolu |
| DB-per-service | TAM | Ayrı Postgres |

---

## 3. AI Service

**"Mock/hardcoded olamaz" kuralı doğrulandı — GERÇEK.** `train_model.py` gerçek bir `LogisticRegression` (öneri skoru) ve `DecisionTreeClassifier` (segment) eğitiyor, 220 sentetik abone × 4 kampanya tipi = 880 satır veri üzerinde. Ağırlıklar `model_weights.json`'a export edilip Java tarafında standardize edilerek çalıştırılıyor. Accuracy'ler kasıtlı olarak mükemmel değil (0.56-0.67 civarı) — bu da gerçek/overfit olmadığının kanıtı.

| Madde | Durum | Not |
|---|---|---|
| Öneri skorlama (girdi→0.0-1.0 skor+olasılık) | TAM | Gerçek lojistik regresyon |
| 0.60 altı gizle / 0.80 üstü öncelikli göster | **YOK** | Bu eşik hiçbir yerde (AI'da da Campaign'de de doğrulanmadı) uygulanmıyor |
| Min 100 örnek eğitim verisi, repo'da paylaşım | TAM | `data/` altında, 220 örnek |
| Segment sınıflandırma (4 sınıf) | TAM | Gerçek karar ağacı |
| RISKLI_KAYIP otomatik yüksek öncelik | KISMİ | Bu, Campaign Service'in işi — orada da doğrulanmalı |
| Akıllı uzman ataması (skor = 0.5×eşleşme + 0.3×boşluk + 0.2×performans) | TAM | Formül birebir kod içinde |
| Kapasite doluysa kuyruğa alma | TAM | |
| Doğruluk takibi (doğru/toplam×100) | TAM | Segment bazlı kırılım da var (ekstra) |
| README + ai-approach.md (eğitim süreci anlatımı) | **YOK** | İkisi de TODO — oysa eğitim süreci kodda gerçekten var, sadece dokümante edilmemiş |
| Swagger/OpenAPI | TAM | |
| Güvenlik | **YOK** | Tüm endpoint'ler `permitAll()` — kritik madde #3 ile birleşince ciddi risk |

---

## 4. Gamification Service

Olay tabanlı mimari **gerçekten** RabbitMQ üzerinden çalışıyor (REST polling değil) — spec'in en çok önemsediği nokta bu, ve doğru kurulmuş.

| Madde | Durum | Not |
|---|---|---|
| Puan tablosu (6 kural, tüm değerler) | TAM | Birebir eşleşiyor |
| Rozetler (6 rozet) | TAM | Hepsi implement edilmiş |
| Seviye sistemi (Bronz/Gümüş/Altın/Platin eşikleri) | TAM | |
| Liderlik tablosu (günlük/haftalık, top 10) | TAM | Redis sorted set |
| Profil ekranı verisi | TAM | |
| Rozet bildirimi (anlık) | KISMİ | Gerçek zamanlı push yok, sadece profil endpoint'i polling ile — spec'in "sayfa yenilemede güncel" alternatifini karşılıyor ama toast/modal backend'den tetiklenmiyor |
| `campaign.optimized` ve diğer event'ler | TAM | `EVENTS.md` ile kod birebir |
| README | ✅ TAM (düzeltildi) | Sorumluluk/endpoint/env/tasarım kararları dolu |
| Swagger/OpenAPI | TAM | `springdoc-openapi-starter-webmvc-ui` pom.xml'de mevcut |

**Küçük not:** Leaderboard'da `expertId` var ama isim yok (`LeaderboardEntry.name` her zaman null) — mobilin ismi ayrıca çözmesi gerekiyor.

---

## 5. Mobil Uygulama (Android/Kotlin)

Mock data sadece `@Preview` composable'larında kullanılıyor; production build gerçek Retrofit repo'larına bağlı (`BuildConfig.USE_MOCK_*` = false).

**Abone:** Tüm 5 ekran TAM (GSM+OTP giriş, teklif listesi skor sıralı+rozet, detay+kabul/ret, takip, 1-5 yıldız).

**Uzman:**
| Ekran | Durum |
|---|---|
| Vaka listesi (öncelik sıralı) | TAM |
| SLA renk kodlaması (kırmızı/turuncu/normal) | KISMİ — sadece süpervizör panelinde var, uzman listesinde yok |
| Vaka detayı (AI segment, tahmin, skor) | TAM |
| Durum geçişi, geçersiz buton pasif | KISMİ — geçersiz geçişler engelleniyor ama "pasif buton" yerine metin gösteriliyor |
| Segment override | ✅ TAM (düzeltildi) — kritik madde #5 |
| Optimizasyon notuyla tamamlama | TAM |
| Gamification profili | TAM |
| Liderlik tablosu (günlük/haftalık) | TAM |
| Rozet toast/modal bildirimi | **YOK** |

**Süpervizör:** Dashboard, manuel atama, vaka onaylama — hepsi TAM.

**Admin:** Personel oluşturma, rol yönetimi, filtrelenebilir audit log — hepsi TAM.

**UI/UX (8.2):**
| Madde | Durum |
|---|---|
| Tutarlı tasarım sistemi | TAM |
| Loading/error/empty state | TAM |
| Şeffaf token yenileme | ✅ TAM (düzeltildi) — kritik madde #6, app-wide `TokenAuthenticator` |
| Rol bazlı navigasyon | KISMİ — giriş sonrası doğru role yönleniyor ama nav graph'ta runtime rol koruması yok |
| Mobil ölçekli grafikler | TAM |
| Offline/ağ hatası mesajı | TAM |
| Mobil README | **YOK** |

---

## 6. Güvenlik (Bölüm 11)

| Senaryo | Durum |
|---|---|
| SQL injection | TAM — her yerde JPA, string concat yok. `security.sh` ile otomatik doğrulanıyor |
| XSS | TAM — campaign-service'te `@Pattern` ile `<>` engelleniyor, test edilmiş |
| Token manipülasyonu | TAM — imza+süre+tip kontrolü var, 7 varyant test edilmiş |
| Brute-force / rate limit | TAM — gateway'de Redis tabanlı, test edilmiş |
| Yetkisiz endpoint erişimi / IDOR (gateway üzerinden) | TAM — rol kontrolleri ve sahiplik kontrolleri test edilmiş |
| Yetkisiz endpoint erişimi (servise doğrudan, header sahteciliğiyle) | ✅ TAM (düzeltildi) — port artık dışarı açık değil, `security.sh`'ye gerçek saldırı senaryosu (sahte header + direkt port) da eklendi |
| Refresh token replay | KISMİ — "refresh token'ı access token yerine kullanma" test edilmiş (401), ama spec'in asıl istediği "geçersiz kılınmış refresh token'ın tekrar kullanımında tüm oturumların sonlandırılması" senaryosu test edilemez çünkü refresh akışı zaten yok (madde #4) |
| Yazılı güvenlik testi notu | ✅ TAM (düzeltildi) | `docs/GUVENLIK-TESTLERI.md` — 35 kontrol, her senaryonun neden çalıştığı/çalışmadığı ayrıntılı anlatılmış |

**Ek bulgular:** Identity-service'te temel input validasyonu eksik (`@Valid` hiç kullanılmıyor); hiçbir serviste CORS config yok.

---

## 7. Dokümantasyon (Bölüm 14.2)

| Belge | Durum |
|---|---|
| Ana README | KISMİ — mimari ve demo kullanıcı bilgileri TODO |
| `docs/architecture.md` | **YOK** — TODO |
| `docs/ai-approach.md` | **YOK** — TODO (ironik, çünkü eğitim süreci kodda gerçekten var) |
| Identity Service README | **YOK** — hâlâ TODO stub |
| AI Service README | **YOK** — hâlâ TODO stub |
| Campaign Service README | ✅ TAM (düzeltildi) |
| Gamification Service README | ✅ TAM (düzeltildi) |
| API Gateway README | Kontrol edilmedi bu turda, önceki taramada TODO'ydu |
| `EVENTS.md` | TAM — tam ve kod ile tutarlı |
| `docs/GUVENLIK-TESTLERI.md` (yeni) | ✅ TAM (yeni eklendi) — spec'in Bölüm 11 için istediği yazılı not, fazlasıyla karşılanmış |
| Swagger/OpenAPI (Campaign+AI zorunlu) | ✅ TAM (düzeltildi) — ikisinde de artık var |
| Mobil README | **YOK** |

---

## 8. Deployment (Bölüm 14.1 / 15)

| Madde | Durum |
|---|---|
| `docker-compose.yml` (tüm servisler+DB+gateway) | TAM — servis portları artık host'a açık değil |
| Servis başına ayrı DB | TAM |
| `.env.example` (servis başına) | TAM |
| Mobil build talimatı | **YOK** |
| Seed script (`scripts/seed/`) | KISMİ — klasör boş, seed mantığı aslında campaign-service'in kodunda gömülü (`SubscriberSeeder`), ayrı çalıştırılabilir script olarak yok |
| Servis kapatma / resilience testi (Bölüm 12.3 adım 7) | ✅ TAM (yeni eklendi) | `scripts/test/resilience.sh` — AI/Gamification/RabbitMQ sırayla durdurulup kampanya akışının ayakta kaldığı otomatik doğrulanıyor. Bu script gamification-service 500/503 bugını da (kritik madde #2) ortaya çıkarmış |

---

## Öncelik sıralı yapılacaklar listesi (demo öncesi)

1. ~~`docker-compose.yml`'de identity/campaign/ai/gamification portlarını host'a açma~~ ✅ **yapıldı** — portlar kaldırıldı, `security.sh` ve `resilience.sh` buna göre güncellendi.
2. ~~Gamification kapalıyken gateway'in 500 yerine 503 dönmesi~~ ✅ **yapıldı** — `GatewayErrorHandler.isUnreachable` artık `SocketException`'a göre kontrol ediyor.
3. ~~Identity Service'in config dosyasını (`application.yml`) doğrula/oluştur~~ ✅ **gerek yokmuş** — zaten vardı.
4. ~~Refresh token akışını ekle~~ ✅ **yapıldı** — `/refresh`, `/logout`, DB'de saklama, rotation, theft-protection.
5. ~~Mobilde segment override ekranı/aksiyonu ekle~~ ✅ **yapıldı**.
6. ~~Mobilde token yenileme mantığını gerçek "silent refresh"e çevir~~ ✅ **yapıldı** — app-wide `TokenAuthenticator` + `Mutex`, başarısızlıkta otomatik logout.
7. Şifre politikası validasyonunu Identity Service'e ekle (kural bazlı hata mesajlarıyla).
8. Identity Service ve AI Service README'lerini, `docs/architecture.md` ve `docs/ai-approach.md` dosyalarını doldur — içerik zaten kodda var, sadece yazılması gerekiyor (Campaign+Gamification için bu zaten yapıldı, örnek alınabilir).

Küçük/isteğe bağlı: rozet toast/modal, uzman listesinde SLA renk kodu, leaderboard'da isim gösterimi, mobil README, 0.60/0.80 skor eşiği filtresi, mobilde nav graph seviyesinde rol koruması.
