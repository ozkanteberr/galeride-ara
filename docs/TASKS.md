# Atomik çalışma listesi

Her görev tek amaç taşır. Başlamadan önce kapsam ve kabul ölçütü belirtilir; görev doğrulanıp sonuç bildirildikten sonra sıradakine geçilir. Alt görevler toplu bir özellik değişikliğine dönüştürülmez. Bu liste bütün maddelerin aynı oturumda uygulanması için talimat değildir.

## İlk başlangıç paketinin durumu

İlk istek üzerine iskelet, galeri erişimi ve arama çekirdeği aynı değişiklik grubunda oluşturuldu. Kullanıcının atomik ilerleme tercihi sonrasında yeni özellik ekleme durduruldu. Mevcut kodun derleme/test doğrulamasıyla bu başlangıç adımı kapatılacak.

## Sıradaki görevler

| ID | Tek amaç | Tamamlanma ölçütü |
|---|---|---|
| A01 (tamamlandı) | Başlangıç iskeletini doğrula | 5 çekirdek testi geçti; debug APK üretildi; lint: 0 hata, 9 uyarı |
| A02 | Galeri izin akışını cihazda doğrula | Ret/tam/sınırlı izin ve ayarlardan dönüş senaryoları kayıtlı |
| A03 | PicQuery model sözleşmesini sabitle | Referans commit, model/tokenizer/ön işleme girdileri belgeli |
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
