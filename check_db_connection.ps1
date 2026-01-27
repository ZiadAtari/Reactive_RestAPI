$ErrorActionPreference = "Stop"

Write-Host "--- ReactiveAPI Connectivity Diagnostic ---" -ForegroundColor Cyan

# 1. Check if Docker is running
Write-Host "`n[1] Checking Docker Service..."
try {
    docker info > $null
    Write-Host "PASS: Docker is running." -ForegroundColor Green
} catch {
    Write-Host "FAIL: Docker is NOT running. Please start Docker Desktop." -ForegroundColor Red
    exit
}

# 2. Check Container Status
Write-Host "`n[2] Checking MySQL Container..."
$container = docker ps --filter "name=mysql" --format "{{.Status}} -- {{.Ports}}"
if ($container) {
    Write-Host "PASS: MySQL Container is running: $container" -ForegroundColor Green
} else {
    Write-Host "FAIL: MySQL Container is NOT running. Run 'docker-compose up -d'." -ForegroundColor Red
}

# 3. Check Port Listening on Host
Write-Host "`n[3] Checking Port 3307 on Host..."
$portCheck = Get-NetTCPConnection -LocalPort 3307 -ErrorAction SilentlyContinue
if ($portCheck) {
    Write-Host "PASS: Something is listening on port 3307 (State: $($portCheck.State))" -ForegroundColor Green
} else {
    Write-Host "FAIL: Nothing is listening on port 3307. Check if docker-compose mapping 3307:3306 is active." -ForegroundColor Red
}

# 4. Check Connection
Write-Host "`n[4] Testing Socket Connection..."
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $connect = $tcp.BeginConnect("localhost", 3307, $null, $null)
    $wait = $connect.AsyncWaitHandle.WaitOne(2000, $false)
    if ($wait -and $tcp.Connected) {
        Write-Host "PASS: Successfully connected to localhost:3307" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Connection to localhost:3307 timed out or refused." -ForegroundColor Red
    }
    $tcp.Close()
} catch {
    Write-Host "FAIL: Could not connect: $_" -ForegroundColor Red
}

Write-Host "`n--- End Diagnostic ---" -ForegroundColor Cyan
