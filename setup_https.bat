@echo off
:: Kiem tra va yeu cau quyen Administrator (UAC)
NET SESSION >nul 2>&1
if %errorLevel% == 0 (
    goto :admin_granted
) else (
    echo Vui long cho, dang yeu cau quyen Administrator (Run as Admin)...
    powershell -Command "Start-Process '%~dpnx0' -Verb RunAs"
    exit /B
)

:admin_granted
echo ========================================================
echo HOC PHAN: MAT MA UNG DUNG - SETUP HTTPS AUTO SCRIPT
echo ========================================================
echo.

echo [1/3] Dang cap nhat file hosts...
set "hosts_path=%WINDIR%\System32\drivers\etc\hosts"
set "domain=tienthienvienman.site.je"
set "ip=127.0.0.1"

:: Kiem tra xem domain da ton tai trong file hosts chua
findstr /C:"%ip% %domain%" "%hosts_path%" >nul
if %errorlevel% neq 0 (
    echo. >> "%hosts_path%"
    echo %ip% %domain% >> "%hosts_path%"
    echo [OK] Da them %domain% vao file hosts.
) else (
    echo [OK] %domain% da ton tai san trong file hosts.
)

echo.
echo [2/3] Khoi dong lai Apache Server...
set "APACHE_BIN=D:\HKII-2026\MatMaUngDung\project\web\Apache24\bin\httpd.exe"
set "APACHE_CONF=D:\HKII-2026\MatMaUngDung\project\web\Apache24\conf\httpd.conf"

:: Tat cac tien trinh Apache cu (neu co) de tranh loi trung Port
taskkill /F /IM httpd.exe >nul 2>&1

:: Chay Apache doc lap duoi nen (Background)
start "" "%APACHE_BIN%" -f "%APACHE_CONF%"
echo [OK] Apache dang chay ngam tren he thong (kiem tra Task Manager).

echo.
echo [3/3] XONG! HE THONG DA SAN SANG.
echo.
echo ========================================================
echo Vui long mo trinh duyet va vao: https://tienthienvienman.site.je
echo Nhan phim bat ky de thoat...
echo ========================================================
pause >nul
