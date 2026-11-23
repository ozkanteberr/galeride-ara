# Atomik çalışma listesi

Her görev tek amaç taşır. Markdown dosyalarına yeni tarih eklenmez. Görevden önce önceki adımın sonucu ve sıradaki kapsam kullanıcıya anlatılır; kullanıcı onayından sonra başlanır. Mimari kararlar üç alternatif, artı/eksi yönleri ve öneriyle kullanıcıya sunulur; kullanıcı seçim yapmadan uygulanmaz. Görev sonunda tek cümlelik İngilizce commit mesajı önerilir; commit işlemini kullanıcı yapar. Başlamadan önce kapsam ve kabul ölçütü belirtilir; görev doğrulanıp sonuç bildirildikten sonra sıradakine geçilir. Alt görevler toplu bir özellik değişikliğine dönüştürülmez. Bu liste bütün maddelerin aynı oturumda uygulanması için talimat değildir.

## İlk başlangıç paketinin durumu

İlk istek üzerine iskelet, galeri erişimi ve arama çekirdeği aynı değişiklik grubunda oluşturuldu. Kullanıcının atomik ilerleme tercihi sonrasında yeni özellik ekleme durduruldu. Mevcut kodun derleme/test doğrulamasıyla bu başlangıç adımı kapatılacak.

## Sıradaki görevler

| ID | Tek amaç | Tamamlanma ölçütü |
|---|---|---|
| A01 (tamamlandı) | Başlangıç iskeletini doğrula | 5 çekirdek testi geçti; debug APK üretildi; lint: 0 hata, 9 uyarı |
| A01.1 (tamamlandı) | Galeri arayüzünü sorumluluklarına göre dosyalara ayır | Debug APK ve 5 test başarılı; lint 0 hata, 9 uyarı; emülatörde açılış, izin düğmesi, liste, önizleme ve kapatma doğrulandı |
| A02 (tamamlandı) | Galeri izin akışını cihazda doğrula | API 35 üzerinde ret/tam/sınırlı erişim, seçim değişikliği ve ayarlardan dönüş doğrulandı; tam erişimde ayarlara yönlendirme düzeltildi; [kontrol kaydı](validation/A02_GALLERY_PERMISSIONS.md) |
| A03 (tamamlandı) | PicQuery model sözleşmesini sabitle | Sabit uygulama/dışa aktarıcı/model sürümleri, I/O, tokenizer ve ön işleme belgeli; kullanıcı OpenCLIP referansını seçti; [önceki aday sözleşmesi](MOBILECLIP_MODEL_CONTRACT.md), [kaynak özetleri](MODEL_REFERENCES.json) |
| A03.1 (tamamlandı) | Ödev koşulları ve rapor planını çıkar | PDF'nin 3 sayfası incelendi; gereksinimler, rapor yapısı ve 27 Aralık 2026 hedefli takvim [belgelendi](ASSIGNMENT_REQUIREMENTS.md) |
| A04 (tamamlandı) | Türkçe sorgu işleme yaklaşımını seç | Kullanıcı 1. seçeneği, standart çok dilli CLIP + eşleşen CLIP ViT-B/32 çiftini seçti; çeviri yok; [karar ve kaynaklar](A04_TURKISH_QUERY_OPTIONS.md) kayıtlı |
| A04.1 (tamamlandı) | Değerlendirme planını belgele | [Protokol](EVALUATION_PLAN.md) ve [ayarlar](EVALUATION_PLAN.json): 200 fotoğraf, 40 pozitif + 5 negatif sorgu, veri seti adayları, relevance, kalite/cihaz ölçümleri; deneyler henüz yapılmadı |
| A04.2 (tamamlandı) | Seçilen çok dilli CLIP model sözleşmesini uyarla | [Etkin sözleşme](MODEL_CONTRACT.md), [kaynak/kurallar](MODEL_CONTRACT.json) ve [14 tokenizer referansı](TOKENIZER_REFERENCE_CASES.json) hazır; görsel processor, pooling/Dense, hedef I/O ve toleranslar sabit; model çıkarımı henüz yapılmadı |
| A05 (tamamlandı) | Tek görselin ONNX vektörünü üret | Görsel + projeksiyon FP32 ONNX üretildi; SHA-256/I/O ve Python–masaüstü–API 35 Android karşılaştırması geçti; [kontrol kaydı](validation/A05_VISUAL_ENCODER.md) |
| A06 (tamamlandı) | Tek sorgunun metin vektörünü üret | Saf Kotlin tokenizer + seçilen U8U8 model + pooling/Dense; 24 örnek × 2 profil, Python–Android karşılaştırması ve L2 kontrolü geçti; [kayıt](validation/A06_TEXT_ENCODER.md) |
| A07 | Doğrudan Türkçe sorgu akışını QueryEncoder adaptörüne bağla | Sabit manifest kimliği, tokenizer ve metin modeliyle çevirisiz kodlama; boş sorgu, iptal ve hata davranışı doğrulanmış |
| A08 | Vektörün Room'a yazılıp okunmasını ekle | Float32 round-trip ve model sürümü ayrımı testli |
| A09 | Tek fotoğrafı kalıcı indeksle | Vektör ve metadata tutarlı kaydediliyor |
| A10 | Artımlı iş listesini ekle | Değişmeyen fotoğraf atlanıyor, değişen tekrar işleniyor |
| A11 | İndeksleme işini WorkManager'a taşı | Tek benzersiz iş ve iptal/yeniden başlama davranışı doğrulanmış |
| A12 | Kalıcı vektörleri aramaya bağla | Yalnız güncel model ve erişilebilir fotoğraflar sonuç oluyor |
| A13 | Arama ekranını etkinleştir | Türkçe sorgu gerçek sonuç ve boş/hata durumlarını gösteriyor |
| A14 | Değerlendirme sorgu setini hazırla | Fotoğraf izinleri, sorgular ve ilgili sonuç etiketleri kayıtlı |
| A15 | Başarım deneyini çalıştır | Erişim ve cihaz performans ölçümleri yeniden üretilebilir |
| A16 | Ön raporu hazırla | PDF bölüm yapısı; use case ve component çizimleri; numaralı atıflar; tarihli takvim; Times New Roman 12, tek satır; kapak/içindekiler hariç en çok 10 sayfa; PDF görsel kontrolü |

Ödev koşulları ve tarihli plan [ASSIGNMENT_REQUIREMENTS.md](ASSIGNMENT_REQUIREMENTS.md) dosyasındadır. PicQuery yalnız referanstır; çeviri PDF zorunluluğu değildir. A04 nihai modeli değiştiren bir kararla sonuçlanırsa A03 aday sözleşmesi, A05 başlamadan önce ayrı atomik güncellemeyle uyarlanır. Raporun son yazımı A16'da yapılır; kaynaklar, karar gerekçeleri ve deney kanıtları her görevde biriktirilir. 27 Aralık 2026 kullanıcı tarafından önerilen planlama hedefidir; kesin ders teslim tarihi olarak varsayılmaz.

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

## A03.1 ödev ve rapor uyumu kaydı - 23 Kasım 2026

- Kullanıcının verdiği Proje_Mobil_26.pdf dosyasının üç sayfası metin ve görsel olarak incelendi. Puanlama, içerik, çizim ve biçim koşulları kaynak sayfalarıyla eşleştirildi.
- Kullanıcı PicQuery'nin yalnız referans olduğunu belirtti; 27 Aralık 2026 tarihinin planlama hedefi olabileceğini bildirdi.
- A04 yalnız çeviri seçimine daraltılmadı; henüz model/çeviri kararı alınmadı. A03'te tamamlanan aday incelemesi korunur.
- Veri seti adaylarının ve deney planının rapor sonuna bırakılmaması için A04.1 eklendi. A14 gerçek değerlendirme setini hazırlama, A15 deneyleri yürütme görevleri olarak kalır.
- Ön rapor taslağının içeriği ve 9 sayfalık başlangıç bütçesi çıkarıldı; bu görevde nihai rapor PDF'si yazılmadı.
- Uygulama kodu değiştirilmedi; derleme veya uygulama testi gerektiren bir değişiklik yapılmadı. Belge bağlantıları ve değişiklik kapsamı kontrol edildi. Commit oluşturulmadı.

## A04 araştırma kaydı - 23 Kasım 2026

- Standart çok dilli CLIP, Türkçe için sözlüğü küçültülmüş CLIP ve MobileCLIP2-S0 + bağımsız OPUS-MT çevirisi karşılaştırıldı. Kullanıcıya öneri 1. seçenektir; henüz seçim alınmadı.
- Altı model deposunun sabit revision'ı ve yayımlanmış ağırlık metadata'sı kaydedildi; 30 küçük kaynak/yapılandırma dosyasının SHA-256 değeri yerelde doğrulandı. Model ağırlığı indirilmedi.
- Standart metin ONNX dosyasının tam arama zinciri olmadığı; pooling ve 768 -> 512 Dense katmanlarının gerektiği kaydedildi. Farklı nicemleme biçimlerindeki dosya boyutları eşdeğer performans ölçümü olarak kullanılmadı.
- Android uygulama yolları ve kalan doğrulama işleri açıklandı. Model dışa aktarma, gerçek çıkarım veya cihaz ölçümü yapılmadı. A04 kullanıcı seçimi gelmeden tamamlandı sayılmayacak.
- Önceki belge değişiklikleri korundu. Kullanıcının isteği doğrultusunda commit oluşturulmadı.

## A04 seçim sonucu ve A04.1 değerlendirme planı

- Kullanıcı standart çok dilli CLIP seçeneğini seçti. A04 tamamlandı; çeviri katmanı eklenmeyecek. Android model çıkarımı henüz uygulanmadı.
- XM3600, kontrollü/izinli yerel galeri ve Flickr30k veri seti adayları karşılaştırıldı. Ana kalite deneyi için XM3600'dan sabit 200 görsel; 40 pozitif sorgu (10 geliştirme, 30 test) ve 5 negatif kontrol planlandı.
- İlgililik dereceleri, bütün galeri üzerinde etiketleme, Recall/Precision/Hit/nDCG tanımları, aynı veride karşılaştırma ve cihaz ölçüm protokolü belgelendi. Veri indirme, etiketleme ve deney yürütme A14/A15 kapsamında kalır.
- Modelden bağımsız arama çekirdeği korunur; seçilen modelin yeni sözleşmesi A04.2 olarak ayrıldı. Önceki MobileCLIP sözleşmesi kaynak incelemesi olarak korunur; A05 için geçerli sözleşme sayılmaz.
- Kullanıcının tercihi kaydedildi: Markdown dosyalarına yeni tarih eklenmeyecek. Uygulama kodu değiştirilmedi; commit oluşturulmadı.

## A04.2 model sözleşmesi sonucu

- Seçilen standart çok dilli CLIP çiftinin etkin sözleşmesi MODEL_CONTRACT.md/JSON olarak yazıldı. Önceki MobileCLIP incelemesi ayrı arşiv belgesinde korundu.
- 224 piksel BICUBIC/merkez kırpma ve CLIP mean/std; cased WordPiece, 128 token üst sınırı; masked mean pooling ve 768 -> 512 Dense; görsel projeksiyon ve ortak L2 kuralları sabitlendi.
- 22 kaynak dosyasının özeti doğrulandı. Gerçek sabit tokenizer ile 14 örneğin token kimlikleri, truncation/padding ve attention mask davranışı kontrol edildi. Bu örnekler Kotlin tokenizer testinin yerine geçmez.
- Hedef görsel ONNX arayüzü ve sayısal kabul bütçeleri belgelendi. Yayımlanan metin grafiğinin gerçek I/O kontrolü A06'da yapılacak; henüz üretilmeyen görsel ONNX'in özeti boş bırakıldı.
- Sinir ağı ağırlığı indirilmedi; model/Android çıkarımı yapılmadı. Bu görev uygulama kodunu veya bağımlılıklarını değiştirmedi. Sıradaki görev A05; commit veya push oluşturulmadı.

## A05 tek görsel doğrulama sonucu

- Sabit kaynak ağırlığı indirildi ve doğrulandı; yalnız görsel dal/projeksiyon ONNX'e aktarıldı. Model 351610456 bayt; dosya özeti ve gerçek I/O etkin sözleşmeye işlendi.
- Bağımsız Transformers/PyTorch referansı, masaüstü ONNX ve API 35 x86_64 Android CPU çıktısı aynı kontrollü RGB PNG ile karşılaştırıldı. Sözleşmenin tensör, ön işleme ve tam görsel toleransları geçti; sayısal sonuçlar [kontrol kaydında](validation/A05_VISUAL_ENCODER.json).
- 5 çekirdek testi ve 1 Android instrumentation testi geçti; debug/test APK üretildi; lint 0 hata, 12 uyarı. Uyarıların 3'ü eklenen bağımlılıklar için sürüm önerisidir; başlangıçtaki 9 uyarı korunur.
- Çalıştırıcı ve Bitmap ön işleme yardımcısı eklendi. Model Git'e veya APK'ya gömülmedi. Galeri URI/EXIF/RGBA/HDR adaptörü, metin dalı, kalıcı indeks, arama ve gerçek cihaz performansı tamamlanmış sayılmaz.
- Sıradaki atomik görev A06: tek sorgunun uyumlu 512 boyutlu metin vektörünü tokenizer/pooling/Dense dahil doğrulamak. Commit veya push oluşturulmadı.

## A06 tek sorgu doğrulama sonucu

- Kullanıcı üç alternatif arasından saf Kotlin WordPiece'i seçti. Türkçe casing ve aksanlar korunur; çeviri veya ek Unicode normalizasyonu eklenmez. Önceki 14 örnek ve 10 sınır örneği iki padding profiliyle tam token/mask eşleşmesini geçti.
- İlk U8S8/ARM64 adayı mevcut x86 ortamında kabul eşiğini aşan fark üretti. Tamsayı katmanı teşhisi ve bütün başarısız karşılaştırmalar kaydedildi; ARM cihazı hakkında sonuç çıkarılmadı. Kullanıcı üç varyant seçeneğinden tek yayımlanmış U8U8 INT8 dosyasını seçti; model ailesi değişmedi.
- Seçilen metin gövdesi, maskeli ortalama ve 768 -> 512 Dense zinciri çalıştı. 48 Android/masaüstü vektörü birebir aynı; FP32 referansa en düşük cosine 0.993826400315, en yüksek göreli L2 0.11148372; sabit 0.99/0.15 bütçeleri geçti. Normalize norm hatası en çok 3.44253572e-08.
- Debug ve test APK üretildi; 5 çekirdek testi ile 2 A06 Android testi geçti; lint 0 hata, 12 uyarı. Yeni native bağımlılık eklenmedi. Kod, [runtime manifesti](MODEL_RUNTIME_MANIFEST.json), kaynak özetleri, bağımsız fixture'lar ve [doğrulama kaydı](validation/A06_TEXT_ENCODER.md) hazır.
- Model ve Dense ağırlıkları Git'e/APK'ya eklenmedi. Fiziksel ARM, retrieval başarımı, URI/EXIF/decoder ve galeri/Room/Worker bağlantısı tamamlanmış sayılmaz. Sıradaki atomik görev A07: seçilen doğrudan Türkçe sorgu akışını QueryEncoder adaptörüne bağlamak; arama ekranını etkinleştirme A13'te kalır. Commit veya push yapılmadı.
