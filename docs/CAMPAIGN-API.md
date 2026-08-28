## Campaign Service Endpoint Listesi

| #   | Metot   | Yol                                      | Kim çağırır              | Durum      |
| --- | ------- | ---------------------------------------- | ------------------------ | ---------- |
| 1   | `POST`  | `/api/v1/campaigns`                      | Uzman, Süpervizör        | Çalışıyor  |
| 2   | `GET`   | `/api/v1/campaigns`                      | Uzman, Süpervizör, Admin | Çalışıyor  |
| 3   | `GET`   | `/api/v1/campaigns/{campaignNo}`         | Uzman, Süpervizör, Admin | Çalışıyor  |
| 4   | `PATCH` | `/api/v1/campaigns/{campaignNo}/segment` | Uzman, Süpervizör        | Tasarlandı |
| 5   | `GET`   | `/api/v1/campaigns/dashboard`            | Süpervizör, Admin        | Tasarlandı |
| 6   | `GET`   | `/api/v1/cases`                          | Uzman, Süpervizör        | Çalışıyor  |
| 7   | `GET`   | `/api/v1/cases/{caseId}`                 | Uzman, Süpervizör, Admin | Çalışıyor  |
| 8   | `PATCH` | `/api/v1/cases/{caseId}/status`          | Uzman, Süpervizör        | Çalışıyor  |
| 9   | `POST`  | `/api/v1/cases/{caseId}/assign`          | Süpervizör               | Çalışıyor  |
| 10  | `GET`   | `/api/v1/offers`                         | Abone                    | Tasarlandı |
| 11  | `POST`  | `/api/v1/offers/{offerId}/respond`       | Abone                    | Tasarlandı |
| 12  | `POST`  | `/api/v1/offers/{offerId}/rate`          | Abone                    | Tasarlandı |

---

## 1. Kampanya Oluştur

```
POST /api/v1/campaigns
```

**Ne için:** Uzmanın kampanya oluşturma formunu kaydeder.
**Yetki:** Uzman, Süpervizör. Abone çağırırsa `403 FORBIDDEN`.

### İstek

```json
{
	"title": "Yaz Ek Paket Kampanyası",
	"type": "EK_PAKET",
	"targetSegment": "YUKSEK_DEGER",
	"discountRate": 20,
	"validUntil": "2026-12-31T23:59:59Z"
}
```

| Alan            | Tip     | Kural                                               |
| --------------- | ------- | --------------------------------------------------- |
| `title`         | String  | Zorunlu, en fazla 200 karakter, `<` ve `>` içeremez |
| `type`          | enum    | Zorunlu — `CampaignType`                            |
| `targetSegment` | enum    | Zorunlu — `Segment`                                 |
| `discountRate`  | Int     | Zorunlu, 0–100                                      |
| `validUntil`    | Instant | Zorunlu, gelecekte bir tarih                        |

### Yanıt — 201

```json
{
	"success": true,
	"data": {
		"campaignNo": "CMP-2026-000001",
		"title": "Yaz Ek Paket Kampanyası",
		"type": "EK_PAKET",
		"targetSegment": "YUKSEK_DEGER",
		"aiSegment": "BELIRSIZ",
		"segment": "BELIRSIZ",
		"discountRate": 20,
		"validUntil": "2026-12-31T23:59:59Z",
		"status": "YENI",
		"priority": "ORTA",
		"conversionProbability": null,
		"createdAt": "2026-08-25T11:49:24.021674Z"
	},
	"error": null
}
```

### Hatalar

| Kod                | HTTP | Ne zaman                                                         |
| ------------------ | ---- | ---------------------------------------------------------------- |
| `VALIDATION_ERROR` | 400  | Eksik/hatalı alan. `message` hangi alanın hatalı olduğunu söyler |
| `FORBIDDEN`        | 403  | Rol yetersiz                                                     |

### Mobil notu

- Yanıttaki `campaignNo` kullanıcıya gösterilir ("CMP-2026-000001 numarasıyla oluşturuldu").
- `aiSegment` ve `conversionProbability` şu an sırasıyla `BELIRSIZ` ve `null` geliyor çünkü
  AI Service henüz bağlı değil. **Bu alanları null-safe okuyun**, ileride dolacaklar.
- `title` alanına `<` veya `>` yazılırsa 400 döner. Form validasyonunu istemcide de yapın ki
  kullanıcı sunucuya gitmeden uyarılsın.

---

## 2. Kampanya Listesi

```
GET /api/v1/campaigns?status=&segment=&page=0&size=20
```

**Ne için:** Uzman ve süpervizörün kampanya listesi ekranı.
**Yetki:** Uzman (ileride sadece kendine atananlar), Süpervizör, Admin (tümü).

### Query parametreleri

| Parametre | Tip  | Varsayılan | Not                                                      |
| --------- | ---- | ---------- | -------------------------------------------------------- |
| `status`  | enum | yok        | Verilmezse tüm durumlar                                  |
| `segment` | enum | yok        | Verilmezse tüm segmentler                                |
| `page`    | Int  | `0`        | Sıfırdan başlar                                          |
| `size`    | Int  | `20`       | En fazla 100; üzerini isterseniz sessizce 100'e kırpılır |

### Yanıt — 200

```json
{
	"success": true,
	"data": {
		"items": [
			{
				"campaignNo": "CMP-2026-000001",
				"title": "Yaz Ek Paket",
				"...": "..."
			},
			{
				"campaignNo": "CMP-2026-000002",
				"title": "Churn Önleme",
				"...": "..."
			}
		],
		"total": 3,
		"page": 0,
		"size": 20
	},
	"error": null
}
```

`items` içindeki her nesne, endpoint 1'in döndürdüğü kampanya nesnesinin aynısıdır.

### Hatalar

| Kod                | HTTP | Ne zaman                                                |
| ------------------ | ---- | ------------------------------------------------------- |
| `VALIDATION_ERROR` | 400  | `page=abc` gibi tip uyuşmazlığı, tanınmayan enum değeri |

### Mobil notu

- Filtre seçilmediğinde parametreyi **hiç göndermeyin**, boş string göndermeyin.

---

## 3. Kampanya Detayı

```
GET /api/v1/campaigns/{campaignNo}
```

**Ne için:** Listeden bir kampanyaya tıklanınca açılan detay ekranı.
**Yetki:** Uzman, Süpervizör, Admin.

Yol parametresi UUID değil, okunabilir numara: `CMP-2026-000001`.

### Yanıt — 200

Endpoint 1'in döndürdüğü kampanya nesnesinin aynısı.

### Hatalar

| Kod         | HTTP | Ne zaman                     |
| ----------- | ---- | ---------------------------- |
| `NOT_FOUND` | 404  | Bu numaraya ait kampanya yok |

---

## 4. Segment Override

```
PATCH /api/v1/campaigns/{campaignNo}/segment
```

**Ne için:** AI'ın atadığı segmenti uzmanın düzeltmesi.
**Yetki:** Uzman, Süpervizör.

### İstek

```json
{
	"segment": "PASIF",
	"reason": "Kullanım verisi güncel değildi"
}
```

| Alan      | Tip    | Kural                          |
| --------- | ------ | ------------------------------ |
| `segment` | enum   | Zorunlu — yeni segment         |
| `reason`  | String | Zorunlu — neden değiştirildiği |

### Yanıt — 200

Güncellenmiş kampanya nesnesi. **`aiSegment` değişmez**, sadece `segment` değişir.

### Mobil notu

- Bu işlem AI'ın doğruluk metriğini etkiler. Kullanıcıya "bu değişiklik AI modelinin
  değerlendirilmesinde kullanılacak" gibi bir bilgi göstermek isteyebilirsiniz.
- Ekranda `aiSegment` ile `segment` farklıysa "AI önerisi düzeltildi" rozeti gösterilebilir.

---

## 5. Süpervizör Dashboard

```
GET /api/v1/campaigns/dashboard
```

**Ne için:** Süpervizör dashboard'unun tüm kartlarını tek istekte doldurur.
**Yetki:** Süpervizör, Admin.

### Yanıt — 200

```json
{
	"success": true,
	"data": {
		"segmentDistribution": {
			"YUKSEK_DEGER": 42,
			"RISKLI_KAYIP": 18,
			"YENI_ABONE": 30,
			"PASIF": 10
		},
		"conversionRate": 0.34,
		"slaComplianceRate": 0.91,
		"slaBreachedActiveCases": 3,
		"pendingQueueCount": 5
	},
	"error": null
}
```

| Alan                     | Nerede kullanılır                   |
| ------------------------ | ----------------------------------- |
| `segmentDistribution`    | Pasta veya bar grafik               |
| `conversionRate`         | Yüzdeye çevirin: `0.34` → `%34`     |
| `slaComplianceRate`      | Yüzdeye çevirin                     |
| `slaBreachedActiveCases` | Kırmızı uyarı kartı                 |
| `pendingQueueCount`      | Bekleyen optimizasyon kuyruğu kartı |

### Mobil notu

Dashboard tek çağrıyla dolar; altı ayrı loading state yönetmeniz gerekmez.

---

## 6. Vaka Listesi

```
GET /api/v1/cases?assignedTo=me&sort=priority&page=0&size=20
```

**Ne için:** Uzmanın "bana atanan vakalar" ekranı; süpervizörün tüm vakalar ekranı.
**Yetki:** Uzman (yalnızca kendine atananlar), Süpervizör (tümü).

### Query parametreleri

| Parametre      | Değer               | Not                                                |
| -------------- | ------------------- | -------------------------------------------------- |
| `assignedTo`   | `me`                | Kullanıcı id'si gönderilmez, sunucu token'dan okur |
| `sort`         | `priority` \| `sla` | Öncelik veya kalan süreye göre                     |
| `status`       | enum                | Opsiyonel filtre                                   |
| `page`, `size` | Int                 | Standart sayfalama                                 |

**Bugünkü davranış (JWT ve SLA turları öncesi):**

- `assignedTo` şu an yalnızca uzman UUID'si kabul ediyor, `me` değil — token olmadığı için
  sunucu "ben"in kim olduğunu bilmiyor. `me` gönderilirse `VALIDATION_ERROR` döner.
- `sort` henüz okunmuyor; liste **her zaman** öncelik sıralı geliyor (`KRITIK` en üstte,
  eşitlikte en eski vaka önce). Parametreyi göndermek hata vermez, yok sayılır.
  `sort=sla` SLA turunda gelecek.

### Yanıt — 200

```json
{
	"success": true,
	"data": {
		"items": [
			{
				"caseId": "d4e5a1b2-...",
				"campaignNo": "CMP-2026-000123",
				"title": "Churn Önleme Kampanyası",
				"segment": "RISKLI_KAYIP",
				"priority": "YUKSEK",
				"status": "ATANDI",
				"slaDeadline": "2026-08-17T22:00:00Z",
				"slaRemainingSeconds": 27600,
				"assignedExpertId": "a7f3c2d1-..."
			}
		],
		"total": 8,
		"page": 0,
		"size": 20
	},
	"error": null
}
```

`items` içindeki her nesne, endpoint 7'nin döndürdüğü vaka nesnesinin aynısıdır — liste
ve detay aynı şekli döner, listede eksik alan yoktur.

### Mobil notu

- **`slaRemainingSeconds` renk kodunun kaynağıdır.** Negatifse SLA aşılmış demektir.
  Önerilen eşikler: `< 0` kırmızı, toplam sürenin `%25`'inden azsa turuncu, aksi halde normal.
- Bu alan sunucuda her okumada hesaplanır, veritabanında tutulmaz. Ekranda geri sayım
  göstermek isterseniz istemci tarafında saniyede bir azaltın, sunucuyu tekrar çağırmayın.
- `caseId` bir UUID, kampanya numarası gibi okunabilir değil — kullanıcıya göstermeyin,
  `campaignNo` gösterin.
- **Şu an `slaDeadline` ve `slaRemainingSeconds` `null` geliyor** (SLA turu henüz yapılmadı).
  Renk kodunu null-safe yazın: değer yoksa normal renk gösterin, ekran çökmesin.

---

## 7. Vaka Detayı

```
GET /api/v1/cases/{caseId}
```

**Ne için:** Uzman vakayı açtığında gördüğü ekran.
**Yetki:** Uzman (yalnızca kendine atanan), Süpervizör, Admin.

### Yanıt — 200

```json
{
	"success": true,
	"data": {
		"caseId": "d4e5a1b2-...",
		"campaignNo": "CMP-2026-000123",
		"title": "Churn Önleme Kampanyası",
		"segment": "RISKLI_KAYIP",
		"aiSegment": "RISKLI_KAYIP",
		"priority": "YUKSEK",
		"status": "ATANDI",
		"conversionProbability": 0.31,
		"recommendationScore": 0.62,
		"slaDeadline": "2026-08-17T22:00:00Z",
		"slaRemainingSeconds": 27600,
		"optimizationNote": null,
		"assignedExpertId": "a7f3c2d1-...",
		"createdAt": "2026-08-17T13:40:02Z",
		"completedAt": null
	},
	"error": null
}
```

**Şu an `null` gelen alanlar:** `recommendationScore` (AI Service bağlanınca dolacak),
`slaDeadline` ve `slaRemainingSeconds` (SLA turunda dolacak). Alanlar yanıtta **var**,
sadece değerleri yok — data class'ınızı şimdiden bu şekilde yazabilirsiniz, sonra değişmeyecek.

### Hatalar

| Kod         | HTTP | Ne zaman                                       |
| ----------- | ---- | ---------------------------------------------- |
| `NOT_FOUND` | 404  | Vaka yok                                       |
| `FORBIDDEN` | 403  | Başka bir uzmana atanmış vakayı açmaya çalışma |

`FORBIDDEN` **henüz uygulanmıyor** — JWT gelmeden sunucu çağıranın kim olduğunu bilmiyor,
şu an her vaka herkese açık. Mobil tarafta yine de ele alın, JWT turunda devreye girecek.

### Mobil notu

`FORBIDDEN` senaryosunu mutlaka ele alın — bu bir hata değil, güvenlik davranışı.
Kullanıcıya "bu vaka size atanmamış" mesajı gösterin, ham hata kodunu değil.

---

## 8. Vaka Durum Geçişi

```
PATCH /api/v1/cases/{caseId}/status
```

**Ne için:** Uzmanın vakayı ilerletmesi. Uygulamanın en kritik endpoint'i.
**Yetki:** Uzman ve Süpervizör — hangi geçişi kimin yapabildiği aşağıdaki tabloda.

### İstek

```json
{
	"targetStatus": "TAMAMLANDI",
	"optimizationNote": "A/B testi ile indirim oranı %15'e çekildi"
}
```

| Alan               | Tip    | Kural                                                                                                             |
| ------------------ | ------ | ----------------------------------------------------------------------------------------------------------------- |
| `targetStatus`     | enum   | Zorunlu — hedef durum                                                                                             |
| `optimizationNote` | String | Yalnızca `TAMAMLANDI`'ya geçişte **zorunlu**, diğerlerinde opsiyonel. En fazla 1000 karakter, `<` ve `>` içeremez |

Yalnızca boşluktan oluşan bir not (`"   "`) boş sayılır ve `OPTIMIZATION_NOTE_REQUIRED` döner.

### İzinli geçişler

| Mevcut durum        | Hedef durum         | Kim yapabilir               |
| ------------------- | ------------------- | --------------------------- |
| `YENI`              | `ATANDI`            | Sistem (AI) veya Süpervizör |
| `ATANDI`            | `OPTIMIZE_EDILIYOR` | Uzman                       |
| `OPTIMIZE_EDILIYOR` | `TEST_EDILIYOR`     | Uzman                       |
| `TEST_EDILIYOR`     | `OPTIMIZE_EDILIYOR` | Sistem                      |
| `OPTIMIZE_EDILIYOR` | `TAMAMLANDI`        | Uzman (not zorunlu)         |
| `TAMAMLANDI`        | `YAYINDA`           | Süpervizör                  |
| `YAYINDA`           | `ARSIVLENDI`        | Sistem                      |

Tabloda olmayan her geçiş reddedilir — kendi durumuna geçiş (`ATANDI` → `ATANDI`) dahil.
`ARSIVLENDI` son duraktır, çıkışı yoktur.

**Kim yapabilir sütunu henüz uygulanmıyor:** JWT gelmeden rol okunamadığı için sunucu şu an
geçişin _kuralına_ bakıyor ama _kimin_ yaptığına bakmıyor. Geçiş tablosunun kendisi
sunucuda zorunlu tutuluyor, o kısım çalışıyor.

### Yanıt — 200

Güncellenmiş vaka nesnesi (endpoint 7 ile aynı şekil).

### Hatalar

| Kod                          | HTTP | Ne zaman                              |
| ---------------------------- | ---- | ------------------------------------- |
| `INVALID_STATE_TRANSITION`   | 422  | Geçiş tabloda yok                     |
| `OPTIMIZATION_NOTE_REQUIRED` | 400  | `TAMAMLANDI`'ya notsuz geçiş denemesi |
| `FORBIDDEN`                  | 403  | Bu rol bu geçişi yapamaz              |

```json
{
	"success": false,
	"data": null,
	"error": {
		"code": "INVALID_STATE_TRANSITION",
		"message": "YENI durumundan doğrudan TAMAMLANDI'ya geçilemez"
	}
}
```

### Mobil notu

- **Geçersiz geçişin butonu pasif gösterilmeli.** Mevcut `status` değerine bakarak yukarıdaki
  tablodan izinli hedefleri hesaplayın; diğer butonları disabled yapın.
- Sunucu yine de kontrol eder — istemci tarafı doğrulama kullanıcı deneyimi içindir,
  güvenlik için değil.
- `TAMAMLANDI` butonuna basıldığında not alanı boşsa **isteği hiç göndermeyin**, formu uyarın.
- Bu geçiş başarılı olduğunda arka planda puan ve rozet hesaplanır. Gamification profilini
  hemen çağırmak yerine kullanıcı o ekrana geldiğinde yenileyin — işlem asenkron.
  (`campaign.optimized` event'i henüz yayınlanmıyor, RabbitMQ turunda gelecek — o ana kadar
  puan tarafında bir şey değişmez.)

---

## 9. Manuel Atama

```
POST /api/v1/cases/{caseId}/assign
```

**Ne için:** Süpervizörün AI'ın atamasını geçersiz kılması.
**Yetki:** Yalnızca Süpervizör.

### İstek

```json
{ "expertId": "a7f3c2d1-..." }
```

### Yanıt — 200

Güncellenmiş vaka nesnesi. `status` `YENI` ise `ATANDI`'ya geçer. Vaka zaten başlamışsa
(`OPTIMIZE_EDILIYOR` gibi) durum değişmez, yalnızca `assignedExpertId` değişir — devretmek
işi baştan başlatmaz.

### Hatalar

| Kod                        | HTTP | Ne zaman                                                      |
| -------------------------- | ---- | ------------------------------------------------------------- |
| `VALIDATION_ERROR`         | 400  | `expertId` eksik veya UUID değil                              |
| `FORBIDDEN`                | 403  | Uzman rolüyle çağrılırsa (henüz uygulanmıyor, JWT bekliyor)   |
| `NOT_FOUND`                | 404  | Vaka yok                                                      |
| `INVALID_STATE_TRANSITION` | 422  | Kapanmış vakaya atama (`TAMAMLANDI`, `YAYINDA`, `ARSIVLENDI`) |

### Mobil notu

Uzman listesi Campaign Service'ten gelmez — o Identity Service'in verisi.
Atama ekranındaki uzman listesini `/api/v1/admin/staff` benzeri bir Identity endpoint'inden alın.

---

## 10. Aboneye Özel Teklifler

```
GET /api/v1/offers?page=0&size=20
```

**Ne için:** Abonenin uygulamayı açtığında gördüğü teklif listesi.
**Yetki:** Abone — yalnızca kendi teklifleri.

Abone id'si istekte gönderilmez, sunucu token'dan okur.

### Yanıt — 200

```json
{
	"success": true,
	"data": {
		"items": [
			{
				"offerId": "f1a2b3c4-...",
				"campaignNo": "CMP-2026-000123",
				"title": "Yaz Ek Paket Kampanyası",
				"type": "EK_PAKET",
				"discountRate": 20,
				"validUntil": "2026-09-30T23:59:59Z",
				"score": 0.83,
				"highlighted": true,
				"status": "PENDING",
				"stars": null
			}
		],
		"total": 3,
		"page": 0,
		"size": 20
	},
	"error": null
}
```

### Mobil notu

- **Liste skora göre azalan sırada gelir**, tekrar sıralamanıza gerek yok.
- `highlighted: true` olanlar skoru 0.80 üzerindeki tekliflerdir — rozet veya vurgulu kart ile gösterin.
- Skoru 0.60 altındaki teklifler bu listeye **hiç girmez**, filtrelemenize gerek yok.
- `status` `PENDING` değilse kabul/ret butonlarını gizleyin.
- `stars` doluysa puanlama yapılmış demektir, yıldız bileşenini salt okunur gösterin.

---

## 11. Teklife Yanıt

```
POST /api/v1/offers/{offerId}/respond
```

**Ne için:** Abonenin "Kabul" veya "İlgilenmiyorum" demesi.
**Yetki:** Abone — yalnızca kendi teklifi.

### İstek

```json
{ "response": "ACCEPTED" }
```

`response`: `ACCEPTED` veya `DECLINED`.

### Yanıt — 200

Güncellenmiş teklif nesnesi (`status` değişmiş halde).

### Hatalar

| Kod                       | HTTP | Ne zaman                          |
| ------------------------- | ---- | --------------------------------- |
| `FORBIDDEN`               | 403  | Başkasının `offerId`'si denenirse |
| `OFFER_ALREADY_RESPONDED` | 409  | Bu teklife zaten yanıt verilmiş   |
| `NOT_FOUND`               | 404  | Teklif yok                        |

### Mobil notu

`409` bir hata ekranı değil — kullanıcı muhtemelen butona iki kez bastı ya da başka bir
cihazdan yanıtladı. Listeyi yenileyip güncel durumu gösterin.

---

## 12. Memnuniyet Puanlama

```
POST /api/v1/offers/{offerId}/rate
```

**Ne için:** Teklif etkileşimi sonrası 1–5 yıldız puanlama.
**Yetki:** Abone — yalnızca kendi teklifi, **tek seferlik**.

### İstek

```json
{ "stars": 4 }
```

`stars`: 1–5 arası tam sayı.

### Yanıt — 200

Güncellenmiş teklif nesnesi (`stars` ve `ratedAt` dolu).

### Hatalar

| Kod                   | HTTP | Ne zaman                     |
| --------------------- | ---- | ---------------------------- |
| `VALIDATION_ERROR`    | 400  | `stars` 1–5 aralığında değil |
| `FORBIDDEN`           | 403  | Başkasının teklifi           |
| `OFFER_ALREADY_RATED` | 409  | Zaten puanlanmış             |

### Mobil notu

Puanlama geri alınamaz. Kullanıcıya göndermeden önce onay isteyin veya yıldıza bastıktan
sonra kısa bir "geri al" penceresi tanıyın.

---

## Enum Değerleri

Kotlin tarafında `enum class` olarak tanımlayın. Sunucudan tanımadığınız bir değer gelirse
uygulamanın çökmemesi için bir `UNKNOWN` dalı bulundurun.

**`CampaignType`**
`EK_PAKET` · `TARIFE_YUKSELTME` · `CIHAZ_FIRSATI` · `SADAKAT`

**`Segment`**
`YUKSEK_DEGER` · `RISKLI_KAYIP` · `YENI_ABONE` · `PASIF` · `BELIRSIZ`

`BELIRSIZ`, AI'ın sınıflandırma yapamadığı durumu gösterir. Ekranda "belirlenmedi" olarak
gösterin, boş bırakmayın.

**`Priority`**
`DUSUK` · `ORTA` · `YUKSEK` · `KRITIK`

**`CampaignStatus`** (kampanyanın kendi durumu)
`YENI` · `YAYINDA` · `ARSIVLENDI`

**`CaseStatus`** (optimizasyon vakasının durumu — kampanya durumundan farklıdır)
`YENI` · `ATANDI` · `OPTIMIZE_EDILIYOR` · `TEST_EDILIYOR` · `TAMAMLANDI` · `YAYINDA` · `ARSIVLENDI`

**`OfferStatus`**
`PENDING` · `ACCEPTED` · `DECLINED`

---

## SLA Süreleri

Vaka oluşturulduğunda başlar, `TAMAMLANDI`'ya geçince durur.

| Öncelik  | Süre    | Ekranda                   |
| -------- | ------- | ------------------------- |
| `KRITIK` | 2 saat  | Kırmızı, listede en üstte |
| `YUKSEK` | 8 saat  | Turuncu                   |
| `ORTA`   | 24 saat | Görsel uyarı              |
| `DUSUK`  | 72 saat | Görsel uyarı              |

Demo sırasında bu süreler `SLA_TIME_UNIT` environment değişkeniyle dakikaya indirilir —
test ederken 2 saat beklemeniz gerekmez.

---

## Hata Kodları Özeti

| Kod                          | HTTP | Kullanıcıya ne gösterilmeli                       |
| ---------------------------- | ---- | ------------------------------------------------- |
| `VALIDATION_ERROR`           | 400  | Formdaki hatalı alanı işaretleyin                 |
| `OPTIMIZATION_NOTE_REQUIRED` | 400  | "Tamamlamak için optimizasyon notu girin"         |
| `FORBIDDEN`                  | 403  | "Bu işlem için yetkiniz yok"                      |
| `NOT_FOUND`                  | 404  | "Kayıt bulunamadı", listeye geri dön              |
| `OFFER_ALREADY_RESPONDED`    | 409  | "Bu teklife zaten yanıt verdiniz", listeyi yenile |
| `OFFER_ALREADY_RATED`        | 409  | "Bu teklife zaten puan verdiniz"                  |
| `INVALID_STATE_TRANSITION`   | 422  | "Bu işlem şu anki durumda yapılamaz"              |
| `INTERNAL_ERROR`             | 500  | "Bir şeyler ters gitti, tekrar deneyin"           |

Tam katalog: [`docs/ERROR-CODES.md`](ERROR-CODES.md)
