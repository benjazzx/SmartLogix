// Prueba de ESTRES sobre el camino de ESCRITURA real: POST /api/ordenes.
// A diferencia de stress-test.js (que solo lee el catalogo), esto dispara la
// logica de negocio completa: reserva atomica de stock, llamadas HTTP reales
// a Users y Producto, circuit breaker + timeout, publicacion a Kafka y
// escritura en Postgres. Apunta directo a Orden (8084), el mismo camino que
// usaria el BFF, usando el mecanismo de confianza interna (BffTrustFilter).
import http from 'k6/http';
import { check, sleep } from 'k6';

const ORDEN_URL = __ENV.ORDEN_URL || 'http://localhost:8084';
const INTERNAL_KEY = __ENV.INTERNAL_KEY || 'changeme-internal-key';
// UUID de cliente real, visto creando ordenes exitosamente en los logs del servicio.
const USER_ID = __ENV.USER_ID || '7a95a466-e9e8-47cb-9ba9-4532e4fd8648';
// Producto con stock alto (399 unidades) para que el cuello de botella sea el
// sistema, no quedarse sin inventario en los primeros segundos.
const PRODUCTO_ID = __ENV.PRODUCTO_ID || 'cd2a785e-070e-4ce7-b083-e83acfc2ad64';

export const options = {
  scenarios: {
    escritura_progresiva: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 10 },
        { duration: '20s', target: 25 },
        { duration: '20s', target: 50 },
        { duration: '20s', target: 75 },
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

  check(res, {
    'respuesta 200 (creada) o 400 (stock agotado -- rechazo correcto, no error)': (r) =>
      r.status === 200 || r.status === 400,
    'no hubo 500/502/503 (fallo real del sistema)': (r) => r.status < 500,
    'no hubo timeout': (r) => r.status !== 0,
  });

  sleep(0.5);
}
