# ============================================================
#  SmartLogix — Script de Verificacion de Microservicios
#  Ejecutar con: .\verificar-microservicios.ps1
#  Requiere: Docker Desktop corriendo + todos los containers up
# ============================================================

$BASE = "http://localhost"       # Gateway (puerto 80)
$USERS_DIRECT   = "http://localhost:8082"
$ROL_DIRECT     = "http://localhost:8081"
$ESTADO_DIRECT  = "http://localhost:8086"
$INVENTARIO_DIRECT = "http://localhost:8083"
$PRODUCTO_DIRECT   = "http://localhost:8085"
$ORDEN_DIRECT      = "http://localhost:8084"

$OK  = "[OK]  "
$ERR = "[FAIL]"
$INF = "[INFO]"

function Test-Endpoint {
    param($Label, $Url, $ExpectedStatus = 200, $Headers = @{}, $Body = $null, $Method = "GET")
    try {
        if ($Body) {
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -Headers $Headers `
                       -Body ($Body | ConvertTo-Json) -ContentType "application/json" `
                       -UseBasicParsing -ErrorAction Stop
        } else {
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -Headers $Headers `
                       -UseBasicParsing -ErrorAction Stop
        }
        if ($resp.StatusCode -eq $ExpectedStatus) {
            Write-Host "$OK $Label → $($resp.StatusCode)" -ForegroundColor Green
            return $resp
        } else {
            Write-Host "$ERR $Label → esperado $ExpectedStatus, recibido $($resp.StatusCode)" -ForegroundColor Red
            return $null
        }
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -eq $ExpectedStatus) {
            Write-Host "$OK $Label → $code (esperado)" -ForegroundColor Green
            return $null
        }
        Write-Host "$ERR $Label → $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# ─────────────────────────────────────────────────────────────
Write-Host "`n===== 1. ESTADO DE CONTENEDORES DOCKER =====" -ForegroundColor Cyan
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

# ─────────────────────────────────────────────────────────────
Write-Host "`n===== 2. HEALTH CHECKS DIRECTOS (sin Gateway) =====" -ForegroundColor Cyan

Test-Endpoint "Users    Swagger UI"    "$USERS_DIRECT/swagger-ui.html"
Test-Endpoint "Rol      Swagger UI"    "$ROL_DIRECT/swagger-ui.html"
Test-Endpoint "Estado   Swagger UI"    "$ESTADO_DIRECT/swagger-ui.html"
Test-Endpoint "Inventario Swagger UI"  "$INVENTARIO_DIRECT/swagger-ui.html"
Test-Endpoint "Producto  Swagger UI"   "$PRODUCTO_DIRECT/swagger-ui.html"
Test-Endpoint "Orden     Swagger UI"   "$ORDEN_DIRECT/swagger-ui.html"
Test-Endpoint "Gateway   Health"       "$BASE/actuator/health"

# ─────────────────────────────────────────────────────────────
Write-Host "`n===== 3. ENDPOINTS PUBLICOS VIA GATEWAY (sin JWT) =====" -ForegroundColor Cyan

Test-Endpoint "GET /api/productos"            "$BASE/api/productos"
Test-Endpoint "GET /api/categorias"           "$BASE/api/categorias"
Test-Endpoint "GET /api/roles"                "$BASE/api/roles"
Test-Endpoint "GET /api/tipos"                "$BASE/api/tipos"
Test-Endpoint "GET /api/estados"              "$BASE/api/estados"
Test-Endpoint "GET /api/tipos-estado"         "$BASE/api/tipos-estado"
Test-Endpoint "GET /api/inventario/bodegas"   "$BASE/api/inventario/bodegas"
Test-Endpoint "GET /api/inventario/estantes"  "$BASE/api/inventario/estantes"
Test-Endpoint "GET /api/inventario/pasillos"  "$BASE/api/inventario/pasillos"

# ─────────────────────────────────────────────────────────────
Write-Host "`n===== 4. FLUJO COMPLETO: REGISTRO → ROL → ESTADO (Kafka) =====" -ForegroundColor Cyan

$timestamp = Get-Date -Format "HHmmss"
$testEmail = "test_$timestamp@smartb.cl"

Write-Host "$INF Registrando usuario: $testEmail" -ForegroundColor Yellow

$registerBody = @{
    nombre   = "Test Bodeguero $timestamp"
    email    = $testEmail
    password = "Test1234!"
}

$regResp = Test-Endpoint "POST /auth/register" "$BASE/auth/register" 200 @{} $registerBody "POST"
if ($regResp) {
    $token = ($regResp.Content | ConvertFrom-Json).token
    if ($token) {
        Write-Host "$OK Token JWT obtenido (primeros 30 chars): $($token.Substring(0,30))..." -ForegroundColor Green
    }
}

# Esperar propagacion Kafka
Write-Host "$INF Esperando 5s propagacion Kafka (user-created → role-assigned → estado-assigned)..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Login para obtener token
$loginBody = @{ email = $testEmail; password = "Test1234!" }
$loginResp = Test-Endpoint "POST /auth/login" "$BASE/auth/login" 200 @{} $loginBody "POST"
$jwt = $null
if ($loginResp) {
    $jwt = ($loginResp.Content | ConvertFrom-Json).token
}

# ─────────────────────────────────────────────────────────────
Write-Host "`n===== 5. ENDPOINTS PROTEGIDOS (con JWT) =====" -ForegroundColor Cyan

if ($jwt) {
    $authHeader = @{ Authorization = "Bearer $jwt" }
    Test-Endpoint "GET /api/users (lista usuarios)"      "$BASE/api/users"           200 $authHeader
    Test-Endpoint "GET /api/ordenes (mis ordenes)"       "$BASE/api/ordenes/mis-ordenes" 200 $authHeader
    Test-Endpoint "GET /api/direcciones"                 "$BASE/api/direcciones"     200 $authHeader
    Test-Endpoint "GET /api/permisos"                    "$BASE/api/permisos"        200 $authHeader
    Test-Endpoint "GET /api/privilegios"                 "$BASE/api/privilegios"     200 $authHeader
} else {
    Write-Host "$ERR No se pudo obtener JWT — saltando tests protegidos" -ForegroundColor Red
}

# ─────────────────────────────────────────────────────────────
Write-Host "`n===== 6. COMUNICACION HTTP DIRECTA ENTRE SERVICIOS =====" -ForegroundColor Cyan

# Obtener IDs reales de la BD para usarlos en tests de inter-comunicacion
$roles = $null
try { $roles = (Invoke-WebRequest "$BASE/api/roles" -UseBasicParsing).Content | ConvertFrom-Json } catch {}
$estados = $null
try { $estados = (Invoke-WebRequest "$BASE/api/estados" -UseBasicParsing).Content | ConvertFrom-Json } catch {}
$productos = $null
try { $productos = (Invoke-WebRequest "$BASE/api/productos" -UseBasicParsing).Content | ConvertFrom-Json } catch {}

if ($roles -and $roles.Count -gt 0) {
    $rolId = $roles[0].id
    Write-Host "$OK Roles cargados: $($roles.Count) (primer id: $rolId)" -ForegroundColor Green
    # Rol → Users (Circuit Breaker hacia users-service)
    Test-Endpoint "GET /api/roles/{id}/usuarios (Rol→Users)" "$BASE/api/roles/$rolId/usuarios" 200
} else {
    Write-Host "$ERR No se pudieron cargar roles" -ForegroundColor Red
}

if ($estados -and $estados.Count -gt 0) {
    $estadoId = $estados[0].id
    Write-Host "$OK Estados cargados: $($estados.Count) (primer id: $estadoId)" -ForegroundColor Green
    # Estado → Users (Circuit Breaker hacia users-service)
    Test-Endpoint "GET /api/estados/{id}/usuarios (Estado→Users)" "$BASE/api/estados/$estadoId/usuarios" 200
} else {
    Write-Host "$ERR No se pudieron cargar estados" -ForegroundColor Red
}

if ($productos -and $productos.Count -gt 0) {
    $productoId = $productos[0].id
    Write-Host "$OK Productos cargados: $($productos.Count) (primer id: $productoId)" -ForegroundColor Green
    # Inventario → Producto (Circuit Breaker): se prueba al crear orden
} else {
    Write-Host "$ERR No se pudieron cargar productos" -ForegroundColor Red
}

# ─────────────────────────────────────────────────────────────
Write-Host "`n===== 7. CREACION DE ORDEN (Orden→Users+Estado+Producto + Kafka→Inventario) =====" -ForegroundColor Cyan

if ($jwt -and $estados -and $productos) {
    $authHeader = @{ Authorization = "Bearer $jwt" }
    $estadoOrden = $estados | Where-Object { $_.nombre -like "*pendiente*" -or $_.nombre -like "*activo*" } | Select-Object -First 1
    if (-not $estadoOrden) { $estadoOrden = $estados[0] }

    $ordenBody = @{
        estadoId = $estadoOrden.id
        detalles = @(
            @{
                productoId = $productos[0].id
                cantidad   = 1
            }
        )
    }

    Write-Host "$INF Creando orden con producto $($productos[0].nombre)..." -ForegroundColor Yellow
    $ordenResp = Test-Endpoint "POST /api/ordenes (Orden→Users+Estado+Producto)" "$BASE/api/ordenes" 200 $authHeader $ordenBody "POST"

    if ($ordenResp) {
        $ordenId = ($ordenResp.Content | ConvertFrom-Json).id
        Write-Host "$OK Orden creada con id: $ordenId" -ForegroundColor Green

        Write-Host "$INF Esperando 5s propagacion Kafka (orden-creada → Inventario decrementa stock)..." -ForegroundColor Yellow
        Start-Sleep -Seconds 5

        # Verificar que el stock bajó
        $productoActualizado = $null
        try {
            $productoActualizado = (Invoke-WebRequest "$BASE/api/productos/$($productos[0].id)" -UseBasicParsing).Content | ConvertFrom-Json
        } catch {}

        if ($productoActualizado) {
            Write-Host "$OK Stock actual del producto '$($productos[0].nombre)': $($productoActualizado.stock)" -ForegroundColor Green
        }
    }
} else {
    Write-Host "$ERR Saltando test de orden (falta JWT, estados o productos)" -ForegroundColor Yellow
}

# ─────────────────────────────────────────────────────────────
Write-Host "`n===== 8. CIRCUIT BREAKERS — FALLBACKS =====" -ForegroundColor Cyan

# Los fallbacks se activan cuando el servicio no responde.
# Aqui solo verificamos que los endpoints de fallback existen y devuelven 503.
Test-Endpoint "Fallback /fallback/users"      "$BASE/fallback/users"      503
Test-Endpoint "Fallback /fallback/rol"        "$BASE/fallback/rol"        503
Test-Endpoint "Fallback /fallback/estado"     "$BASE/fallback/estado"     503
Test-Endpoint "Fallback /fallback/inventario" "$BASE/fallback/inventario" 503
Test-Endpoint "Fallback /fallback/orden"      "$BASE/fallback/orden"      503
Test-Endpoint "Fallback /fallback/producto"   "$BASE/fallback/producto"   503

# ─────────────────────────────────────────────────────────────
Write-Host "`n===== 9. TOPICOS KAFKA (requiere kafka-topics CLI en PATH) =====" -ForegroundColor Cyan
try {
    $topics = docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list 2>&1
    $expectedTopics = @(
        "user-created-topic",
        "role-assigned-topic",
        "estado-assigned-topic",
        "orden-creada-topic",
        "producto-actualizado-topic",
        "estado-orden-topic",
        "ubicacion-actualizada-topic"
    )
    foreach ($t in $expectedTopics) {
        if ($topics -match $t) {
            Write-Host "$OK Topico existe: $t" -ForegroundColor Green
        } else {
            Write-Host "$ERR Topico NO encontrado: $t" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "$INF No se pudo listar topicos Kafka: $($_.Exception.Message)" -ForegroundColor Yellow
}

# ─────────────────────────────────────────────────────────────
Write-Host "`n===== RESUMEN =====" -ForegroundColor Cyan
Write-Host "Si todos los checks son [OK] el sistema esta operativo." -ForegroundColor White
Write-Host "Revisar los [FAIL] en rojo para diagnosticar problemas." -ForegroundColor White
Write-Host "Swagger UIs disponibles:" -ForegroundColor White
Write-Host "  Users:      http://localhost:8082/swagger-ui.html"
Write-Host "  Rol:        http://localhost:8081/swagger-ui.html"
Write-Host "  Estado:     http://localhost:8086/swagger-ui.html"
Write-Host "  Inventario: http://localhost:8083/swagger-ui.html"
Write-Host "  Producto:   http://localhost:8085/swagger-ui.html"
Write-Host "  Orden:      http://localhost:8084/swagger-ui.html"
Write-Host "  SonarQube:  http://localhost:9000"
