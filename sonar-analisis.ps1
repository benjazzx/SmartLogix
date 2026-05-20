param([string]$Token = $env:SONAR_TOKEN)

$ROOT = $PSScriptRoot

if (-not $Token) {
    Write-Host "[ERROR] Token de SonarQube no configurado." -ForegroundColor Red
    Write-Host "        Configura: `$env:SONAR_TOKEN = 'squ_...'" -ForegroundColor Yellow
    exit 1
}

$env:SONAR_TOKEN = $Token

$services = @(
    @{ dir = "Gateway";    key = "SmartLogix-Gateway" },
    @{ dir = "Users";      key = "SmartLogix-Users" },
    @{ dir = "Rol";        key = "SmartLogix-Rol" },
    @{ dir = "Estado";     key = "SmartLogix-Estado" },
    @{ dir = "Inventario"; key = "SmartLogix-Inventario" },
    @{ dir = "Producto";   key = "SmartLogix-Producto" },
    @{ dir = "Orden";      key = "SmartLogix-Orden" }
)

$failed = @()

Write-Host ""
Write-Host "===== SmartLogix - ANALISIS SONARQUBE =====" -ForegroundColor Cyan
Write-Host "SonarQube: http://localhost:9000" -ForegroundColor Gray

foreach ($svc in $services) {
    $svcPath    = Join-Path $ROOT $svc.dir
    $projectKey = $svc.key
    $svcName    = $svc.dir

    Write-Host ""
    Write-Host "[->] Analizando $svcName (key=$projectKey)..." -ForegroundColor Yellow

    $buildResult = & mvn -f "$svcPath/pom.xml" verify -q 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  FALLO al compilar/testear $svcName" -ForegroundColor Red
        $failed += $svcName
        continue
    }

    $sonarArgs = @(
        "-f", "$svcPath/pom.xml",
        "sonar:sonar",
        "-Dsonar.token=$Token",
        "-Dsonar.host.url=http://localhost:9000",
        "-Dsonar.projectKey=$projectKey",
        "-Dsonar.projectName=SmartLogix - $svcName",
        "-q"
    )
    & mvn @sonarArgs 2>&1 | Out-Null

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] $svcName analizado" -ForegroundColor Green
    } else {
        Write-Host "  [FALLO] $svcName" -ForegroundColor Red
        $failed += $svcName
    }
}

Write-Host ""
Write-Host "===== Resultado =====" -ForegroundColor Cyan
if ($failed.Count -eq 0) {
    Write-Host "Todos los microservicios analizados correctamente." -ForegroundColor Green
} else {
    Write-Host "Fallaron: $($failed -join ', ')" -ForegroundColor Red
}
Write-Host "Dashboard: http://localhost:9000/projects" -ForegroundColor White
