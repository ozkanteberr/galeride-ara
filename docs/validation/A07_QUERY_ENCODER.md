# A07 — Doğrudan Türkçe QueryEncoder

Durum: tamamlandı. Kullanıcının son seçimi adaptör başına bir ONNX oturumudur; bu örneği açan sahip, işi bittiğinde kapatır. `ClipQueryEncoder`, `QueryEncoder` arayüzünden ortak model kimliğiyle normalize 512 boyutlu Embedding üretir. Application genelinde paylaşılan oturum eklenmedi.

## Uygulanan davranış

- `open(ClipTextFiles)` sözlük/model/Dense dosyalarını A06'nın sabit özetleriyle IO dispatcher'da doğrular; yalnız tamamen hazırlanmış adaptörü döndürür. Açılış iptal edilirse oluşturulan oturum kapatılır.
- `encodeTurkish` Default dispatcher'da token üretimi, aynı oturumda masked mean/Dense ve core Embedding L2 zincirini çalıştırır. Casing/aksanlar korunur; çeviri yoktur.
- Boş ve tokenizer temizliğinden sonra içeriksiz kalan sorgu reddedilir. Aynı adaptörde sorgular Mutex ile sıralanır; bekleyen sorgu iptal edilebilir. Hatalar ve CancellationException çağırana iletilir; adaptör örtülü olarak kapanmaz.
- Başlamış native çağrı zorla durdurulmaz. Çağrı bittiğinde iptal kontrolü, sonucu döndürmeyi engeller; sonraki geçerli sorgu oturumu kullanabilir.
- `close()` idempotenttir, mevcut çalışmayı bekler ve yeni sorguları kapalı durum hatasıyla reddeder. Bloklayıcı kapatma ana iş parçacığı dışında çağrılmalıdır.

## Gerçek model ve bağımsız referans

API 35 `x86_64` emülatöründe test gerçek QueryEncoder arayüzünü ana dispatcher'dan çağırdı. Önceki fixture'lardaki iki boş örnek dışındaki 22 sorgu dinamik padding ile üretildi. Aynı masaüstü ONNX'in normalize referansına değer başına `1e-6` toleransla karşılaştırıldı. FP32 referanslar da önce aynı core Embedding kuralıyla normalize edildi; bu görevdeki göreli L2 böyle normalize vektörlere aittir. A06'nın ham vektör karşılaştırması ayrıca regresyon olarak yeniden geçti.

| Ölçüm | Sonuç | Kabul |
|---|---|---|
| Gerçek adaptör sorgusu | 22/22 | Referans karşılaştırmaları başarılı |
| Normalize çıktı / FP32 referans minimum cosine | 0.993826423024 | >=0.99 |
| Normalize çıktı / FP32 referans maksimum göreli L2 | 0.111117946 | <=0.15 |
| Maksimum norm hatası | 3.44253572e-08 | <=1e-5 |
| A06 ham vektör regresyonu | 48/48 | Önceki sabit bütçeler |

Ortak revision `b01d72b7de9160a38730f6a8102bbbcbc799bb5a153caf9df6e21eec302a310d`. `MultilingualClipProfile` kimlik ve bileşen özetleri [runtime manifesti](../MODEL_RUNTIME_MANIFEST.json) ve A06 fixture'ıyla karşılaştırıldı. Sayısal bileşenler/profiller değiştirilmedi; yeni revision üretilmedi. Yeni model ağırlığı veya `.f32` referansı eklenmedi.

## Yaşam döngüsü ve hata kanıtı

11 A07 testi: manifest kimliği; gerçek 22 sorgu ve uç boşluk eşdeğerliği; boş/temizlenmiş boş girdilerin modele ulaşmaması; hatanın aynen iletilmesi ve yeniden kullanım; ana iş parçacığı dışında seri çalışma; kuyruktaki iptal; çalışma sırasında iptal edilmiş sonucun elenmesi; sürmekte olan çağrıyı bekleyen/idempotent kapatma; açılış iptalinde kaynak kapatma; eksik/değişmiş dosya reddi; birbirinden bağımsız adaptör sahipliği.

Eşzamanlılık, hata ve iptal senaryoları kontrollü oturumlar ve latch'lerle deterministik olarak kuruldu. Bunlar gerçek native ORT'ye yapay hata enjekte edildiği veya native çağrı durdurma gecikmesinin ölçüldüğü anlamına gelmez. Gerçek model karşılaştırması ve dosya reddi ayrı testlerde çalıştı. Kullanılan [withContext](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-context.html), [Mutex](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/-mutex/lock.html) ve [ensureActive](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html) davranışları yapılandırılmış coroutine akışında kullanılır.

## Derleme ve kapsam

Debug ve test APK üretildi; 5 çekirdek testi ile 13 Android testi (11 A07 + 2 A06 regresyon) geçti. Lint 0 hata, 12 uyarı. Yeni bağımlılık eklenmedi. Mevcut emülatör veri silmeden `install -r` ile güncellendi; önceki model/Dense dosyaları SHA-256 kontrolünden sonra yeniden kullanıldı.

Tekrar üretim: [araç ve komutlar](../../tools/README.md); bütün test adları ve ölçümler [JSON kaydında](A07_QUERY_ENCODER.json). Fiziksel ARM, retrieval kalitesi, büyük galeri ve performans dağılımları bu kapsamda değildir. GalleryApplication, ViewModel, Room, Worker ve arama ekranı bağlanmadı. A08 vektörün Room'a yazılıp okunmasını ele alır. Commit veya push yapılmadı.
