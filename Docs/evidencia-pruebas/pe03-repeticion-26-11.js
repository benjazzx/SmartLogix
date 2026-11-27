// PE-03 repetida (26/11/2026), medida M-05: estrés de escritura con STOCK SUFICIENTE y MAYOR CARGA.
// Mismo camino que stress-test-write.js (22/10): POST /api/ordenes directo a Orden (:8084).
// Cambios: escalones 10 -> 25 -> 50 -> 75 -> 100 -> 150 VUs (antes llegaba a 75), producto con stock
// mayor que la cantidad de solicitudes y contadores por código de respuesta.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const ORDEN_URL = __ENV.ORDEN_URL || 'http://localhost:8084';
const INTERNAL_KEY = __ENV.INTERNAL_KEY || 'changeme-internal-key';
const USER_ID = __ENV.USER_ID || '7a95a466-e9e8-47cb-9ba9-4532e4fd8648';
const PRODUCTO_ID = __ENV.PRODUCTO_ID || 'd5dab102-1d10-4b06-877d-8b735bcb7ff9';
const CALENTAMIENTO = __ENV.CALENTAMIENTO === '1';

const c200 = new Counter('status_200_creada');
const c400 = new Counter('status_400_rechazo');
const c503 = new Counter('status_503_saturado');
const cOtro = new Counter('status_otro');
const duracion200 = new Trend('duracion_200', true);

export const options = {
  scenarios: {
    escritura_progresiva: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: CALENTAMIENTO
        ? [{ duration: '30s', target: 5 }, { duration: '5s', target: 0 }]
        : [
            { duration: '20s', target: 10 },
            { duration: '20s', target: 25 },
            { duration: '20s', target: 50 },
            { duration: '20s', target: 75 },
            { duration: '20s', target: 100 },
            { duration: '20s', target: 150 },
            { duration: '20s', target: 0 },
          ],
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
    userNombre: 'Carga k6',
    detalles: [{ productoId: PRODUCTO_ID, cantidad: 1 }],
  });
  const res = http.post(`${ORDEN_URL}/api/ordenes`, body, { headers });

  if (res.status === 200) { c200.add(1); duracion200.add(res.timings.duration); }
  else if (res.status === 400) c400.add(1);
  else if (res.status === 503) c503.add(1);
  else cOtro.add(1);

  check(res, {
    'orden creada (200)': (r) => r.status === 200,
    'sin errores distintos de 503 (500/502/timeout)': (r) => r.status === 503 || (r.status !== 0 && r.status < 500),
  });
  sleep(0.5);
}
