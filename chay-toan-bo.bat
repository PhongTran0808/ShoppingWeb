@echo off
title VaultCommerce - Khoi Dong He Thong
color 0A

echo =================================================================
echo        KHOI DONG TOAN BO HE THONG (BACKEND + FRONTEND)
echo =================================================================
echo.

echo [1/3] Dang mo Backend (5 Microservices + Vault) chay ngam...
start "Backend Services" powershell -NoExit -ExecutionPolicy Bypass -Command "& { .\start-all.ps1 }"

set "APACHE_EXE=%~dp0web\Apache24\bin\httpd.exe"
echo [2/3] Dang khoi dong Apache HTTPS (tienthienvienman.site.je)...
if exist "%APACHE_EXE%" (
    tasklist /fi "imagename eq httpd.exe" 2>NUL | find /i "httpd.exe" >NUL
    if errorlevel 1 (
        start "Apache HTTPS" /min "%APACHE_EXE%"
        timeout /t 2 /nobreak >NUL
    )
) else (
    echo [LOI] Khong tim thay thu muc Apache! Xin loi, chi co the vao bang localhost.
)

echo [3/3] Dang mo Frontend (Next.js)...
echo Vui long doi khoang 30 giay roi vao link: https://tienthienvienman.site.je
echo.
echo De tat he thong: 
echo - Voi Frontend: An Ctrl + C o cua so nay
echo - Voi Backend: Sang cua so Powershell va an ENTER
echo - Voi Apache: TAT bang Task Manager hoac chay script dong.
echo =================================================================
echo.

cd frontend
call npm run dev
