# A02 - Galeri izinlerinin cihazda doğrulanması

Tarih: 26 Ekim 2026. Sonuç: tamamlandı; bir işlev hatası düzeltildi.

## Ortam ve yöntem

- Android 15 / API 35, Pixel 4 emülatörü (`emulator-5554`).
- Uygulama: `com.galeriara`, debug APK, hedef API 36.
- Başlangıçta galeri boştu ve iki medya izni de verilmişti (`USER_SET`).
- Üç geçici PNG kullanıldı: kırmızı, yeşil ve mavi. Gerçek kullanıcı fotoğrafı kullanılmadı.
- Sistem izin pencereleri, fotoğraf seçici ve uygulama ayarları ADB/UIAutomator ile kullanıldı. Görünen fotoğraf adları, erişim mesajları ve ilgili pencere öğeleri UI ağacından kontrol edildi.
- Yalnız başlangıç durumunu hazırlamak, düzeltme kontrolü için tam erişimi kurmak ve sonunda izinleri geri yüklemek amacıyla ADB izin komutları kullanıldı. Ret, seçim ekleme/çıkarma ve ayarlardan erişim daraltma/kaldırma işlemleri sistem arayüzünde yapıldı.

## Senaryolar

| No | İşlem | Gözlenen sonuç | Durum |
|---|---|---|---|
| 1 | İzin verilmemişken açılış | Fotoğraf yok; erişim ve uygulama ayarları düğmeleri görünür | Geçti |
| 2 | Sistem izin isteğini reddetme | Uygulama açık kalır, fotoğraf gösterilmez | Geçti |
| 3 | Retten sonra kapatıp yeniden açma | Ret durumu korunur; eski fotoğraf görünmez | Geçti |
| 4 | İlk retten sonra yeniden izin isteme | Sistem izin penceresi tekrar açılır | Geçti |
| 5 | İkinci ret ve tekrar isteme | Android pencereyi açmaz; uygulama ayarları düğmesiyle izin sayfasına ulaşılır | Geçti |
| 6 | Ayarlardan tam erişim verme ve geri dönme | Üç test fotoğrafı listelenir | Geçti |
| 7 | Tam erişimle yeniden başlatma | Aynı üç fotoğraf tekrar listelenir | Geçti |
| 8 | Ayarlardan tam erişimi sınırlı erişime düşürme, seçim yok | Eski üç fotoğraf temizlenir; sınırlı erişim ve boş liste mesajları görünür | Geçti |
| 9 | Sınırlı erişimde tek fotoğraf seçme | Yalnız kırmızı fotoğraf görünür; diğer ikisi görünmez | Geçti |
| 10 | Sınırlı erişimle yeniden başlatma | Yalnız kırmızı fotoğraf tekrar görünür | Geçti |
| 11 | Seçime yeşil fotoğrafı ekleme | Kırmızı ve yeşil görünür; mavi görünmez | Geçti |
| 12 | Kırmızı fotoğrafı seçimden çıkarma | Yalnız yeşil kalır; kırmızı listeden temizlenir | Geçti |
| 13 | Ayarlardan tüm erişimi kaldırıp uygulamaya dönme | Önceki yeşil fotoğraf kaybolur; erişim isteme ekranı gösterilir | Geçti |
| 14 | Düzeltmeli APK'da tam erişimde Erişimi değiştir | Uygulama ayarları açılır | Geçti |
| 15 | Düzeltmeli APK'da sınırlı erişimde Erişimi değiştir | İzin akışı üzerinden fotoğraf seçici açılır; iptal edilince uygulamaya dönülür | Geçti |

Android izin daraltıldığında uygulama işlemini sonlandırabilir. Bu nedenle geri dönüş kontrolü mevcut işlemin mutlaka hayatta kalacağı varsayımıyla yapılmadı; yeniden açılış da kontrol edildi. Sistem bazen sınırlı erişim için önce izin penceresini, sonraki seçimlerde doğrudan fotoğraf seçicisini gösterdi; iki yol da gözlendi.

## Bulunan hata ve dar kapsamlı düzeltme

Tam erişim verilmişken Erişimi değiştir düğmesi aynı izinleri yeniden istiyordu. Android zaten verilmiş izinlerde pencere açmadan callback'i tamamladığı için düğme görünür bir işlem yapmıyordu.

`GalleryRoute.kt` içinde güncel erişim durumu kontrol edildi: tam erişimde sistem uygulama ayarlarına gidilir; sınırlı veya reddedilmiş erişimde mevcut izin isteği çalışır. Ayarlara gitme işlemi ortak bir callback'te toplandı. Yeni mimari katman, bağımlılık veya ekran eklenmedi.

Senaryolar 1-13 hatanın araştırıldığı sürümde yürütüldü. Düzeltmeli APK ayrıca reddedilmiş ve tam erişimle açıldı; değişen tam erişim dalı ve korunan sınırlı erişim dalı tekrar kontrol edildi (14-15).

## Proje kontrolleri ve temizlik

- `:core:test :app:assembleDebug :app:lintDebug` başarılı. Mevcut 5 çekirdek testi sonucu Gradle tarafından yeniden kullanıldı; bunlar cihaz izin testleri değildir.
- Lint: 0 hata, mevcut 9 uyarı.
- Üç test fotoğrafı MediaStore kayıtlarıyla birlikte kaldırıldı. Galeri tekrar boş olarak doğrulandı.
- Başlangıçtaki iki tam erişim izni ve `USER_SET` bayrakları geri yüklendi; uygulama açık bırakıldı.
- Commit oluşturulmadı.

## Sınırlar

Bu sonuç Android 15/API 35 emülatörüne aittir. Android 8-12'nin depolama izni, Android 13'ün medya izni ve diğer üreticilerin cihazları çalıştırılarak doğrulanmadı. Bu belge cihazda yürütülen kontrol kaydıdır; CI üzerinde çalışan kalıcı UI test takımı değildir. Fotoğraf arama ve model entegrasyonu A02 kapsamı dışındadır.
