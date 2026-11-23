# A04.2 - Çok dilli CLIP model sözleşmesi

Sözleşme sürümü: `multilingual-clip-v1`. Durum: kaynaklar ve arayüz kuralları sabitlendi. Bu belge **A05/A06 için etkin sözleşmedir**; A05 görsel dalı masaüstünde ve API 35 emülatöründe doğrulandı. A06 metin zinciri de API 35 x86_64 üzerinde doğrulandı. Galeri entegrasyonu bekliyor; [A06 kontrol kaydı](validation/A06_TEXT_ENCODER.md) ile [A05 kontrol kaydı](validation/A05_VISUAL_ENCODER.md) kapsamı ve sınırları açıklar.

Makine tarafından okunabilir kurallar ve kaynak özetleri [MODEL_CONTRACT.json](MODEL_CONTRACT.json), gerçek tokenizer örnekleri [TOKENIZER_REFERENCE_CASES.json](TOKENIZER_REFERENCE_CASES.json) dosyasındadır. Önceki adayın incelemesi [MobileCLIP arşivinde](MOBILECLIP_MODEL_CONTRACT.md) korunur.

## 1. Seçilen uyumlu model çifti

| Rol | Kaynak | Sabit revision |
|---|---|---|
| Çok dilli metin | `sentence-transformers/clip-ViT-B-32-multilingual-v1` | `58edf8cada9e398793dca955574a48cbb7f18be2` |
| Görsel | `sentence-transformers/clip-ViT-B-32` içindeki CLIP ViT-B/32 görsel dalı | `327ab6726d33c0e22f920c83f2ff9e4bd38ca37f` |

Kullanıcı bu çifti seçti; ayrı çeviri katmanı kullanılmayacak. Çok dilli metin kodlayıcısı yukarıdaki görsel uzaya hizalıdır. Görsel ve metin çıktıları 512 boyutludur; boyut eşitliği başka modellerle uyumluluk anlamına gelmez.

Kaynak görsel paketinin orijinal İngilizce metin dalı kullanılmayacak. MobileCLIP görsel kodlayıcısı, CLIP BPE, 77 token ve 256 piksel kuralları bu sözleşmenin parçaları değildir.

## 2. Varlıklar ve doğrulama durumu

Kaynak revision'ları ve dosya adları birlikte sabittir. Her dosyanın yayımlanmış veya yerelde hesaplanmış SHA-256 değeri JSON sözleşmesinde tutulur.

| Varlık | Rol | Bu görevdeki durum |
|---|---|---|
| Görsel depo: `0_CLIPModel/model.safetensors` | Görsel modelin kaynak ağırlık paketi | İndirildi; 605157884 bayt ve SHA-256 doğrulandı |
| Görsel depo: `0_CLIPModel/config.json` | Model ve 768 -> 512 görsel projeksiyon yapılandırması | Yerel dosya özeti doğrulandı |
| Görsel depo: `0_CLIPModel/preprocessor_config.json` | 224 piksel ve CLIP normalizasyon ayarları | Yerel dosya özeti doğrulandı |
| Metin depo: `model.safetensors` | FP32 DistilBERT referans gövdesi | İndirildi; sabit SHA-256 ile doğrulandı ve çalıştırıldı |
| Metin depo: `onnx/model_quint8_avx2.onnx` | Seçilen tek U8U8 INT8 metin gövdesi | İndirildi; SHA-256, gerçek I/O ve masaüstü/Android sayısal uyum doğrulandı |
| Metin depo: `onnx/model_qint8_arm64.onnx` | Önceki U8S8 adayının teşhis kaydı | İndirildi; mevcut x86 ortamında toleransı geçmedi; etkin çalışma dosyası değil, ARM sonucu değildir |
| Metin depo: `2_Dense/model.safetensors` | Zorunlu metin projeksiyon ağırlığı | Kaynak ve Float32 çalışma dosyası ayrı SHA-256 ile doğrulandı |
| Metin depo: `tokenizer.json`, `vocab.txt`, özel token/config dosyaları | Gerçek cased WordPiece tanımı | İndirildi; sözlük ve özetler karşılaştırıldı |
| Metin depo: `modules.json`, pooling/Dense config | Metin zincirinin modül sırası | Yerel dosya özetleri doğrulandı |

605 MB civarındaki kaynak CLIP paketi görsel ve orijinal İngilizce metin dallarını birlikte içerir; yalnız görsel ONNX boyutu gibi sunulmaz. Yayımlanan INT8 dosyası yalnız metin Transformer gövdesidir; son cümle vektörü için pooling ve Dense gerekir.

A05'te `clip_vit_b32_visual_fp32.onnx` üretildi: 351610456 bayt, SHA-256 `4bd22c1cab0e6b2a725a3d9e729c3a3c0338f5a2ecf34349cbed4c6aeaeb17bd`. Kaynak checkpoint ve ONNX özetleri ayrı ayrı doğrulandı. Model çalışma dizinindedir; Git veya APK içine eklenmedi.

## 3. Referans uygulama ve sürümler

Referans kod: Sentence Transformers `5.1.2` ve Transformers `4.57.1` kaynakları. Çekirdek Python oracle sürümleri: PyTorch `2.9.1`, Tokenizers `0.22.1`, Pillow `12.0.0`, NumPy `2.4.0`, ONNX `1.19.1`, ONNX Runtime `1.23.2`.

Bu sürümlerle A05 görsel referansı çalıştırıldı. Sabit CLIP yapılandırması ve görsel/projeksiyon ağırlıkları Transformers CLIPVisionModelWithProjection ile doğrudan yüklendi; processor, PyTorch ve ONNX Runtime CPU çıktıları karşılaştırıldı. [Çalıştırma araçları](../tools/README.md) ve doğrulama JSON'u gerçek ortamı kaydeder. Doğrudan paket sürümleri sabittir; bütün transitif bağımlılıkların platformlar arası kilidi oluşturulmadı. A06 FP32 DistilBertModel referansı strict ağırlık yüklemeyle çalıştırıldı; maskeli ortalama ve Dense dahil U8U8 çıktısıyla karşılaştırıldı.

Oracle CPU ve FP32 ile `eval()`/çıkarım modunda çalıştırıldı. Rastgele başlatılmış model veya dropout açık çıktı karşılaştırma referansı sayılmaz. Hızlandırıcı ve nicemleme farkları aynı testmiş gibi karıştırılmaz.

## 4. Görselin kanonik hazırlanması

Model processor'ından önce uygulama girdisi, yönü düzeltilmiş opak 8-bit RGB görüntü olarak tanımlanır:

1. EXIF yön/ayna dönüşümü bir kez uygulanır. Oracle örneği `ImageOps.exif_transpose`; Android decoder zaten uyguladıysa ikinci kez uygulanmaz.
2. Alfa içeren görsel beyaz arka plan üzerine birleştirilip RGB yapılır. Bu, GaleriAra'nın decoder sınırıdır; kaynak modelin kendiliğinden alfa/EXIF işlediği iddiası değildir.
3. İlk referanslar 8-bit sRGB PNG/JPEG ve alfa PNG üzerinde hazırlanır. HDR, farklı renk uzayları veya diğer decoder yolları doğrulanmadan aynı doğruluk kapsamına dahil edilmez; okunamayan görsel kontrollü hata üretmelidir.

Bu sınırdan sonraki model processor akışı:

1. Kısa kenar **224** olacak şekilde en-boy oranı korunur; küçük görseller de büyütülür.
2. Uzun kenar `int(224 * uzun / kısa)` ile belirlenir; yukarı yuvarlanmaz. Örneğin 320 x 240 görüntü 298 x 224 olur.
3. **Pillow BICUBIC**, kaynak ayardaki `resample = 3`, kullanılır. Bilinear veya rastgele Android resize varsayılanı eşdeğer kabul edilmez.
4. Merkezden 224 x 224 kırpılır: `left = (width - 224) // 2`, `top = (height - 224) // 2`. Tek piksel fazlası varsa konum tam sayı bölümüyle belirlenir.
5. RGB baytları 255'e bölünür; Float32 dönüşümünden sonra aşağıdaki kanal normalizasyonu uygulanır:

| Kanal | Mean | Std |
|---|---|---|
| R | 0.48145466 | 0.26862954 |
| G | 0.45782750 | 0.26130258 |
| B | 0.40821073 | 0.27577711 |

Formül: `normalized[c] = (pixel[c] / 255 - mean[c]) / std[c]`. Kaynak helper ölçeklemeyi Float64 üzerinden yapıp Float32'ye indirger; son normalizasyon Float32'dir. Tamamen siyah piksellerin model girdisi sıfır vektörü değildir.

Tensör RGB/CHW düzeninde Float32 `[1,3,224,224]`; düz indeks `c*224*224 + y*224 + x`. Önizleme küçük görseli, ekran görüntüsü veya UI ölçeklemesi model girdisi yerine kullanılmaz. Model processor'ı görüntüyü zaten ölçeklemişse 255'e ikinci kez bölünmez.

## 5. Görsel modelin arayüzü

A05'te üretilen görsel grafiğin doğrulanan arayüzü:

| Özellik | Kural |
|---|---|
| Hedef dosya | `clip_vit_b32_visual_fp32.onnx` |
| Girdi | `pixel_values`, Float32 `[N,3,224,224]` |
| Çıktı | `image_features`, Float32 `[N,512]` |
| İlk uygulama | `N = 1`; yalnız batch ekseni dinamik |
| Dışa aktarma hedefi | Opset 17, FP32, eager attention; gerçek dosya metadata'sı ayrıca kontrol edilir |

Çıktı, CLIP `vision_model` havuzlanmış 768 boyutlu temsilinin **`visual_projection`** katmanından geçirilmiş ham 512 boyutlu hâlidir. Son projeksiyon atlanmayacak; orijinal İngilizce text model çağrısı eklenmeyecek. Görsel grafikte L2 normalizasyonu zorunlu değildir; uygulama sınırında ortak normalizasyon uygulanır.

Bu arayüz ONNX checker ve gerçek ONNX Runtime oturum metadata'sıyla doğrulandı. Masaüstü ve Android aynı dosya özetini kullanarak sabit görsel karşılaştırmasını geçti.

## 6. Türkçe tokenizer sözleşmesi

Kaynak: sabit metin deposunun `tokenizer.json` dosyası ve Sentence Transformers `Transformer.tokenize()` akışı.

- Metin kaynaktaki `str.strip()` davranışıyla uç boşluklarından arındırılır. Modelin casing'i korunur; Türkçe veya İngilizce küçük harfe çevirme, çeviri, HTML çözümü, ftfy veya ek NFC/NFKC dönüşümü eklenmez.
- BertNormalizer: `clean_text = true`, Chinese karakter ayırma açık, `lowercase = false`, `strip_accents = null`. Bu cased profilde aksanlar korunur. Kontrol karakteri/boşluk işlemleri tokenizer'a aittir.
- BertPreTokenizer ve WordPiece; devam parçası `##`, kelime başına üst sınır 100 karakter. Kaynak sözlükte **119.547** öğe vardır.
- `[PAD]=0`, `[UNK]=100`, `[CLS]=101`, `[SEP]=102`, `[MASK]=103`.
- Tek sorgu biçimi `[CLS] + içerik + [SEP]`. Üst sınır özel token'lar dahil **128**; en çok 126 içerik token'ı. Kesme sağdan, kaynak ayarı `longest_first`; son `[SEP]` korunur.
- Tek sorgu için esas profil dinamik uzunluktur: `S = 2..128`. Sabit 128 profili kullanılırsa sağa 0 dolgusu yapılır; dolgu attention mask'i 0 olmalıdır. Her iki profil referans örneklerinde doğrulanmıştır.
- `input_ids` ve `attention_mask` Int64 `[1,S]`. Gerçek token'lar ile `[CLS]/[SEP]` maskesi 1; dolgular 0. DistilBERT metin çağrısına `token_type_ids` gerekmez.

Doğrulanmış bir örnek: `kırmızı bir araba` -> `[101,179,52401,10561,45131,10113,102]`. Boş metin tokenizer düzeyinde `[101,102]` üretir; mevcut `SearchPhotos` boş sorguyu model çağrısından önce reddeder.

`I` + birleşen nokta gibi Unicode dizilerinin, görsel olarak benzer başka dizilerle aynı token'ı vereceği varsayılmayacak. Ek normalizasyon eklemek bu sözleşmeyi değiştirir; fixture'larla tam eşleşme aranır.

## 7. Metin gövdesi, pooling ve Dense

Kaynak zincir `Transformer -> Pooling -> Dense` biçimindedir. Normalizasyon sonradan uygulanan ortak arama işlemidir.

| Bileşen | Beklenen tür/şekil ve işlem |
|---|---|
| Metin gövdesi | Int64 `input_ids`, `attention_mask` `[N,S]` -> Float32 token temsilleri `[N,S,768]` |
| Pooling | Maskeli ortalama -> Float32 `[N,768]` |
| Dense | Float32 `[512,768]` ağırlık; bias yok; Identity -> ham `[N,512]` |
| Normalizasyon | Sonlu ve sıfır olmayan 512 vektöre L2 |

Pooling formülü: `p[j] = sum_t(h[t,j] * mask[t]) / max(sum_t(mask[t]), 1e-9)`. **Yalnız padding dışlanır; maskesi 1 olan `[CLS]` ve `[SEP]` ortalamaya dahildir.** Yalnız `[CLS]` almak veya özel token'ları keyfi çıkarmak referansı değiştirir.

Dense formülü: `y[d] = sum_j(W[d,j] * p[j])`; `W` saklama şekli `[512,768]`. Boyutları ters okumak veya ek tanh/bias uygulamak hatadır. Bu ağırlık aynı sabit metin deposundaki `2_Dense` varlığından alınacak.

Seçilen U8U8 dosyasında gerçek I/O doğrulandı: Int64 `input_ids`, `attention_mask` `[batch_size,sequence_length]`; Float32 `last_hidden_state` `[batch_size,sequence_length,768]`. Kotlin oturumu adları, türleri ve rank/boyutları denetler. İlk output'u denetimsiz seçmek, token temsillerini son cümle vektörü saymak veya 768 boyutu 512'ye kesmek kabul edilmez.

INT8 gövde Float32 token temsilleri vermelidir; çıktı türü dosyadan doğrulanacak. Nicemleme sadece aynı metin modelinin uygulama varyantıdır; farklı model veya farklı Dense ile birleştirme izni değildir.

## 8. Ortak vektör ve model kimliği

Ham görsel ve metin vektörü 512 sonlu Float32 değerden oluşur. Sıfır norm geçersizdir. Arama için `v / sqrt(sum(v_i^2))` kullanılır. Mevcut `core/Embedding` bu doğrulama ve normalizasyonu zaten yapar; bunun için yeni bir mimari katman eklenmez.

Arama skoru normalize vektörlerin nokta çarpımıdır. CLIP `logit_scale`, softmax ve sınıf olasılığı üretimi top-k fotoğraf sıralamasına eklenmez.

`ModelIdentity.id` önerilen sabit aile adı: `clip-vit-b32-multilingual-v1`; `dimensions = 512`. Nihai `revision`, canonical JSON bileşen manifestinin SHA-256 değeri olacak. Canonical JSON: UTF-8, anahtarlar sıralı, gereksiz boşluk yok; manifest gerçek görsel ONNX, metin ONNX, Dense ve tokenizer özetlerini, ön işleme/pooling/normalizasyon profilini içerir. Hesaplanan revision kendi hash girdisine eklenmez.

Görsel, seçilen metin ONNX, Dense, tokenizer ve işlem profilleri [runtime manifestinde](MODEL_RUNTIME_MANIFEST.json) sabitlendi. `revision`, bu JSON'un revision alanı çıkarılıp UTF-8/sıralı anahtar/kompakt biçimde yazılan içeriğinin SHA-256 değeridir. Bu belgenin sürümü veya kaynak commit'i revision yerine kullanılmaz; manifest bileşenleri değişince özet yeniden hesaplanır. A05 prototip çıktısı, model çiftinin bütün varlıkları doğrulanmadan kalıcı galeri indeksi olarak sunulmaz. Model veya profil değiştiğinde eski ve yeni vektörler karıştırılmaz.

## 9. Sayısal kabul kuralları

Aşağıdaki eşikler model ölçümünden önce sabitlendi. A05 görsel ve A06 metin kontrolleri kendi kayıtlı kapsamlarında geçti; bu sonuç fiziksel ARM cihazı, bütün decoder profilleri veya retrieval kalitesinin doğrulandığı anlamına gelmez. Tüm vektör karşılaştırmalarında önce boyut, sonlu değerler ve pozitif norm doğrulanır.

| Kontrol | Kabul kuralı |
|---|---|
| Tokenizer, kesme ve maskeler | Bütün fixture kimlikleri ve maskeleri birebir eşit |
| Aynı 224 RGB baytlarının tensöre dönüşümü | Float32 girdide en büyük mutlak hata <= `1e-6` |
| Kanonik RGB'den resize/crop | Boyut ve kırpma koordinatı tam aynı; referans RGB'ye göre kanal başına en çok 2/255 seviye farkı. Normalize tensörde her kanal sınırı `2/(255*std[c])` |
| FP32 PyTorch -> FP32 görsel ONNX, aynı tensör | Cosine >= `0.99999`, ham vektörde göreli L2 <= `0.001` |
| Aynı ONNX + aynı tensör, desktop CPU -> Android CPU | Cosine >= `0.99999`, ham vektörde göreli L2 <= `0.001` |
| Decoder dahil sabit dosyanın tam görsel akışı | Aynı yön, kırpma ve RGB politikası; son vektör cosine >= `0.999`, göreli L2 <= `0.02` |
| INT8 metin zinciri -> FP32 referans, aynı token/maskeler | Son Dense çıktısında cosine >= `0.99`, ham vektörde göreli L2 <= `0.15` |
| Son normalize vektör | `abs(norm - 1) <= 1e-5` |

Göreli L2: `norm(actual - reference) / norm(reference)`; cosine ham vektörlerin normalize nokta çarpımıdır. INT8 kontrolü token gövdesine bakmakla bitmez; pooling ve Dense dahil son vektör ölçülür. Sabit/dinamik padding eşdeğerliği de aynı modelin referansıyla karşılaştırılır.

Eşik aşılırsa hangi aşamanın fark ürettiği araştırılır; sessizce daha gevşek eşik, başka tokenizer veya başka modelle geçilmiş sayılmaz. Arama kalitesi ölçümleri [değerlendirme protokolünde](EVALUATION_PLAN.md) ayrıca tanımlıdır; sayısal yakınlık tek başına Türkçe retrieval başarısı değildir.

## 10. Referans örnekleri ve bu görevde doğrulananlar

14 gerçek tokenizer örneği: boş/boşluk metni, Türkçe sorgu/sahne, büyük-küçük harf, Türkçe harfler, birleşen Unicode, noktalama, tab/yeni satır, 100 karakteri aşan kelime, 128 token'a kesilen uzun sorgu, literal özel token ve kontrol karakteri.

`tokenizer.json` ve `vocab.txt` kimlik eşlemesi bütünüyle karşılaştırıldı. Tokenizers `0.22.1` ile gerçek token dizileri üretildi; hem dinamik hem sabit 128 padding/maskeleri kontrol edildi. A06 saf Kotlin tokenizer bu 14 örneğe ek 10 sınır örneğinde, dinamik ve sabit 128 profillerinde birebir karşılaştırıldı: toplam 48 token/mask kontrolü geçti. Android Unicode kategorileri kullanıldığından API 35 dışındaki sürümler ve bütün Unicode aralığı bu testle doğrulanmış sayılmaz.

Siyah, beyaz, kırmızı ve gri RGB örneklerinde mean/std aritmetiği ayrıca kontrol edildi. Bu aritmetik kontrolü bütün Transformers image processor'ının veya modelin çalıştırılması değildir.

## 11. Kalan işler ve kapsam

- **A05 tamamlandı:** Kaynak ağırlık ve görsel ONNX özeti/I/O doğrulandı; tek opak RGB PNG için Python–masaüstü ONNX–Android karşılaştırması geçti. URI/EXIF/RGBA/HDR ve fiziksel cihaz doğrulaması tamamlanmış sayılmaz.
- **A06 tamamlandı:** Saf Kotlin tokenizer ve tek U8U8 dosyası kullanıcı tarafından seçildi. 24 örneğin iki profili için token/maskeler tam eşleşti; pooling, Dense ve normalizasyon dahil 48 Android vektör karşılaştırması geçti.
- **A07 ve sonrası:** Doğrudan Türkçe sorgu akışını QueryEncoder arayüzüne bağla; kalıcı indeksleme ve aramayı ilgili sonraki görevlerde etkinleştir.

A04.2 kaynak sözleşmesi aşamasında uygulama kodu veya model çıkarımı yoktu. A05 ile görsel ön işleme ve ONNX çalıştırıcısı ile bağımsız referans testi eklendi; ONNX Runtime Android bağımlılığı tanımlandı. A06 ile saf Kotlin tokenizer ve ham metin çalıştırıcısı eklendi. Galeri, Room ve arama bağlantısı sonraki görevlerde kalır. Ham görsel ve metin çıktıları 512 boyutludur; uygulama sınırındaki ortak L2 kuralı korunur.

## Kaynaklar ve lisans kaydı

1. [Sabit çok dilli metin deposu](https://huggingface.co/sentence-transformers/clip-ViT-B-32-multilingual-v1/tree/58edf8cada9e398793dca955574a48cbb7f18be2): tokenizer, modules, pooling/Dense ve model varlıkları.
2. [Sabit görsel depo](https://huggingface.co/sentence-transformers/clip-ViT-B-32/tree/327ab6726d33c0e22f920c83f2ff9e4bd38ca37f): CLIP ve processor yapılandırmaları.
3. [Sentence Transformers CLIPModel](https://github.com/huggingface/sentence-transformers/blob/v5.1.2/sentence_transformers/models/CLIPModel.py), [Transformer](https://github.com/huggingface/sentence-transformers/blob/v5.1.2/sentence_transformers/models/Transformer.py), [Pooling](https://github.com/huggingface/sentence-transformers/blob/v5.1.2/sentence_transformers/models/Pooling.py), [Dense](https://github.com/huggingface/sentence-transformers/blob/v5.1.2/sentence_transformers/models/Dense.py).
4. [Transformers CLIP image processor](https://github.com/huggingface/transformers/blob/v4.57.1/src/transformers/models/clip/image_processing_clip.py), [image transforms](https://github.com/huggingface/transformers/blob/v4.57.1/src/transformers/image_transforms.py), [CLIP model](https://github.com/huggingface/transformers/blob/v4.57.1/src/transformers/models/clip/modeling_clip.py).

Metin model kartı Apache-2.0 bildirir. Eşleşen görsel kartında ayrı lisans etiketi bulunmadığından kaynak kökeni ve OpenAI CLIP MIT lisans kaydı birlikte izlenir; metin lisansı bütün zincirin lisansı sayılmaz. Kaynak bağlantıları ve CLIP/MIT ile metin/Apache-2.0 bildirimleri [araçların lisans dizininde](../tools/licenses) korunur. Dağıtım biçimi henüz uygulanmadı. Önceki Apple model lisansı yeni modelin lisansı olarak kullanılmaz.

## A06 varyant kararı ve ölçüm sonucu

Kullanıcı üçer alternatiften saf Kotlin WordPiece ve tek yayımlanmış U8U8 INT8 dosyasını seçti. Etkin metin dosyası `onnx/model_quint8_avx2.onnx`, 135377779 bayt; SHA-256 `fbc8fbeaa5237d96bd1bf430057c70d34de8334a463e933a5faa305f6caeed9c`. Dosya adındaki avx2, yayımlayıcının nicemleme profili adıdır; fiziksel ARM uyumu/performansı bu görevde ölçülmedi. İkinci bir işlemciye göre model seçme yolu eklenmedi.

Önceki `model_qint8_arm64.onnx` mevcut x86 ortamında 48 karşılaştırmanın hiçbirinde FP32 toleransını geçmedi. İlk tamsayı katmanındaki 5376 sonuçtan 900'ü tam Int32 hesapla farklıdır. Bu, ONNX Runtime'ın belgelediği x86 U8S8 doyma sorunuyla tutarlıdır; modelin ARM üzerinde hatalı olduğunu göstermez. U8U8 aynı bütçelerle yeniden sınandı; eşikler gevşetilmedi. [Teşhis kaydı](validation/A06_INT8_X86_DIAGNOSTIC.json) ve [tekrar üretim aracı](../tools/a06_quantization_diagnostic.py) korunur.

Runtime bileşen revision'ı `b01d72b7de9160a38730f6a8102bbbcbc799bb5a153caf9df6e21eec302a310d`. Bu kimlik hazır olsa da uygulamada QueryEncoder/ImageEncoder bağlantısı ve kalıcı indeks henüz yoktur. Test içindeki `a06-numerical-validation` kimliği yalnız L2 doğrulaması içindir; üretim model kimliği olarak kullanılmaz.
