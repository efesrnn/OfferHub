# Events

Servisler arası tüm asenkron iletişim RabbitMQ üzerinden, tek bir **topic exchange** ile yürür: `offerhub.events`. Her event kendi `eventType` değerini **routing key** olarak kullanır. Dinleyen servis, ilgilendiği routing key'e kendi kuyruğunu bind eder.

Zarf formatı sabit (bkz. `docs/API-CONTRACT.md` ve `docs/ORTAK-KARARLAR.md` — camelCase kararı):

```json
{ "eventType": "...", "timestamp": "ISO-8601 UTC", "payload": { } }
```

`eventType` değerinin kendisi (`campaign.optimized` gibi) nokta ayraçlı, sabit bir tanımlayıcıdır — camelCase kuralına tabi değildir, routing key olarak da aynen kullanılır. `payload` içindeki alan adları camelCase'dir.

---

## campaign.created

**Yayınlayan:** Campaign Service
**Dinleyen:** Şimdilik yok (loglama/analiz için ayrılmış, ileride bir raporlama servisi eklenirse buraya bind edilir)
**Ne zaman tetiklenir:** Yeni bir kampanya oluşturulduğunda, AI Service'in senkron `recommend` cevabından sonra

```json
{
  "eventType": "campaign.created",
  "timestamp": "2026-08-17T14:00:00Z",
  "payload": {
    "campaignNo": "CMP-2026-000123",
    "type": "EK_PAKET",
    "targetSegment": "YUKSEK_DEGER",
    "priority": "ORTA",
    "createdBy": "a7f3..."
  }
}
```

---

## campaign.optimized

**Yayınlayan:** Campaign Service
**Dinleyen:** Gamification Service (puan/rozet hesaplama tetikleyicisi)
**Ne zaman tetiklenir:** Bir optimizasyon vakası `OPTIMIZE_EDILIYOR` → `TAMAMLANDI` durumuna geçtiğinde

```json
{
  "eventType": "campaign.optimized",
  "timestamp": "2026-08-17T14:22:10Z",
  "payload": {
    "caseId": "d4e5...",
    "campaignNo": "CMP-2026-000123",
    "expertId": "a7f3...",
    "segment": "RISKLI_KAYIP",
    "priority": "YUKSEK",
    "conversionLift": 0.18,
    "createdAt": "2026-08-17T13:40:02Z",
    "completedAt": "2026-08-17T14:22:10Z",
    "slaDeadline": "2026-08-17T21:40:02Z"
  }
}
```

`slaDeadline` payload'a bilinçli olarak konuldu: SLA süresini belirleyen `SLA_TIME_UNIT`
ayarının sahibi Campaign Service'tir, dolayısıyla bir vakanın ne zaman gecikmiş sayılacağını
yalnızca o bilebilir. Gamification'ın bunu kendi tarafında "2 saat" gibi sabit bir süreyle
yeniden hesaplaması, demo için birim dakikaya indirildiğinde SLA'yı aşmış bir vakaya
"SLA içinde tamamlandı" bonusu verilmesine yol açardı. Vaka SLA takibinden önce
oluşturulmuşsa alan `null` gelir; bu durumda bonus verilmez.

**Gamification Service tarafında beklenen işlem:** süre hesapla (completedAt - createdAt), +10 puan (temel), +5 (2 saatten kısa sürdüyse), +15 (conversionLift hedefi aştıysa), priority `KRITIK` ve `completedAt < slaDeadline` ise ek +15; ardından rozet koşullarını kontrol et, uygunsa `badge.earned` yayınla.

---

## segment.changed

**Yayınlayan:** Campaign Service
**Dinleyen:** AI Service (doğruluk takibi / "yanlış sınıflandırma" kaydı için)
**Ne zaman tetiklenir:** Uzman veya süpervizör, AI'ın atadığı segmenti override ettiğinde

```json
{
  "eventType": "segment.changed",
  "timestamp": "2026-08-17T14:05:00Z",
  "payload": {
    "campaignNo": "CMP-2026-000123",
    "changedBy": "a7f3...",
    "changedByRole": "EXPERT",
    "originalSegment": "YUKSEK_DEGER",
    "correctedSegment": "PASIF"
  }
}
```

**AI Service tarafında beklenen işlem:** bu kaydı "yanlış sınıflandırma" olarak işaretle, doğruluk oranı hesaplamasında (`doğru / toplam × 100`) payda ve pay güncellensin.

Aynı kampanya birden fazla kez düzeltilebilir, dolayısıyla bu olay aynı `campaignNo` için
tekrar gelebilir. AI tarafı olayları **saymamalı**, `campaignNo` başına son değeri
tutmalıdır: doğruluk sorusu "AI kaç kez düzeltildi" değil, "AI'ın ilk kararı sonunda doğru
muydu" sorusudur. Segmenti aynı değere set eden istek hiç olay doğurmaz — düzeltme
sayılmaz.

---

## offer.responded

**Yayınlayan:** Campaign Service
**Dinleyen:** AI Service (öneri skorunu güncellemek için — "ilgilenmiyorum" cevabı benzer kampanyaların skorunu düşürür)
**Ne zaman tetiklenir:** Abone bir teklife Kabul/İlgilenmiyorum yanıtı verdiğinde

```json
{
  "eventType": "offer.responded",
  "timestamp": "2026-08-17T15:10:00Z",
  "payload": {
    "offerId": "f1a2...",
    "subscriberId": "b7e1...",
    "campaignNo": "CMP-2026-000123",
    "response": "DECLINED"
  }
}
```

---

## offer.rated

**Yayınlayan:** Campaign Service
**Dinleyen:** Gamification Service (düşük puan cezası)
**Ne zaman tetiklenir:** Abone bir teklifi 1–5 yıldız puanladığında (puanlama tek seferliktir)

```json
{
  "eventType": "offer.rated",
  "timestamp": "2026-08-17T15:30:00Z",
  "payload": {
    "offerId": "f1a2...",
    "subscriberId": "b7e1...",
    "campaignNo": "CMP-2026-000123",
    "expertId": "a7f3...",
    "stars": 2
  }
}
```

Bu olay şemanın ilk halinde yoktu; case 5.6 ("Puan verildiğinde Gamification Service'e event
gönderilir") ve 7.1'deki −3 kuralı için zorunlu olduğu için eklendi.

**`expertId` neden payload'da:** Gamification'ın kampanya verisi yok, bir kampanyanın
hangi uzman tarafından optimize edildiğini kendisi bulamaz. Bu bağı yalnızca Campaign
kurabilir. Kampanya için hiç optimizasyon vakası açılmamışsa alan `null` gelir ve kimsenin
puanı kırılmaz.

**Gamification Service tarafında beklenen işlem:** `stars` 1 veya 2 ise −3 puan; 3 ve üzeri
için hiçbir şey yapılmaz — puan tablosunda iyi puanın ödülü yok. İdempotency `offerId` +
`LOW_RATING` çifti üzerinden sağlanır.

---

## sla.breached

**Yayınlayan:** Campaign Service (arka planda çalışan bir zamanlayıcı/scheduler, SLA süresini dolan aktif vakaları tarar)
**Dinleyen:** Gamification Service (-5 puan kırılımı)
**Ne zaman tetiklenir:** Bir optimizasyon vakasının SLA süresi, vaka `TAMAMLANDI` durumuna geçmeden dolduğunda

Vaka başına **bir kez** yayınlanır: tarama, aşımı ilk gördüğünde vakaya `slaBreachedAt`
damgası atar ve sonraki taramalar damgalı vakaları hiç görmez. `expertId`, vaka henüz
kimseye atanmamışken aşıma girdiyse `null` gelir — aşım yine kaydedilir, sadece kimsenin
puanı kırılmaz.

```json
{
  "eventType": "sla.breached",
  "timestamp": "2026-08-17T16:00:00Z",
  "payload": {
    "caseId": "d4e5...",
    "campaignNo": "CMP-2026-000123",
    "expertId": "a7f3...",
    "priority": "KRITIK",
    "slaDeadline": "2026-08-17T15:40:02Z"
  }
}
```

---

## badge.earned

**Yayınlayan:** Gamification Service
**Dinleyen:** Mobil (Kotlin) — anlık toast/modal bildirimi için. Gerçek zamanlı push zorunlu değil; ilgili ekran `GET /api/v1/game/profile` ile pollingle de bu bilgiyi görebilir (case'in "sayfa yenilemede güncel" alternatifi).
**Ne zaman tetiklenir:** Bir uzman, herhangi bir rozet koşulunu (İlk Kampanya, Hız Ustası, Dönüşüm Kralı, Maratoncu, Churn Avcısı, Uzman) sağladığında

```json
{
  "eventType": "badge.earned",
  "timestamp": "2026-08-17T14:22:11Z",
  "payload": {
    "expertId": "a7f3...",
    "badge": "HIZ_USTASI",
    "totalPoints": 1240
  }
}
```

---

## RabbitMQ Kurulum Notu

`docker-compose.yml`'e eklenecek servis:

```yaml
rabbitmq:
  image: rabbitmq:3-management
  ports:
    - "5672:5672"    # AMQP protokolü, servislerin bağlandığı port
    - "15672:15672"  # Management UI (http://localhost:15672, varsayılan guest/guest)
```

Her tüketici servis (`Gamification`, `AI`) kendi kuyruğunu ilgili routing key'lere bind eder:

| Servis | Dinlediği routing key'ler |
|---|---|
| Gamification Service | `campaign.optimized`, `sla.breached`, `offer.rated` |
| AI Service | `segment.changed`, `offer.responded` |

Mobil, event'leri doğrudan dinlemez (RabbitMQ'ya bağlanmaz) — `badge.earned` gibi kullanıcıya gösterilecek sonuçlar, ilgili REST endpoint'i (`/api/v1/game/profile`) üzerinden okunur.
