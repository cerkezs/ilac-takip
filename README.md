# 💊 İlaç Takip & Hatırlatıcı (Android Native)

Bu uygulama, hayati öneme sahip kardiyovasküler ilaçlarınızı (Kardoritm, Ecoprin Pro, Plavix) asla unutmamanız için geliştirilmiş **Android Native** ilaç hatırlatıcı ve kayıt uygulamasıdır.

---

## 🕒 İlaç Programı ve Alım Şekli

| İlaç Adı | Hatırlatma Saati | Aç / Tok Durumu | Açıklama |
| :--- | :---: | :---: | :--- |
| **Kardoritm** | **10:00** (Sabah) | Aç veya Tok karna | Sabah kardiyak ritim dozu |
| **Ecoprin Pro** | **19:00** (Akşam) | Aç karna | Akşam aç karnına kan sulandırıcı dozu |
| **Plavix** | **20:00** (Akşam) | Tok karna | Akşam yemek sonrası tok karnına dozu |

---

## 🚀 Öne Çıkan Özellikler

1. **📞 Telefon Araması Gibi Çalan Tam Ekran Alarm (Full-Screen Intent):**
   * Telefon kilitli veya uyku modunda olsa dahi ekran otomatik olarak uyanır.
   * Gerçek bir gelen telefon araması gibi zil sesi çalar ve titreşir.
   * Ekranda büyük ve net olarak ilacın adı, saati ve aç/tok talimatı gösterilir.
2. **✅ "İlacı Kullandım" Butonu:**
   * Butona basıldığı an ses susar ve alım tarihi, tam saati (saniyesine kadar) telefonun yerel veritabanına (**Room Database**) kaydedilir.
3. **⏰ "5 Dakika Ertele" Butonu:**
   * O an müsait değilseniz veya yoldaysanız butona basıldığında tam 5 dakika sonraya yeni bir arama alarmı kurar.
4. **🛡️ 60 Saniye Güvenlik Zaman Aşımı:**
   * Telefon uzaktaysa veya duymadıysanız, 60 saniye boyunca herhangi bir butona basılmazsa sistem bunu "alınmadı" kabul eder ve **5 dakika sonra otomatik olarak tekrar arar/çalar**.
5. **📅 Alım Geçmişi & Takip Sekmesi:**
   * Hangi gün, saat tam kaçta hangi ilacın alındığı veya ertelendiği geçmiş sekmesinden kronolojik olarak listelenir.
6. **📱 "Aramayı Dene" Butonu:**
   * Saatleri beklemeden sistemin nasıl çalıştığını hemen görmek için ana ekrandaki herhangi bir ilacın yanında bulunan **"Aramayı Dene"** butonuna basabilirsiniz; 5 saniye sonra arama ekranı tetiklenir.

---

## ⚙️ Android İzinleri ve Kritik Ayarlar

Uygulamanın Android'in katı arka plan pil tasarrufundan etkilenmemesi için uygulama açıldığında şu izinler tek tıkla verilebilir:

1. **Pil Optimizasyonu (Sınırsız / Unrestricted):**
   * Android'in uygulamayı uyku moduna almaması için gereklidir. Ana ekrandaki "Pil İzni" butonuna basarak uygulamayı "Kısıtlamasız" yapabilirsiniz.
2. **Tam Saatinde Alarm İzni (`SCHEDULE_EXACT_ALARM`):**
   * 10:00, 19:00 ve 20:00 alarmlarının 1 dakika bile şaşmadan çalmasını sağlar.
3. **Bildirim & Tam Ekran Arama İzni:**
   * Kilit ekranında arama ekranının açılması için gereklidir.

---

## 📦 Projeyi APK Olarak Derleme ve Telefona Yükleme

### Yöntem 1: Android Studio ile (En Kolay)
1. Bilgisayarınıza [Android Studio](https://developer.android.com/studio) indirin ve kurun.
2. Android Studio'yu açıp **"Open"** diyerek bu klasörü (`d:\Web Tasarım\ilaçlar için program`) seçin.
3. Telefonunuzu USB kablosuyla bilgisayara bağlayın (veya telefonunuza doğrudan **Build > Build Bundle(s) / APK(s) > Build APK(s)** seçeneğiyle `.apk` çıktısı alıp WhatsApp/Telegram/Kablo ile telefonunuza atıp kurun).

### Yöntem 2: GitHub Actions ile (Bilgisayara Hiçbir Şey Kurmadan)
* Proje içine `.github/workflows/build-apk.yml` dosyası eklenmiştir.
* Projeyi GitHub'a push ettiğinizde GitHub otomatik olarak 2 dakika içinde debug APK'yı derler ve "Artifacts" kısmından telefonunuza indirmenize sunar.
