# A04 - Türkçe sorgu işleme seçenekleri

İnceleme: 23 Kasım 2026. Durum: **kullanıcı 1. seçeneği seçti; A04 tamamlandı**. Seçilen çift standart çok dilli CLIP metin kodlayıcısı ve eşleşen CLIP ViT-B/32 görsel kodlayıcısıdır. Çeviri katmanı eklenmeyecek. Model entegrasyonu henüz yapılmadı.

Kapsam: [ödev koşulları](ASSIGNMENT_REQUIREMENTS.md) doğrultusunda üç somut adayın kaynak, Türkçe desteği, uyumlu görsel kodlayıcı, lisans, boyut ve Android uygulama yolu açısından karşılaştırılması. Çalışan model çıkarımı ve cihaz performansı bu görevin sonucu olarak iddia edilmez.

## 1. Karar için kısa karşılaştırma

| Seçenek | Model akışı | Güçlü yanı | Temel bedeli / doğrulama ihtiyacı |
|---|---|---|---|
| **1 - Kullanıcının seçimi** | Türkçe -> standart çok dilli CLIP metin kodlayıcısı; fotoğraflar -> eşleşen CLIP ViT-B/32 görsel kodlayıcısı | Türkçe desteği açıkça belgeli; çeviri katmanı yok; yayımlanmış ARM64 INT8 metin ONNX var | MobileCLIP görsel kodlayıcısı değişir. Görsel ONNX çıkarımı ve metin pooling/projeksiyon eşdeğerliği doğrulanmalıdır |
| **2 - Daha küçük metin modeli adayı** | Türkçe -> sözlüğü Türkçeye göre küçültülmüş CLIP metin kodlayıcısı; aynı CLIP ViT-B/32 görsel kodlayıcısı | Metin tarafının parametre sayısı azalır; çeviri yok | Yayımlanan pakette ONNX yok; dışa aktarma/nicemleme ve standart modele göre kalite kaybı ölçümü gerekir |
| **3 - Mevcut görsel adayını koru** | Türkçe -> cihazda OPUS-MT Türkçe-Ingilizce çeviri -> MobileCLIP2-S0 metin kodlayıcısı; fotoğraflar -> MobileCLIP2-S0 görsel kodlayıcısı | A03'te incelenen görsel/metin çifti korunur; Türkçe-Ingilizce çeviri kaynağı bellidir | Ek çeviri modeli, SentencePiece, encoder/decoder üretim döngüsü ve anlam kaybı olasılığı eklenir |

Bu seçeneklerin Türkçe fotoğraf aramasındaki başarımı henüz aynı veri setinde ölçülmedi. Öneri bir entegrasyon başlangıcıdır; en yüksek doğruluğun veya en düşük mobil maliyetin kanıtı değildir.

## 2. Sabit sürümler

Kaynak dosya özetleri ve ağırlıkların indirilmeden alınmış sunucu metadata'sı [A04_MODEL_REFERENCES.json](A04_MODEL_REFERENCES.json) dosyasındadır.

| Kaynak | Revision |
|---|---|
| [Standart çok dilli CLIP metin modeli](https://huggingface.co/sentence-transformers/clip-ViT-B-32-multilingual-v1/tree/58edf8cada9e398793dca955574a48cbb7f18be2) | `58edf8cada9e398793dca955574a48cbb7f18be2` |
| [Eşleşen CLIP ViT-B/32](https://huggingface.co/sentence-transformers/clip-ViT-B-32/tree/327ab6726d33c0e22f920c83f2ff9e4bd38ca37f) | `327ab6726d33c0e22f920c83f2ff9e4bd38ca37f` |
| [Türkçe için küçültülmüş metin modeli](https://huggingface.co/alphaedge-ai/clip-ViT-B-32-multilingual-v1-tr-16384/tree/5ea48296bd771861465fa5da69814ce6f294f45c) | `5ea48296bd771861465fa5da69814ce6f294f45c` |
| [OPUS-MT Türkçe-Ingilizce](https://huggingface.co/Helsinki-NLP/opus-mt-tr-en/tree/19c65427cc2af5f191337d4899e0348c4af25902) | `19c65427cc2af5f191337d4899e0348c4af25902` |

MobileCLIP2-S0 kaynakları [önceki A03 sözleşmesinde](MOBILECLIP_MODEL_CONTRACT.md) sabittir. Model adı veya dal adı tek başına aynı ağırlık baytlarının kullanıldığını kanıtlamaz; gerçek indirme sırasında yayımlanmış özetler ayrıca doğrulanacaktır.

## 3. Seçenek 1 - Standart çok dilli CLIP

**Model çifti:** `sentence-transformers/clip-ViT-B-32-multilingual-v1` metin modeli ve `sentence-transformers/clip-ViT-B-32` içindeki görsel kodlayıcı.

**Türkçe kanıtı:** Model kartı, metin uzayının hizalanmasında kullanılan diller arasında `tr` kodunu açıkça sayar. Çok dilli DistilBERT metin kodlayıcısı, CLIP ViT-B/32 görsel uzayına hizalanmıştır. Bu, Türkçe desteği için kaynak kanıtıdır; GaleriAra sorgu setinde ölçülmüş başarı değildir.

**Beklenen metin akışı:** Tokenizer -> en çok 128 token -> DistilBERT token temsilleri (768 boyut) -> attention mask ile ortalama pooling -> `2_Dense` katmanı (768 -> 512, bias yok, Identity) -> karşılaştırma için L2 normalizasyonu. Kaynak yapılandırma `do_lower_case = false` belirtir; A03'teki CLIP BPE ve 77 token sözleşmesi burada kullanılmaz.

**ONNX sınırı:** Resmî Sentence Transformers belgeleri, standart ONNX dışa aktarmanın Transformer bileşenini dönüştürdüğünü, pooling/normalizasyonun dış kullanımda ayrıca uygulanması gerektiğini belirtir. Burada Dense katmanı da `modules.json` içinde ayrı modüldür. Yayımlanan metin ONNX dosyasının tek başına 512 boyutlu son arama vektörü verdiği varsayılmayacak; gerçek I/O metadata'sı A06'da incelenecek.

**Görsel tarafı:** Eşleşen CLIP yapılandırması 224 piksel görüntü ve 512 boyutlu projeksiyon tanımlar. MobileCLIP'in 256 piksel ve `[0,1]` ön işlemesi taşınmayacak; CLIP'in kendi processor/normalizasyonu yeni sözleşmede sabitlenecek. Görsel modelin gerekli projeksiyonu korunacak, yalnız havuzlanmış ara temsil kullanılmayacak.

**Android yolu:** Yayımlanmış `onnx/model_qint8_arm64.onnx` + doğru WordPiece/tokenizer davranışı + masked mean pooling + Dense ağırlıkları + eşleşen görsel ONNX. ONNX Runtime'ın Java/Kotlin Android paketi bu tür yerel çalıştırma için bir uygulama yoludur; belirli cihazda operatör desteği, çıktı uyumu, bellek ve gecikme ayrıca sınanacaktır.

**Lisans kaydı:** Çok dilli metin modelinin kartı Apache-2.0 bildirir. Eşleşen görsel model kartında ayrı lisans etiketi yoktur; orijinal OpenAI CLIP kaynak lisansı MIT'dir. Yeniden dağıtımda metin modeli, görsel ağırlıkların kökeni ve tokenizer bildirimleri ayrı kaydedilecektir.

## 4. Seçenek 2 - Türkçe için küçültülmüş CLIP

**Model çifti:** `alphaedge-ai/clip-ViT-B-32-multilingual-v1-tr-16384` metin modeli ve seçenek 1 ile aynı CLIP ViT-B/32 görsel kodlayıcısı. Aday, standart çok dilli modelin sözlüğünün Türkçe metinlerden seçilen token'larla daraltılmış türevidir; yeni görsel kodlayıcı eğitimi olarak sunulmaz.

**Doğrulanan yapı:** Sözlük 119.547 yerine 16.384 token; metin gövdesi 55.504.896 parametre. Ayrı Dense katmanıyla toplam metin tarafı 55.898.112 parametre olur. Standart model için aynı toplam 135.127.296'dır. Pooling ve 768 -> 512 Dense yapısı korunur.

**Fayda:** Daha az sözlük parametresi, daha küçük bir metin modeli oluşturur. Bunun toplam telefon belleğinde veya sorgu süresinde aynı oranda iyileşme sağlayacağı ileri sürülmez; çalışma zamanı, ara tensörler ve görsel kodlayıcı da maliyete dahildir.

**Belirsizlikler:** Kaynak kart benzer kalite beklediğini söyler, ancak kartta aynı Türkçe fotoğraf arama setinde önce/sonra ölçümü bulunmuyor. İncelenen revision'da ONNX dosyası yayımlanmamış. Dışa aktarma, tokenizer eşdeğerliği, nicemleme ve standart modele karşı hata/kalite kontrolü gerekir.

Karttaki kullanım örneğinde depo adı/yazımı ve `32768` varyantı gerçek `tr-16384` adayına uymuyor. Bu nedenle örnek komut körlemesine kullanılmayacak; bu belgede sabitlenen gerçek depo ve tokenizer esas alınacaktır.

**Lisans kaydı:** Türev model kartında MIT etiketi var; temel çok dilli model kartı Apache-2.0 bildirir. Temel modelin kaynak ve bildirimleri türev etiket nedeniyle yok sayılmayacaktır. Dağıtım dosyaları seçilen somut pakete göre izlenecektir.

## 5. Seçenek 3 - MobileCLIP2-S0 + bağımsız OPUS çevirisi

**Çeviri adayı:** `Helsinki-NLP/opus-mt-tr-en`; Türkçe kaynak, Ingilizce hedef, Marian `transformer-align`. Model kartı normalizasyon + SentencePiece ön işlemesini belirtir. Bu bağımsız model yolu ML Kit API'sini veya bir çeviri sunucusunu gerektirecek şekilde tasarlanmamıştır.

**Gerekli akış:** Türkçe sorgu -> kaynak SentencePiece/tokenizer -> Marian encoder -> adım adım decoder -> hedef metin -> A03'teki MobileCLIP metin akışı. Kaynak depoda `source.spm`, `target.spm` ve `vocab.json` bulunur. Model yapılandırması 6 encoder / 6 decoder katmanı ve 512 gizli boyut tanımlar.

**Üretim ayrıntısı:** Modelin `generation_config.json` dosyası `num_beams = 6` tanımlar. Android'de daha kolay diye greedy decoding seçmek referansla aynı çıktı verecek kabul edilemez; üretim ayarları ayrıca kararlaştırılıp sınanmalıdır. EOS, başlangıç/dolgu token'ları ve maksimum çıktı uzunluğu doğru taşınmalıdır.

**Android yolu:** Marian encoder/decoder ONNX dışa aktarma, uygun SentencePiece uygulaması ve Kotlin/native üretim döngüsü gerekir. Optimum belgelerinde encoder-decoder dışa aktarma yapısı açıklanır; bu somut modelin Android'de çalışan hazır paketini veya ölçümünü bu görevde doğrulamadık. Bu seçenek teknik olarak araştırılabilir bir entegrasyon yoludur; cihazda hazır çözüm olarak sunulmaz.

**Lisans kaydı:** Çeviri kartı Apache-2.0 bildirir. MobileCLIP2 ağırlıklarının ayrı Apple model lisansı A03'te kayıtlıdır. Çeviri lisansı bütün model zincirinin tek lisansı sayılmayacaktır.

**Kalite riski:** Kısa fotoğraf sorgularında renk, olumsuzluk, özel ad ve ilişkilerin çeviride korunması gerekir. Çeviri BLEU/chrF sonuçları tek başına fotoğraf arama başarımını göstermez. Son değerlendirme Türkçe sorgudan bulunan fotoğrafa kadar uçtan uca yapılmalıdır.

## 6. Boyutların doğru okunması

Aşağıdaki boyutlar model depolarının yayımladığı dosya metadata'sıdır; ağırlıklar indirilmedi. MB ondalık birimdir (1 MB = 1.000.000 bayt). Dosyalar farklı hassasiyetlerde olduğundan tablo aynı hassasiyette bir performans karşılaştırması değildir.

| Varlık | Yayımlanmış dosya | Boyut | Dahil olmayan / anlamı |
|---|---|---|---|
| Seçenek 1 metin gövdesi | `model.safetensors`, FP32 | 538,95 MB | Dense, tokenizer ve görsel model ayrı |
| Seçenek 1 metin ONNX | `model_qint8_arm64.onnx`, INT8 | 135,34 MB | Son arama zinciri değil; pooling/Dense/görsel tarafı ayrı |
| Seçenek 1 ve 2 Dense | `2_Dense/model.safetensors`, FP32 | 1,57 MB | Her iki depoda da aynı dosya adı ve boyut; bayt eşitliği indirilmeden varsayılmaz |
| Seçenek 2 metin gövdesi | `model.safetensors`, FP32 | 222,03 MB | ONNX/nicemlenmiş paket henüz yayımlanmamış |
| Seçenek 1 ve 2 için kaynak CLIP paketi | `0_CLIPModel/model.safetensors`, FP32 | 605,16 MB | Görsel ve orijinal Ingilizce metin tarafını birlikte içerir; bu değer yalnız görsel encoder boyutu değildir |
| Seçenek 3 çeviri modeli | `pytorch_model.bin` | 306,73 MB | Nihai Android ONNX boyutu değil; MobileCLIP çifti ve tokenizer ayrıca gerekir |

Kaynak CLIP paketinden yalnız gerekli görsel dal dışa aktarılabilir; nihai dosya boyutu üretilmeden kesin söylenmeyecek. INT8 veya FP16 küçültme olasılığı, doğrulanmış hazır dosya ve doğruluk sonucu yerine geçmez. Disk boyutu RAM ihtiyacıyla eşit değildir.

## 7. Ödev ve mevcut kodla ilişki

Üç seçenek de fotoğrafları önceden indeksleyip sorguda kalıcı vektörleri sıralayacak şekilde tasarlanabilir. Seçenek 1 ve 2 ek sorgu çevirisi gerektirmez. Seçenek 3 arama sırasında ek çeviri çıkarımı gerektirir; PDF'nin dinamik araç kullanımı ifadesiyle ilişkisi ayrıca gerekçelendirilmelidir. Bu inceleme öğretim elemanının koşul yorumunu garanti etmez.

`core/SearchPhotos` ve `Embedding` arayüzleri model ailesinden bağımsız kalabilir. Üç akışta da son vektör 512 boyutlu olabilir; boyut eşitliği modelleri birbirine uyumlu yapmaz. Farklı modellerin vektörleri karıştırılmayacak, model/sürüm değişince eski indeks yeniden üretilecektir.

Seçilen 1. seçeneğin [etkin model sözleşmesi](MODEL_CONTRACT.md) A04.2'de hazırlandı; önceki MobileCLIP incelemesi [arşiv belgesinde](MOBILECLIP_MODEL_CONTRACT.md) korunur. A05 yeni sözleşmeye göre yürütülecek.

## 8. Ana üçlüye alınmayan seçenekler

- **ML Kit çevirisi:** Resmî API cihazda çeviri sağlar; ancak PDF'nin arama sırasında ML Kit gibi araçlara dinamik ihtiyaç duyulmaması ifadesi varken ana öneri olarak seçilmedi. Bu, bütün ML Kit kullanımlarının kesin yasak olduğuna ilişkin bir hüküm değildir.
- **OPUS-MT tc-big-tr-en:** Türkçe-Ingilizce için kaynak var; incelenen dosya 469,72 MB FP16 ve kart lisansı CC-BY-4.0. Basit OPUS adayından farklı büyüklük/lisans taşır; kısa takvim için üçüncü seçenekte daha küçük `opus-mt-tr-en` incelendi. Küçük modelin daha doğru olduğu iddia edilmez.
- **SigLIP 2 Base:** Çok dilli model için kaynak var; incelenen FP32 dosya 1.500,80 MB. Bu adayın Android ve Türkçe deneyi bu görevde yapılmadığından ana üçlüye alınmadı.

## 9. Öneri ve kapanış ölçütü

**Öneri: 1. seçenekle başlamak.** Türkçe desteğinin açık kaynak kaydı, standart model yapısı ve yayımlanmış ARM64 ONNX gövdesi, 27 Aralık hedefine yönelik başlangıç için gerekçedir. Çeviri zinciri eklenmez ve raporda Türkçe sorgu akışı açıkça anlatılabilir. Bu bir mühendislik değerlendirmesidir; ölçülmüş Türkçe kalite veya cihaz hızı sıralaması değildir.

2. seçenek, standart modelden sonra kaliteyi koruyan küçültme deneyi olarak anlamlıdır. 3. seçenek, mevcut görsel modelini korumanın öncelikli olduğu durumda değerlendirilebilir; çeviri taşıma ve üretim döngüsü ek kapsamdır.

Kullanıcı 1. seçeneği seçti. Seçim A04 kaynak manifestine ve mimari belgesine kaydedildi; A04 kapatıldı. Değerlendirme protokolü [A04.1 planında](EVALUATION_PLAN.md) bulunur. A04.2'nin tokenizer, görsel processor, pooling/Dense ve doğrulama sözleşmesi hazırdır; sıradaki görev A05'tir.

## 10. Kaynaklar

1. Yukarıdaki sabit model depolarının README, modules/config, pooling ve Dense yapılandırmaları. Dosya özetleri [kaynak manifestinde](A04_MODEL_REFERENCES.json).
2. [Sentence Transformers - ONNX ve bileşen sınırları](https://www.sbert.net/docs/sentence_transformer/usage/efficiency.html), erişim 23 Kasım 2026.
3. [ONNX Runtime - Android/mobil uygulama yolu](https://onnxruntime.ai/docs/tutorials/mobile/), erişim 23 Kasım 2026.
4. [Optimum - ONNX dışa aktarma ve encoder/decoder yapısı](https://huggingface.co/docs/optimum-onnx/onnx/usage_guides/export_a_model), erişim 23 Kasım 2026.
5. [Transformers - MarianMT](https://huggingface.co/docs/transformers/model_doc/marian), erişim 23 Kasım 2026.
6. [ML Kit - cihazda çeviri API'si](https://developers.google.com/ml-kit/language/translation), erişim 23 Kasım 2026.
7. OpenAI CLIP kaynak lisansı ve A03'te kayıtlı Apple model lisansı; ayrı lisans koşulları manifest ve sözleşmede izlenir.
