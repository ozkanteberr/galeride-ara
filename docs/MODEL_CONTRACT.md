# A03 — Model girdi ve çıktı sözleşmesi

İnceleme ve kullanıcı kararı: 23 Kasım 2026. Belge sürümü: `a03-openclip-reference-v1`.

Durum: A03 kaynak incelemesi ve sözleşme belgelemesi tamamlandı. Bu belge çalışan model entegrasyonu veya cihazda ölçülmüş doğruluk sonucu değildir. Android adaptörleri henüz uygulanmadı.

## Amaç ve karar

Mevcut MobileCLIP2-S0 adayının görsel/metin çiftini değiştirmeden, girdilerin hazırlanmasında model dışa aktarıcısının Python/OpenCLIP referansı esas alınacaktır. PicQuery uygulaması yaklaşım ve entegrasyon referansıdır; onun Dart ön işlemesi doğruluk ölçütü olarak birebir kopyalanmayacaktır.

Kullanıcıya üç seçenek sunuldu:

1. OpenCLIP referans akışını esas almak — önerilen ve kullanıcı tarafından seçilen seçenek.
2. PicQuery uygulamasının akışını birebir izlemek.
3. İki akışı ayrı deneyle karşılaştırıp sonra seçmek.

Karar gerekçesi: kaynaklarda küçük görsellerin ölçeklenmesi ve tokenizer davranışı bakımından farklar bulundu. Modelin kendi referansıyla karşılaştırılabilir bir başlangıç seçildi. Android'de eşdeğerlik A05/A06 sırasında ölçülecek.

## Sabit referanslar

| Kaynak | Sabit sürüm | Rol |
|---|---|---|
| [PicQuery](https://github.com/greyovo/PicQuery/tree/ba83267b06d48b11fc991d9d37a5dd186fa1c477) | `ba83267b06d48b11fc991d9d37a5dd186fa1c477` | Uygulama ve ONNX çağrı referansı |
| [picquery-models](https://github.com/greyovo/picquery-models/tree/4efa0ea182641611aeff68d3a06ff4e8ddf6503a) | `4efa0ea182641611aeff68d3a06ff4e8ddf6503a` | Dışa aktarma kodu ve birlikte gelen OpenCLIP referansı |
| [Apple ml-mobileclip](https://github.com/apple/ml-mobileclip/tree/48faa0fea4b08d74188b3841771aca6ff2c92852) | `48faa0fea4b08d74188b3841771aca6ff2c92852` | Model yapılandırması ve lisans karşılaştırması |
| [Apple MobileCLIP2-S0](https://huggingface.co/apple/MobileCLIP2-S0/tree/3136ea51c8ed56b9f9abfab04cb816735aaad6cb) | `3136ea51c8ed56b9f9abfab04cb816735aaad6cb` | Orijinal checkpoint kaynağı |
| [timm OpenCLIP paketi](https://huggingface.co/timm/MobileCLIP2-S0-OpenCLIP/tree/095906d28bf54d7584dc411e8ffe448f34289e05) | `095906d28bf54d7584dc411e8ffe448f34289e05` | Dışa aktarıcının varsayılan ağırlık kaynağı ve ön işleme yapılandırmasının karşılaştırılması |

Kaynak URL'leri, indirilen kaynakların SHA-256 değerleri ve sunucunun yayımladığı ağırlık metadata'sı [MODEL_REFERENCES.json](MODEL_REFERENCES.json) dosyasında bulunur. Dal adı veya `main/master` indirme bağlantısı bir sürüm sabitlemesi sayılmaz.

## Model çifti ve dosyalar

[Dışa aktarıcı](https://github.com/greyovo/picquery-models/blob/4efa0ea182641611aeff68d3a06ff4e8ddf6503a/models/ml-mobileclip/export_mobileclip_models.py), `MobileCLIP2-S0` / `dfndr2b` modelinin aynı yüklenmiş örneğinden `model.visual` ve `model.text` parçalarını çıkarır. Çıkarımdan önce model `eval()` durumuna alınır ve yeniden parametreleştirilir. Birbiriyle bağımsız modeller birleştirilmeyecektir.

| Varlık | Kaynak ve durum |
|---|---|
| `mobileclip2_s0.pt` | Apple'ın sabit model deposunda 299.864.852 bayt; sunucu LFS SHA-256: `bc3bc861baa680df2f9e3dc1aa0acbe5a21af21032df385026dda70c33505aa6` |
| `mobileclip2_s0_visual.onnx` | Sabit dışa aktarıcının beklenen görsel çıktısı; çoğunlukla FP16 parametreler, FP32 giriş/çıkış |
| `mobileclip2_s0_text.onnx` | Aynı dışa aktarmanın beklenen metin çıktısı; MatMul/Gather için kanal bazlı dinamik INT8 ağırlık nicemleme, FP32 çıkış |
| `bpe_simple_vocab_16e6.txt.gz` | Sabit OpenCLIP kaynağı; yerel SHA-256: `924691ac288e54409236115652ad4aa250f48203de50a9e4722a6ecd48d6804a` |

Checkpoint indirilmedi; belirtilen checkpoint özeti sunucunun metadata'sından okundu ve yerel ağırlık doğrulaması yapılmadı. ONNX dosyaları üretilmedi veya indirilmedi; bu nedenle onların SHA-256 değerleri henüz yoktur. Dışa aktarıcı commit'i, üretilen ONNX dosyasının özeti yerine geçmez.

Dışa aktarıcı yerel checkpoint varsa onu, yoksa `timm/MobileCLIP2-S0-OpenCLIP` kaynağını kullanabiliyor. A05'te kullanılan gerçek ağırlık dosyası açıkça belirtilecek; hareketli etiketle sessiz indirme kullanılmayacak. Apple ve timm dosyalarının sayısal eşdeğerliği bu görevde sınanmadı; alternatif metadata'nın kaydedilmesi eşdeğerlik iddiası değildir.

## Beklenen ONNX arayüzü

Bu tablo indirilen ONNX grafiğinin incelemesi değil, sabit dışa aktarma kodu ve yapılandırmadan çıkarılan beklentidir. A05/A06 gerçek oturum metadata'sını bu tabloyla karşılaştırmalıdır.

| Özellik | Görsel kodlayıcı | Metin kodlayıcı |
|---|---|---|
| Dosya | `mobileclip2_s0_visual.onnx` | `mobileclip2_s0_text.onnx` |
| Girdi adı | `image` | `text` |
| Girdi tipi | Float32 | Int64 |
| Girdi şekli | `[batch, 3, 256, 256]` | `[batch, 77]` |
| Çıktı adı | `image_features` | `text_features` |
| Çıktı tipi/şekli | Float32 `[batch, 512]` | Float32 `[batch, 512]` |
| Uygulamanın ilk hedefi | `batch = 1` | `batch = 1` |

Dışa aktarma ONNX opset 18 kullanır ve yalnız batch eksenini dinamik tanımlar. Görsel ve token uzunluğu sabittir. CLIP metin grafiği için ayrıca bir `attention_mask` girdisi bu dışa aktarıcı tarafından tanımlanmamıştır. Parametrelerin FP16/INT8 olması uygulama girdilerinin de bu türlerde olacağı anlamına gelmez.

## Görsel hazırlama sözleşmesi

Referans: sabit dışa aktarıcının `load_reference()` fonksiyonu, birlikte gelen [pretrained.py](https://github.com/greyovo/picquery-models/blob/4efa0ea182641611aeff68d3a06ff4e8ddf6503a/models/ml-mobileclip/third_party/open_clip/src/open_clip/pretrained.py) ve [transform.py](https://github.com/greyovo/picquery-models/blob/4efa0ea182641611aeff68d3a06ff4e8ddf6503a/models/ml-mobileclip/third_party/open_clip/src/open_clip/transform.py).

1. Referans görüntü RGB olarak hazırlanır. Kaynak örnekleri PIL `convert("RGB")` kullanır.
2. En-boy oranı korunarak kısa kenar 256 piksele getirilir. `resize_mode = shortest`, interpolasyon `bilinear` kullanılır. Küçük görseller de büyütülür.
3. Merkezden 256 × 256 kırpılır. Boyut yuvarlama, kırpma koordinatı ve yeniden örnekleme davranışı Python referansına göre karşılaştırılmalıdır; yalnız aynı yöntem adını kullanmak piksel eşitliği kanıtı değildir.
4. RGB kanalları CHW düzeninde Float32 tensöre çevrilir. Her kanal değeri 255'e bölünür; girdi aralığı `[0,1]` olur.
5. S0 için mean `(0,0,0)`, std `(1,1,1)` olduğundan ayrıca ImageNet veya standart CLIP mean/std dönüşümü uygulanmaz.
6. Tek görsel için şekil `[1,3,256,256]` olur. Düz dizide R düzlemi, ardından G ve B düzlemleri bulunur; indeks `c*256*256 + y*256 + x` biçimindedir.

EXIF ve saydamlık sınırı: incelenen Python örneği EXIF dönüşünü açıkça uygulamıyor; PicQuery de yönü codec'e bırakıyor. Bu yüzden platformların aynı görüntü yönünü ürettiği varsayılmayacak. A05'in ilk sayısal örneği EXIF yönü olmayan opak RGB görsel olacak. EXIF'li JPEG, saydam görsel ve decoder/renk dönüşümü davranışları Android adaptörü kabul edilmeden ayrıca doğrulanacak. Bu belge tüm görüntü biçimleri için decoder eşdeğerliğini kanıtlamaz.

## Metin ve tokenizer sözleşmesi

Referans: aynı dışa aktarma deposundaki [OpenCLIP tokenizer.py](https://github.com/greyovo/picquery-models/blob/4efa0ea182641611aeff68d3a06ff4e8ddf6503a/models/ml-mobileclip/third_party/open_clip/src/open_clip/tokenizer.py). Model yapılandırması bağlam uzunluğunu 77, sözlük boyutunu 49.408 olarak tanımlar.

1. Varsayılan `clean = lower`: `ftfy.fix_text`, iki kez HTML entity çözümü, uç boşlukların temizlenmesi, ardışık boşlukların tek boşluğa dönüştürülmesi ve küçük harfe çevirme sırası izlenir. Kotlin'de Türkçe cihaz yereline bağlı küçük harfe çevirme uygulanması referansla eşdeğer kabul edilmeyecek.
2. Özel token'ları, İngilizce kısaltmaları, Unicode harfleri, tek tek rakamları ve noktalama gruplarını tanıyan kaynak regex kullanılır.
3. UTF-8 baytları CLIP byte-to-Unicode eşlemesine çevrilir. Kelimenin son parçasına `</w>` eklenir ve sıralı BPE birleştirmeleri uygulanır.
4. Kaynak gzip dosyasının başlık sonrası **ilk 48.894 birleştirmesi** kullanılır: Python kaynak dilimi `merges[1:49152-256-2+1]`. Dosyadaki tüm birleştirmeleri okumak eşdeğer değildir.
5. Başlangıç token'ı 49.406, bitiş token'ı 49.407, dolgu 0'dır. En fazla 75 içerik token'ı kullanılır. Uzun girdide bitiş token'ı son konumda korunur; çıktı tam 77 elemanlıdır.
6. Model girdisi Int64 `[1,77]` olur. Boş metnin tokenizer çıktısı başlangıç + bitiş + dolgu olabilir; uygulamanın mevcut `SearchPhotos` sınıfı boş arama sorgusunu ayrıca reddeder.

A06'da Kotlin token kimlikleri Python referansıyla **tam eşit** olmalıdır. Örnekler: `a photo of a cat` için başlangıç kısmı `[49406,320,1125,539,320,2368,49407]`; `fjord` içerik token'ları `[31260,323]`; `gorillas` içerik token'ları `[1747,23404]`. Son iki örnek kaynak algoritmasından türetilmiştir; Android veya tam OpenCLIP runtime çalıştırması sonucu değildir. HTML, Unicode, özel token ve uzun metin örnekleri de karşılaştırmaya dahil edilmelidir.

## PicQuery'den ayrılan doğrulanmış noktalar

| Konu | PicQuery uygulaması | Seçilen OpenCLIP referansı |
|---|---|---|
| Küçük görsel | Ölçek en fazla 1; küçük görsel büyütülmez, siyah dolgu yapılır | Kısa kenar 256'ya büyütülür, merkez kırpılır |
| Yeniden örnekleme | Flutter codec hedef boyutlarla çağrılır | Kaynak transform içinde bilinear tanımlıdır |
| Metin temizleme | Dart tokenizer doğrudan `toLowerCase()` kullanır | ftfy, HTML ve boşluk temizleme de bulunur |
| BPE kuralları | 262.144 satırın tamamı yüklenir; sözlükte bulunmayan parça atlanır | İlk 48.894 birleştirme ve onlardan kurulan sözlük kullanılır |
| Özel token yazımı | Sözlükte `<\|startoftext\|>` / `<\|endoftext\|>` | Kaynakta `<start_of_text>` / `<end_of_text>`; sayısal kimlikler aynıdır |

Kaynaklar: [PicQuery görsel ön işleme](https://github.com/greyovo/PicQuery/blob/ba83267b06d48b11fc991d9d37a5dd186fa1c477/lib/src/engine/image_preprocess.dart) ve [Dart tokenizer](https://github.com/greyovo/PicQuery/blob/ba83267b06d48b11fc991d9d37a5dd186fa1c477/lib/src/engine/clip_tokenizer.dart). Kaynaklardan türetilen kontrolde `fjord</w>` ve `gorillas</w>` fazla kurallarla tek parçaya birleşip sözlükte bulunmadığı için atlanır. Bu fark, PicQuery tokenizer'ını değişikliksiz taşımanın referans uyumunu sağlamadığını gösterir; genel arama başarımı bu görevde ölçülmemiştir.

## Vektör ve mevcut çekirdekle ilişki

Her iki ONNX çıktısı 512 boyutlu ham vektördür. Aramada kullanılmadan önce L2 normalizasyonu gerekir. Mevcut `core/Embedding` zaten boyut, sonlu değer ve sıfır norm kontrolü yapıp normalize eder; yalnız model bağlamak için yeni normalizasyon katmanı gerektiren bir karar alınmadı.

Görsel ve sorgu `Embedding` nesnelerinin `ModelIdentity(id, revision, dimensions)` değerleri aynı olmalıdır; `dimensions = 512` bu sözleşmede doğrulanmıştır. `id` ortak model ailesini/çiftini, `revision` ise gerçek görsel/metin dosyaları, tokenizer ve ön işlemenin birlikte uyumlu sürümünü temsil eder. Sadece model adının aynı olması yeterli değildir.

`a03-openclip-reference-v1` bu belgenin sürümüdür; henüz üretilememiş ONNX dosyalarının nihai çalışma zamanı kimliği değildir. A05/A06'da gerçek dosya özetleri kaydedildikten sonra somut kimlik değeri kesinleştirilecek. Bu görev mevcut `Search.kt` veya Room şemasını değiştirmez.

## Lisans ve dosya kökeni

PicQuery uygulama kodu MIT lisanslıdır. Model ağırlıkları aynı lisansa tabi varsayılmayacaktır: Apple model kartı `apple-amlr` bildirir ve sabit [LICENSE_MODELS](https://github.com/apple/ml-mobileclip/blob/48faa0fea4b08d74188b3841771aca6ff2c92852/LICENSE_MODELS) ayrı koşullar içerir. Apple deposu, Apple checkpoint deposu ve dışa aktarma deposundaki model lisansı dosyalarının SHA-256 değerleri aynıdır: `30c59d255d2b4075ba11b81ce5aa0109c28d28138d66d52f7f5d8cbba814df8b`.

Birlikte gelen [OpenCLIP kaynak lisansı](https://github.com/greyovo/picquery-models/blob/4efa0ea182641611aeff68d3a06ff4e8ddf6503a/models/ml-mobileclip/third_party/open_clip/LICENSE) MIT koşullarını içerir; dosya özeti manifestte kayıtlıdır. Bu görevde uygulamaya üçüncü taraf kaynak kodu veya model varlığı eklenmedi. Model paketleme/dağıtımı yapılırken ilgili lisans ve atıf dosyaları da birlikte ele alınmalıdır.

## Sonraki görevlerin doğrulama kapıları

- **A04:** Türkçe sorgu dönüşümü için kaynak/lisans/boyut/Android uygulanabilirliği araştırılır; bu belge çeviri modeli seçmez.
- **A05:** Kullanılan checkpoint'in indirilen baytları doğrulanır; üretilen görsel ve metin ONNX dosyalarının SHA-256 değerleri kaydedilir. Görsel modelinin gerçek I/O metadata'sı, ön işlem tensörü ve tek görsel çıktısı Python referansıyla karşılaştırılır. EXIF/decoder farkları ayrıca sınanır.
- **A06:** Kotlin token dizileri referansla tam karşılaştırılır; gerçek metin grafiğinin I/O metadata'sı ve sabit metin vektörü doğrulanır. A05 ile aynı model çifti kullanılır.
- Dışa aktarma deposunun regresyon eşikleri görsel cosine ≥ 0,9998 / göreli L2 ≤ 0,02 ve metin cosine ≥ 0,99 / göreli L2 ≤ 0,15'tir. Bunlar upstream test ölçütleridir; GaleriAra'da çalıştırılmış sonuç veya kendiliğinden kabul edilmiş Android eşiği değildir.
- PicQuery kaynaklarında Android CPU/BASIC optimizasyonuyla ilgili deney notları vardır. Bunlar Java/Android oturum ayarlarının bu görevde seçildiği veya doğrulandığı anlamına gelmez; A05'te sürüm ve ayarlar açıkça ele alınmalıdır.

## A03 doğrulama kaydı

- Sabit Git commit'leri ve model deposu revision'ları API üzerinden okundu.
- Kaynak dosyaları sabit URL'lerden indirildi; dosya SHA-256 değerleri yerelde hesaplandı.
- Apple/model dışa aktarma yapılandırmalarında 256 piksel, 77 token ve 512 boyut bilgileri karşılaştırıldı.
- BPE kaynak dilimi, sözlük ve fazla birleştirme farkı kaynaklardan türetilen örneklerle kontrol edildi.
- Girdi/çıktı adları ve veri tipleri dışa aktarma kodu ile PicQuery çağrıları üzerinden karşılaştırıldı.
- Model ağırlığı indirme, ONNX dışa aktarma, çıkarım, Android cihaz testi veya yeni derleme yapılmadı. A03 yalnız belge değişikliğidir.
