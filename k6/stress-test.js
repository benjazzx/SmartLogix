// Prueba de ESTRES — empuja la carga mas alla de lo esperado para encontrar el
// punto donde el sistema empieza a degradarse o fallar. A diferencia de load-test.js,
// aqui SI se espera ver errores/latencia alta en algun punto; el objetivo es ubicar
// ese punto, no evitarlo.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';

export const options = {
  scenarios: {
    estres_progresivo: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '30s', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '30s', target: 150 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  // Sin thresholds que aborten la corrida — en estres queremos ver COMO se degrada,
  // no detener la prueba apenas empieza a fallar.
};

export default function () {
  const res = http.get(`${BASE_URL}/api/productos`);
  check(res, {
    'status 200': (r) => r.status === 200,
  });
  sleep(0.3);
}
