@echo off
title VaultCommerce - He Thong Tong Hop
color 0A

:: Kiem tra va tat cac node process dang chay ngam do Next.js de lai tu lan truoc
taskkill /F /IM node.exe >NUL 2>&1

powershell -ExecutionPolicy Bypass -File "%~dp0run-everything.ps1"
