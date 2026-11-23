# Model doğrulama araçları

OpenAI CLIP kaynak lisans bildirimi [licenses/OPENAI_CLIP_LICENSE.txt](licenses/OPENAI_CLIP_LICENSE.txt), alınan kaynağın URL ve içerik özeti [licenses/visual-license-source.json](licenses/visual-license-source.json) dosyasında korunur. Bu kayıt, metin modelinin veya bütün dağıtım paketinin lisans değerlendirmesi değildir.

Bu araçlar A05'in tek kontrollü görsel karşılaştırmasını tekrarlar. Galeri taramaz, Room'a yazmaz ve aramayı etkinleştirmez. Python 3.11 ve JDK 17 ile çalıştırıldı.

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
