# ============================================================
#  SmartLogix — Rebuild y redeploy de todos los microservicios
#  Uso: .\rebuild-all.ps1
#  Requiere: Docker Desktop corriendo
# ============================================================

$ROOT = $PSScriptRoot
Set-Location $ROOT

Write-Host "`n===== SmartLogix — REBUILD COMPLETO =====" -ForegroundColor Cyan
Write-Host "Directorio: $ROOT" -ForegroundColor Gray

# Compilar todos los microservicios con Maven primero
$services = @("Gateway", "Users", "Rol", "Estado", "Inventario", "Producto", "Orden")

Write-Host "`n[1/3] Compilando microservicios con Maven..." -ForegroundColor Yellow
foreach ($svc in $services) {
    $svcPath = Join-Path $ROOT $svc
    Write-Host "  → mvn package -DskipTests en $svc..." -NoNewline
    $result = & mvn -f "$svcPath/pom.xml" package -DskipTests -q 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host " OK" -ForegroundColor Green
    } else {
        Write-Host " FALLO" -ForegroundColor Red
        Write-Host $result
        Write-Host "Abortando rebuild — corrige el error de compilacion primero." -ForegroundColor Red
        exit 1
    }
}

# Reconstruir imágenes Docker
Write-Host "`n[2/3] Reconstruyendo imagenes Docker (sin cache)..." -ForegroundColor Yellow
docker compose build --no-cache
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error al construir imagenes Docker." -ForegroundColor Red
    exit 1
}

# Reiniciar todos los servicios (excepto kafka que no cambió)
Write-Host "`n[3/3] Reiniciando servicios..." -ForegroundColor Yellow
docker compose up -d --force-recreate `
    gateway-service users-service rol-service estado-service `
    inventario-service producto-service orden-service

Write-Host "`n===== Rebuild completado =====" -ForegroundColor Green
Write-Host "Esperando que los servicios arranquen (healthchecks ~60s)..." -ForegroundColor Gray
Write-Host "Ejecuta .\verificar-microservicios.ps1 para confirmar que todo esta operativo."
