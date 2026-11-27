// M-05 (26/11/2026): búsqueda del punto de quiebre de la escritura (POST /api/ordenes directo a Orden).
// Mismo camino que PE-03, con escalones hasta 1.000 usuarios y stock suficiente. Se registra la serie
// completa y el punto de quiebre es el primer tramo con p95 > 2 s (RNF-01) o más de 1 % de errores.
import http from 'k6/http';
import { check, sleep } from 'k6';

const ORDEN_URL = __ENV.ORDEN_URL || 'http://localhost:8084';
const INTERNAL_KEY = __ENV.INTERNAL_KEY || 'changeme-internal-key';
const USER_ID = __ENV.USER_ID || '7a95a466-e9e8-47cb-9ba9-4532e4fd8648';
const PRODUCTO_ID = __ENV.PRODUCTO_ID || 'd5dab102-1d10-4b06-877d-8b735bcb7ff9';

export const options = {
  scenarios: {
    quiebre: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 150 },
        { duration: '30s', target: 300 },
        { duration: '30s', target: 500 },
        { duration: '30s', target: 750 },
        { duration: '30s', target: 1000 },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    // red de seguridad: si el sistema deja de responder, la prueba se detiene
    http_req_failed: [{ threshold: 'rate<0.30', abortOnFail: true, delayAbortEval: '20s' }],
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
    userNombre: 'Carga k6',
    detalles: [{ productoId: PRODUCTO_ID, cantidad: 1 }],
  });
  const res = http.post(`${ORDEN_URL}/api/ordenes`, body, { headers, timeout: '30s' });
  check(res, { 'orden creada (200)': (r) => r.status === 200 });
  sleep(0.5);
}
