# GaleriAra

Kotlin ile geliştirilen Android dönem projesi: Türkçe doğal dil sorgularıyla yerel fotoğraf arama.

## Mevcut durum

Başlangıç iskeleti, Compose galeri ekranı, MediaStore erişimi, Room katalog şeması ve saf Kotlin vektör arama çekirdeği eklendi. **Görsel/metin modelleri henüz bağlanmadı; anlamsal arama kullanıma hazır değil.** Galeri taramak, fotoğrafları anlamsal olarak indekslemek anlamına gelmez.

## Çalıştırma

- JDK 17 ve Android SDK 36 gerekir. Gradle 8.13 Wrapper projededir.
- Android Studio'da bu klasörü açın, Gradle JDK olarak JDK 17 seçin.
- SDK konumunu Android Studio ile veya izlenmeyen `local.properties` dosyasında ayarlayın.
- Windows terminali: `./gradlew.bat :core:test :app:assembleDebug :app:lintDebug`
- Android 8.0+ cihazda `app` yapılandırmasını çalıştırın.
- İlk derleme bağımlılıkları indirmek için internet gerektirir.

Windows/JDK 17'de klasör adındaki Türkçe karakterler Gradle 8 test çalıştırıcısını
etkileyebilir. Kaynakları taşımadan yalnız çekirdek derleme çıktılarını ASCII bir
geçici klasöre yönlendirmek için PowerShell'de:

```powershell
./gradlew.bat :core:test "-PcoreBuildDir=$env:TEMP/galeriara-core-build"
./gradlew.bat :app:assembleDebug :app:lintDebug
```

Bu seçenek kullanılırsa çekirdek test raporu belirtilen klasörün
`reports/tests/test/index.html` yolunda oluşur.

## Belgeler

- [Mimari kararları](docs/ARCHITECTURE.md)
- [Atomik görevler](docs/TASKS.md)

## Referans

[PicQuery](https://github.com/greyovo/PicQuery) yaklaşım referansıdır. Kaynak kodu bu projeye kopyalanmadı. İleride kullanılan kod ve model varlıklarının lisansları ayrıca kaydedilecek.
