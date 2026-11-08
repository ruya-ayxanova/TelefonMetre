# Smart Meter 3D

Android ölçüm uygulaması (Kotlin, Jetpack Compose, MVVM). Paket: `com.smartmeter3d`, minSdk 24.

## APK alma
1. Bu klasörü GitHub deposuna push edin.
2. Actions sekmesinde **Android CI** çalışır; bitince **app-debug** artifact'ını indirin.
3. İçindeki `app-debug.apk` dosyasını telefona kurun (bilinmeyen kaynaklara izin gerekir).

Çıktı yolu: `app/build/outputs/apk/debug/app-debug.apk`

## Durum
- [x] Aşama 1: Gradle, tema, TR/FA + RTL, ana ekran, ayarlar
- [x] Aşama 6: GitHub Actions
- [ ] Hesaplama motoru, Room, fotoğraftan ölçüm, ARCore
