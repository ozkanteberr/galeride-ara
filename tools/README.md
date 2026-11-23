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

## A07 — QueryEncoder adaptörü

`ClipQueryEncoder.open(ClipTextFiles(model, dense, vocabulary))` her çağrıda ayrı doğrulanmış oturum açar. Aynı adaptörde sorgular oturumu yeniden kullanır. Kimlik ve dosya özetleri `MultilingualClipProfile` üzerinden sabittir; ortak manifestle test edilir. `open` IO, `encodeTurkish` Default dispatcher'da çalışır. Mevcut uygulamanın Application veya ekranında otomatik model yüklemesi yoktur.

Sahip, kullandıktan sonra adaptörü kapatmalıdır. `close()` bloklayıcıdır; devam eden çağrıyı bekler. Coroutine sahibi için örnek kullanım:

```kotlin
val encoder = ClipQueryEncoder.open(ClipTextFiles(modelFile, denseFile, vocabFile))
try {
    val embedding = encoder.encodeTurkish("kırmızı bir araba")
    // Embedding ortak manifest kimliğini taşır ve L2 ile normalize edilmiştir.
} finally {
    withContext(NonCancellable) {
        withContext(Dispatchers.IO) { encoder.close() }
    }
}
```

Boş/temizlenmiş boş sorgu IllegalArgumentException üretir. Kaynaklar kapandıktan sonraki sorgu IllegalStateException üretir. Dosya doğrulama ve model hataları çağırana ulaşır; iptal bir başarıya veya genel hataya çevrilmez. Bekleyen sorgu iptal edilir, başlamış native hesap tamamlanabilir; iptal edilen sonuç dönmez. Native çağrıya anında son verme garantisi yoktur. Application singleton'ı eklenmedi; bu yaşam döngüsü kullanıcının adaptör başına oturum seçimidir.

Önce A06 model dosyalarını hazırlayın. Ardından, JDK 17 ve doğru cihaz seri numarasıyla:

```powershell
./gradlew.bat :core:test "-PcoreBuildDir=$env:TEMP/galeriara-core-build" :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
python tools/a07_android_check.py --serial emulator-5554 --model tmp/a06/source/onnx/model_quint8_avx2.onnx --dense tmp/a06/clip_multilingual_dense.f32 --app-apk app/build/outputs/apk/debug/app-debug.apk --test-apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk --output tmp/a07/android_report.json
```

Gerekirse `--adb <adb.exe yolu>` ve A06'daki gibi `--remote-dir /storage/<kart-kimliği>/Android/data/com.galeriara/files/a06` ekleyin. Cihazda aynı SHA-256 ile hazır olan dosyalar yeniden gönderilmez. İki APK argümanı birlikte atlanabilir. Araç manifest revision'ını ve yerel modellerin özetini kontrol eder; yalnız A07 ile A06 test sınıflarını çalıştırır. 13 testin ve rapor kimliğinin doğrulanması gerekir; instrumentation çıktısı da kaydedilir. Bu araç model indirmez, galeri indeksi veya arama ekranını etkinleştirmez.

## A08 — Room vektör saklama

`RoomVectorRepository(db)` var olan VectorRepository okuma arayüzünü uygular ve `save(photoUri, embedding)` sağlar. Fotoğraf satırı önceden bulunmalıdır. Model id/revision SQL ile ayrılır; aynı model/revision altında farklı boyut yazılması transaction'da reddedilir. Kodlama/çözme Default dispatcher'da, suspend veritabanı çağrıları Room yürütücülerinde çalışır. Veritabanının sahibi çağırandır; repository DB'yi kapatmaz.

`Float32VectorCodec` normalize Float32 değerleri little-endian olarak saklar. Okumada `Embedding.fromNormalized` doğrulayıp kopyalar, tekrar normalizasyon yapmaz. Bozuk kayıt bütün okumayı VectorIntegrityException ile durdurur ve veriyi korur. Yeni indeksleme veya arama ekranı çağrısı eklenmedi. Schema version 1 değişmedi; migration yoktur.

Model dosyası gerektirmeyen A08 testlerini JDK 17 ve doğru cihaz seri numarasıyla çalıştırın:

```powershell
./gradlew.bat :core:test "-PcoreBuildDir=$env:TEMP/galeriara-core-build" :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
python tools/a08_android_check.py --serial emulator-5554 --app-apk app/build/outputs/apk/debug/app-debug.apk --test-apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk --output tmp/a08/android_report.json
```

Gerekirse `--adb <adb.exe yolu>` ekleyin; iki APK argümanı birlikte atlanabilir. Araç 12 Room testinin ve bit eşitliği raporunun başarısını denetler, instrumentation çıktısını da kaydeder. Testler gerçek gallery.db'yi kullanmaz; yalnız in-memory ve kendilerine ait UUID adlı test veritabanlarını açıp temizler. Yeni bağımlılık veya fixture gerekmez.

Çekirdek ham Embedding constructor'ının gerçek sorgu çıktısı için hedefli A07 regresyonu da çalıştırıldı. Tekrar etmek için A06 model/Dense cihazda hazırken aşağıdaki hedefi kullanın; modelPath/densePath değerlerini kendi dosya konumunuza uyarlayın:

```powershell
adb -s emulator-5554 shell am instrument -w -r -e modelPath /storage/<kart-kimliği>/Android/data/com.galeriara/files/a06/model_quint8_avx2.onnx -e densePath /storage/<kart-kimliği>/Android/data/com.galeriara/files/a06/clip_multilingual_dense.f32 -e class 'com.galeriara.data.encoding.ClipQueryEncoderInstrumentedTest#realAdapterNormalizesTurkishQueriesAgainstIndependentReferences' com.galeriara.test/androidx.test.runner.AndroidJUnitRunner
```

Bu komut tek A07 testinde 22 sorguyu karşılaştırır; A08'in model gerektirmeyen 12 testine dahil değildir. A07 testlerinin tamamını tekrar etmek isterseniz önceki bölümün runner'ını kullanın. [A08 doğrulama kaydı](../docs/validation/A08_ROOM_VECTORS.md) kapsamı ve ölçüm sınırlarını açıklar.
