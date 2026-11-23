# A08 — Room vektör round-trip ve sürüm ayrımı

Durum: tamamlandı. Mevcut embeddings tablosuna EmbeddingDao ve RoomVectorRepository bağlandı; Float32VectorCodec little-endian IEEE-754 Float32 saklar. Kullanıcı bozuk kayıt için üç alternatiften bütün okumayı açık bir bütünlük hatasıyla durdurmayı seçti. Kayıt korunur.

## Veri sözleşmesi ve somut sonuç

| Kontrol | Sonuç |
|---|---|
| Model kimliği | id + revision + boyut; URI/id/revision birleşik anahtar |
| BLOB biçimi | Başlıksız, ardışık normalize Float32, little-endian |
| Gerçek disk testi | 512 boyut, 2048 bayt; 3 kapatma/yeniden açma döngüsünde bitleri aynı |
| Kenar değerler | Signed zero ve en küçük pozitif subnormal bitleri korundu |
| Kontrollü BLOB SHA-256 | `b10e78e25e8bbc32d5f96591d2411980bf7cff7918ae37b088ca6f4d54145d64` |
| Room sürüm/kimlik | 1 / `a119765104aef865a7368184c9ba26f2`; şema değişmedi |
| Model revision | `b01d72b7de9160a38730f6a8102bbbcbc799bb5a153caf9df6e21eec302a310d`; değişmedi |

`Embedding.fromNormalized` boyut, sonlu değerler, pozitif norm ve `abs(norm-1) <= 1e-5` koşullarını denetler; sonra savunmalı kopya alır. Değerleri tekrar normalize etmez. Disk testi özellikle normu kabul bütçesi içinde olan fakat ikinci L2 işleminde bitleri değişecek bir vektör kullanır. Böylece yalnız yaklaşık benzerlik yerine Float32 bitleri ve bağımsız oluşturulan bayt dizisi karşılaştırıldı. Yeni `.f32` dosyası veya model ağırlığı eklenmedi.

## DAO ve repository davranışı

`save` mevcut fotoğraf için normalize Embedding'i yazar. Aynı model id/revision için farklı boyut bulunması transaction içinde hata üretir; mevcut satır veya diğer fotoğraflar değiştirilmez. Sonra anahtar bazında upsert yapılır. Fotoğraf kaydı yoksa FK yazmayı reddeder. Fotoğraf oluşturma/kodlama A09'un konusudur.

`load` id/revision'ı SQL ile seçer, URI sırasıyla döndürür. Seçilen satırda boyut uyumsuzluğu, yanlış byte uzunluğu, NaN/Infinity, sıfır norm veya normalize olmayan değer VectorIntegrityException üretir. Boyut hatası sessizce filtrelenmez; kısmi sonuç, kayıt silme veya otomatik onarım yoktur. Farklı aile/revision kayıtları ayrı yüklenir. Diğer revision'lar aynı URI için birlikte saklanabilir.

Mevcut PhotoDao.invalidate bütün fotoğraf vektörlerini; fotoğraf silme FK cascade'i bağlı revision'ları kaldırır. Upsert aynı fotoğrafın diğer model/revision kayıtlarını silmez. Paralel yazma testinde aynı model/revision için farklı boyutlu iki yazarın yalnız biri başarılı oldu; veri karışmadı.

## Test kanıtı

- 9 çekirdek testi: önceki 5 test ve 4 yeni normalized-restore testi. Bit eşitliği, savunmalı kopyalar, geçersiz veri reddi ve ham constructor'ın önceki L2 davranışı geçti.
- API 35 `x86_64` üzerinde 12 gerçek Room testi: disk close/reopen, LE byte düzeni, aile/revision ayrımı, hedef anahtar upsert'i, sıralama/boş model, bozuk BLOB'un korunması, nonfinite/zero/norm reddi, boyut uyuşmazlığı, farklı fotoğraflarda boyut koruması, invalidate/cascade, FK/boş URI hatası ve paralel yazma.
- Çekirdek constructor değişikliği için ayrıca A07'nin tek gerçek model testi tekrar çalıştı: 22 sorgu; minimum cosine 0.993826423024, maksimum norm hatası 3.44253572e-08. Önceki çıktı değişmedi. A07'nin diğer yaşam döngüsü testleri bu adımda yeniden çalıştırılmış gibi sayılmadı.
- Toplam 13 Android testi başarılı; debug/test APK üretildi. Lint: 0 hata, 12 uyarı. Şema JSON'u önceki içerikle karşılaştırıldı; migration veya yeni bağımlılık eklenmedi.

Testler üretim `gallery.db` dosyasını kullanmadı. In-memory veritabanları ve yalnız bu testte üretilen `a08-test-<UUID>.db` dosyası kullanılıp kapatıldı/temizlendi. Emülatör verisi silinmedi. [Makine tarafından okunabilir kanıt](A08_ROOM_VECTORS.json) test adlarını ve sonuçları içerir.

## Tekrar üretim ve sınırlar

[Araç/komutlar](../../tools/README.md) ve [model sözleşmesi](../MODEL_CONTRACT.md). A08 Room testleri model dosyası indirmeyi veya neural inference'ı gerektirmez. İsteğe bağlı A07 gerçek sorgu regresyonu A06 model/Dense dosyalarını kullanır. [Room DAO](https://developer.android.com/training/data-storage/room/accessing-data) ve [asenkron sorgu](https://developer.android.com/training/data-storage/room/async-queries) kuralları mevcut Room 2.8.5 bağımlılığıyla uygulandı.

Okuma seçilen modelin bütün satırlarını belleğe alır; büyük galeri performansı/sayfalama ölçülmedi. Repository metadata anlık görüntüsünü kaynak fotoğrafın erişilebilirliğiyle yeniden doğrulamaz; galeri ve indeks yaşam döngüsü sonraki görevlerde bağlanır. Fiziksel cihaz/ARM veya retrieval başarımı bu kalıcılık testinin sonucu değildir. A09 tek fotoğrafı metadata ve embedding ile tutarlı indekslemeye geçer. Commit veya push yapılmadı.
