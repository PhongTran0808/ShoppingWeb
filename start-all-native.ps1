# Native Startup Script for Secure E-commerce Platform
# Starts all services on localhost ports using Adoptium JDK 21 and Next.js frontend

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " STARTING ALL E-COMMERCE SERVICES NATIVELY    " -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

$java21Path = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
$mvnPath = "D:\Apache NetBeans\java\maven\bin\mvn.cmd"

# Check ports
$ports = @(8080, 8081, 8082, 8083, 8084, 3000)
foreach ($port in $ports) {
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($conn) {
        Write-Host "WARNING: Port $port is already in use! Ensure no conflicts exist." -ForegroundColor Yellow
    }
}

Write-Host "Starting 5 Spring Boot Microservices in separate windows..." -ForegroundColor Yellow
Start-Process cmd -ArgumentList "/c title Gateway Service (8080) && set JAVA_HOME=$java21Path && cd backend\gateway-service && `"$mvnPath`" spring-boot:run"
Start-Process cmd -ArgumentList "/c title Catalog Service (8081) && set JAVA_HOME=$java21Path && cd backend\catalog-service && `"$mvnPath`" spring-boot:run"
Start-Process cmd -ArgumentList "/c title Cart Service (8082) && set JAVA_HOME=$java21Path && cd backend\cart-service && `"$mvnPath`" spring-boot:run"
Start-Process cmd -ArgumentList "/c title Order Service (8083) && set JAVA_HOME=$java21Path && cd backend\order-service && `"$mvnPath`" spring-boot:run"
Start-Process cmd -ArgumentList "/c title Payment Service (8084) && set JAVA_HOME=$java21Path && cd backend\payment-service && `"$mvnPath`" spring-boot:run"

Write-Host "Starting Next.js Frontend..." -ForegroundColor Yellow
Start-Process cmd -ArgumentList "/c title Frontend (3000) && cd frontend && npm run dev"

Write-Host "==============================================" -ForegroundColor Green
Write-Host "ALL SERVICES LAUNCHED! 6 terminal windows opened." -ForegroundColor Green
Write-Host "Please wait 30-40 seconds for the backend to initialize." -ForegroundColor Green
Write-Host "Open your browser and navigate to: http://localhost:3000" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
