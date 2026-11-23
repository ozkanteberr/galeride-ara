# A06 — Tek Türkçe sorgunun metin vektörü

Durum: tamamlandı. Kullanıcının seçtiği standart çok dilli CLIP modelinde saf Kotlin cased WordPiece -> Int64 token/maskeler -> U8U8 DistilBERT -> maskeli ortalama -> Dense -> ham 512 vektör zinciri doğrulandı. L2 normalizasyonu mevcut `core/Embedding` ile ayrıca kontrol edildi. Çeviri katmanı eklenmedi.

## Kararlar ve somut dosyalar

Kullanıcı tokenizer için Kotlin, Rust/JNI ve ONNX Runtime Extensions seçeneklerinden saf Kotlin'i seçti. Nicemleme teşhisi sonrasında tek U8U8, işlemciye göre iki INT8 ve FP32 seçeneklerinden tek U8U8 dosyasını seçti. Bunlar yeni model ailesi seçimi değildir.

| Varlık | Boyut | SHA-256 |
|---|---|---|
| `onnx/model_quint8_avx2.onnx` | 135377779 bayt | `fbc8fbeaa5237d96bd1bf430057c70d34de8334a463e933a5faa305f6caeed9c` |
| Kaynak Dense safetensors | 1572984 bayt | `d12568dc7300970a4d3dbb49068ad16cd89b99840b74b026f8e48071e9414f74` |
| Çalışma Dense Float32 LE `[512,768]` | 1572864 bayt | `e5f6548cbcef6c62631b3946d89042ab4c8451762c7bb0a3be11eb6c480b686d` |
| Tokenizer JSON | 1961847 bayt | `5b4e1a8171c81dfd666ae40265b9530c6e0b3d53923fe8ac493dcc84229adf81` |
| Sözlük | 119547 öğe | `fe0fda7c425b48c516fc8f160d594c8022a0808447475c1a7c6d6479763f310c` |

Sabit metin revision'ı `58edf8cada9e398793dca955574a48cbb7f18be2`. Gerçek girdi `input_ids`, `attention_mask`, Int64 `[batch_size,sequence_length]`; çıktı `last_hidden_state`, Float32 `[batch_size,sequence_length,768]`. Dense bias içermez, Identity aktivasyonludur; CLS/SEP maskeli ortalamaya dahildir. Model/Dense çalışma dosyaları `tmp/a06` altındadır; Git veya APK içinde değildir. Sözlük ve küçük bağımsız referans vektörleri test APK'sındadır.

## Bağımsız referans ve sonuç

Python, strict yüklenen FP32 DistilBertModel ve safetensors Dense ile referans üretir. Tokenizers 0.22.1 sabit tokenizer.json'u çalıştırır; ayrı Kotlin uygulaması bununla karşılaştırılır. INT8 masaüstü çıktısının pooling/Dense hesabı NumPy Float64 ile yapılır; Kotlin kendi döngüleriyle hesaplar. Dropout kapalıdır.

Önceki 14 örneğe ek özel token'ın kelime içindeki kullanımı, temizleme sonrası özel token'a benzeyen metin, Unicode boşlukları, Python strip kontrol karakterleri, özel kullanım/atanmamış karakter, supplementary CJK/emoji, ZWJ, 100/101 karakter sınırı ve casing/aksan örnekleri eklendi. 24 örnek × dinamik/sabit 128 profil = 48 karşılaştırma.

| Kontrol | Ölçülen sonuç | Önceden belirlenen kabul |
|---|---|---|
| Kotlin token/maskeler | 48/48 birebir eşleşme | Tam eşitlik |
| Android / aynı masaüstü ONNX vektörü | Min cosine 1; maks göreli L2 0 | >=0.99999, <=0.001 |
| Android INT8 / FP32 referans | Min cosine 0.993826400315; maks göreli L2 0.11148372 | >=0.99, <=0.15 |
| Normalize vektör norm hatası | Maks 3.44253572e-08 | <=1e-5 |
| FP32 dinamik / sabit padding | 24/24 geçti | >=0.99999, <=0.001 |

Boş sorgu tokenizer/model düzeyinde referans kapsamındadır; kullanıcı aramasında mevcut `SearchPhotos` boş sorguyu reddeder. Literal `[PAD]` token'ı ile eklenen padding ayrımı attention mask'inden yapılır.

## U8S8 teşhisi ve varyant değişimi

Önceki `model_qint8_arm64.onnx` mevcut x86 ortamında 48/48 kontrolde FP32 bütçesini aşan fark üretti. İlk MatMulInteger katmanında 5376 öğenin 900'ü bağımsız Int32 çarpımdan farklı; en büyük tamsayı farkı 21377. Türkçe örnekte eski varyant cosine 0.640436146, göreli L2 0.794493710 verdi.

Bu bulgu [ONNX Runtime'ın x86 U8S8 doyma açıklamasıyla](https://onnxruntime.ai/docs/performance/model-optimizations/quantization.html) tutarlıdır. ARM cihazında aynı hata olduğu sonucuna varılmaz. Yayımlanmış U8U8 dosyası sabit özetle doğrulandı ve aynı eşikleri geçti; kabul bütçeleri değiştirilmedi. [Başarısız sonuçlar ve teşhis](A06_INT8_X86_DIAGNOSTIC.json) korunur; [teşhis aracı](../../tools/a06_quantization_diagnostic.py) tekrar üretir.

## Derleme, kapsam ve tekrar üretim

- Debug/test APK üretildi; 5 çekirdek testi, 2 A06 instrumentation testi başarılı. Lint: 0 hata, 12 uyarı. Native bağımlılık eklenmedi.
- API 35 `x86_64` emülatör, CPU, ONNX Runtime Android 1.23.2, BASIC optimizasyonu, 2 intra-op/1 inter-op thread. Masaüstü sürümlerinin tamamı [JSON kaydında](A06_TEXT_ENCODER.json).
- Mevcut emülatörün uygulaması `install -r` ile güncellendi; kullanıcı verisi temizlenmedi ve emülatör kapatılmadı. Test modelleri uygulamanın SD kart dizinine aktarıldı.
- [Araçlar ve komutlar](../../tools/README.md), [etkin sözleşme](../MODEL_CONTRACT.md), [runtime manifesti](../MODEL_RUNTIME_MANIFEST.json). Revision: `b01d72b7de9160a38730f6a8102bbbcbc799bb5a153caf9df6e21eec302a310d`.

Fiziksel ARM cihazı, bütün Unicode aralığı/API sürümleri, retrieval kalitesi, bellek/p50/p95 veya performans deneyi bu görevde doğrulanmadı. Kotlin Unicode kategori sorguları Android tablosuna bağlıdır; burada API 35 referansları ölçüldü. URI/EXIF/renk profili görsel adaptörü ve model dağıtımı da tamamlanmış sayılmaz. Model kimliği bileşenleri sabitlense de prototip henüz QueryEncoder adaptörüne/galeriye/Room'a/Worker'a bağlı değildir. A07 doğrudan Türkçe sorgu adaptörünü bağlar; arama ekranı A13'te etkinleştirilir. Commit veya push yapılmadı.
