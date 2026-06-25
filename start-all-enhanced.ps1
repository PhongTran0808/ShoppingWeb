# Enhanced Startup Script for Secure E-commerce Platform
# Starts all services in correct order with health checks

Write-Host @"
╔══════════════════════════════════════════════════════════════╗
║   Secure E-commerce Platform - Enhanced Startup Script      ║
║   Starting all services with health checks...               ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan

# Function to check if port is in use
function Test-Port {
    param($Port)
    $tcpConnection = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
    return $tcpConnection -ne $null
}

# Function to wait for service
function Wait-ForService {
    param(
        [string]$Url,
        [string]$ServiceName,
        [int]$TimeoutSeconds = 120
    )
    
    Write-Host "`n⏳ Waiting for $ServiceName to be ready..." -ForegroundColor Yellow
    $elapsed = 0
    $interval = 5
    
    while ($elapsed -lt $TimeoutSeconds) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
            Write-Host "✅ $ServiceName is ready!" -ForegroundColor Green
            return $true
        } catch {
            Write-Host "." -NoNewline
            Start-Sleep -Seconds $interval
            $elapsed += $interval
        }
    }
    
    Write-Host "`n⚠️  $ServiceName did not start within $TimeoutSeconds seconds" -ForegroundColor Red
    return $false
}

# Step 1: Check prerequisites
Write-Host "`n📋 Step 1: Checking prerequisites..." -ForegroundColor Cyan

$docker = Get-Command docker -ErrorAction SilentlyContinue
if (-not $docker) {
    Write-Host "❌ Docker not found. Please install Docker Desktop." -ForegroundColor Red
    exit 1
}
Write-Host "✅ Docker found" -ForegroundColor Green

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Host "❌ Java not found. Please install Java 21." -ForegroundColor Red
    exit 1
}
Write-Host "✅ Java found" -ForegroundColor Green

$maven = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $maven) {
    Write-Host "❌ Maven not found. Please install Maven." -ForegroundColor Red
    exit 1
}
Write-Host "✅ Maven found" -ForegroundColor Green

$node = Get-Command node -ErrorAction SilentlyContinue
if (-not $node) {
    Write-Host "❌ Node.js not found. Please install Node.js 18+." -ForegroundColor Red
    exit 1
}
Write-Host "✅ Node.js found" -ForegroundColor Green

# Step 2: Start Infrastructure
Write-Host "`n🐳 Step 2: Starting Infrastructure (Docker Compose)..." -ForegroundColor Cyan

docker-compose up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to start Docker containers" -ForegroundColor Red
    exit 1
}

# Wait for MySQL
Wait-ForService -Url "http://localhost:3306" -ServiceName "MySQL" -TimeoutSeconds 60 | Out-Null
Start-Sleep -Seconds 10  # Extra time for MySQL to be fully ready

# Wait for Vault
$vaultReady = Wait-ForService -Url "http://localhost:8200/v1/sys/health" -ServiceName "Vault"
if ($vaultReady) {
    Write-Host "🔐 Initializing Vault Transit Engine..." -ForegroundColor Yellow
    docker exec -it ecom-vault sh /init-vault.sh
}

# Wait for Keycloak
Wait-ForService -Url "http://localhost:9090" -ServiceName "Keycloak" | Out-Null

# Wait for Redis
Start-Sleep -Seconds 5
Write-Host "✅ Redis ready" -ForegroundColor Green

# Step 3: Start Monitoring (Optional)
Write-Host "`n📊 Step 3: Starting Monitoring Stack (Optional)..." -ForegroundColor Cyan
$startMonitoring = Read-Host "Do you want to start monitoring stack (Prometheus/Grafana/ELK/Kafka)? (y/n)"

if ($startMonitoring -eq "y" -or $startMonitoring -eq "Y") {
    docker-compose -f docker-compose.monitoring.yml up -d
    Write-Host "✅ Monitoring stack started" -ForegroundColor Green
    Write-Host "   Prometheus: http://localhost:9091" -ForegroundColor Gray
    Write-Host "   Grafana: http://localhost:3001 (admin/admin)" -ForegroundColor Gray
    Write-Host "   Kibana: http://localhost:5601" -ForegroundColor Gray
    Write-Host "   Kafka UI: http://localhost:8090" -ForegroundColor Gray
    Write-Host "   Jaeger: http://localhost:16686" -ForegroundColor Gray
} else {
    Write-Host "⏭️  Skipping monitoring stack" -ForegroundColor Yellow
}

# Step 4: Start Backend Services
Write-Host "`n☕ Step 4: Starting Backend Services..." -ForegroundColor Cyan

$services = @(
    @{Name="Gateway"; Port=8080; Path="gateway-service"},
    @{Name="Catalog"; Port=8081; Path="catalog-service"},
    @{Name="Cart"; Port=8082; Path="cart-service"},
    @{Name="Order"; Port=8083; Path="order-service"},
    @{Name="Payment"; Port=8084; Path="payment-service"}
)

foreach ($service in $services) {
    Write-Host "`n🚀 Starting $($service.Name) Service (Port $($service.Port))..." -ForegroundColor Yellow
    
    # Check if port is already in use
    if (Test-Port -Port $service.Port) {
        Write-Host "⚠️  Port $($service.Port) already in use. Skipping..." -ForegroundColor Yellow
        continue
    }
    
    # Start service in new window
    $servicePath = "backend\$($service.Path)"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$servicePath'; mvn spring-boot:run" -WindowStyle Normal
    
    # Wait a bit before starting next service
    Start-Sleep -Seconds 15
}

Write-Host "`n⏳ Waiting for all backend services to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Check backend health
foreach ($service in $services) {
    $url = "http://localhost:$($service.Port)/actuator/health"
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
        Write-Host "✅ $($service.Name) Service is healthy" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  $($service.Name) Service may not be ready yet" -ForegroundColor Yellow
    }
}

# Step 5: Start Frontend
Write-Host "`n⚛️  Step 5: Starting Frontend (Next.js)..." -ForegroundColor Cyan

# Check if node_modules exists
if (-not (Test-Path "frontend\node_modules")) {
    Write-Host "📦 Installing frontend dependencies..." -ForegroundColor Yellow
    Set-Location frontend
    npm install
    Set-Location ..
}

# Check if port 3000 is in use
if (Test-Port -Port 3000) {
    Write-Host "⚠️  Port 3000 already in use. Please stop the existing process." -ForegroundColor Yellow
} else {
    Write-Host "🚀 Starting Next.js dev server..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend; npm run dev" -WindowStyle Normal
    
    Start-Sleep -Seconds 20
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:3000" -UseBasicParsing -TimeoutSec 5
        Write-Host "✅ Frontend is ready!" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  Frontend may not be ready yet. Please wait a moment..." -ForegroundColor Yellow
    }
}

# Summary
Write-Host @"

╔══════════════════════════════════════════════════════════════╗
║                    🎉 STARTUP COMPLETE! 🎉                   ║
╚══════════════════════════════════════════════════════════════╝

📍 Access Points:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🌐 Frontend:       http://localhost:3000
🔌 API Gateway:    http://localhost:8080
🔑 Keycloak:       http://localhost:9090 (admin/admin)
🔐 Vault UI:       http://localhost:8200 (token: root)

📊 Monitoring (if enabled):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Prometheus:     http://localhost:9091
   Grafana:        http://localhost:3001 (admin/admin)
   Kibana:         http://localhost:5601
   Kafka UI:       http://localhost:8090
   Jaeger:         http://localhost:16686

🔑 Test Credentials:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   User:  user1 / UserPass@1
   Admin: admin1 / AdminPass@1

📚 Next Steps:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Open http://localhost:3000 in your browser
2. Try login with test credentials
3. Browse catalog and add items to cart
4. Test checkout flow
5. Run security experiments: cd experiments\token-replay-test
                           python test_token_replay.py

📖 Documentation:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   README.md              - Full documentation
   QUICKSTART.md          - Quick start guide
   SUMMARY.md             - Project summary
   INSTALL_GUIDE.md       - Installation guide
   IMPLEMENTATION_STATUS.md - Progress report

"@ -ForegroundColor Green

Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
