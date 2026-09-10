## Gamification Service Endpoint Listesi

| #   | Metot  | Yol                                    | Kim çağırır        | Durum     |
| --- | ------ | -------------------------------------- | ------------------ | --------- |
| 1   | `GET`  | `/api/v1/game/profile`                 | Uzman, Süpervizör  | Çalışıyor |
| 2   | `GET`  | `/api/v1/game/leaderboard`             | Uzman, Süpervizör  | Çalışıyor |
| 3   | `GET`  | `/api/v1/game/badges`                  | Uzman, Süpervizör  | Çalışıyor |
| 4   | `POST` | `/api/v1/game/leaderboard/rebuild`     | Süpervizör, Admin  | Çalışıyor |

Swagger arayüzü: `http://localhost:8084/swagger-ui.html`

---

## Önce bilinmesi gereken

**Bu servise puan yazan bir endpoint yok.** Puanlar Campaign Service'in RabbitMQ'ya
yayınladığı olaylardan geliyor. Yukarıdaki dört yol yalnızca sonucu okuyor. Mobil tarafta
"puan ekle" diye bir çağrı aramayın, öyle bir şey yok ve olmayacak.

Puanın oluşma anı ile ekranda görünmesi arasında kısa bir gecikme olabilir. Uzman vakayı
`TAMAMLANDI` yaptığında Campaign olayı yayınlıyor, Gamification tüketip puanı yazıyor.
Normalde bir saniyenin altında, ama tamamlama isteğinin yanıtı döner dönmez profili çekerseniz
puanı henüz görmeyebilirsiniz. Tamamlama ekranından profile geçerken profili yeniden çekin.

**Uzman kimliği hiçbir yolda geçmiyor.** Profil ve rozetler her zaman token'daki kişiye ait.
Başka bir uzmanın profilini sorgulayacak bir yol yok.

---

## 1. Profil

```
GET /api/v1/game/profile
```

**Ne için:** Uzman panelindeki profil ekranı. Toplam puan, seviye, rozetler, sıralama.
**Yetki:** Uzman, Süpervizör. Abone çağırırsa `403 FORBIDDEN`.

### Yanıt, 200

```json
{
	"success": true,
	"data": {
		"totalPoints": 505,
		"level": "GUMUS",
		"badges": ["ILK_KAMPANYA", "HIZ_USTASI"],
		"dailyRank": 1,
		"weeklyRank": 1,
		"casesResolved": 18,
		"avgPointsPerCase": 28.1
	},
	"error": null
}
```

| Alan               | Tip          | Açıklama                                                        |
| ------------------ | ------------ | --------------------------------------------------------------- |
| `totalPoints`      | Int          | Tüm zamanların toplamı. Negatif puanlar da dahil                |
| `level`            | enum         | `BRONZ`, `GUMUS`, `ALTIN`, `PLATIN`                              |
| `badges`           | String dizi  | Yalnızca kazanılmış rozetler. Hepsi için endpoint 3'e bakın     |
| `dailyRank`        | Int, nullable| Günlük sıralamadaki yer. Bugün hiç puan yoksa `null`            |
| `weeklyRank`       | Int, nullable| Haftalık sıralamadaki yer. Bu hafta hiç puan yoksa `null`       |
| `casesResolved`    | Int          | Tamamlanmış vaka sayısı                                          |
| `avgPointsPerCase` | Double       | Vaka başına ortalama puan                                        |

### Seviye eşikleri

| Seviye   | Puan aralığı  |
| -------- | ------------- |
| `BRONZ`  | 0 ile 499     |
| `GUMUS`  | 500 ile 1499  |
| `ALTIN`  | 1500 ile 2999 |
| `PLATIN` | 3000 ve üstü  |

### Hatalar

| Kod           | HTTP | Ne zaman                              |
| ------------- | ---- | ------------------------------------- |
| `FORBIDDEN`   | 403  | Abone rolüyle çağrıldı                |
| `TOKEN_INVALID` | 401 | Token yok, bozuk veya süresi dolmuş   |

### Mobil notu

- `dailyRank` ve `weeklyRank` **null olabilir**. Uzman o gün hiç puan almadıysa sıralamada
  yer almıyor demektir. Ekranda "henüz sıralamada değilsiniz" göstermek, `0` göstermekten
  daha doğru olur.
- `level` doğrudan gösterilecek bir etiket, istemcide puandan yeniden hesaplamayın. Eşik
  değişirse iki yerde birden değiştirmek zorunda kalırsınız.
- `avgPointsPerCase` ondalıklı geliyor, biçimlendirmeyi istemci yapsın.

---

## 2. Liderlik Tablosu

```
GET /api/v1/game/leaderboard?period=daily
GET /api/v1/game/leaderboard?period=weekly
```

**Ne için:** Uzman panelindeki günlük ve haftalık sekmeli liderlik tablosu.
**Yetki:** Uzman, Süpervizör.

| Parametre | Zorunlu | Varsayılan | Değerler            |
| --------- | ------- | ---------- | ------------------- |
| `period`  | Hayır   | `daily`    | `daily` \| `weekly` |

### Yanıt, 200

```json
{
	"success": true,
	"data": {
		"period": "weekly",
		"items": [
			{
				"rank": 1,
				"expertId": "aaaaaaaa-1111-1111-1111-111111111111",
				"name": null,
				"points": 174
			}
		]
	},
	"error": null
}
```

| Alan       | Tip             | Açıklama                                               |
| ---------- | --------------- | ------------------------------------------------------ |
| `period`   | String          | İsteğin hangi dönem için cevaplandığı                  |
| `rank`     | Int             | 1'den başlar                                           |
| `expertId` | UUID            | Uzmanın kimliği                                        |
| `name`     | String, nullable| **Şu an her zaman `null`**, aşağıdaki nota bakın       |
| `points`   | Int             | O dönemde kazanılan puan, tüm zamanların toplamı değil |

En fazla 10 kayıt döner.

### Hatalar

| Kod                | HTTP | Ne zaman                                     |
| ------------------ | ---- | -------------------------------------------- |
| `VALIDATION_ERROR` | 400  | `period` değeri `daily` veya `weekly` değil  |
| `FORBIDDEN`        | 403  | Abone rolüyle çağrıldı                       |

### Mobil notu

- **`points` ile profildeki `totalPoints` farklı sayılardır ve bu bir hata değil.**
  Liderlik tablosu bir dönemin puanını gösteriyor, profil ise tüm zamanların toplamını.
  Yukarıdaki örnekte aynı uzman profilde 505, haftalık tabloda 174 puanla görünüyor.
  Ekranda ikisini yan yana koyacaksanız etiketleri buna göre yazın.
- **`name` alanı `null` geliyor.** Bu servis yalnızca uzman kimliklerini tutuyor, isimler
  Identity Service'in verisi. İsim göstermek istiyorsanız
  `GET /api/v1/users/staff?query=&role=` üzerinden çözün. Kararlaştırılmadığı için şu an
  boş bırakılıyor, alanı **null-safe okuyun**.
- Geçersiz `period` sessizce `daily` olarak kabul edilmiyor, 400 dönüyor. Sekme
  değiştirirken gönderdiğiniz değerin bu ikisinden biri olduğundan emin olun.

---

## 3. Rozetler

```
GET /api/v1/game/badges
```

**Ne için:** Rozet ekranı. Kazanılmamış rozetlerin de kilitli olarak gösterilebilmesi için
altı rozetin **hepsi** dönüyor.
**Yetki:** Uzman, Süpervizör.

### Yanıt, 200

```json
{
	"success": true,
	"data": [
		{
			"badge": "ILK_KAMPANYA",
			"earned": true,
			"earnedAt": "2026-08-31T08:27:18.718574Z"
		},
		{
			"badge": "DONUSUM_KRALI",
			"earned": false,
			"earnedAt": null
		}
	],
	"error": null
}
```

| Alan       | Tip              | Açıklama                                   |
| ---------- | ---------------- | ------------------------------------------ |
| `badge`    | enum             | Rozet kodu                                 |
| `earned`   | Boolean          | Kazanılmış mı                              |
| `earnedAt` | Instant, nullable| Kazanılmadıysa `null`                      |

### Rozet koşulları

| Rozet            | Koşul                                       |
| ---------------- | ------------------------------------------- |
| `ILK_KAMPANYA`   | İlk tamamlanan optimizasyon                 |
| `HIZ_USTASI`     | 2 saatin altında 10 optimizasyon            |
| `DONUSUM_KRALI`  | 10 kez dönüşüm hedefinin aşılması           |
| `MARATONCU`      | 24 saatlik pencerede 20 optimizasyon        |
| `CHURN_AVCISI`   | 10 tamamlanmış `RISKLI_KAYIP` vakası        |
| `UZMAN`          | Tek bir segmentte 50 tamamlanmış vaka       |

### Mobil notu

- Liste her zaman altı elemanlı ve sırası sabit. `earned` alanına göre kilitli ve açık
  gösterebilirsiniz.
- Rozet kazanıldığı anda `badge.earned` olayı yayınlanıyor. Toast veya modal bildirimi bu
  olaya bağlamak istiyorsanız akış `EVENTS.md` dosyasında. Şu anki uygulamada bildirim,
  vaka tamamlandıktan sonra profil yeniden çekildiğinde yeni bir rozet görülerek de
  tetiklenebilir.

---

## 4. Liderlik Tablosunu Yeniden Kur

```
POST /api/v1/game/leaderboard/rebuild
```

**Ne için:** Onarım işlemi. Normal akışın parçası değil.
**Yetki:** Süpervizör, Admin. Uzman çağırırsa `403 FORBIDDEN`.

Sıralama Redis'te tutuluyor ama bu türetilmiş bir veri. Her puanın sebebi `point_entries`
tablosunda duruyor ve doğruluk kaynağı orası. Redis silinir veya sıfırlanırsa bu endpoint
sıralamayı defterden yeniden kuruyor.

### Yanıt, 200

```json
{
	"success": true,
	"data": 2,
	"error": null
}
```

`data` alanı yeniden işlenen kayıt sayısı.

### Mobil notu

Bu endpoint'i normal akışta çağırmayın. Süpervizör ekranına bir düğme olarak koyacaksanız
bile "yenile" değil "sıralamayı onar" gibi bir şey yazın, çünkü yaptığı şey veri okumak
değil yeniden kurmak.

---

## Puan tablosu

Aşağıdaki puanlar olay geldiğinde otomatik işleniyor. Hiçbiri bir API çağrısıyla
tetiklenmiyor.

| Sebep                        | Puan | Koşul                                                  |
| ---------------------------- | ---- | ------------------------------------------------------ |
| `OPTIMIZATION_COMPLETED`     | +10  | Her tamamlanan optimizasyon                            |
| `FAST_OPTIMIZATION`          | +5   | Vaka açılışından tamamlanmasına kadar 2 saatten kısa   |
| `CONVERSION_TARGET_EXCEEDED` | +15  | Ölçülen dönüşüm artışı 0.10'un üstünde                 |
| `CRITICAL_WITHIN_SLA`        | +15  | `KRITIK` öncelikli vaka SLA içinde tamamlanmış         |
| `SLA_BREACH`                 | -5   | Her SLA aşımı                                          |
| `LOW_RATING`                 | -3   | Abone teklife 1 veya 2 yıldız vermiş                   |

Aynı olay iki kez gelirse ikinci kez puan yazılmıyor. Tekillik `(sourceId, reason)`
çiftinde, yani kuyruğun aynı mesajı yeniden teslim etmesi puanı şişirmiyor.

### Demoda bilinmesi gereken

`CONVERSION_TARGET_EXCEEDED` pratikte yalnızca segment veya tür değiştiğinde oluşuyor.
Dönüşüm artışı, kampanya oluşturulurken alınan tahmin ile optimizasyondan sonra AI'ın
verdiği yeni tahminin farkı olarak ölçülüyor. AI'ın `recommend` çağrısı yalnızca
`subscriberId` ve `campaignType` alıyor, indirim oranına bakmıyor. Dolayısıyla başlığı veya
indirimi değiştiren bir optimizasyon tam olarak 0.000 artış üretiyor ve bonus çıkmıyor.

Bu bir hata değil, AI'ın imzasının sonucu. Demoda +15 bonusunu göstermek istiyorsanız
segmenti değiştiren bir optimizasyon yapın.

### SLA ihlali geri alınmıyor

Öncelik düşürülünce SLA son tarihi ileri gidiyor, ama ihlal damgası kalıyor ve kesilen 5
puan iade edilmiyor. Bu bilinçli: ihlal olmuş bir şeydir, sonradan önceliği düşürmek
geçmişi değiştirmez.
