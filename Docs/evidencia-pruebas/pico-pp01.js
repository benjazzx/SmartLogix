// PP-01 — Prueba de PICO re-ejecutada con STOCK SUFICIENTE.
// Objetivo del revisor: con stock suficiente, un pico de 1.000 solicitudes NO
// debe producir rechazos 400 (el 400 solo es legitimo cuando Producto confirma
// 409 = sin stock). Si el sistema se saturara, el codigo responde 503
// (ServicioOcupadoException), nunca 400. Aqui el stock (1200) supera las 1000
// solicitudes, asi que el stock nunca es el cuello de botella.
//
// Pico: 100 VUs concurrentes se reparten 1.000 iteraciones sin pausa (burst).
// Apunta directo a Orden (8084), mismo camino interno que usa el BFF.
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const ORDEN_URL = __ENV.ORDEN_URL || 'http://localhost:8084';
const INTERNAL_KEY = __ENV.INTERNAL_KEY || 'changeme-internal-key';
const USER_ID = __ENV.USER_ID || '7a95a466-e9e8-47cb-9ba9-4532e4fd8648';
// Producto QA con stock 1200 (> 1000 solicitudes) -> el stock nunca se agota.
const PRODUCTO_ID = __ENV.PRODUCTO_ID || 'd5dab102-1d10-4b06-877d-8b735bcb7ff9';

const c200 = new Counter('status_200_creada');
const c400 = new Counter('status_400_rechazo');
const c503 = new Counter('status_503_saturado');
const cOtro = new Counter('status_otro');

export const options = {
  scenarios: {
    pico_1000: {
      executor: 'shared-iterations',
      vus: 100,
      iterations: 1000,
      maxDuration: '3m',
    },
  },
  thresholds: {
    // La afirmacion a probar: CERO rechazos 400 cuando hay stock suficiente.
    'status_400_rechazo': ['count==0'],
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
    userNombre: 'Pico PP-01',
    detalles: [{ productoId: PRODUCTO_ID, cantidad: 1 }],
  });

  const res = http.post(`${ORDEN_URL}/api/ordenes`, body, { headers });

  if (res.status === 200) c200.add(1);
  else if (res.status === 400) c400.add(1);
  else if (res.status === 503) c503.add(1);
  else cOtro.add(1);

  check(res, {
    'no es 400 (con stock suficiente no debe haber rechazo de negocio)': (r) => r.status !== 400,
    'no es 5xx inesperado distinto de 503': (r) => r.status < 500 || r.status === 503,
  });
}
