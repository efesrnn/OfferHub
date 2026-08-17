# OfferHub — Kişiselleştirilmiş Kampanya ve Öneri Platformu

**Staj Projesi · 20 İş Günü · 3 Kişilik Ekip**

| | |
|---|---|
| **Süre** | 20 iş günü (4 hafta) |
| **Ekip** | 3 stajyer — 2 backend, 1 mobil |
| **Mimari** | Mikroservis (zorunlu) |
| **Backend** | Serbest dil/framework |
| **Veritabanı** | Servis başına ayrı DB (zorunlu) |
| **Frontend** | Yalnızca mobil uygulama (iOS/Android/cross-platform serbest) |
| **Deployment** | Docker Compose (zorunlu) |
| **AI araçları** | Serbest (Copilot, Claude, Cursor vb.) |

---

## 1. Proje Tanımı

Bir telekomünikasyon operatörünün abonelerine **doğru zamanda doğru teklifi** sunan, yapay zeka destekli, mikroservis mimarili bir kişiselleştirilmiş kampanya ve öneri platformu geliştireceksiniz. Bu proje tek bir uygulama değil, birbiriyle konuşan bir **sistem ekosistemi** kurma pratiğidir.

OfferHub dört bağımsız mikroservisten oluşur:

- **Identity** — kimlik ve yetki yönetimi
- **Campaign** — kampanya yaşam döngüsü
- **AI** — öneri ve tahmin motoru
- **Gamification** — personel motivasyon sistemi

Servisler bir API Gateway arkasında çalışır ve tüm sistem `docker compose up` ile tek komutta ayağa kalkar.

### 1.1 Senaryo

Operatörün milyonlarca abonesi vardır ve her birinin kullanım alışkanlığı farklıdır. Herkese aynı kampanyayı göndermek hem bütçe israfı hem de müşteri rahatsızlığı yaratır. Bir abonenin ilgisini çekecek doğru teklifi (ek paket, tarife yükseltme, cihaz fırsatı) doğru anda sunmak, hem dönüşüm oranını artırır hem de müşteri memnuniyetini yükseltir.

OfferHub bu süreci akıllı hale getirir: bir abone için yapay zeka kullanım profilini analiz eder, en uygun kampanyayı önerir, dönüşüm olasılığını tahmin eder ve düşük dönüşümlü segmentleri kampanya uzmanına yönlendirir. Uzmanlar kampanyaları optimize ettikçe puan kazanır. Yöneticiler tüm kampanya performansını ve modelin isabetini tek ekrandan izler.

### 1.2 Kullanıcı Rolleri

| Rol | Kim? | Ne yapar? |
|---|---|---|
| **Abone** | Operatör müşterisi | Kişiselleştirilmiş teklifleri görür, kabul/ret eder, geri bildirim verir |
| **Kampanya Uzmanı** | Pazarlama çalışanı | Kampanya oluşturur, düşük performanslı segmentleri optimize eder, rozet kazanır |
| **Süpervizör** | Operasyon yöneticisi | Dashboard izler, model isabetini ve KPI'ları takip eder, manuel atama yapar |
| **Admin** | Sistem yöneticisi | Personel hesapları oluşturur, rol yönetir, audit log görür |

---

## 2. Ekip Yapısı ve İş Bölümü

Üç stajyer paralel çalışır. Servis sahiplikleri net olmalı; ancak **API sözleşmeleri ve event şemaları ortak kararla** belirlenir.

### Backend 1 — Kimlik, Gateway ve AI

- API Gateway kurulumu, routing, rate limiting, JWT doğrulama
- Identity Service (tamamı)
- AI Service (tamamı)
- Sistem genelinde güvenlik sertleştirmesi

### Backend 2 — Kampanya ve Oyunlaştırma

- Campaign Service (tamamı: state machine, SLA, segment yönetimi)
- Gamification Service (tamamı)
- Event altyapısı (message queue veya pub/sub kurulumu)
- Docker Compose orkestrasyonu ve seed veri

### Mobil — Tüm Kullanıcı Arayüzü

- Abone akışı (giriş, teklif listesi, kabul/ret, puanlama)
- Uzman paneli (vaka listesi, vaka detayı, durum geçişleri, optimizasyon notu)
- Süpervizör dashboard'u (grafikler, KPI kartları, manuel atama)
- Gamification ekranları (profil, rozetler, liderlik tablosu, rozet bildirimi)
- Ortak tasarım sistemi, loading/error/empty state yönetimi

### Ortak Sorumluluklar

- İlk 2 gün: API sözleşmesi ve event şeması tasarımı (üçü birlikte)
- Günlük 15 dakikalık stand-up
- Kod review: her PR en az bir takım arkadaşı tarafından incelenir
- Dokümantasyon: herkes kendi servisinin README'sini yazar

> **Not:** Mobil geliştirici, backend servisleri hazır olmadan bloke olmamalıdır. İlk hafta mock/stub API'lerle (örn. Postman Mock Server, MSW, json-server) çalışılır. API sözleşmesi bu yüzden gün 2'de kilitlenir.

---

## 3. Mimari Gereksinimler

### 3.1 Zorunlu Servisler

En az 4 bağımsız mikroservis + 1 API Gateway:

```
                    ┌─────────────────┐
   Mobil App ─────▶ │   API GATEWAY   │
                    └────────┬────────┘
          ┌────────────┬─────┴─────┬─────────────┐
          ▼            ▼           ▼             ▼
    ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌────────────┐
    │Identity │  │Campaign │  │   AI    │  │Gamification│
    │ Service │  │ Service │  │ Service │  │  Service   │
    └────┬────┘  └────┬────┘  └────┬────┘  └─────┬──────┘
         ▼            ▼            ▼             ▼
      [DB-1]       [DB-2]       [DB-3]        [DB-4]
```

| Servis | Sorumluluk |
|---|---|
| **API Gateway** | Tek giriş noktası. Routing, rate limiting, JWT doğrulama. Hazır gateway (Kong, Ocelot, Spring Cloud Gateway, Express Gateway) veya kendi yazdığınız reverse proxy kabul edilir. |
| **Identity Service** | Kayıt, giriş, token yönetimi, rol/yetki, audit log, hesap kilitleme. |
| **Campaign Service** | Kampanya yaşam döngüsü, segment ataması, SLA takibi, uzman notu. |
| **AI Service** | Öneri motoru, dönüşüm tahmini, akıllı uzman ataması. |
| **Gamification Service** | Puan, rozet, seviye, liderlik. Event ile tetiklenir. |

### 3.2 Mimari Kurallar

- **Database-per-service:** Her servis kendi veritabanına sahip olmalıdır. Bir servis başka servisin veritabanına doğrudan erişemez. DB tipi servise göre serbesttir (örn. Identity için PostgreSQL, Gamification için Redis + PostgreSQL).
- **Servisler arası iletişim:** REST çağrıları minimum kabul edilir. Message queue (RabbitMQ, Redis pub/sub, Kafka) kullanımı **hedeflenen** yaklaşımdır.
- **Bağımsızlık:** Bir servis çöktüğünde diğerleri çalışmaya devam etmelidir. Örneğin AI Service kapalıyken kampanya yine oluşturulabilmeli (segment `BELIRSIZ` olarak işaretlenir, manuel atamaya düşer).
- **Docker Compose:** Tüm sistem (servisler + veritabanları + gateway) `docker compose up` komutuyla ayağa kalkmalıdır.
- **Her servisin kendi README'si olmalı:** sorumluluk, endpoint listesi, environment değişkenleri.
- **Mobil uygulama** yalnızca API Gateway ile konuşur; servislere doğrudan istek atmaz.

---

## 4. Identity Service

### 4.1 Kayıt ve Giriş

- **Abone kaydı:** GSM numarası + OTP doğrulama (simülasyon: sabit kod `1234`). Kayıt alanları: ad, soyad, GSM, e-posta (opsiyonel).
- **Personel hesapları** (uzman ve süpervizör): Admin tarafından oluşturulur, e-posta + şifre ile giriş yapar. Oluştururken uzmanlık/bölge alanları atanır (birden fazla seçilebilir).
- **Şifre politikası:** minimum 8 karakter, en az 1 büyük harf, 1 rakam, 1 özel karakter. İhlalde **hangi kuralın** ihlal edildiğini belirten net hata mesajı dönmelidir.
- Şifreler **bcrypt veya argon2** ile hash'lenmelidir. Düz metin veya MD5/SHA1 kabul edilmez.
- **Hesap kilitleme:** 5 başarısız girişte hesap 15 dakika kilitlenir. Kilitli hesaba girişte kalan süre bilgisi dönmelidir.

### 4.2 Token Yönetimi

- **Access token:** JWT, 15 dakika geçerlilik. Payload'da `user_id`, rol, uzmanlık/bölge alanları.
- **Refresh token:** 7 gün geçerlilik, veritabanında saklanır.
- **Token rotation:** Refresh token kullanıldığında yeni refresh token üretilir, eskisi geçersiz kılınır. Geçersiz kılınmış bir refresh token tekrar kullanılmaya çalışılırsa o kullanıcının **tüm oturumları sonlandırılır** (token theft koruması).
- **Logout:** refresh token geçersiz kılınır.

### 4.3 Rol ve Yetki Matrisi

Endpoint seviyesinde uygulanmalıdır. Yetkisiz erişim denemesi **403** dönmeli ve audit log'a yazılmalıdır.

| İşlem | Abone | Uzman | Süpervizör | Admin |
|---|:---:|:---:|:---:|:---:|
| Kampanya oluşturma | – | ✓ | ✓ | – |
| Kendi kayıtlarını görme | ✓ | ✓ (atanan) | ✓ (tümü) | ✓ (tümü) |
| Vaka durumu değiştirme | – | ✓ | ✓ | – |
| Manuel atama | – | – | ✓ | – |
| Segment/tür değiştirme (AI override) | – | ✓ | ✓ | – |
| Dashboard görüntüleme | – | – | ✓ | ✓ |
| Personel hesabı oluşturma | – | – | – | ✓ |
| Audit log görüntüleme | – | – | – | ✓ |

### 4.4 Audit Log

Kaydedilmesi gereken işlemler:

- Başarılı ve başarısız giriş denemeleri
- Hesap kilitlenmesi
- Rol değişiklikleri
- Yetkisiz erişim denemeleri (403)
- Kampanya silme ve kritik durum değişiklikleri

Her log kaydında: **kim** (`user_id`), **ne** (işlem tipi), **ne zaman** (timestamp), **nereden** (IP), **sonuç** (başarılı/başarısız), **detay** (ilgili kaynak id'si).

---

## 5. Campaign Service

### 5.1 Kampanya Oluşturma

- Alanlar: başlık, tip (`EK_PAKET`, `TARIFE_YUKSELTME`, `CIHAZ_FIRSATI`, `SADAKAT`), hedef segment, indirim oranı, geçerlilik süresi.
- Kampanya bir abone segmentine hedeflendiğinde otomatik olarak AI Service'e gönderilir: her abone için dönüşüm olasılığı, öneri skoru ve segment atanır.
- AI Service erişilemez durumdaysa kampanya **yine oluşturulmalı** (segment: `BELIRSIZ`, öncelik: `ORTA`) ve manuel optimizasyon kuyruğuna düşmelidir.
- **Kampanya numarası:** benzersiz ve okunabilir (örn. `CMP-2026-000123`).

### 5.2 Optimizasyon Vakası Yaşam Döngüsü

Düşük dönüşümlü kampanyalar bir optimizasyon vakasına dönüşür. Kural dışı geçiş **422** dönmelidir.

| Mevcut Durum | Hedef Durum | Kim Yapabilir | Koşul |
|---|---|---|---|
| `YENI` | `ATANDI` | Sistem (AI) / Süpervizör | Uzman belirlendi |
| `ATANDI` | `OPTIMIZE_EDILIYOR` | Uzman | Uzman çalışmaya başladı |
| `OPTIMIZE_EDILIYOR` | `TEST_EDILIYOR` | Uzman | A/B testi başlatıldı |
| `TEST_EDILIYOR` | `OPTIMIZE_EDILIYOR` | Sistem | Test sonuçlandı |
| `OPTIMIZE_EDILIYOR` | `TAMAMLANDI` | Uzman | Optimizasyon notu zorunlu |
| `TAMAMLANDI` | `YAYINDA` | Süpervizör | Onay verildi |
| `YAYINDA` | `ARSIVLENDI` | Sistem | Geçerlilik doldu |

### 5.3 Segment Türleri ve Öncelik

- **Segmentler:** `YUKSEK_DEGER`, `RISKLI_KAYIP`, `YENI_ABONE`, `PASIF`, `BELIRSIZ`
- Segment AI tarafından atanır; uzman veya süpervizör değiştirebilir.
- Uzman segment değiştirdiğinde bu değişiklik AI Service'e bildirilmelidir (doğruluk metriği için).
- **Öncelikler:** `DUSUK`, `ORTA`, `YUKSEK`, `KRITIK`. AI dönüşüm potansiyeline göre atar. `RISKLI_KAYIP` segment → minimum `YUKSEK`. Süpervizör manuel değiştirebilir.

### 5.4 SLA Kuralları

| Öncelik | SLA Süresi | Aşım Durumunda |
|---|---|---|
| `KRITIK` | 2 saat | Vaka kırmızı işaretlenir, süpervizör ekranında en üstte görünür |
| `YUKSEK` | 8 saat | Vaka turuncu işaretlenir |
| `ORTA` | 24 saat | Görsel uyarı |
| `DUSUK` | 72 saat | Görsel uyarı |

SLA süresi vaka oluşturma anından itibaren sayılır, optimizasyon tamamlanınca durur. Kalan SLA hem uzman hem süpervizör ekranında görünür olmalıdır.

> **Demo kolaylığı:** SLA sürelerini environment değişkeniyle (örn. `SLA_TIME_UNIT=minutes`) kısaltılabilir yapın; aksi halde 2 saatlik SLA aşımını demoda gösteremezsiniz.

### 5.5 Abone Geri Bildirimi

- Aboneye kişiselleştirilmiş teklif bildirimi gönderilir (simülasyon: uygulama içi).
- Abone teklifi **Kabul** veya **İlgilenmiyorum** olarak yanıtlar; yanıt dönüşüm verisine işlenir.
- Abone "ilgilenmiyorum" derse benzer kampanyaların öneri skoru düşer.

### 5.6 Abone Memnuniyeti

- Teklif etkileşimi sonrasında abone deneyimi **1–5 yıldız** puanlar (alakasız teklif → düşük puan).
- Puanlama tek seferliktir. Puan verildiğinde Gamification Service'e event gönderilir.

---

## 6. AI Service

Bu servis projenin kalbidir. Üç görevi vardır ve her üçü de zorunludur.

### 6.1 Görev 1 — Öneri Skorlama

- **Girdi:** abone profili (kullanım verisi, mevcut tarife, geçmiş kabuller, harcama).
  **Çıktı:** her kampanya için öneri skoru (0.0–1.0) + dönüşüm olasılığı.
- Öneri skoru **0.60'ın altındaki** kampanyalar aboneye gösterilmez; skor **> 0.80** ise öncelikli gösterilir.
- **Yaklaşım serbesttir:**
  - (a) kendi eğittiğiniz klasik ML modeli (scikit-learn vb.)
  - (b) kural tabanlı + ML hibrit
  - (c) LLM API entegrasyonu
- Kendi veri setinizle model eğitirseniz eğitim verisini repository'de paylaşın ve README'de eğitim sürecini anlatın.
- **Eğitim/test verisini kendiniz oluşturacaksınız:** gerçekçi Türkçe abone profili örnekleri ve kampanya kabul/ret geçmişi (örn. "yüksek veri kullanımı + sık paket alımı", "düşük kullanım + şikayet geçmişi"). Minimum 100 örnek önerilir; AI araçlarıyla üretebilirsiniz.

> **Kritik kural:** AI servisi mock/hardcoded olamaz. Her girdiye sabit çıktı dönen bir servis kabul edilmez — girdi değiştiğinde çıktı da değişmelidir.

### 6.2 Görev 2 — Segment Sınıflandırma

- **Girdi:** abone davranış verisi. **Çıktı:** `YUKSEK_DEGER`, `RISKLI_KAYIP`, `YENI_ABONE`, `PASIF`.
- `RISKLI_KAYIP` (churn riski) segmenti otomatik yüksek öncelik alır; uzman önce bunlara odaklanır.

### 6.3 Görev 3 — Akıllı Uzman Ataması

Segmenti/skoru belirlenen optimizasyon vakası, uygun kişiye otomatik atanmalıdır. Atama bir **skorlama algoritmasına** dayanmalıdır. Örnek formül (kendi formülünüzü tasarlayabilirsiniz):

```
skor = (uzmanlik_eslesme × 0.5) + (bosluk_orani × 0.3) + (performans × 0.2)
```

- `uzmanlik_eslesme`: Uzmanın uzmanlık alanı (örn. churn önleme) vaka segmentiyle eşleşiyorsa 1, değilse 0
- `bosluk_orani`: `1 - (aktif vaka / maksimum kapasite)`. Kapasite uzman başına 10 aktif vaka
- `performans`: Uzmanın ortalama dönüşüm artışı skoru
- En yüksek skorlu uzmana atama yapılır. Kapasite yoksa vaka kuyruğa alınır
- Süpervizör her zaman manuel atama yapabilir

### 6.4 Doğruluk Takibi

- Uzman veya süpervizör AI'ın atadığı segmenti/türü değiştirirse bu **"yanlış sınıflandırma"** olarak kaydedilmelidir.
- Süpervizör ekranında AI doğruluk oranı gösterilmelidir: `doğru / toplam × 100`.
- Segment bazlı doğruluk kırılımı (hangi türde ne kadar isabetli) hedeflenen bir ekstradır.

---

## 7. Gamification Service

Personelin motivasyonunu artıran puan, rozet, seviye ve liderlik sistemi. Bu servis Campaign Service'ten gelen **event'lerle** çalışır — doğrudan çağrı değil, olay tabanlı mimari beklenir.

### 7.1 Puan Tablosu

| Olay | Puan | Koşul |
|---|---|---|
| Optimizasyon tamamlandı (`TAMAMLANDI`) | +10 | Her tamamlama |
| Hızlı optimizasyon bonusu | +5 | 2 saatten kısa |
| Dönüşüm hedefi aşıldı | +15 | Hedef üstü sonuç |
| `KRITIK` vaka SLA içinde tamamlandı | +15 | SLA içinde |
| SLA aşımı | −5 | Her aşım |
| Abone düşük puan verdi (alakasız teklif) | −3 | 1–2 yıldız |

### 7.2 Rozetler

| Rozet | Kazanılma Koşulu |
|---|---|
| İlk Kampanya | İlk optimizasyonu tamamlama |
| Hız Ustası | 2 saatin altında 10 optimizasyon |
| Dönüşüm Kralı | 10 kampanyada hedef aşımı |
| Maratoncu | Bir günde 20 optimizasyon |
| Churn Avcısı | 10 `RISKLI_KAYIP` vakayı kurtarma |
| Uzman | Tek segmentte 50 optimizasyon |

### 7.3 Seviye Sistemi

| Seviye | Puan Aralığı | Görsel |
|---|---|---|
| Bronz | 0 – 499 | Bronz rozet/çerçeve |
| Gümüş | 500 – 1.499 | Gümüş rozet/çerçeve |
| Altın | 1.500 – 2.999 | Altın rozet/çerçeve |
| Platin | 3.000+ | Platin rozet/çerçeve |

### 7.4 Liderlik Tablosu ve Profil

- Günlük ve haftalık liderlik tablosu: ilk 10 kişi, puan sıralı.
- Liderlik tablosu gerçek zamanlı veya sayfa yenilemede güncel olmalıdır.
- **Profil ekranı:** toplam puan, seviye, kazanılan rozetler, günlük/haftalık sıralama, çözülen vaka sayısı, ortalama puan.
- Rozet kazanıldığı anda kişiye görsel bildirim gösterilmelidir (toast/modal).

---

## 8. Mobil Uygulama Gereksinimleri

Tek platform (iOS **veya** Android **veya** cross-platform) yeterlidir. Teknoloji seçimi serbesttir (React Native, Flutter, native Kotlin/Swift).

### 8.1 Zorunlu Ekranlar

**Abone**
- GSM + OTP giriş ekranı
- Kişiselleştirilmiş teklif listesi (skoru yüksek teklif üstte, rozet/etiket ile vurgulu)
- Teklif detayı + Kabul / İlgilenmiyorum
- Kabul edilen kampanyaları takip
- 1–5 yıldız memnuniyet puanlama

**Uzman**
- E-posta + şifre giriş
- Atanan vakalar listesi (öncelik sıralı, SLA renk kodlu: kırmızı/turuncu/normal)
- Vaka detayı: AI segmenti, dönüşüm tahmini, öneri skoru
- Durum geçiş aksiyonları (state machine'e uygun; geçersiz geçiş butonu pasif)
- Segment override (AI doğruluk takibini tetikler)
- Optimizasyon notu ile tamamlama
- Gamification profili: puan, seviye, rozetler
- Liderlik tablosu (günlük/haftalık sekmeli)
- Rozet kazanıldığında toast/modal bildirimi

**Süpervizör**
- Dashboard (mobil düzende, dikey kaydırmalı kartlar):
  - Segment bazlı kampanya dağılımı — pasta veya bar grafik
  - Dönüşüm oranları ve trend
  - SLA uyum oranı + SLA aşmış aktif vakalar
  - AI doğruluk metriği (dönüşüm tahmini isabet oranı)
  - Uzman performansı: tamamlanan vaka, ortalama dönüşüm artışı, süre
  - Bekleyen optimizasyon kuyruğu (`BELIRSIZ` veya kapasite bekleyen)
- Manuel atama ekranı
- Vaka onaylama (`TAMAMLANDI` → `YAYINDA`)

**Admin**
- Personel hesabı oluşturma (uzmanlık/bölge seçimi ile)
- Rol yönetimi
- Audit log listesi (filtrelenebilir)

### 8.2 UI/UX Beklentileri

- Tutarlı bir tasarım sistemi: renk paleti, tipografi, komponent kütüphanesi
- Her ekranda **loading / error / empty** state'leri
- Token yenileme şeffaf olmalı (access token dolunca kullanıcı atılmamalı)
- Rol bazlı navigasyon: kullanıcı yetkisi olmayan ekranı göremez
- Grafikler mobil ekrana uygun ölçeklenmeli (yatay kaydırma değil, responsive)
- Offline/ağ hatası durumunda anlamlı mesaj

---

## 9. API Tasarımı

Tüm endpoint listesi verilmemektedir. API'nizi **RESTful prensiplere uygun olarak kendiniz tasarlayacaksınız.**

### 9.1 Gateway Routing Örneği

```
/api/v1/auth/**        → Identity Service
/api/v1/campaigns/**   → Campaign Service
/api/v1/ai/**          → AI Service
/api/v1/game/**        → Gamification Service
```

### 9.2 Örnek Endpoint'ler

| Method | Endpoint | Açıklama |
|---|---|---|
| `POST` | `/api/v1/campaigns` | Kampanya oluştur (AI öneri analizi tetiklenir) |
| `POST` | `/api/v1/ai/recommend` | Öneri skorlama (Campaign Service çağırır) |
| `GET` | `/api/v1/game/leaderboard?period=daily` | Günlük liderlik tablosu |
| `GET` | `/api/v1/subscribers/:id/offers` | Aboneye özel teklifler |

Geri kalan tüm endpoint'leri kendiniz tasarlayın. Standart response formatı kullanın:

```json
{ "success": true, "data": { }, "error": null }
```

API'nizi **Swagger/OpenAPI** ile dokümante edin (en az Campaign ve AI servisleri için).

---

## 10. Servisler Arası Event Akışı

Servisler arası iletişimde **olay (event) tabanlı** tasarım beklenir. Aşağıda uçtan uca bir örnek verilmiştir; diğer tüm event'leri kendiniz tasarlayıp dokümante edeceksiniz.

### 10.1 Örnek Akış — `campaign.optimized`

1. Uzman vakayı `TAMAMLANDI` durumuna çeker (Campaign Service)
2. Campaign Service `campaign.optimized` event'i yayınlar
3. Gamification Service dinler, süreyi ve dönüşümü hesaplar
4. Puan ekler (+10, hedef aşıldıysa +15)
5. Rozet koşullarını kontrol eder, `badge.earned` üretir
6. Mobil uygulamaya bildirim yansır

### 10.2 Örnek Payload

```json
{
  "event_type": "campaign.optimized",
  "timestamp": "2026-07-18T14:22:10Z",
  "payload": {
    "case_id": "CMP-2026-000123",
    "expert_id": "a7f3...",
    "segment": "RISKLI_KAYIP",
    "priority": "YUKSEK",
    "conversion_lift": 0.18,
    "created_at": "2026-07-18T13:40:02Z",
    "completed_at": "2026-07-18T14:22:10Z"
  }
}
```

Tasarlamanız gereken diğer event örnekleri: kampanya oluşturuldu, segment değiştirildi (AI doğruluk takibi), abone teklifi yanıtladı, SLA aşıldı.

Event'lerinizi README veya ayrı bir **`EVENTS.md`** dosyasında dokümante edin.

---

## 11. Güvenlik Gereksinimleri

Kapanış demosunda mentörler sisteminize kasıtlı saldırı senaryoları deneyecektir. Bu bir "yakalanma" testi değil, **güvenli kod yazma pratiğidir** — hangi açığın neden oluştuğunu anlatabilmeniz de sonucu kadar önemlidir.

Test edilecek senaryolar (tam liste değildir):

- **SQL injection:** form alanlarına `' OR 1=1 --` benzeri girdiler
- **Yetkisiz endpoint erişimi:** abone token'ıyla süpervizör endpoint'i çağırma
- **IDOR:** kayıt ID'sini değiştirerek başkasının verisini görme
- **Token manipülasyonu:** süresi dolmuş veya değiştirilmiş JWT ile istek
- **Refresh token replay:** geçersiz kılınmış refresh token'ın yeniden kullanımı
- **XSS:** metin alanına script etiketi enjeksiyonu
- **Brute-force:** ardışık hızlı giriş denemeleri (rate limit testi)

Her stajyerin kendi servisi için bu senaryoları **kendi kendine test etmiş** olması beklenir. Bulduğunuz ve kapattığınız açıkları kısa bir notla kayda geçirin.

---

## 12. Kullanıcı Akışları ve Kapanış Demosu

### 12.1 Abone Akışı

GSM + OTP ile giriş → Kişiselleştirilmiş teklifi gör → AI önerisi: en uygun kampanya + indirim → Teklifi kabul et veya reddet → Kabul edilen kampanyayı takip et → Deneyimi 1–5 yıldız puanla

### 12.2 Kampanya Uzmanı Akışı

Panele giriş → Atanan optimizasyon vakalarını öncelik sıralı gör → Vakayı aç: AI segment + dönüşüm tahmini görünür → A/B testi yap → Optimizasyonu notla tamamla → Puan ve rozet kazan

### 12.3 Kapanış Demosu — Zorunlu Senaryo

Son gün aşağıdaki uçtan uca akışı **canlı** göstermeniz gerekir:

1. `docker compose up` ile tüm sistemi ayağa kaldır
2. Kampanya uzmanı olarak bir kampanya oluştur ve segmente hedefle
3. AI'ın öneri skoru + segment + dönüşüm tahmini atamasını göster
4. Düşük performanslı segmentin doğru uzmana atandığını göster
5. Uzman olarak optimizasyonu tamamla
6. Puanın liderlik tablosuna yansıdığını göster
7. **Bir servisi kapat** (`docker stop`) ve sistemin geri kalanının çalıştığını kanıtla
8. Güvenlik senaryolarını birlikte deneyin

> **Servis kapatma adımı bu projenin en önemli anıdır.** Mikroservis mimarinizin gerçekten bağımsız çalıştığını burada kanıtlarsınız. Bu adım için mutlaka önceden prova yapın.

---

## 13. 20 Günlük Plan

### Hafta 1 — Temel ve Sözleşme (Gün 1–5)

| Gün | Hedef |
|---|---|
| 1 | Kickoff, case okuma, teknoloji seçimi, repo kurulumu, branch stratejisi |
| 2 | **API sözleşmesi ve event şeması tasarımı (ortak).** Mimari diyagram. Mobil için mock API kurulumu |
| 3 | Docker Compose iskeleti + boş servisler ayağa kalkıyor. Gateway routing çalışıyor |
| 4–5 | Identity: kayıt/giriş/JWT · Campaign: entity + CRUD · Mobil: tasarım sistemi + giriş ekranları |

**Hafta sonu çıktısı:** `docker compose up` çalışıyor, dört servis health-check dönüyor, mobil uygulama mock API ile giriş yapabiliyor.

### Hafta 2 — Çekirdek İş Mantığı (Gün 6–10)

| Gün | Hedef |
|---|---|
| 6–7 | Identity: rol matrisi, token rotation, hesap kilitleme, audit log · Campaign: state machine + kampanya numarası · Mobil: abone teklif listesi |
| 8–9 | AI Service: veri seti üretimi + öneri skorlama + segment sınıflandırma · Campaign: AI entegrasyonu + fallback (`BELIRSIZ`) · Mobil: uzman vaka listesi |
| 10 | **Ara demo:** Kampanya oluşturma → AI skorlama → segment atama akışı uçtan uca çalışıyor |

**Hafta sonu çıktısı:** Uçtan uca ilk akış ayakta. Mobil gerçek API'ye bağlandı.

### Hafta 3 — Event Mimarisi ve İkincil Akışlar (Gün 11–15)

| Gün | Hedef |
|---|---|
| 11–12 | Message queue kurulumu · `campaign.optimized` event'i yayınlanıyor ve dinleniyor · Gamification: puan motoru |
| 13 | Gamification: rozet + seviye + liderlik tablosu · AI: akıllı uzman ataması |
| 14 | Campaign: SLA takibi + SLA aşım event'i · AI doğruluk takibi (segment override) |
| 15 | Mobil: süpervizör dashboard grafikleri, gamification profili, rozet bildirimi |

**Hafta sonu çıktısı:** Tüm zorunlu fonksiyonlar çalışıyor. `EVENTS.md` yazıldı.

### Hafta 4 — Sertleştirme, Test, Teslim (Gün 16–20)

| Gün | Hedef |
|---|---|
| 16 | Güvenlik sertleştirmesi: rate limiting, input validation, IDOR kontrolü, XSS temizliği. Kendi güvenlik testlerinizi çalıştırın |
| 17 | Unit + integration testler · Swagger/OpenAPI tamamlama · Seed veri scripti |
| 18 | README'ler, `EVENTS.md`, AI yaklaşım dokümanı · Servis kapatma (resilience) testleri |
| 19 | **Prova:** demo senaryosunu baştan sona en az iki kez çalıştırın. Bug fix. Temiz kurulumda `docker compose up` testi |
| 20 | Kapanış sunumu ve canlı demo |

---

## 14. Teslimat

### 14.1 Kod

- Git repository (monorepo veya servis başına ayrı repo — ikisi de kabul)
- Ana branch'te **çalışır durumda** kod, anlamlı commit geçmişi
- Kök dizinde `docker-compose.yml`: tüm servisler + veritabanları + gateway
- `.env.example` dosyaları (servis başına)
- Mobil uygulama için build talimatı ve (varsa) APK/TestFlight bağlantısı

### 14.2 Dokümantasyon

- **Ana README:** sistem genel bakış, mimari diyagram, kurulum (`docker compose up` + seed), demo kullanıcı bilgileri
- **Servis başına README:** sorumluluk, endpoint listesi, environment değişkenleri
- **`EVENTS.md`:** tasarladığınız tüm event'ler ve payload'ları
- **AI yaklaşım dokümanı:** hangi yöntemi seçtiniz, neden, nasıl çalışıyor. Model eğittiyseniz eğitim verisi ve süreci
- **Swagger/OpenAPI** dokümantasyonu (en az Campaign ve AI servisleri için)
- **Mobil README:** kurulum, ekran listesi, state yönetimi yaklaşımı

### 14.3 Kapanış Sunumu (~20 dakika)

- **Canlı demo (8–10 dk):** Bölüm 12.3'teki zorunlu senaryonun tamamı
- **Mimari anlatımı (5 dk):** servis sorumlulukları, event akışı, AI yaklaşımı, güvenlik önlemleri
- **Zorluklar ve çözümler (3 dk):** neyi neden değiştirdiniz, nerede takıldınız
- **Soru-cevap ve güvenlik denemeleri**

---

## 15. Çalışma Kuralları

- AI araçları (Copilot, ChatGPT, Claude, Cursor vb.) **tamamen serbesttir** — ancak yazdığınız her satırı açıklayabilmeniz beklenir
- Boş proje scaffold/boilerplate ile başlamak serbesttir
- Hazır, önceden yazılmış iş mantığı kodu ile başlamak beklenmez — iş mantığını siz kuracaksınız
- Mentörler teknik yönlendirme yapar, kod yazmaz
- Her stajyer **günlük öğrenme kaydı** tutar: ne yaptım, nerede takıldım, ne öğrendim

### Kabul Kriterleri (Minimum)

Proje aşağıdakiler sağlandığında "tamamlandı" sayılır:

- [ ] `docker compose up` ile sistem tek komutta ayağa kalkıyor
- [ ] Dört servis birbirinden bağımsız, her biri kendi veritabanını kullanıyor
- [ ] Bir servis kapatıldığında diğerleri çalışmaya devam ediyor
- [ ] AI servisi gerçek bir hesaplama yapıyor (girdi değişince çıktı değişiyor)
- [ ] State machine kural dışı geçişleri reddediyor (422)
- [ ] Rol yetki matrisi endpoint seviyesinde uygulanmış
- [ ] Event tabanlı puan/rozet akışı çalışıyor
- [ ] Mobil uygulamada dört rolün de akışı gösterilebiliyor
- [ ] Dokümantasyon eksiksiz

---

**Başarılar. 🎯 Doğru teklif, doğru kişiye.**
