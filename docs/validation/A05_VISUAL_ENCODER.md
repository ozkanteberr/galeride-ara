# A05 — Tek görselin ONNX vektörü

Durum: tamamlandı. Seçilen CLIP ViT-B/32 görsel dalı ve 768 -> 512 projeksiyonu, FP32 opset 17 ONNX olarak üretildi. Sabit checkpoint revision'ı `327ab6726d33c0e22f920c83f2ff9e4bd38ca37f`; kaynak checkpoint ve çıktı dosyası ayrı SHA-256 ile doğrulandı.

## Somut çıktı

- Dosya: `clip_vit_b32_visual_fp32.onnx`, 351610456 bayt.
- SHA-256: `4bd22c1cab0e6b2a725a3d9e729c3a3c0338f5a2ecf34349cbed4c6aeaeb17bd`.
- Girdi: `pixel_values`, Float32 `[batch,3,224,224]`; çıktı: `image_features`, Float32 `[batch,512]`. Görsel projeksiyon dahil; çıktı ham vektördür.
- Checkpoint'teki orijinal İngilizce metin dalı kullanılmadı. Yalnız eski deterministik `position_ids` tamponu beklenen diziyle birebir karşılaştırılarak uyarlanır; diğer ağırlıklar strict yüklenir.
- Model çalışma dizinindedir. APK ve Git model ağırlığı içermez; depoda test fixture'ları ve tekrar üretim araçları bulunur.

## Sayısal karşılaştırma

Kişisel fotoğraf kullanılmadı. Araç deterministik 320×240 opak RGB deseninden PNG üretir. Python gerçek Transformers CLIPImageProcessor ve PyTorch görsel dalını çalıştırır; Kotlin ön işleme aynı PNG'den bağımsız girdi üretir. Android ve masaüstü aynı ONNX özetini kullanır.

| Karşılaştırma | Kosinüs | Göreli L2 | Kabul |
|---|---|---|---|
| Aynı tensör: masaüstü ONNX / PyTorch | 0.999999999998 | 1.78559661e-06 | >=0.99999 ve <=0.001 |
| Aynı tensör: Android / masaüstü ONNX | 1.000000000000 | 0 | >=0.99999 ve <=0.001 |
| Aynı tensör: Android / PyTorch | 0.999999999998 | 1.78559661e-06 | >=0.99999 ve <=0.001 |
| Tam PNG akışı: Android / PyTorch | 0.999999999998 | 1.78559661e-06 | >=0.999 ve <=0.02 |

Ön işleme tensöründe en büyük mutlak fark: `0`. Her kanalda `2/(255*std[c]) + 1e-6` sınırı ayrı ayrı kontrol edildi. Ham vektörler 512 boyutlu, sonlu ve sıfırdan farklıdır. Bu karşılaştırma ortak L2 uygulama sınırını değiştirmez.

## Doğrulama ortamı ve kontroller

- Masaüstü: CPU, PyTorch 2.9.1, Transformers 4.57.1, Pillow 12.0.0, ONNX 1.19.1, ONNX Runtime 1.23.2.
- Android: API 35, `x86_64` emülatör, ONNX Runtime Android 1.23.2 CPU; BASIC graph optimizasyonu, 2 intra-op/1 inter-op thread.
- Çekirdek: 5 test, 0 hata; Android: 1 instrumentation testi başarılı. Debug ve test APK üretildi. Lint: 0 hata, 12 uyarı; başlangıçtaki 9 uyarıya 3 bağımlılık sürüm önerisi eklendi.
- Mevcut emülatörün iç alanı APK kurulumu için yetersizdi. Kullanıcı verisi temizlenmeden ayrı geçici kullanıcı veri dosyası kullanıldı. Emülatör başlatma/güç sorunları sayısal model hatası olarak raporlanmaz.
- [Tekrar üretim komutları](../../tools/README.md), [makine tarafından okunabilir sonuç](A05_VISUAL_ENCODER.json), [etkin sözleşme](../MODEL_CONTRACT.md).

## Kapsam sınırı ve sonraki görev

Yalnız kontrollü opak RGB PNG doğrulandı. URI okuma, EXIF aynalama/yönü, şeffaflık, JPEG/HDR/renk profilleri, çok büyük fotoğraf belleği ve fiziksel ARM cihazı henüz doğrulanmadı. Bitmap yardımcısındaki alfa bileştirme kodu bu testle RGBA doğrulaması kazanmaz. Tek masaüstü çıkarım süresi tekrar, ısınma ve dağılım içermediğinden performans ölçümü olarak kullanılmaz.

Galeri/Room/Worker ve arama bağlantısı yapılmadı. Bütün model çiftinin runtime revision'ı metin ONNX ve Dense varlıklarıyla tamamlanacak. Sıradaki atomik görev A06, tek sorgunun metin vektörüdür. Commit veya push yapılmadı.
