# A04.1 - Türkçe fotoğraf araması değerlendirme planı

Durum: değerlendirme protokolü belgelendi. Fotoğraflar henüz indirilmedi, sorgu/etiket seti henüz oluşturulmadı ve deney çalıştırılmadı. Bu belge A14'te veri hazırlamayı, A15'te ölçümleri yönlendirecektir.

## 1. Amaç ve seçilen yöntem

Kullanıcı A04'te **standart çok dilli CLIP** seçeneğini seçti: `sentence-transformers/clip-ViT-B-32-multilingual-v1` metin kodlayıcısı ve onunla eşleşen CLIP ViT-B/32 görsel kodlayıcısı. Ayrı çeviri katmanı eklenmeyecek. Kaynak revision'ları [A04 manifestinde](A04_MODEL_REFERENCES.json) bulunur; A04.2'nin [etkin teknik sözleşmesi](MODEL_CONTRACT.md) hazırdır.

Değerlendirme dört soruya cevap verecek:

1. Türkçe sorgular ilgili fotoğrafları ilk sonuçlara taşıyor mu?
2. Android/ONNX uygulaması, Python referansının tokenizer ve vektör akışını koruyor mu?
3. Kalıcı indeks, izin ve fotoğraf değişikliklerinde tutarlı kalıyor mu; aramada görsel analiz tekrar çalışıyor mu?
4. Model edinimi, indeksleme ve sorgu işlemlerinin süre, bellek ve depolama maliyeti nedir?

PDF 1.1.2 için veri seti adayları ve gerekçeler, 1.1.3 için deney/ölçüm planı, 2.1 için ön indeksleme kanıtı bu belgede ele alınır. Sayısal protokol [EVALUATION_PLAN.json](EVALUATION_PLAN.json) dosyasında da bulunur.

## 2. Veri seti adayları ve kullanım kararı

| Aday | Belgelenmiş özellik ve kullanım koşulu | Bu projedeki rolü |
|---|---|---|
| **XM3600 - ana değerlendirme kaynağı** | Birincil kaynakta 3.600 görsel, 36 dil ve Türkçe `tr` açıklamalar kayıtlı. Proje sayfası açıklamalar için CC BY 4.0 ve görsel atıf dosyası sağlıyor. Görsellerin kaynak/atıf bilgileri ayrıca korunacak | Türkçe ve yeniden hazırlanabilir bir başlangıç sağlar. Sabit 200 görsellik alt küme üzerinde kendi kısa sorgularımız ve ilgili-fotoğraf etiketlerimiz hazırlanacak |
| **Kontrollü/izinli yerel galeri** | Kendimizin ürettiği test görselleri veya kullanımı açıkça izinli fotoğraflar; kaynak, hak sahibi ve kullanım kapsamı manifestte tutulur | İzin, EXIF, bozuk dosya, değiştirme/silme ve indeks yaşam döngüsü kontrolleri için küçük ayrı set. Kullanıcıya ait gerçek galeri kendiliğinden okunmayacak |
| **Flickr30k - alternatif, ana deneye alınmıyor** | Kaynak proje 31.783 görsel ve 158.915 açıklama bildiriyor. Görsellerin hakları yayıncıya ait değil; erişim isteği ve ticari olmayan araştırma/eğitim koşulları açıklanıyor | Erişim süreci ve hazır Türkçe kısa sorgu/etiket ihtiyacı ek kapsam getiriyor. Bu aşamada aynı görev için ikinci büyük veri seti indirilmez |

XM3600'ın resmî açıklamalarındaki görsel eşleşmesi, galerideki bütün diğer fotoğrafların ilgisiz olduğu anlamına gelmez. Bir sorguya birden çok fotoğraf uyabilir; kendi relevance etiketlerimiz bütün 200 aday için hazırlanacaktır.

XM3600 sonuçları kişisel telefon galerilerindeki ekran görüntüsü, belge, bulanıklık ve tekrar fotoğraflarına otomatik genellenmeyecek. Veri setinin ön eğitim verileriyle olası örtüşmesi bu projede dışlanmış değildir; sonuçlar sıfırdan ayrılmış bir eğitim testi olarak sunulmaz.

## 3. Sabit galeri ve yeniden üretilebilir seçim

- Ana galeri hedefi **200 farklı görsel**. Kaynak görseller, atıflar ve ilgili `tr` açıklamaları A14'te edinilecek.
- Kaynak kimlikleri sıralanır; sabit tohum `42` ile örneklenir. Seçim sonucu kimlik listesi ayrıca kaydedilir; yeniden üretimde asıl kayıt bu liste ve dosya özetleridir.
- Bozuk, kullanım koşulu kaydı eksik veya bayt düzeyinde yinelenen görsel varsa gerekçesiyle elenir; aynı sıralı aday akışından yenisi alınır. Kimlik listesi model sonuçlarına bakılarak değiştirilmez.
- Her görsel için `image_id`, kaynak URL/kimlik, yerel SHA-256, dosya boyutu, boyutlar, atıf/lisans bilgisi ve varsa yakın-kopya grubu tutulur. Uygulama URI'si deney kimliğinden ayrı tutulur.
- Yakın kopyalar ayrı fotoğraf olarak kalırsa bu durum raporlanır; birden fazla benzer sonucun başarıyı şişirmemesi için grup başına sonuç incelemesi ayrıca gösterilir.
- Kalite deneyinde galeri ve erişim kapsamı bütün yöntemlerde aynıdır. İzin daraltma deneyleri ayrı işlev grubunda yürütülür.

Bu sayı PDF zorunluluğu değil, bu proje için belirlenen başlangıç deney kapsamıdır. Daha büyük galeriye geçilirse veri revision'ı değişir ve önceki sonuçlarla aynı deneymiş gibi birleştirilmez.

## 4. Türkçe sorgu seti

**40 pozitif sorgu** hazırlanır: nesne, sahne, renk, eylem ve ilişki kategorilerinin her birinde 8 sorgu. Her sorgunun galeride en az bir açık ilgili fotoğrafı olmalıdır.

| Ayrım | Her kategoriden | Toplam | Kullanım |
|---|---|---|---|
| Geliştirme | 2 | 10 | Tokenizer/ön işleme sorunlarını araştırma ve çalışma zamanı ayarlarını belirleme |
| Son değerlendirme | 6 | 30 | Dondurulmuş yöntemle nihai kalite ölçümü |

Ek olarak **5 negatif kontrol sorgusu** seçilir: galeride açık veya kısmi ilgili görseli olmadığı bütün adaylar incelenerek doğrulanır. Bunlar pozitif kalite ortalamasına karıştırılmaz. Toplam sorgu sayısı 45'tir.

Örnek sorgu biçimleri: `kırmızı bir araba`, `sahilde gün batımı`, `çimenlerde koşan köpek`, `masanın üzerinde bir fincan`. Bunlar hazır test verisi veya galeride bulunduğu doğrulanmış sorgu değildir; gerçek sorgular galeri görüldükten sonra ve model sonuçları görülmeden yazılır.

Türkçenin karakterleri ve ekleri doğal biçimde korunur. Eş anlamlı/parafraz sorgular aynı anlam grubunda tutulur; aynı grubun geliştirme ve test ayrımlarına sızması önlenir. Galeri iki ayrımda ortak olabilir; model/ayar seçimi yalnız geliştirme sorgularına dayanır.

Sorgular, etiketler ve ayrım dosyaları nihai deney öncesi dondurulur. Test sonuçlarına bakarak sorgu silmek, etiket değiştirmek veya ön işlem ayarı seçmek yeni revision ve tekrar ölçüm gerektirir; değişiklik gerekçesi raporlanır.

## 5. İlgililik etiketleme protokolü

Her pozitif sorgu için 200 fotoğrafın tamamı incelenir: **40 x 200 = 8.000 sorgu-fotoğraf etiketi**. Negatif kontrollerin 1.000 çifti de doğrulanır.

| Derece | Anlam | Örnek yorum |
|---|---|---|
| 2 | Sorgudaki temel nesne, özellik ve ilişki açıkça karşılanıyor | `kırmızı araba` için görünür kırmızı araba |
| 1 | Kısmi eşleşme var; bir temel özellik eksik veya belirsiz | Araba var, kırmızı olduğu doğrulanamıyor |
| 0 | Sorgu karşılanmıyor | İstenen nesne/sahne yok |

Binary Precision/Recall/Hit ölçümlerinde yalnız **derece 2** ilgili sayılır. nDCG, 0/1/2 derecelerinin tamamını kullanır. Bir sorgunun yalnız derece 1 eşleşmesi varsa pozitif sorgu koşulunu karşılamaz; model sonuçları açılmadan uygun biçimde yeniden yazılır. Negatif kontrollerde hem derece 1 hem derece 2 eşleşme bulunmamalıdır.

Etiketler model skorları ve sıralamaları gösterilmeden hazırlanır. İlk geçişten sonra çiftlerin en az %10'u önceki etiket görülmeden yeniden gözden geçirilir; uyuşmazlıklar ve gerekçeleri kaydedilir. Tek değerlendirici kullanılırsa bu sınırlama belirtilir; kendini tekrar kontrol etme, bağımsız değerlendiriciler arası uyum olarak sunulmaz.

## 6. Karşılaştırılacak deneyler

| Kod | Yöntem | Amacı |
|---|---|---|
| B0 | Galeri için önceden sabitlenmiş Türkçe açıklama/etiket token'ları üzerinde Jaccard sözcük eşleşmesi | Basit etiket/sözcük tabanlı aramaya karşı anlam temelli aramanın davranışını görmek |
| B1 | Seçilen çok dilli CLIP'in Python FP32 referansı, doğru pooling/Dense ve görsel projeksiyonuyla | Doğru model akışının referans sonuçları |
| B2 | Aynı model çiftinin Android/ONNX sürümü; seçilen gerçek ağırlık ve tokenizer özetleriyle | Uygulamanın sayısal uyumunu, kaliteyi ve cihaz maliyetini görmek |

B0'daki açıklamalar sorgulara bakılarak yazılmaz; varsa XM3600 Türkçe açıklamaları kullanılır. Unicode NFC ve Türkçe küçük harf dönüşümünden sonra harf/rakam token kümeleri çıkarılır; Jaccard = kesişim/ birleşim. Boş birleşim skoru 0'dır. Bu baseline'ın Türkçe metin metadatasına ihtiyaç duyduğu ve uygulamaya yeni arama özelliği olarak eklenmediği açıklanır.

B1/B2 için sorgu, galeri, model kimliği, veri revision'ı ve etiketler aynıdır. B2 nicemlenmiş metin gövdesi kullanıyorsa fark ayrıca belirtilir. Sonuçlar iki farklı modelden gelen fotoğraf/metin vektörleri karıştırılarak üretilmez.

Eşit skorda kalıcı `image_id` sırası kullanılır. URI yeniden oluşturulması sıralamayı değiştirmeyecek şekilde sonuç kimliklerine eşlenir. Eksik veya başarısız kodlanan fotoğraflar sessizce atılıp galeri küçültülmez; kapsama oranı ve hatalar raporlanır, tam kapsamlı kalite deneyi tamamlanmadan ana sonuç ilan edilmez.

## 7. Arama kalitesi ölçümleri

`R_q`, ilgili sorgu için derece 2 olan bütün fotoğrafların kümesidir. `TopK_q`, tekrar etmeyen ilk K sonuçtur.

- **Recall@K:** `|TopK_q ∩ R_q| / |R_q|`; K = 5 ve 10.
- **Precision@5:** `|Top5_q ∩ R_q| / 5`. Beşten az sonuç varsa eksik konumlar ilgisiz sayılır.
- **Hit@K:** İlk K sonuçta en az bir derece 2 fotoğraf varsa 1, yoksa 0; K = 5 ve 10. Bu ölçüm Recall@K ile aynı değildir.
- **DCG@10:** `sum((2^grade_i - 1) / log2(i + 1))`, i = 1..10.
- **nDCG@10:** `DCG@10 / IDCG@10`; IDCG, galerinin bütün etiketleri ideal sıraya konularak hesaplanır. Yalnız dönen sonuçları yeniden sıralamak IDCG hesabı değildir.

Pozitif test grubundaki 30 sorgunun aritmetik ortalaması ana değerdir; her kategorinin 6 sorguluk ortalaması ve sorgu bazlı sonuçları da saklanır. Geliştirme ve test sonuçları karıştırılmaz.

Negatif kontrollerde `R_q` boş olduğundan Recall/nDCG ortalaması hesaplanmaz. Dönen en yüksek skor ve davranış ayrı kaydedilir. Mevcut top-k çekirdeğinde anlamsal sonuç reddetme eşiği yoktur; ilgisiz sorguya boş sonuç döndürüldüğü iddia edilmeyecek. Böyle bir özellik eklenirse ayrı karar ve geliştirme grubunda kalibrasyon gerekir.

Tek ilgili görsel varsa Recall ve Hit aynı sayıyı alabilir; çoklu ilgili görsellerde bu eşitlik beklenmez. Kalite yüzdesi için PDF'de zorunlu bir eşik verilmediğinden ölçülmemiş başarı hedefi sağlanmış gibi yazılmaz. Eşik/ayar sonradan sonuçlara göre değiştirilmez.

## 8. Sayısal uyum ve işlev kontrolleri

- **A05:** Sabit opak RGB görüntüde referans ve Android girdi tensörü; aynı tensörde model çıkışı; boyut, sonlu değer, norm ve vektör farkı karşılaştırılır. EXIF, saydamlık ve küçük/büyük görüntü örnekleri ayrıca ele alınır.
- **A06:** Boş metin dahil tokenizer birim örnekleri, Türkçe karakterler, ekler, Unicode/boşluk ve uzun sorgu için token kimlikleri referansla tam karşılaştırılır. Masked mean pooling ve Dense projeksiyonu dahil son 512 boyutlu vektör doğrulanır. Boş sorgunun arama API'si tarafından reddedilmesi ayrı kontroldür.
- **A08-A11:** Float32 saklama round-trip, model revision ayrımı, tek fotoğraf kaydı, değişmeyeni atlama, değişeni yeniden işleme, bozuk fotoğraf, iptal/yeniden başlama, çift iş başlatmama.
- **A12-A13:** Sadece güncel model ve güncel izin kapsamındaki sonuçlar; izin kaldırma/daraltma, silme ve değiştirme; boş galeri; sonuç ve önizleme hataları.
- **Ön indeksleme kanıtı:** Model yükleme/kodlama çağrıları sayılır. Tekrarlanan sorgularda görsel encoder çağrı sayısı 0; değişmeyen galeri yenilemesinde yeniden görsel kodlama 0 olmalıdır. Bir fotoğraf değiştiğinde yalnız ilgili kayıt güncellenmelidir.

Tamamen aynı nicemlenmiş ağırlıkla runtime uyumu ile FP32 -> INT8 nicemleme farkı ayrı karşılaştırmalardır. Sayısal toleranslar A04.2'nin [etkin model sözleşmesinde](MODEL_CONTRACT.md) sabittir; gerçek model/Android karşılaştırması geçmeden A05/A06 tamamlanmaz. Önceki MobileCLIP sözleşmesi yeni model için kullanılmaz.

## 9. Cihaz performansı protokolü

Ortam kaydı: cihaz/emülatör ayrımı, Android API, ABI, RAM, uygulama sürümü, runtime sürümü, execution provider, thread ayarları, model/tokenizer SHA-256, veri revision'ı. Emülatör ölçümleri fiziksel telefon performansı olarak sunulmaz.

| İşlem | Tekrar / kapsam | Kaydedilecek değer |
|---|---|---|
| Model edinimi | Ayrı işlem; ağ varsa ayrıca | İndirilen baytlar, süre ve doğrulama; sorgu süresine dahil edilmez |
| Soğuk model yükleme | Uygulama işlemi yeniden başlatılarak 3 tekrar | Oturum hazır olana kadar geçen süre ve uygulama PSS |
| İlk indeksleme | Sabit 200 fotoğraf; temiz anlamsal indeks | Toplam süre, fotoğraf başına süre, başarılı/başarısız sayı, depolama ve gözlenen tepe PSS |
| Değişmeyen galeri | Aynı indeks üzerinde 3 yenileme | Süre ve yeniden kodlanan fotoğraf sayısı |
| Tek fotoğraf ekle/değiştir/sil | İşlemler ayrı kurulumlarla | İlgili kayıt/vektör değişikliği, süre ve yeniden kodlanan sayı |
| Sıcak sorgu | 5 geliştirme sorgusuyla ısınma; 30 test sorgusu x 3 tekrar | Toplam 90 örnekte metin kodlama, vektör okuma, sıralama ve uçtan uca süre |

Sorgu sırası sabit tohumla karıştırılır; çıktı saklanır. `p50` ve `p95`, sıralanmış sürelerde nearest-rank (`ceil(p*n)`) yöntemiyle hesaplanır; ortalama da verilir. Model yükleme, saf sıralama ve kullanıcının sonuçları görmesine kadar süre ayrı tutulur; tek bir "arama süresi" ile karıştırılmaz.

PSS KiB olarak kaydedilir; Python/model dosya boyutu Android RAM yerine kullanılmaz. Tepe bellek, örnekleme aralığı da kaydedilen gözlenmiş en büyük PSS'dir; tam anlık tepe olduğu ileri sürülmez. Pil/ağ durumu ve termal uyarılar not edilir; gerçek enerji ölçümü yapılmadan enerji tasarrufu yüzdesi yazılmaz.

## 10. Deney kayıtlarının biçimi

Veri dosyaları A14/A15'te üretilecek; aşağıdakiler gerçek sonuç dosyaları değildir:

- `images.jsonl`: image_id, source_id/url, sha256, width/height, attribution/license, duplicate_group, local_uri_mapping.
- `queries.jsonl`: query_id, Turkish text, category, semantic_group, split, positive/control türü.
- `qrels.tsv`: query_id, image_id, grade; pozitif sorgular için eksiksiz 8.000 çift ve negatifler için 1.000 doğrulama.
- `runs.jsonl`: method_id, model_revision, query_id, image_id, rank, similarity; kullanılan bütün çıktı kimlikleri galeride bulunmalı.
- `timings.jsonl`: phase, repetition, query_id/image_id, duration_ms, device/runtime koşulları.
- `summary.json`: veri/model kimlikleri, kapsam, kalite ölçümleri, p50/p95, bellek/depolama ve hatalar.

Dosya özetleri ve sabit kimlikler yeniden üretilebilirliğin dayanağıdır. Fotoğrafların edinilme veya rapor yazılma zamanı, deney kimliği yerine kullanılmaz.

## 11. Tamamlanma ve sonraki görev

A04.1'in çıktısı bu protokol ve onun makine tarafından okunabilir ayarlarıdır. Veri setinin indirilmesi/etiketlenmesi veya modelin uygulanması bu görevin tamamlandığı iddiasına dahil değildir.

A04.2, seçilen standart çok dilli CLIP çiftinin kaynak sözleşmesini ve doğrulama toleranslarını tamamladı. Sıradaki atomik görev **A05: tek görselin ONNX vektörünü üretmek ve referansla karşılaştırmak**. Gerçek model dosyası/çıktı doğrulaması bu sonraki görevdedir.

## Kaynaklar

1. [XM3600 proje sayfası ve indirme/atıf koşulları](https://google.github.io/crossmodal-3600/).
2. [XM3600 birincil makalesi](https://arxiv.org/html/2205.12522v2), dil listesi, görsel sayısı ve açıklama protokolü.
3. [Flickr30k kaynak proje ve kullanım koşulları](https://shannon.cs.illinois.edu/DenotationGraph/).
4. [Apache Spark sıralama değerlendirme ölçümleri](https://spark.apache.org/docs/3.5.6/mllib-evaluation-metrics.html). Bu planın dereceli nDCG kazancı yukarıda açıkça tanımlıdır; bir kütüphanenin varsayılan binary kazancına bırakılmaz.
5. [Seçilen modelin kaynakları ve bileşen sınırları](A04_MODEL_REFERENCES.json).
