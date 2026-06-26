@echo off
title Frontend - Next.js + Apache HTTPS
setlocal

REM === Lay duong dan tuyet doi tu vi tri file bat nay ===
REM %~dp0 = thu muc chua file bat (tu dong, ai dat o dau cung duoc)
set "PROJECT_DIR=%~dp0"
set "APACHE_EXE=%PROJECT_DIR%web\Apache24\bin\httpd.exe"
set "FRONTEND_DIR=%PROJECT_DIR%frontend"

echo ========================================
echo  VaultCommerce - Khoi dong He thong
echo  Thu muc du an: %PROJECT_DIR%
echo ========================================

REM === Kiem tra va khoi dong Apache (neu co) ===
if exist "%APACHE_EXE%" (
    echo.
    echo [Apache] Tim thay Apache tai: %APACHE_EXE%
    tasklist /fi "imagename eq httpd.exe" 2>NUL | find /i "httpd.exe" >NUL
    if errorlevel 1 (
        echo [Apache] Dang khoi dong Apache HTTPS...
        start "Apache HTTPS" /min "%APACHE_EXE%"
        timeout /t 2 /nobreak >NUL
        echo [Apache] Da khoi dong! Truy cap: https://tienthienvienman.site.je
    ) else (
        echo [Apache] Apache da dang chay san roi.
    )
) else (
    echo [Apache] Khong tim thay Apache, bo qua.
    echo [Apache] Ban co the truy cap qua: http://localhost:3000
)

REM === Khoi dong Next.js Frontend ===
echo.
echo [Next.js] Dang khoi dong Frontend...
echo [Next.js] Truy cap tai: http://localhost:3000
echo.
if not exist "%FRONTEND_DIR%" (
    echo [LOI] Khong tim thay thu muc frontend: %FRONTEND_DIR%
    pause
    exit /b 1
)

cd /d "%FRONTEND_DIR%"
call npm run dev
