-- Kampanya basliklarindan ic kullanime ait "(Uzman3)"/"(Uzman4)" etiketlerini kaldirir.
-- Bu etiket olusturan uzmani ayirt etmek icin eklenmisti ama abonelere goruntulenen
-- baslikta gorunmemeliydi.

UPDATE campaigns SET title = 'Riskli Kayıp - Ek Paket Fırsatı' WHERE campaign_no = 'CMP-2026-000025';
UPDATE campaigns SET title = 'Pasif - Ek Paket Denemesi' WHERE campaign_no = 'CMP-2026-000026';
UPDATE campaigns SET title = 'Yeni Abone - Sadakat Başlangıcı' WHERE campaign_no = 'CMP-2026-000027';
UPDATE campaigns SET title = 'Riskli Kayıp - Tarife Teklifi' WHERE campaign_no = 'CMP-2026-000028';
UPDATE campaigns SET title = 'Riskli Kayıp - Cihaz Fırsatı' WHERE campaign_no = 'CMP-2026-000029';
UPDATE campaigns SET title = 'Pasif - Tarife Yükseltme Fırsatı' WHERE campaign_no = 'CMP-2026-000030';
