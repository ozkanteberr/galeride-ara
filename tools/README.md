# Model doğrulama araçları

OpenAI CLIP kaynak lisans bildirimi [licenses/OPENAI_CLIP_LICENSE.txt](licenses/OPENAI_CLIP_LICENSE.txt), alınan kaynağın URL ve içerik özeti [licenses/visual-license-source.json](licenses/visual-license-source.json) dosyasında korunur. Bu kayıt, metin modelinin veya bütün dağıtım paketinin lisans değerlendirmesi değildir.

Bu araçlar A05'in tek kontrollü görsel karşılaştırmasını tekrarlar. Galeri taramaz, Room'a yazmaz ve aramayı etkinleştirmez. Python 3.11 ve JDK 17 ile çalıştırıldı.

## Git kapsamı ve Float32 dosyaları

`app/src/androidTest/assets` altındaki izin verilen `.f32` dosyaları bağımsız test referanslarıdır ve kaynak kontrolünde tutulur. A05'in 3 dosyası toplam 592 KiB; A06'nın 96 dosyası toplam 192 KiB'dır. Bu dosyalar olmadan Android karşılaştırma testleri çalışmaz. Model ağırlığı veya kişisel fotoğraf içermezler; Git LFS gerekmez.

Diğer `.f32` dosyaları ile `.onnx`, `.ort`, `.safetensors` modelleri ve `tmp/` çalışma çıktıları `.gitignore` ile dışarıda kalır. Örneğin `clip_multilingual_dense.f32` çalışma ağırlığı Git'e eklenmez. `.gitattributes`, test referanslarını `binary` olarak işaretler; Float32 baytları satır sonu dönüşümüne uğramaz. Test dosyalarını dışlamak için genel bir ignore kuralı ekleyip izin istisnalarını kaldırmayın.

## Masaüstü referansı

Depo kökünde, yeterli disk alanı olan bir çalışma dizini kullanın. İlk çalıştırmada yaklaşık 605 MB kaynak checkpoint indirilir; görsel ONNX yaklaşık 352 MB'dır. Modelleri Git'e eklemeyin. `tmp/` ve ONNX dosyaları mevcut ignore kurallarıyla dışarıda kalır.

```powershell
python -m venv tmp/model-venv
tmp/model-venv/Scripts/python.exe -m pip install -r tools/requirements-models.txt
tmp/model-venv/Scripts/python.exe tools/a05_visual_reference.py --contract docs/MODEL_CONTRACT.json --work-dir tmp/a05
```

Araç sabit revision/özetleri doğrular; görsel dal ve projeksiyonu yükler; deterministik 320×240 RGB PNG üretir; gerçek Transformers processor + PyTorch çıktısını FP32 ONNX Runtime CPU çıktısıyla karşılaştırır. Görsel dal dışındaki İngilizce metin ağırlıkları dışa aktarılmaz. Checkpoint'in eski `position_ids` tamponu yalnız beklenen deterministik diziyle birebir eşitse çıkarılır; kalan durum sözlüğü strict yüklenir.

`desktop_report.json` gerçek I/O, SHA-256, sayısal farklar ve kullanılan paket sürümlerini kaydeder. `fixtures/` PNG, NCHW giriş, iki referans vektörü ve metadata içerir. Bağımlılık dosyası doğrudan paketleri sabitler; bütün platformların transitif bağımlılık kilidi değildir. Tek çıkarım süresi performans deneyi sayılmaz. Kaynak kökeni ve lisans bağlantıları [model sözleşmesindedir](../docs/MODEL_CONTRACT.md); model dağıtımı bu görevin kapsamı dışındadır.

## Android karşılaştırması

Önce `adb devices` ile açıkça hedef cihazı belirleyin. Aşağıdaki kurulum aynı paketlerin mevcut kurulumunu günceller; uygulama verisini silmez. Model dosyası test sırasında cihaza aktarılır; APK içine gömülmez. İç depolamada model ve APK'lar için yeterli alan gerekir.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot'
./gradlew.bat :core:test "-PcoreBuildDir=$env:TEMP/galeriara-core-build" :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
python tools/a05_android_check.py --serial emulator-5556 --model tmp/a05/clip_vit_b32_visual_fp32.onnx --app-apk app/build/outputs/apk/debug/app-debug.apk --test-apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk --output tmp/a05/android_report.json
```

SDK'nin `platform-tools` dizini PATH üzerinde değilse `--adb <adb.exe yolu>` ekleyin. Paketler zaten kuruluysa iki APK argümanını birlikte atlayabilirsiniz. Varsayılan aktarım `run-as` ile debug uygulamanın özel dosya dizinine yapılır. İsteğe bağlı `--remote-model` uygulamanın okuyabildiği, açıkça seçilmiş bir cihaz yolu içindir; dosya bu yola kopyalanır. Araç uygulama verisini temizlemez ve emülatör oluşturmaz.

Android testi depodaki `app/src/androidTest/assets/a05` fixture'larını kullanır. Yeniden dışa aktarılan modelin özeti farklıysa araç durur; yalnız test geçsin diye metadata değiştirilmez. Farkın kaynağı incelenir, bağımsız referanslar birlikte yenilenir ve sözleşme kaydı güncellenir.

Kontroller: PNG -> BICUBIC/merkez kırpma/normalizasyon; aynı tensörde Android–masaüstü ONNX ve Android–PyTorch; tam PNG akışında Android–PyTorch vektörü. Başarı raporu yalnız bütün eşikler geçerse yazılır. Başarısız instrumentation çıkışı da kaydedilir. Sonuç tek kontrollü opak PNG ve belirtilen cihaz içindir; URI/EXIF/HDR/RGBA, fiziksel ARM cihazı, büyük fotoğraf belleği ve galeri başarımı ayrıca doğrulanmalıdır.

## A06 — Metin referansı

Kullanıcı saf Kotlin cased WordPiece ve tek yayımlanmış U8U8 INT8 dosyasını seçti. Varsayılan referans aracı yalnız seçilen metin ONNX, FP32 kaynak gövde ve Dense dosyalarını indirir/doğrular. Önceki ARM64 adlı U8S8 dosyası yalnız isteğe bağlı teşhis içindir. Çalıştırma doğrudan paketleri sabitlenmiş A05 Python ortamıyla yapılabilir:

```powershell
tmp/model-venv/Scripts/python.exe tools/a06_text_reference.py --contract docs/MODEL_CONTRACT.json --tokenizer-cases docs/TOKENIZER_REFERENCE_CASES.json --work-dir tmp/a06
./gradlew.bat :core:test "-PcoreBuildDir=$env:TEMP/galeriara-core-build" :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
python tools/a06_android_check.py --serial emulator-5554 --model tmp/a06/source/onnx/model_quint8_avx2.onnx --dense tmp/a06/clip_multilingual_dense.f32 --app-apk app/build/outputs/apk/debug/app-debug.apk --test-apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk --output tmp/a06/android_report.json
```

Gerekirse `--adb <adb.exe yolu>` kullanın. İki APK argümanı birlikte atlanabilir. Varsayılan model aktarımı debug uygulamanın özel dosyalarına `run-as` ile yapılır. İç alan yetersizse uygulamanın okuyabildiği açık bir cihaz dizinini `--remote-dir /storage/<kart-kimliği>/Android/data/com.galeriara/files/a06` ile seçebilirsiniz. Kart kimliğini `adb shell sm list-volumes all` ile bulun. Kurulum/aktarım mevcut dosyaları günceller, uygulama verisini silmez; örnek seri numarası yerine kendi hedefinizi kullanın.

Araç yalnız iki A06 sınıfını çalıştırır; eski A05 testi farklı model gerektirdiğinden bütün instrumentation paketini kontrolsüz çalıştırmaz. Tokenizer testi 24 örneğin iki profilini, metin testi 48 ham vektörü ve L2 normunu kontrol eder. `OK (2 tests)` ve aynı model özetli başarı raporları olmadan başarılı sayılmaz. `.instrumentation.txt` çıkışı da kaydedilir.

Bağımsız referanslar `app/src/androidTest/assets/a06` içindedir. Modelin, tokenizer'ın, Dense'in veya fixture'ın özeti değişirse yalnız testi geçirmek için kayıtları değiştirmeyin; kaynak farkını araştırıp ilgili referansları birlikte yeniden üretin. Model ve Dense çalışma ağırlıkları test APK'sına da gömülmez. API 35 x86_64 sonuçları fiziksel ARM veya retrieval başarımı değildir. Unicode kategori davranışı Android sürümüne bağlı olduğundan diğer API'ler ayrıca sınanmalıdır.

Önceki U8S8 varyantının x86 teşhisini tekrar üretmek için A06 referansını hazırladıktan sonra:

```powershell
tmp/model-venv/Scripts/python.exe tools/a06_quantization_diagnostic.py --contract docs/MODEL_CONTRACT.json --tokenizer-cases docs/TOKENIZER_REFERENCE_CASES.json --work-dir tmp/a06
```

Bu araç teşhis için eski dosyayı sabit özetle indirir, ilk tamsayı katmanını NumPy Int32 çarpımla karşılaştırır ve `quantization_diagnostic.json` yazar. Çalışma modelini seçmez veya etkin manifesti değiştirmez. [Model kartı ve lisans bildirimi](licenses/A06_MODEL_NOTICE.txt) ile [Apache-2.0 metni](licenses/APACHE-2.0.txt) korunur.
