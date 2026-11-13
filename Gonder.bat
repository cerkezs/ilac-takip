@echo off
chcp 65001 > nul
echo ===================================================
echo     İlaç Takip Projesi GitHub'a Gönderiliyor...
echo ===================================================
echo.
git push -u origin main
echo.
echo ===================================================
echo İşlem tamamlandı! Pencereyi kapatabilirsiniz.
echo ===================================================
pause
