// Prueba de PICO (spike) -- 1000 solicitudes reales contra POST /api/ordenes,
// disparadas con alta concurrencia (100 VUs) en vez de una rampa suave, para
// forzar contencion real. Orden tiene el pool de Postgres limitado a solo 3
// conexiones (hikari.maximum-pool-size=3) -- este es el cuello de botella mas
// probable bajo esta carga, no la CPU ni la red.
import http from 'k6/http';
import { check, sleep } from 'k6';

const ORDEN_URL = __ENV.ORDEN_URL || 'http://localhost:8084';
const INTERNAL_KEY = __ENV.INTERNAL_KEY || 'changeme-internal-key';
const USER_ID = __ENV.USER_ID || '7a95a466-e9e8-47cb-9ba9-4532e4fd8648';
const PRODUCTO_ID = __ENV.PRODUCTO_ID || '3964b3ba-09f8-48f2-acb7-b5d2d415d767'; // "dd", stock 99

export const options = {
  scenarios: {
    pico_1000: {
      executor: 'shared-iterations',
      vus: 100,
      iterations: 1000,
      maxDuration: '3m',
    },
  },
};

const headers = {
  'Content-Type': 'application/json',
  'X-Internal-Key': INTERNAL_KEY,
  'X-User-Id': USER_ID,
  'X-User-Roles': 'ROLE_cliente',
};

export default function () {
  const body = JSON.stringify({
    direccionId: '11111111-1111-1111-1111-111111111111',
    userNombre: 'Pico k6',
    detalles: [{ productoId: PRODUCTO_ID, cantidad: 1 }],
  });

  const res = http.post(`${ORDEN_URL}/api/ordenes`, body, { headers });

  check(res, {
    '200 (creada) o 400 (stock agotado)': (r) => r.status === 200 || r.status === 400,
    'sin 5xx': (r) => r.status < 500,
    'sin timeout': (r) => r.status !== 0,
  });
}
