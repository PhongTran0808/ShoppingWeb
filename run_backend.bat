@echo off
title Backend - Spring Boot :8081
echo ========================================
echo  Khoi dong Backend (Spring Boot :8081)
echo  Swagger UI: http://localhost:8081/swagger-ui/index.html
echo ========================================

echo [1/2] Build shared-security...
cd /d "%~dp0backend"
call mvn install -pl shared-security --no-transfer-progress
if errorlevel 1 (
    echo.
    echo [LOI] Build shared-security that bai! Xem log phia tren.
    pause
    exit /b 1
)

echo.
echo [2/2] Khoi dong catalog-service (Spring Boot)...
echo     Cho den khi thay "Started CatalogApplication"...
echo.
cd /d "%~dp0backend\catalog-service"
call mvn spring-boot:run --no-transfer-progress

if errorlevel 1 (
    echo.
    echo [LOI] Spring Boot khoi dong that bai!
    pause
)
