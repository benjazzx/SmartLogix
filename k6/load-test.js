// Prueba de CARGA — trafico normal esperado, sostenido en el tiempo.
// Objetivo: confirmar que el sistema responde dentro de los umbrales aceptables
// bajo una carga realista, no buscar el punto de quiebre (eso es stress-test.js).
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';

export const options = {
  scenarios: {
    carga_normal: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },  // rampa de subida
        { duration: '1m',  target: 10 },  // sostenido — carga normal esperada
        { duration: '30s', target: 0 },   // rampa de bajada
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],   // 95% de las respuestas bajo 500ms
    http_req_failed: ['rate<0.01'],     // menos de 1% de errores
  },
};

export default function () {
  const resHealth = http.get(`${BASE_URL}/actuator/health`);
  check(resHealth, {
    'health: status 200': (r) => r.status === 200,
  });

  // Catalogo de productos — unico endpoint de negocio publico sin JWT,
  // real (lee de Postgres a traves de Producto), representativo de uso normal.
  const resProductos = http.get(`${BASE_URL}/api/productos`);
  check(resProductos, {
    'productos: status 200': (r) => r.status === 200,
    'productos: respuesta es JSON': (r) => r.headers['Content-Type'] && r.headers['Content-Type'].includes('application/json'),
  });

  sleep(1);
}
