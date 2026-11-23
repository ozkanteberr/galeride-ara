# Ödev koşulları ve rapor planı

İnceleme tarihi: 23 Kasım 2026. Kaynak: kullanıcının verdiği üç sayfalık `Proje_Mobil_26.pdf`. Kullanıcı, PicQuery'nin yalnız bir referans olduğunu açıkça belirtti.

Kaynak SHA-256: `3b2635e023e39db70429bc2a46526ce61b9523f13c4b6a145dcd8731520acb1a`.

Planlama hedefi: **27 Aralık 2026**. Kullanıcı bu tarihe göre plan yapılabileceğini belirtti; tarih PDF'de yazmıyor ve kesinleşmiş ders teslim tarihi olarak sunulmayacak.

## 1. PDF'nin istediği teslim ve kapsam

Belgenin başlığı **Türkçe İstem Kullanarak Galeride Fotoğraf Arama - Mobil Programlama Dersi Dönem Projesi Ön Raporu**. Kullanıcının isteği doğrultusunda raporun son yazımı geliştirme sonunda yapılacak; yöntem gerekçeleri, kaynaklar ve deney kayıtları geliştirme boyunca toplanacak.

PDF bir ön rapor şablonudur: planlanan yöntemler ve düzenlenecek deneyler de anlatılmalıdır. Henüz yapılmamış deneyler gerçekleştirilmiş gibi, aday modeller kesin karar gibi veya kaynaklardan alınan sonuçlar GaleriAra ölçümü gibi yazılmayacaktır.

PicQuery, PDF 1.1.1'de incelenebilecek bir bağlantı olarak verilir. Aynı modelin, tokenizer'ın, çeviri yönteminin veya programlama çatısının kullanılması şart koşulmaz. A03, önceki MobileCLIP2-S0 adayının kaynak incelemesi olarak korunur. Kullanıcı A04'te standart çok dilli CLIP metin kodlayıcısı ve eşleşen CLIP ViT-B/32 görsel kodlayıcısını seçti; çeviri katmanı eklenmeyecek.

## 2. Gereksinim ve kanıt eşlemesi

| PDF bölümü | İstenen içerik | Projede hazırlayacağımız kanıt / mevcut durum |
|---|---|---|
| 1. Algoritmalar ve yazılım mimarileri - 50 puan | Planlanan algoritma ve mimarilerin açıklanması | Algoritma akışı, gerekçeli yöntem seçimi, mimari ve çizimler; arama çekirdeği mevcut, model entegrasyonu henüz yok |
| 1.1.1 | Literatürdeki diğer yöntemler ve adayların belirlenmesi | Kaynaklı aday karşılaştırması; Türkçe doğrudan kodlama ve çeviri yaklaşımı birlikte değerlendirilecek. Karşılaştırma yöntemi olarak etiket tabanlı arama ayrıca araştırılabilir |
| 1.1.2 | Kullanımı değerlendirilecek veri setlerinin açıklanması ve seçilme nedenleri | Veri seti aday tablosu: kaynak, lisans/izin, Türkçe sorgu uygunluğu, boyut, sınırlamalar ve seçim gerekçesi. Tüm adayların kullanılması gerekmez |
| 1.1.3 | Test/geçerleme deneyleri ve performans ölçüm yöntemleri | Sorgu ve ilgili fotoğraf etiketleme planı; arama kalitesi ve cihaz maliyeti ayrı ölçülecek. Galeri izin kontrolü tek başına arama başarımı deneyi değildir |
| 1.2.1 | Test ve kullanım süreçlerini gösteren kullanım senaryosu (use case) çizeneği | Kullanıcı, izin verme, indeksleme başlatma/güncelleme, Türkçe sorgulama, sonuç/önizleme ve hata durumları; test/geçerleme akışı ayrıca görünür olacak. Henüz hazırlanmadı |
| 1.2.2 | Kullanılan teknolojileri içeren bileşen (component) çizeneği | Compose, ViewModel, MediaStore, Room, core, seçilen model çalıştırıcısı ve planlanan WorkManager sınırları. Mevcut akış şeması, kendi başına tamamlanmış UML bileşen çizeneği sayılmayacak |
| 2. Özgünlük - 30 puan | Yöntemin özgünlüğe katkısının açıklanması | Aşağıdaki ön indeksleme ve arama zamanı bağımlılık koşullarının uygulama akışı ve deneylerle gösterilmesi |
| 2.1(a) | Fotoğrafların önceden analiz edilmesi ve Türkçe istemlerle eşleşebilen indeks oluşturulması | Önceden üretilmiş kalıcı fotoğraf vektörleri ve Türkçe sorgularla çalışan arama. Room fotoğraf kataloğu mevcut; anlamsal indeks henüz üretilmiyor |
| 2.1(b) | Arama sırasında ML Kit gibi araçların dinamik kullanımına ihtiyaç duyulmaması | Arama yolunun fotoğrafları tekrar analiz etmediği; sorgu kodlama ve kayıtlı vektör sıralaması kullandığı gösterilecek. Mevcut SearchPhotos tasarımı bunu hedefliyor; uçtan uca doğrulama henüz yok |
| 3. Proje Takvimi - 10 puan | Kilometre taşlarının açıklaması ve tarihleri | Aşağıdaki 27 Aralık hedefli çalışma takvimi; tamamlanan ve planlanan tarihler ayrı tutulacak |
| 4. Referanslar - 10 puan | Rapordaki alıntı ve kaynaklara numaralı atıf | Metin içi [1], [2] biçiminde atıflar ve bunlarla eşleşen kaynakça; model/dataset lisansları ile alınan algoritmaların kaynakları da izlenecek |

## 3. Zorunluluk olmayan teknik tercihler ve yorum sınırı

- PDF belirli bir model ailesi, çeviri kullanımı, ONNX, Room, Compose veya Kotlin sürümü belirtmez. Bunlar projede gerekçelendirilecek teknik tercihlerdir.
- Doğrudan Türkçe destekli uyumlu görsel-metin model çifti kullanmak PDF'ye göre dışlanmış değildir. Modelin destek beyanı, GaleriAra'daki Türkçe arama başarımının ölçüldüğü anlamına gelmez.
- İngilizceye çeviri bir ödev koşulu değildir. A04 yalnız çeviri modeli seçmeye daraltılmayacaktır.
- PDF, 2.1(b)'deki ML Kit ifadesinin bütün arama zamanı işlemleri için kapsamını ayrıca tanımlamıyor. Bu ifade sessizce yalnız görsel analizle sınırlanıp arama anında ML Kit çevirisinin uygun olduğu sonucu çıkarılmayacak. Bu tür bir yardımcı araca dinamik bağımlılık kurmayan adaylar değerlendirilecek; gerektiğinde yorum belirsizliği açıkça belirtilecek.
- Metin vektörünün sorgu anında üretilmesi ile her aramada galerideki fotoğrafların yeniden analiz edilmesi ayrı işlemlerdir. Projenin hedefi fotoğraf analizini indeksleme aşamasına taşımaktır.
- Tam çevrimdışı çalışma ve fotoğrafları sunucuya göndermeme mevcut proje hedefleridir; PDF'de ayrı bir ağ şartı olarak yazılmıyor. İlk model edinimi ile model kurulduktan sonraki kullanım ayrı açıklanacaktır.
- 200-500 fotoğraf, 30 Türkçe sorgu veya belirli bir başarı yüzdesi PDF zorunluluğu değildir. Bunlar deney planında kararlaştırılabilecek önerilerdir.
- Sıfırdan model eğitmek zorunlu değildir; hazır model kullanımı ve yeniden kullanım gerekçelendirilebilir. Özgünlük iddiası yeni bir model eğitildiğine dayandırılmayacaktır.

## 4. Teknik kararları verirken kullanılacak ölçütler

Model/sorgu yaklaşımı için üç alternatif, yararları, sınırlamaları ve öneri kullanıcıya sunulacak; kullanıcı seçmeden mimari değişiklik yapılmayacaktır. Kriterler:

1. Türkçe desteğinin kaynağı, modelin görsel/metin çiftinin uyumu ve Türkçe arama kalitesi.
2. Arama sırasında ek çeviri veya servis bağımlılığı; PDF 2.1(b) ile ilişkinin açık gerekçesi.
3. Android'de model boyutu, yükleme süresi, bellek, fotoğraf başına indeksleme süresi ve sorgu gecikmesi.
4. Lisans, model/tokenizer kaynağı, sürüm sabitleme ve sayısal doğrulanabilirlik.
5. 27 Aralık planlama hedefine kadar uygulanabilir geliştirme kapsamı.

Bu belge nihai model seçmez. Çok dilli metin kodlayıcısı farklı bir görsel kodlayıcıya hizalanmışsa, yalnız vektör boyutu eşit diye MobileCLIP2-S0 ile birleştirilmeyecektir.

## 5. Deney planında erken netleştirilecekler

- **Veri seti adayları:** İzinli/kontrollü yerel fotoğraf seti ve araştırmada bulunacak kamuya açık görsel-metin veri setleri; Türkçe sorgu/etiket üretme maliyeti ve veri kullanım koşulları karşılaştırılacak. Ana değerlendirme kaynağı XM3600 olarak planlandı; gerçek veri edinimi ve ilgili-fotoğraf etiketlemesi henüz yapılmadı.
- **Sorgular:** Nesne, sahne, renk ve ilişki gibi kategorilerden Türkçe sorgular; fotoğraflarla ilgili sonuçlar elle etiketlenecek. Aynı örnekler adayları karşılaştırmada korunacak.
- **Kalite:** Mimari belgesindeki Recall@5/10, Precision@5 ve nDCG@10 önerileri deney planında kesinleştirilecek. Eşikler sonuçlar görüldükten sonra seçilmeyecek.
- **Cihaz maliyeti:** Model yükleme, ilk indeksleme, artımlı güncelleme, sorgu kodlama ve sıralama süreleri ayrı tutulacak; bellek ve indeks boyutu ölçülecek.
- **İşlev:** İzin değişmesi, silinen/değişen fotoğraf, bozuk dosya, iptal/yeniden başlama, boş sorgu ve boş sonuç senaryoları ele alınacak.
- **Özgünlük koşulu:** Tekrarlanan aramalarda fotoğraf kodlayıcısının yeniden çalışmadığı kayıtla gösterilecek. Yalnız mimari şema bu davranışın çalıştığını kanıtlamaz.

Veri seti adayları ve deney planı [A04.1 protokolünde](EVALUATION_PLAN.md) belgelendi; gerçek değerlendirme seti A14'te hazırlanacak, ölçümler A15'te yapılacak.

## 6. Son raporun yapısı ve sayfa bütçesi

PDF'nin bölüm numaraları korunacak. Aşağıdaki bütçe bir çalışma önerisidir; biçimlendirilmiş rapor çıktı olarak üretildiğinde gerçek sayfa sayısı kontrol edilecektir.

| Bölüm | İçerik | Yaklaşık sayfa bütçesi |
|---|---|---|
| Kapak | Üniversite/bölüm, proje ve ders adı, ön rapor, öğrenci numarası/ad-soyad/e-posta | Sayfa sınırının dışında |
| İçindekiler | Bölümler ve doğru sayfa numaraları | Sayfa sınırının dışında |
| 1.1.1 | Problem, literatür, aday karşılaştırması, seçilen yöntemin gerekçesi | 2 |
| 1.1.2 | Veri seti adayları ve seçilme/kullanılmama gerekçeleri | 1 |
| 1.1.3 | Test, geçerleme, deney ve performans ölçüm planı | 1,5 |
| 1.2.1 | Use case çizeneği ve kısa açıklaması | 0,75 |
| 1.2.2 | Teknolojileri içeren component çizeneği ve kısa açıklaması | 0,75 |
| 2 / 2.1 | Özgünlük katkısı, ön indeksleme ve arama zamanı davranışı | 1,5 |
| 3 / 3.1 | Tarihli kilometre taşları | 0,5 |
| 4 | Numaralı kaynakça | 1 |

Toplam öneri: **9 sayfa**, yerleşim ve açıklamalar için 1 sayfa pay. PDF yalnız kapak ve içindekileri sınır dışında bıraktığından kaynakça dahil kalan içerik **10 sayfayı geçmeyecek**.

Biçim koşulları: akademik rapor dili, kapak, içindekiler, referanslar, ardışık sayfa numaraları; **Times New Roman, 12 punto, tek satır aralığı**. Rapor PDF'sinin sayfa sayısı, çizim okunurluğu ve metin içi atıf-kaynakça eşleşmesi teslim öncesi kontrol edilecek. Öğrenci kimlik bilgileri rapor yazımında kullanıcıdan alınacak.

## 7. Tarihli çalışma takvimi

Başlangıç noktası: A01/A01.1/A02/A03 tamamlanmış kayıtlar; A04 ve sonraki işler planlanıyor. Aşağıdaki aralıklar tahmindir, gerçekleşmiş tarihler değildir. Görevler yine tek tek yürütülür; bir aralıkta birden çok görev bulunması toplu uygulama onayı sayılmaz.

| Hedef aralık | Kilometre taşı | İlgili görevler ve kapanış kanıtı |
|---|---|---|
| 23-26 Kasım 2026 | Türkçe sorgu yaklaşımı ve değerlendirme planı | A04 ve A04.1; kullanıcı kararı, kaynaklı aday tablosu, veri seti adayları ve deney planı |
| 27 Kasım-3 Aralık 2026 | Tek görsel ve tek sorgu için doğrulanmış model akışı | A05-A07; referansla token/vektör karşılaştırması ve seçilen Türkçe sorgu akışı |
| 4-10 Aralık 2026 | Kalıcı ve artımlı fotoğraf indeksi | A08-A11; vektör kaydı, tek fotoğraf, değişiklik takibi, iptal/yeniden başlama kayıtları |
| 11-16 Aralık 2026 | Uçtan uca Türkçe arama | A12-A13; güncel/erişilebilir fotoğraflar, sonuçlar, hata/boş durumlar |
| 17-21 Aralık 2026 | Değerlendirme seti ve deneyler | A14-A15; etiketli sorgular, kalite ve cihaz ölçümleri |
| 22-25 Aralık 2026 | Raporun son yazımı | A16; PDF bölüm yapısı, iki çizim, kaynakça ve biçim koşulları |
| 26 Aralık 2026 | Son kontrol ve düzeltme payı | İçerik tutarlılığı, en çok 10 sayfa, atıflar ve PDF görsel kontrolü |
| 27 Aralık 2026 | Planlama hedefi / teslim payı | Kullanıcı tarafından önerilen hedef; resmî teslim tarihi teyidi ayrı tutulacak |

## 8. Rapor için geliştirme boyunca tutulacak kayıtlar

Her atomik görevin sonunda ilgili PDF bölümüne katkısı ve elde edilen kanıt kaydedilecek. Model/sürüm seçimi için aday tablosu, karar gerekçesi ve kaynak; uygulama için ilgili bileşen ve veri akışı; test için ortam, girdi, beklenen/gözlenen davranış; deney için veri kümesi, sorgu seti, ölçüm yöntemi ve ham sonuç tutulacak.

Tamamlanan işler, planlanan işler ve araştırma kaynaklarının bildirdiği sonuçlar ayrı ifade edilecek. PicQuery ve hazır modellerin katkısı açıkça kaynaklandırılacak; GaleriAra'nın katkısı Türkçe sorgu yaklaşımı, mobil entegrasyon, ön indeksleme ve bunların doğrulanması üzerinden somutlaştırılacak. Bunlar uygulanıp ölçülmeden başarı iddiası yapılmayacak.

## 9. Bu incelemenin sonucu

PDF'nin üç sayfası metin çıkarımı ve görsel incelemeyle kontrol edildi. Rapor koşulları, modelden bağımsız ödev hedefleri ve kullanıcı tarafından önerilen takvim kaydedildi. Güncel karar: A04 kullanıcı seçimiyle tamamlandı; standart çok dilli CLIP çifti seçildi ve çeviri katmanı eklenmeyecek. A04.1 değerlendirme protokolü belgelendi; uygulama koduna henüz model bağlantısı eklenmedi.
