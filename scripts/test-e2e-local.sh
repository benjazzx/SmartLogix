+#!/usr/bin/env bash
# Pruebas end-to-end del stack local (sin AWS/Cognito, perfil aws-ep1 inactivo).
# Requiere: docker-compose up -d (todos los servicios healthy) y el .env del repo
# con SEED_ADMIN_PASSWORD / SEED_CLIENTE_PASSWORD / SEED_TRANSPORTISTA_PASSWORD
# correspondientes a admin@smartlogix.cl, cliente@smartlogix.cl y
# transportista@smartlogix.cl (cuentas creadas por el DataInitializer de Users).
#
# Uso: ./scripts/test-e2e-local.sh
set -uo pipefail

BASE="${BASE_URL:-http://localhost}"
ENV_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.env"
[ -f "$ENV_FILE" ] && set -a && . "$ENV_FILE" && set +a

PASS=0
FAIL=0
RUN_ID=$(date +%s)

log_pass() { PASS=$((PASS+1)); echo "  OK   $1"; }
log_fail() { FAIL=$((FAIL+1)); echo "  FAIL $1"; }

# assert_status DESCRIPCION STATUS_ESPERADO STATUS_REAL
assert_status() {
  if [ "$3" = "$2" ]; then log_pass "$1 (HTTP $3)"; else log_fail "$1 (esperado $2, obtuvo $3)"; fi
}

json_field() { grep -o "\"$2\":\"[^\"]*\"" <<<"$1" | head -1 | sed -E "s/\"$2\":\"([^\"]*)\"/\1/"; }

echo "=== 1. Autenticación ==="
RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
  -d "{\"correo\":\"admin@smartlogix.cl\",\"clave\":\"${SEED_ADMIN_PASSWORD:-}\"}")
CODE=$(tail -n1 <<<"$RESP"); BODY=$(sed '$d' <<<"$RESP")
assert_status "Login admin" 200 "$CODE"
TOKEN_ADMIN=$(json_field "$BODY" token)

RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
  -d "{\"correo\":\"cliente@smartlogix.cl\",\"clave\":\"${SEED_CLIENTE_PASSWORD:-}\"}")
CODE=$(tail -n1 <<<"$RESP"); BODY=$(sed '$d' <<<"$RESP")
assert_status "Login cliente" 200 "$CODE"
TOKEN_CLIENTE=$(json_field "$BODY" token)

RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
  -d "{\"correo\":\"transportista@smartlogix.cl\",\"clave\":\"${SEED_TRANSPORTISTA_PASSWORD:-}\"}")
CODE=$(tail -n1 <<<"$RESP"); BODY=$(sed '$d' <<<"$RESP")
assert_status "Login transportista" 200 "$CODE"
TOKEN_TRANSP=$(json_field "$BODY" token)

CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
  -d '{"correo":"admin@smartlogix.cl","clave":"clave-incorrecta"}')
assert_status "Login con clave incorrecta rechazado" 401 "$CODE"

echo "=== 2. Registro y asignación de rol ==="
CORREO_QA="qa.$RUN_ID@smartlogix.cl"
RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/auth/register" -H "Content-Type: application/json" \
  -d "{\"nombre\":\"QA\",\"apellido\":\"E2E\",\"rut\":\"$RUN_ID-K\",\"correo\":\"$CORREO_QA\",\"clave\":\"TestQA123!\"}")
CODE=$(tail -n1 <<<"$RESP")
assert_status "Registro público (rol cliente)" 200 "$CODE"

echo "=== 3. Catálogo ==="
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/productos")
assert_status "Catálogo público sin token" 200 "$CODE"

RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/api/productos" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Producto E2E $RUN_ID\",\"descripcion\":\"Generado por test-e2e-local.sh\",\"precio\":1000,\"stock\":20,\"categoriaId\":\"$(curl -s "$BASE/api/categorias" | grep -o '"id":"[^\"]*"' | head -1 | cut -d'"' -f4)\"}")
CODE=$(tail -n1 <<<"$RESP"); BODY=$(sed '$d' <<<"$RESP")
assert_status "Crear producto como admin" 201 "$CODE"
PRODUCTO_ID=$(json_field "$BODY" id)

CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/productos" -H "Authorization: Bearer $TOKEN_CLIENTE" -H "Content-Type: application/json" \
  -d '{"nombre":"no deberia crearse","precio":1,"stock":1,"categoriaId":"00000000-0000-0000-0000-000000000000"}')
assert_status "Cliente no puede crear productos" 403 "$CODE"

echo "=== 4. Ciclo de vida de una orden ==="
DIRECCION_ID=$(curl -s "$BASE/api/direcciones" -H "Authorization: Bearer $TOKEN_ADMIN" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/api/ordenes" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d "{\"direccionId\":\"$DIRECCION_ID\",\"detalles\":[{\"productoId\":\"$PRODUCTO_ID\",\"cantidad\":1}]}")
CODE=$(tail -n1 <<<"$RESP"); BODY=$(sed '$d' <<<"$RESP")
assert_status "Crear orden (admin, stock recién creado)" 200 "$CODE"
ORDEN_ID=$(grep -o '"id":[0-9]*,"tomada"' <<<"$BODY" | grep -o '[0-9]*')

if [ -n "${ORDEN_ID:-}" ]; then
  ESTADOS_JSON=$(curl -s "$BASE/api/estados" -H "Authorization: Bearer $TOKEN_ADMIN")
  estado_id() { grep -o "{\"id\":\"[a-f0-9-]*\",\"nombre\":\"$1\"[^}]*\"tipoDeEstado\":{\"id\":\"1348bf80-21a9-4b70-8dae-983b6c00045f\"" <<<"$ESTADOS_JSON" | grep -o '^{"id":"[a-f0-9-]*"' | cut -d'"' -f4; }

  ID_PROCESANDO=$(estado_id "Procesando")
  ID_APROBADO=$(estado_id "Aprobado")
  ID_ENTREGADO=$(estado_id "Entregado")

  CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/ordenes/$ORDEN_ID/historial" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
    -d "{\"estadoId\":\"$ID_PROCESANDO\",\"estadoNombre\":\"Procesando\"}")
  assert_status "Avance válido pendiente -> procesando" 200 "$CODE"

  CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/ordenes/$ORDEN_ID/historial" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
    -d "{\"estadoId\":\"$ID_ENTREGADO\",\"estadoNombre\":\"Entregado\"}")
  assert_status "Salto inválido procesando -> entregado rechazado" 400 "$CODE"

  CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/ordenes/$ORDEN_ID/tomar" -H "Authorization: Bearer $TOKEN_TRANSP")
  assert_status "Transportista toma la ruta" 200 "$CODE"

  CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/ordenes/$ORDEN_ID/tomar" -H "Authorization: Bearer $TOKEN_CLIENTE")
  assert_status "Cliente no puede tomar rutas" 403 "$CODE"

  curl -s -o /dev/null -X POST "$BASE/api/ordenes/$ORDEN_ID/historial" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
    -d "{\"estadoId\":\"$ID_APROBADO\",\"estadoNombre\":\"Aprobado\"}"

  RESP=$(curl -s "$BASE/api/estados" -H "Authorization: Bearer $TOKEN_ADMIN")
  TRANSITO_ID=$(grep -o '{"id":"[a-f0-9-]*","nombre":"En tr[^"]*"' <<<"$RESP" | head -1 | grep -o '"id":"[a-f0-9-]*"' | cut -d'"' -f4)
  printf '{"estadoId":"%s","estadoNombre":"En tr\xc3\xa1nsito"}' "$TRANSITO_ID" > /tmp/_e2e_transito.json
  CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/ordenes/$ORDEN_ID/historial" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json; charset=utf-8" --data-binary @/tmp/_e2e_transito.json)
  assert_status "Avance a 'En tránsito'" 200 "$CODE"

  CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/ordenes/$ORDEN_ID/historial" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
    -d "{\"estadoId\":\"$ID_ENTREGADO\",\"estadoNombre\":\"Entregado\"}")
  assert_status "Avance final a 'Entregado'" 200 "$CODE"

  STOCK_ANTES=$(curl -s "$BASE/api/productos/$PRODUCTO_ID" -H "Authorization: Bearer $TOKEN_ADMIN" | grep -o '"stock":[0-9]*' | cut -d: -f2)
  CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/ordenes/$ORDEN_ID/solicitar-devolucion" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
    -d '{"motivo":"Prueba automatizada e2e"}')
  assert_status "Solicitar devolución sobre orden entregada" 200 "$CODE"

  CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/ordenes/$ORDEN_ID/resolver-devolucion" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
    -d '{"aprobada":true,"danado":false,"comentario":"OK e2e"}')
  assert_status "Resolver devolución" 200 "$CODE"

  STOCK_DESPUES=$(curl -s "$BASE/api/productos/$PRODUCTO_ID" -H "Authorization: Bearer $TOKEN_ADMIN" | grep -o '"stock":[0-9]*' | cut -d: -f2)
  if [ "$STOCK_DESPUES" -eq "$((STOCK_ANTES + 1))" ]; then
    log_pass "Stock reintegrado tras devolución ($STOCK_ANTES -> $STOCK_DESPUES)"
  else
    log_fail "Stock NO reintegrado correctamente (antes=$STOCK_ANTES despues=$STOCK_DESPUES)"
  fi
else
  log_fail "No se pudo crear la orden — se omiten los pasos 4.x dependientes"
fi

echo "=== 5. Inventario: bodega / alerta de ocupación ==="
RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/api/inventario/bodegas" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Bodega E2E $RUN_ID\",\"direccion\":\"Test 1\",\"ciudad\":\"Santiago\",\"pais\":\"Chile\",\"capacidadTotal\":1000}")
CODE=$(tail -n1 <<<"$RESP"); BODY=$(sed '$d' <<<"$RESP")
assert_status "Crear bodega" 201 "$CODE"
BODEGA_ID=$(grep -o '"idBodega":[0-9]*' <<<"$BODY" | head -1 | cut -d: -f2)

RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/api/inventario/pasillos" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d "{\"codigo\":\"PAS-E2E-$RUN_ID\",\"descripcion\":\"QA\",\"numeroOrden\":1,\"activo\":true,\"idBodega\":$BODEGA_ID}")
CODE=$(tail -n1 <<<"$RESP"); BODY=$(sed '$d' <<<"$RESP")
assert_status "Crear pasillo" 201 "$CODE"
PASILLO_ID=$(grep -o '"idPasillo":[0-9]*' <<<"$BODY" | head -1 | cut -d: -f2)

RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/api/inventario/estantes" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"codigo":"EST-E2E-'"$RUN_ID"'","descripcion":"QA","numNiveles":1,"capacidadPorNivel":1,"capacidadTotal":1,"activo":true}')
CODE=$(tail -n1 <<<"$RESP"); BODY=$(sed '$d' <<<"$RESP")
assert_status "Crear estante (capacidad=1, para forzar alerta)" 201 "$CODE"
ESTANTE_ID=$(grep -o '"idEstante":[0-9]*' <<<"$BODY" | head -1 | cut -d: -f2)

curl -s -o /dev/null -X POST "$BASE/api/inventario/est-pasi" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d "{\"idEstante\":$ESTANTE_ID,\"idPasillo\":$PASILLO_ID,\"posicion\":\"A1\",\"numeroFila\":1,\"habilitada\":true}"

CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/api/productos/$PRODUCTO_ID" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Producto E2E $RUN_ID\",\"precio\":1000,\"stock\":20,\"categoriaId\":\"$(curl -s "$BASE/api/categorias" | grep -o '"id":"[^\"]*"' | head -1 | cut -d'"' -f4)\",\"idBodega\":$BODEGA_ID,\"idPasillo\":$PASILLO_ID,\"idEstante\":$ESTANTE_ID}")
assert_status "Mover producto a estante lleno" 200 "$CODE"

sleep 3
ALERTAS=$(curl -s "$BASE/api/inventario/alertas" -H "Authorization: Bearer $TOKEN_ADMIN")
if grep -q "\"idEstante\":$ESTANTE_ID" <<<"$ALERTAS"; then
  log_pass "Alerta de bodega generada al superar el umbral"
else
  log_fail "No se generó alerta de bodega esperada"
fi

echo "=== 6. Preferencias de usuario ==="
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/api/config/preferencias/tema" -H "Authorization: Bearer $TOKEN_CLIENTE" -H "Content-Type: application/json" -d '{"valor":"oscuro"}')
assert_status "Guardar preferencia de usuario" 200 "$CODE"
PREFS=$(curl -s "$BASE/api/config/preferencias" -H "Authorization: Bearer $TOKEN_CLIENTE")
if grep -q '"tema":"oscuro"' <<<"$PREFS"; then log_pass "Preferencia persistida"; else log_fail "Preferencia no persistida"; fi

echo ""
echo "=================================================="
echo "RESULTADO: $PASS pasaron, $FAIL fallaron"
echo "=================================================="
[ "$FAIL" -eq 0 ]
