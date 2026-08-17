# OfferHub — Kişi Bazında 20 Günlük Görev Dağılımı

> Bu dokuman, case dokümanındaki "Bölüm 13 — 20 Günlük Plan"ı üç stajyer için (Backend 1, Backend 2, Mobil) günlük iş kırılımına dönüştürür. Amaç: kimin hangi gün ne teslim ettiğinin net olması, blokaj riskinin erken görülmesi.

**Roller (case'e göre):**
- **B1 (Backend 1):** API Gateway, Identity Service, AI Service, sistem güvenliği
- **B2 (Backend 2):** Campaign Service, Gamification Service, event altyapısı (MQ), Docker Compose orkestrasyonu, seed veri
- **M (Mobil):** Tüm ekranlar, tasarım sistemi

---

## Hafta 1 — Temel ve Sözleşme

### Gün 1 — Kickoff
| Kişi | Görev |
|---|---|
| B1 | Case'i oku, teknoloji stack kararı (dil/framework), repo yapısını kur, kendi servis klasörünü aç |
| B2 | Case'i oku, teknoloji stack kararı, repo yapısı + branch stratejisi önerisi, Docker Compose taslak dosyası aç |
| M | Case'i oku, platform/framework kararı (RN/Flutter/native), proje iskeletini aç |
| **Ortak** | Branch stratejisi (git flow / trunk-based) üzerinde anlaşma, ilk sync toplantısı |

### Gün 2 — API Sözleşmesi ve Event Şeması (Tamamı Ortak)
| Kişi | Görev |
|---|---|
| Tamamı | Servisler arası REST sözleşmelerini (endpoint, request/response şeması) birlikte yazıp kilitleme |
| Tamamı | Event şemalarının taslağını çıkarma (`campaign.created`, `campaign.optimized`, `segment.changed`, `offer.responded`, `sla.breached`) |
| B1 | Mimari diyagramı ilk taslağını çizme |
| M | Mock API planı: Postman Mock Server / MSW / json-server seçimi ve kurulum başlangıcı |

### Gün 3 — İskelet Ayağa Kalkıyor
| Kişi | Görev |
|---|---|
| B1 | API Gateway kurulumu (Kong/Ocelot/Spring Cloud Gateway veya custom reverse proxy), routing kuralları (`/api/v1/auth`, `/api/v1/ai`) |
| B2 | Docker Compose iskeleti: 4 servis + 4 DB + gateway için boş container tanımları, `.env.example` dosyaları |
| M | Mock API ile bağlanacak proje yapısı, network layer / API client kurulumu |
| **Hedef** | Gün sonunda `docker compose up` çalışıyor, health-check endpoint'leri dönüyor |

### Gün 4–5 — İlk İş Mantığı
| Kişi | Gün 4 | Gün 5 |
|---|---|---|
| B1 | Identity: kayıt/giriş endpoint iskeleti, şifre hash (bcrypt/argon2) | Identity: JWT üretimi (access+refresh), login flow tamamlama |
| B2 | Campaign: entity/model tasarımı, DB migration | Campaign: CRUD endpoint'leri (create/read/update) |
| M | Tasarım sistemi: renk paleti, tipografi, temel komponentler | Giriş ekranları: abone (GSM+OTP) ve personel (e-posta+şifre) UI, mock API'ye bağlama |

**Hafta sonu çıktısı:** `docker compose up` çalışıyor, dört servis health-check dönüyor, mobil mock API ile giriş yapabiliyor.

---

## Hafta 2 — Çekirdek İş Mantığı

### Gün 6–7
| Kişi | Gün 6 | Gün 7 |
|---|---|---|
| B1 | Rol/yetki matrisi (endpoint seviyesinde, 403 + audit log), hesap kilitleme (5 deneme/15 dk) | Token rotation (refresh token theft koruması), audit log tablosu ve kayıt mekanizması |
| B2 | Campaign state machine (durumlar + geçiş kuralları, 422 hata) | Kampanya numarası üretimi (`CMP-2026-000123`), segment/öncelik alanları |
| M | Abone teklif listesi ekranı (mock veriyle), skor sıralaması | Teklif detayı + Kabul/İlgilenmiyorum aksiyonları |

### Gün 8–9
| Kişi | Gün 8 | Gün 9 |
|---|---|---|
| B1 | AI Service: eğitim/test verisi üretimi (min. 100 Türkçe abone profili örneği) | AI Service: öneri skorlama modeli (ML/hibrit/LLM) + segment sınıflandırma |
| B2 | Campaign → AI Service entegrasyonu (kampanya oluşunca öneri isteği) | AI erişilemez fallback: segment `BELIRSIZ`, öncelik `ORTA`, manuel kuyruk |
| M | Uzman giriş ekranı gerçek Identity API'sine geçiş | Uzman vaka listesi (öncelik sıralı, SLA renk kodu iskeleti) |

### Gün 10 — Ara Demo (Tamamı)
| Kişi | Görev |
|---|---|
| Tamamı | Uçtan uca akışı prova et: kampanya oluştur → AI skorlama → segment atama. Bulunan entegrasyon hatalarını gün içinde kapat |

**Hafta sonu çıktısı:** Uçtan uca ilk akış ayakta, mobil gerçek API'ye bağlı.

---

## Hafta 3 — Event Mimarisi ve İkincil Akışlar

### Gün 11–12
| Kişi | Gün 11 | Gün 12 |
|---|---|---|
| B1 | AI Service: akıllı uzman ataması skorlama algoritması (uzmanlık eşleşme + boşluk oranı + performans) — erken başlangıç | AI ataması API entegrasyonu, güvenlik sertleştirmesine ön hazırlık (rate limit taslağı) |
| B2 | Message queue kurulumu (RabbitMQ/Redis pub-sub/Kafka), `campaign.optimized` event yayınlama | Gamification Service: event dinleyici + puan motoru (puan tablosu Bölüm 7.1'e göre) |
| M | Süpervizör dashboard iskeleti (grafik kütüphanesi seçimi) | Gamification ekranları için mock veri planı |

### Gün 13
| Kişi | Görev |
|---|---|
| B1 | AI Service: akıllı uzman ataması Campaign Service ile entegre |
| B2 | Gamification: rozet motoru, seviye sistemi (Bronz/Gümüş/Altın/Platin), liderlik tablosu endpoint'i |
| M | Uzman panelinde durum geçiş aksiyonları (state machine'e uygun, geçersiz buton pasif) |

### Gün 14
| Kişi | Görev |
|---|---|
| B1 | AI doğruluk takibi: segment override edildiğinde "yanlış sınıflandırma" kaydı, doğruluk oranı hesaplama |
| B2 | Campaign: SLA takibi (öncelik bazlı süre), `sla.breached` event'i, `SLA_TIME_UNIT` env değişkeni (demo kolaylığı) |
| M | Segment override ekranı, optimizasyon notu ile tamamlama akışı |

### Gün 15
| Kişi | Görev |
|---|---|
| B1 | AI Service uçtan uca entegrasyon testleri, `EVENTS.md` için AI event'lerinin dokümantasyonu |
| B2 | `EVENTS.md` için Campaign/Gamification event'lerinin dokümantasyonu, seed veri scripti başlangıcı |
| M | Süpervizör dashboard grafikleri (segment dağılımı, dönüşüm trendi, AI doğruluk metriği), gamification profil ekranı, rozet toast/modal bildirimi |

**Hafta sonu çıktısı:** Tüm zorunlu fonksiyonlar çalışıyor, `EVENTS.md` yazıldı.

---

## Hafta 4 — Sertleştirme, Test, Teslim

### Gün 16 — Güvenlik Sertleştirmesi (Herkes kendi servisinde)
| Kişi | Görev |
|---|---|
| B1 | Identity: brute-force/rate limit testi, JWT manipülasyon testi, refresh token replay testi. AI: input validation |
| B2 | Campaign/Gamification: SQL injection testi, IDOR testi (kayıt ID değiştirme), yetkisiz endpoint erişimi testi |
| M | XSS: form alanlarına script enjeksiyonu testi, input sanitization kontrolü |

### Gün 17
| Kişi | Görev |
|---|---|
| B1 | Identity + AI için unit/integration testler, Swagger/OpenAPI dokümantasyonu (AI zorunlu) |
| B2 | Campaign + Gamification için unit/integration testler, Swagger/OpenAPI (Campaign zorunlu), seed veri scriptini tamamlama |
| M | UI testleri, loading/error/empty state kontrolü tüm ekranlarda |

### Gün 18 — Dokümantasyon ve Dayanıklılık Testi
| Kişi | Görev |
|---|---|
| B1 | Identity + AI Service README'leri, AI yaklaşım dokümanı (yöntem, neden, eğitim süreci) |
| B2 | Campaign + Gamification README'leri, `EVENTS.md` finalize, ana README (mimari, kurulum) |
| M | Mobil README (kurulum, ekran listesi, state yönetimi) |
| **Ortak** | Servis kapatma (resilience) testi: bir servisi durdur, diğerlerinin çalıştığını doğrula |

### Gün 19 — Prova (Tamamı)
| Kişi | Görev |
|---|---|
| Tamamı | Demo senaryosunu (Bölüm 12.3) baştan sona en az iki kez çalıştır, bug fix, temiz kurulumda `docker compose up` testi |

### Gün 20 — Kapanış Sunumu
| Kişi | Görev |
|---|---|
| Tamamı | Canlı demo, mimari anlatımı, zorluklar/çözümler, soru-cevap ve güvenlik denemeleri |

---

## Kritik Bağımlılık Noktaları

- **Gün 2 sözleşme kilidi** olmadan Mobil gerçek API'ye geçemez → mock API şart (Bölüm 2'deki not).
- **Gün 8-9 AI Service** gecikirse Campaign entegrasyonu ve Gün 10 ara demo riske girer — B1 için en kritik pencere.
- **Gün 11-12 Message Queue** B2'nin tek başına kurduğu altyapı; gecikirse Gamification'ın tamamı (Gün 13) zincirleme kayar.
- **Gün 15 Mobil dashboard** üç backend servisinin de (Campaign/AI/Gamification metrikleri) o ana kadar ayakta olmasını gerektirir — en çok entegrasyon riski taşıyan gün.
