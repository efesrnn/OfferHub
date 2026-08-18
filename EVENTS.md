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
    "completedAt": "2026-08-17T14:22:10Z"
  }
}
```

**Gamification Service tarafında beklenen işlem:** süre hesapla (completedAt - createdAt), +10 puan (temel), +5 (2 saatten kısa sürdüyse), +15 (conversionLift hedefi aştıysa), priority `KRITIK` ve SLA içinde tamamlandıysa ek +15; ardından rozet koşullarını kontrol et, uygunsa `badge.earned` yayınla.

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

## sla.breached

**Yayınlayan:** Campaign Service (arka planda çalışan bir zamanlayıcı/scheduler, SLA süresini dolan aktif vakaları tarar)
**Dinleyen:** Gamification Service (-5 puan kırılımı)
**Ne zaman tetiklenir:** Bir optimizasyon vakasının SLA süresi, vaka `TAMAMLANDI` durumuna geçmeden dolduğunda

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
| Gamification Service | `campaign.optimized`, `sla.breached` |
| AI Service | `segment.changed`, `offer.responded` |

Mobil, event'leri doğrudan dinlemez (RabbitMQ'ya bağlanmaz) — `badge.earned` gibi kullanıcıya gösterilecek sonuçlar, ilgili REST endpoint'i (`/api/v1/game/profile`) üzerinden okunur.
