# OfferHub — Spec Karşılaştırma ve Eksik Analizi

*Kaynak: `offerhub-staj-projesi.md` vs. gerçek kod (backend/*, mobile/OfferHub). İlk tarama: 2026-09-10. Güncelleme: 2026-09-10, backend2'nin (Campaign+Gamification) yeni push'u sonrası.*
*Not: Bu, tüm servisleri paralel tarayan agent'ların bulgularının birleştirilmiş halidir. Her madde TAM / KISMİ / YOK olarak işaretli, dosya/satır referanslarıyla.*

---

## ✅ Düzeltilenler

- **Kritik madde #1 kapatıldı: gateway bypass.** `docker-compose.yml`'de identity(8081)/campaign(8082)/ai(8083)/gamification(8084) servislerinin host port eşlemeleri kaldırıldı — bu servisler artık yalnızca docker ağı içinden (servis adıyla) erişilebilir, dışarıdan hiç ulaşılamaz. `CallerIdentityArgumentResolver`'ın header imzası doğrulamaması artık önemsiz, çünkü sahte header'lı isteğin ulaşacağı bir port yok. Yan etki olarak `scripts/test/security.sh`'deki "gateway atlatma" testi güncellendi (artık 403 değil bağlantı reddi `000` bekliyor, ayrıca gerçek saldırı senaryosunu — sahte header + direkt port — da test ediyor) ve `scripts/test/resilience.sh`'deki gamification sağlık kontrolü artık `docker inspect` ile container healthcheck'ine bakıyor (eskiden kapanan porttan actuator'a curl atıyordu).
- Campaign Service ve Gamification Service README'leri artık dolu (sorumluluk, endpoint listesi, env değişkenleri) — önceden TODO stub'dı.
- Campaign Service'e Swagger/OpenAPI eklendi (`OpenApiConfig.java`) — Gamification'da zaten vardı, ikisi de artık TAM.
- Yazılı güvenlik testi notu artık var, spec'in istediğinin çok ötesinde: `docs/GUVENLIK-TESTLERI.md` + `scripts/test/security.sh` — 35 otomatik kontrol (SQL injection, XSS, token manipülasyonu, IDOR, brute-force).
- Resilience/servis kapatma testi eklendi: `scripts/test/resilience.sh` — AI ve Gamification kapatılınca kampanya akışının ayakta kaldığı, AI kapalıyken BELIRSIZ/ORTA fallback'in çalıştığı otomatik doğrulanıyor.
- `TEST_EDILIYOR→OPTIMIZE_EDILIYOR` geçişinin hâlâ sadece Süpervizör'de olması artık bilinçli/dokümante edilmiş bir tasarım kararı ("Sistem" satırlarının arkasında henüz scheduler yok) — unutulmuş bir eksik değil.

## 🔴 Demoyu veya çalışmayı doğrudan riske atan kritik maddeler

1. ~~Gateway bypass açığı~~ ✅ **kapatıldı** — yukarıdaki "Düzeltilenler" bölümüne bakın.
2. **Gamification kapalıyken gateway 500 dönüyor, 503 değil.** `resilience.sh` içinde backend2 tarafından bulunup açıkça "BİLİNEN AÇIK (Backend1)" diye işaretlenmiş: `GatewayErrorHandler.isUnreachable` yalnızca `AnnotatedConnectException`'ı yakalıyor, container durunca bazen fırlatılan `AnnotatedNoRouteToHostException`'ı yakalamıyor. Ortak atası `SocketException` — kontrol ona çevrilmeli. Bu senin (backend1) tarafında düzeltilmesi gereken bir madde.
3. **Identity Service'te `application.yml`/`.properties` bulunamadı** (`src/main/resources` içinde sadece `.gitkeep` var). `jwt.secret`, token süreleri, admin seed, SMS key gibi `@Value` bağlamaları için config kaynağı yok — servis bu haliyle **açılmayabilir**. İlk iş bunu doğrulamak/eklemek.
4. **Refresh token akışı yok (spec 4.2'nin çekirdeği).** `/refresh` ve `/logout` endpoint'leri hiç yok. Refresh token DB'de saklanmıyor, rotation/theft-protection yok. Access token 15 dk dolunca kullanıcı tekrar login olmak zorunda.
5. **Mobilde segment override özelliği tamamen yok** (spec'te açıkça zorunlu, AI doğruluk takibini tetikliyor). Kod içinde `override` diye aratınca sadece Kotlin'in `override fun` anahtar kelimesi çıkıyor.
6. **Mobilde token yenileme "şeffaf" değil, tam tersi.** Spec "access token dolunca kullanıcı atılmamalı" diyor; kodda süre dolunca oturum siliniyor ve kullanıcı login ekranına düşüyor. `AuthRepository`'de `refresh()` metodu bile yok.

Not: 2-6 maddeler Identity Service ve Mobil'e ait, backend2'nin push'u kapsamıyor.

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
| Refresh token (7 gün, DB'de saklı) | **YOK** | Stateless JWT üretiliyor, DB kaydı yok |
| Token rotation + theft protection | **YOK** | `/refresh` endpoint'i yok |
| Logout | **YOK** | `/logout` endpoint'i yok |
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
| Segment override | **YOK** — kritik madde #4 |
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
| Şeffaf token yenileme | **YOK** — kritik madde #5, `refresh()` metodu yok |
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
2. Gamification kapalıyken gateway'in 500 yerine 503 dönmesi için `GatewayErrorHandler.isUnreachable`'ı `SocketException`'a göre kontrol edecek şekilde düzelt (kritik madde #2, backend2 tarafından bulundu).
3. Identity Service'in config dosyasını (`application.yml`) doğrula/oluştur — servis açılmıyor olabilir.
4. Refresh token akışını ekle (`/refresh`, `/logout`, DB'de saklama, rotation) — spec'in çekirdek gereksinimi ve demo senaryosunda test edilebilir.
5. Mobilde segment override ekranı/aksiyonu ekle.
6. Mobilde token yenileme mantığını gerçek "silent refresh"e çevir (401 alınca refresh dene, olmazsa logout).
7. Şifre politikası validasyonunu Identity Service'e ekle (kural bazlı hata mesajlarıyla).
8. Identity Service ve AI Service README'lerini, `docs/architecture.md` ve `docs/ai-approach.md` dosyalarını doldur — içerik zaten kodda var, sadece yazılması gerekiyor (Campaign+Gamification için bu zaten yapıldı, örnek alınabilir).

Küçük/isteğe bağlı: rozet toast/modal, uzman listesinde SLA renk kodu, leaderboard'da isim gösterimi, mobil README, 0.60/0.80 skor eşiği filtresi, mobilde nav graph seviyesinde rol koruması.
