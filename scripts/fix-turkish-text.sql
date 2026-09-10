-- Demo verisindeki kampanya basliklarini ve optimizasyon notlarini duzgun Turkce
-- karakterlerle (ç, ğ, ı, ö, ş, ü) duzeltir. PowerShell/terminal uzerinden gonderilen
-- veriler bilerek ASCII tutulmustu (encoding sorunlarindan kacinmak icin), simdi
-- dogrudan veritabaninda duzeltiliyor.

UPDATE campaigns SET title = 'Yüksek Değer - Sadakat Bonusu' WHERE campaign_no = 'CMP-2026-000013';
UPDATE campaigns SET title = 'Yüksek Değer - Cihaz Yenileme' WHERE campaign_no = 'CMP-2026-000014';
UPDATE campaigns SET title = 'Yüksek Değer - Ek Paket Fırsatı' WHERE campaign_no = 'CMP-2026-000015';
UPDATE campaigns SET title = 'Riskli Kayıp - Özel Ek Paket' WHERE campaign_no = 'CMP-2026-000016';
UPDATE campaigns SET title = 'Riskli Kayıp - Tarife Yükseltme Teklifi' WHERE campaign_no = 'CMP-2026-000017';
UPDATE campaigns SET title = 'Riskli Kayıp - Cihaz Fırsatı' WHERE campaign_no = 'CMP-2026-000018';
UPDATE campaigns SET title = 'Yeni Abone - Hoşgeldin Ek Paketi' WHERE campaign_no = 'CMP-2026-000019';
UPDATE campaigns SET title = 'Yeni Abone - Cihaz Taksit Fırsatı' WHERE campaign_no = 'CMP-2026-000020';
UPDATE campaigns SET title = 'Yeni Abone - Sadakat Programı Girişi' WHERE campaign_no = 'CMP-2026-000021';
UPDATE campaigns SET title = 'Pasif - Geri Kazanım Sadakat' WHERE campaign_no = 'CMP-2026-000022';
UPDATE campaigns SET title = 'Pasif - Tarife Yükseltme Denemesi' WHERE campaign_no = 'CMP-2026-000023';
UPDATE campaigns SET title = 'Pasif - Ek Paket Hatırlatma' WHERE campaign_no = 'CMP-2026-000024';
UPDATE campaigns SET title = 'Riskli Kayıp - Ek Paket (Uzman3)' WHERE campaign_no = 'CMP-2026-000025';
UPDATE campaigns SET title = 'Pasif - Ek Paket Denemesi (Uzman3)' WHERE campaign_no = 'CMP-2026-000026';
UPDATE campaigns SET title = 'Yeni Abone - Sadakat Başlangıcı (Uzman3)' WHERE campaign_no = 'CMP-2026-000027';
UPDATE campaigns SET title = 'Riskli Kayıp - Tarife Teklifi (Uzman4)' WHERE campaign_no = 'CMP-2026-000028';
UPDATE campaigns SET title = 'Riskli Kayıp - Cihaz Fırsatı (Uzman4)' WHERE campaign_no = 'CMP-2026-000029';
UPDATE campaigns SET title = 'Pasif - Tarife Yükseltme (Uzman4)' WHERE campaign_no = 'CMP-2026-000030';

UPDATE optimization_cases SET optimization_note = 'Ek paket limiti artırıldı, hedef kitleye özel indirim yeniden hesaplandı.'
    WHERE campaign_id = (SELECT id FROM campaigns WHERE campaign_no = 'CMP-2026-000019');
UPDATE optimization_cases SET optimization_note = 'Sadakat programı koşulları netleştirildi, indirim oranı optimize edildi.'
    WHERE campaign_id = (SELECT id FROM campaigns WHERE campaign_no = 'CMP-2026-000021');
UPDATE optimization_cases SET optimization_note = 'Tarife yükseltme teklifi pasif abonelere göre yeniden kurgulandı.'
    WHERE campaign_id = (SELECT id FROM campaigns WHERE campaign_no = 'CMP-2026-000023');
UPDATE optimization_cases SET optimization_note = 'Şikayet geçmişi incelendi, mevcut sınıflandırma doğru bulundu, indirim oranı revize edildi.'
    WHERE campaign_id = (SELECT id FROM campaigns WHERE campaign_no = 'CMP-2026-000025');
UPDATE optimization_cases SET optimization_note = 'Segment düzeltmesi sonrası teklif yeniden hesaplandı.'
    WHERE campaign_id = (SELECT id FROM campaigns WHERE campaign_no = 'CMP-2026-000026');
UPDATE optimization_cases SET optimization_note = 'Kampanya türü EK_PAKET''e çevrildi, dönüşüm tahmini yükseldi.'
    WHERE campaign_id = (SELECT id FROM campaigns WHERE campaign_no = 'CMP-2026-000027');
UPDATE optimization_cases SET optimization_note = 'Teklif metni netleştirildi, sınıflandırma değiştirilmedi.'
    WHERE campaign_id = (SELECT id FROM campaigns WHERE campaign_no = 'CMP-2026-000028');
UPDATE optimization_cases SET optimization_note = 'Segment düzeltmesi sonrası cihaz fırsatı yeniden fiyatlandı.'
    WHERE campaign_id = (SELECT id FROM campaigns WHERE campaign_no = 'CMP-2026-000029');
UPDATE optimization_cases SET optimization_note = 'Kampanya türü EK_PAKET''e çevrildi.'
    WHERE campaign_id = (SELECT id FROM campaigns WHERE campaign_no = 'CMP-2026-000030');
