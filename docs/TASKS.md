# Atomik çalışma listesi

Her görev tek amaç taşır. Görevden önce önceki adımın sonucu ve sıradaki kapsam kullanıcıya anlatılır; kullanıcı onayından sonra başlanır. Mimari kararlar üç alternatif, artı/eksi yönleri ve öneriyle kullanıcıya sunulur; kullanıcı seçim yapmadan uygulanmaz. Görev sonunda tek cümlelik İngilizce commit mesajı önerilir; commit işlemini kullanıcı yapar. Başlamadan önce kapsam ve kabul ölçütü belirtilir; görev doğrulanıp sonuç bildirildikten sonra sıradakine geçilir. Alt görevler toplu bir özellik değişikliğine dönüştürülmez. Bu liste bütün maddelerin aynı oturumda uygulanması için talimat değildir.

## İlk başlangıç paketinin durumu

İlk istek üzerine iskelet, galeri erişimi ve arama çekirdeği aynı değişiklik grubunda oluşturuldu. Kullanıcının atomik ilerleme tercihi sonrasında yeni özellik ekleme durduruldu. Mevcut kodun derleme/test doğrulamasıyla bu başlangıç adımı kapatılacak.

## Sıradaki görevler

| ID | Tek amaç | Tamamlanma ölçütü |
|---|---|---|
| A01 (tamamlandı) | Başlangıç iskeletini doğrula | 5 çekirdek testi geçti; debug APK üretildi; lint: 0 hata, 9 uyarı |
| A01.1 (tamamlandı) | Galeri arayüzünü sorumluluklarına göre dosyalara ayır | Debug APK ve 5 test başarılı; lint 0 hata, 9 uyarı; emülatörde açılış, izin düğmesi, liste, önizleme ve kapatma doğrulandı |
| A02 (tamamlandı) | Galeri izin akışını cihazda doğrula | API 35 üzerinde ret/tam/sınırlı erişim, seçim değişikliği ve ayarlardan dönüş doğrulandı; tam erişimde ayarlara yönlendirme düzeltildi; [kontrol kaydı](validation/A02_GALLERY_PERMISSIONS.md) |
| A03 (tamamlandı) | PicQuery model sözleşmesini sabitle | Sabit uygulama/dışa aktarıcı/model sürümleri, I/O, tokenizer ve ön işleme belgeli; kullanıcı OpenCLIP referansını seçti; [sözleşme](MODEL_CONTRACT.md), [kaynak özetleri](MODEL_REFERENCES.json) |
| A04 | Türkçe çeviri adayını seç | Kaynak, lisans, boyut ve Android uygulanabilirliği doğrulanmış |
| A05 | Tek görselin ONNX vektörünü üret | Sabit görsel için referans çıktıyla sayısal karşılaştırma geçiyor |
| A06 | Tek sorgunun metin vektörünü üret | Eşleşen tokenizer ve model, sabit metin için karşılaştırma geçiyor |
| A07 | Türkçe sorgu dönüşümünü bağla | Sabit Türkçe sorgular için beklenen dönüşüm davranışı kayıtlı |
| A08 | Vektörün Room'a yazılıp okunmasını ekle | Float32 round-trip ve model sürümü ayrımı testli |
| A09 | Tek fotoğrafı kalıcı indeksle | Vektör ve metadata tutarlı kaydediliyor |
| A10 | Artımlı iş listesini ekle | Değişmeyen fotoğraf atlanıyor, değişen tekrar işleniyor |
| A11 | İndeksleme işini WorkManager'a taşı | Tek benzersiz iş ve iptal/yeniden başlama davranışı doğrulanmış |
| A12 | Kalıcı vektörleri aramaya bağla | Yalnız güncel model ve erişilebilir fotoğraflar sonuç oluyor |
| A13 | Arama ekranını etkinleştir | Türkçe sorgu gerçek sonuç ve boş/hata durumlarını gösteriyor |
| A14 | Değerlendirme sorgu setini hazırla | Fotoğraf izinleri, sorgular ve ilgili sonuç etiketleri kayıtlı |
| A15 | Başarım deneyini çalıştır | Erişim ve cihaz performans ölçümleri yeniden üretilebilir |
| A16 | Ön raporu hazırla | PDF şablonundaki bölümler, çizimler, kaynaklar ve takvim tamam |

Gerçek model entegrasyonu sırasında çıkan belirsizlikler yeni küçük görevlere ayrılacak. A03-A04 sonuçları gerekirse sonraki model görevlerini güncelleyecek.

## A01 doğrulama kaydı - 26 Ekim 2026

- JDK 17, Gradle 8.13, AGP 8.13.2 ile `:app:assembleDebug` ve `:app:lintDebug` tamamlandı.
- `:core:test -PcoreBuildDir=<TEMP>/galeriara-core-build`: 5 test, 0 hata. Bu ortamda varsayılan Unicode çıktı yoluyla test çalıştırma başarısız; README'deki seçenek kullanılmalı.
- Lint uyarıları: bağımlılık sürümü önerileri (6), Android 12 veri aktarımı/backup kuralları (1), uygulama ikonu (1), KTX stil önerisi (1). Mevcut sürüm cihazda doğrulanmadı ve dağıtıma hazır sayılmıyor.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Model inference, kalıcı vektör adaptörü, indeksleme Worker ve gerçek anlamsal arama henüz uygulanmadı.

## A03 doğrulama kaydı — 23 Kasım 2026

- Kullanıcı üç seçenek arasından OpenCLIP referans akışını seçti. PicQuery uygulamasındaki küçük görsel ölçekleme ve tokenizer farkları sözleşmede açıklandı.
- Referans commit'ler ve model deposu revision'ları sabitlendi; kaynak SHA-256 değerleri yerelde hesaplandı. Ağırlık dosyası özeti sunucu metadata'sından alındı; ağırlık indirilmedi.
- Model yapılandırmaları, dışa aktarma I/O tanımları, BPE kaynakları ve doküman bağlantıları kontrol edildi. Kaynak algoritmasından türetilen token örnekleri runtime testi değildir.
- ONNX dosyası üretimi/özeti, Android çıkarımı, sayısal model karşılaştırması ve EXIF/decoder doğrulaması A05/A06 görevlerinde kalır. Derleme veya uygulama testi çalıştırılmadı; bu görev belge değişikliğidir.
- Commit oluşturulmadı. Sıradaki görev A04; ayrı kapsam açıklaması ve kullanıcı onayıyla başlanır.
