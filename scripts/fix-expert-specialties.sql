-- AI Service'in ExpertAssignmentService'i uzman uygunlugunu campaign.segment (Segment enum:
-- YUKSEK_DEGER/RISKLI_KAYIP/YENI_ABONE/PASIF) ile expert.specialties listesini karsilastirarak
-- belirliyor. Demo hesaplari yanlislikla kampanya turu (TARIFE_YUKSELTME, CIHAZ_FIRSATI, EK_PAKET,
-- SADAKAT) degerleriyle olusturulmustu - bu yuzden hicbir eslesme gerceklesmiyordu. Bu script
-- mevcut deneme2/deneme3/deneme4 hesaplarinin specialty degerlerini duzeltir.

DELETE FROM staff_user_specialties
WHERE staff_user_id = (SELECT id FROM staff_users WHERE email = 'deneme2@offerhub.com');
INSERT INTO staff_user_specialties (staff_user_id, specialty)
SELECT id, s FROM staff_users, UNNEST(ARRAY['YUKSEK_DEGER','RISKLI_KAYIP','YENI_ABONE','PASIF']) AS s
WHERE email = 'deneme2@offerhub.com';

DELETE FROM staff_user_specialties
WHERE staff_user_id = (SELECT id FROM staff_users WHERE email = 'deneme3@offerhub.com');
INSERT INTO staff_user_specialties (staff_user_id, specialty)
SELECT id, s FROM staff_users, UNNEST(ARRAY['RISKLI_KAYIP','PASIF','YENI_ABONE']) AS s
WHERE email = 'deneme3@offerhub.com';

DELETE FROM staff_user_specialties
WHERE staff_user_id = (SELECT id FROM staff_users WHERE email = 'deneme4@offerhub.com');
INSERT INTO staff_user_specialties (staff_user_id, specialty)
SELECT id, s FROM staff_users, UNNEST(ARRAY['RISKLI_KAYIP','PASIF']) AS s
WHERE email = 'deneme4@offerhub.com';
