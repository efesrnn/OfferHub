## Şu şekil çalıştırılabilir

```bash
docker compose up -d
bash scripts/test/security.sh
```

35 kontrolün hepsi geçiyor. Script `scripts/test/security.sh` içinde, her kontrolün yanında
ne beklediği yazılı.

## Özet

| Senaryo                   | Sonuç                                       |
| ------------------------- | ------------------------------------------- |
| SQL enjeksiyonu           | Girdi veri olarak saklanıyor, tablo duruyor |
| XSS                       | Üç alanda da 400                            |
| Token manipülasyonu       | Yedi varyantın hepsi 401                    |
| Refresh token replay      | 401                                         |
| Yetkisiz endpoint erişimi | 403                                         |
| IDOR                      | 403                                         |
| Gateway atlatma           | 403 ve 401                                  |
| Girdi doğrulama           | Sekiz sınır durumu 400                      |
| Brute force               | 429                                         |

## 1. SQL enjeksiyonu

Denenen girdiler: `' OR 1=1 --`, `x'); DROP TABLE campaigns;--`, sorgu parametresine
`YENI' OR 1=1--`, `assignedTo` alanına `1' OR '1'='1`.

Gövdeye yazılan enjeksiyon denemeleri **201 dönüyor ve kampanya oluşuyor**. Bu bir açık
değil, doğru davranış. `' OR 1=1 --` metni bir kampanya başlığı olarak tamamen geçerli bir
metindir. Beklenen şey onun reddedilmesi değil, veri olarak kalıp asla sorgu olarak
çalışmaması. Script bunu tablonun kayıt sayısını önce ve sonra karşılaştırarak doğruluyor.

Neden çalışmıyor: Sorguların hepsi JPA üzerinden ve parametreli. `CampaignRepository.search`
gibi yerlerde değerler `:status` ve `:segment` gibi bağlanmış parametreler olarak gidiyor,
metin birleştirme ile sorgu kurulan tek bir yer yok. Veritabanı parametreyi her zaman değer
olarak görüyor, hiçbir koşulda sorgunun bir parçası olarak değil.

Sorgu parametresine yapılan enjeksiyon ise **400 dönüyor**, çünkü `status` alanı `String`
değil `CampaignStatus` enum'u. Değer enum'a çevrilemediği için istek sorguya hiç ulaşmıyor.
Burada tip sisteminin kendisi bir savunma katmanı, ayrıca bir kontrol yazmaya gerek kalmıyor.

`assignedTo` alanı `String` olduğu için orada elle kontrol var. `CaseController`
`UUID.fromString` deniyor, olmuyorsa `VALIDATION_ERROR` fırlatıyor.

## 2. XSS

Denenen girdiler: `<script>alert(1)</script>`, `<img src=x onerror=alert(1)>`, optimizasyon
notuna script etiketi.

Üçü de **400**.

Neden: Metin kabul eden üç DTO alanında da `@Pattern(regexp = "[^<>]*")` var.
`CreateCampaignRequest.title`, `StatusChangeRequest.optimizationNote` ve
`ClassificationRequest.reason`.

Burada bilinçli bir tercih var. Yaygın yaklaşım girdiyi kabul edip çıktıda kaçışlamaktır.
Biz girişte reddetmeyi seçtik, çünkü bu alanların hiçbirinde `<` ve `>` karakterlerinin
meşru bir kullanımı yok. Bir kampanya başlığında açılı parantez olmaz. Reddetmek, veriyi
kimin nasıl göstereceğine bağlı kalmamayı sağlıyor: mobil uygulama bugün Compose ile
render ediyor ve zaten HTML yorumlamıyor, ama yarın bir web paneli eklenirse veritabanında
bekleyen bir script etiketi kalmamış oluyor.

Kaçışlamanın yerini tutmuyor, tamamlıyor. İkisi birden yapılabilseydi daha iyi olurdu.

## 3. Token manipülasyonu

| Denenen                                     | Sonuç |
| ------------------------------------------- | ----- |
| İmzanın son karakteri değiştirilmiş token   | 401   |
| `alg=none` ile imzasız token                | 401   |
| Süresi dolmuş token                         | 401   |
| Boş token                                   | 401   |
| JWT olmayan rastgele metin                  | 401   |
| Refresh token ile API çağrısı               | 401   |
| İmzası doğru ama `role` alanı olmayan token | 401   |

`alg=none` klasik JWT açığıdır. Kütüphane token'ın kendi belirttiği algoritmaya güvenirse,
saldırgan algoritmayı `none` yapıp imzayı boş bırakarak istediği payload'u geçirebilir.
`JwtAuthenticationFilter` bunu `Jwts.parser().verifyWith(key)` ile kapatıyor. Anahtar
önceden veriliyor ve doğrulama zorunlu, token'ın ne iddia ettiğinin bir önemi yok.

**Refresh token replay** için ayrı bir savunma gerekti. Refresh token access token ile aynı
anahtarla imzalanıyor, dolayısıyla imza doğrulaması ikisini ayırt edemez. Ayıran tek şey
`type` alanı, filtre de onu açıkça kontrol ediyor. Bu olmasaydı yedi gün ömürlü bir refresh
token, on beş dakikalık bir access token yerine geçerdi.

**Rol alanı olmayan token** ilk bakışta önemsiz görünür ama önemli. İmza doğru olduğu için
filtre token'ı kabul ediyor, sonra `null` bir rolden başlık kurmaya çalışıyor ve orada
patlıyordu. Sonuç 401 yerine 500 olurdu. Bozuk bir token'a "sistem çöktü" cevabı vermek hem
yanlış bilgi hem de saldırgana bilgi sızdırır. Filtre şimdi iki alanın da varlığını açıkça
kontrol edip 401 dönüyor.

## 4. Yetkisiz erişim ve IDOR

| Denenen                                          | Sonuç |
| ------------------------------------------------ | ----- |
| Abone token'ı ile süpervizör dashboard'u         | 403   |
| Uzman token'ı ile manuel atama                   | 403   |
| Abone token'ı ile oyunlaştırma profili           | 403   |
| Uzman token'ı ile liderlik tablosu yeniden kurma | 403   |
| Başkasının teklifini okuma                       | 403   |
| Başkasının teklifini kabul etme                  | 403   |
| Başkasının teklifini puanlama                    | 403   |

Rol kontrolü endpoint seviyesinde ve her metodun ilk satırında:
`caller.requireAnyOf(Role.SUPERVISOR)` gibi. Rol yetki matrisi doğrudan bu çağrılara
karşılık geliyor.

IDOR için asıl savunma bir kontrol değil, bir tasarım tercihi. **Abone kimliği hiçbir yolda
ve hiçbir gövdede geçmiyor.** Yollar `/api/v1/subscribers/me/offers` şeklinde, `me` sabit ve
gerçek kimlik token'dan geliyor. Değiştirilecek bir id olmayınca değiştirme saldırısı da
olmuyor. Aynı şey Gamification'da da geçerli, `/api/v1/game/profile` her zaman çağıranın
kendi profilini döndürüyor.

Teklif id'si yolda geçmek zorunda, orası kaçınılmaz. Orada da sahiplik kontrol ediliyor ve
404 değil **403** dönüyor. İkisi arasında seçim yaptık: 404 dönmek bir id'nin var olup
olmadığını sızdırmamayı sağlar, 403 ise daha dürüst bir cevaptır. Teklif id'leri UUID
olduğu için tahmin edilerek numaralandırılamıyorlar, dolayısıyla 404'ün getireceği ek koruma
pratikte çok küçük kalıyor.

## 5. Gateway atlatma

| Denenen                                                 | Sonuç |
| ------------------------------------------------------- | ----- |
| Campaign servisine doğrudan istek (8082)                | 403   |
| Gamification servisine doğrudan istek (8084)            | 403   |
| Token yerine elle `X-User-Id` başlığı                   | 401   |
| Geçerli abone token'ı üzerine elle `X-User-Role: ADMIN` | 403   |

Mimari şöyle: kimlik doğrulama gateway'de, yetkilendirme servislerde. Gateway JWT'yi
doğruluyor ve sonucu `X-User-Id` ve `X-User-Role` başlıklarıyla aşağı geçiriyor. Servisler
bu iki başlığa bakıyor.

Bu tasarımın bariz sorusu şu: o zaman biri başlıkları kendisi yazıp geçemez mi?

Geçemiyor, iki sebeple. Birincisi, `JwtAuthenticationFilter` daha ilk satırda gelen istekten
bu iki başlığı **siliyor**, sonra kendi doğruladığı değerleri yazıyor. İstemcinin gönderdiği
başlık hiçbir koşulda aşağı ulaşmıyor. Yukarıdaki dördüncü satır tam olarak bunu kanıtlıyor:
geçerli bir abone token'ı ile birlikte `X-User-Role: ADMIN` gönderdiğimizde istek yine 403
alıyor, çünkü gateway o başlığı atıp yerine `SUBSCRIBER` yazıyor.

İkincisi, servisler compose ağının dışına açık değil. Test 8082'ye doğrudan gidebiliyor
çünkü geliştirme için portlar yayınlanmış durumda, ve o durumda bile başlıksız istek 403
alıyor.

`campaign/config/SecurityConfig.java` içindeki `permitAll` bu yüzden **kasıtlı**. Servisin
kendi Spring Security zincirinde kimlik doğrulaması yok, çünkü kimlik doğrulama zaten
gateway'de yapılmış durumda. Aynı işi iki kere yapmak, iki ayrı yerde tutulan iki ayrı
doğruluk kaynağı demek olurdu. Yetkilendirme ise servisin kendisinde, çünkü hangi rolün ne
yapabileceği iş kuralıdır ve gateway'in bilmesi gereken bir şey değildir.

## 6. Girdi doğrulama ve kaynak tüketimi

| Denenen                   | Sonuç            |
| ------------------------- | ---------------- |
| `size=99999`              | 100'e kırpılıyor |
| `page=-5`                 | 0'a çekiliyor    |
| İndirim oranı 101         | 400              |
| İndirim oranı -1          | 400              |
| Geçmiş tarihli geçerlilik | 400              |
| Bilinmeyen enum değeri    | 400              |
| 201 karakterlik başlık    | 400              |
| Boş gövde                 | 400              |

Sayfa boyutu kırpması bir doğrulama değil, kasıtlı bir sınır. `Math.clamp(size, 1, 100)`
isteği reddetmiyor, sessizce 100'e indiriyor. Sebebi, `size=99999` gönderen bir istemcinin
kötü niyetli olmak zorunda olmaması. Reddetmek yerine sınırlamak, hem istemciyi çalışır
halde tutuyor hem de tek bir isteğin veritabanından ne kadar çekebileceğine tavan koyuyor.

Diğerleri Jakarta Bean Validation ile, `@Min`, `@Max`, `@Future`, `@Size`, `@NotNull`.
Hepsi `GlobalExceptionHandler` üzerinden aynı `{success, data, error}` zarfıyla ve
`VALIDATION_ERROR` koduyla dönüyor.

## 7. Brute force

`/api/v1/auth/login` adresine 20 ardışık istek atıldığında ilk 10 tanesi 401, kalan 10 tanesi
**429** dönüyor.

Rate limit gateway'de ve **yalnızca token gerektirmeyen yollara** uygulanıyor. Bu bilinçli.
Savunulan şey şifre ve OTP denemesidir, ikisi de kimlik doğrulanmadan önce olur, dolayısıyla
sayacı bağlayacak bir kullanıcı yoktur. Sayaç istemci IP'sine bağlı.

Sayaç Redis'te tutuluyor, bellekte değil. Sebebi, birden fazla gateway kopyası çalıştığında
saldırganın başka bir kopyaya yönlendirilerek sıfır kredi almasını engellemek.

Redis'e ulaşılamazsa sistem **açık kalıyor**, kapalı değil. Bu bir tercih: alternatifi, önbellek
arızasının bütün sistemde girişi kapatması olurdu. Rate limit bir savunma katmanıdır, tek
başına kimlik doğrulama değildir, o yüzden kaybedildiğinde sistemi durdurmaması daha doğru.
