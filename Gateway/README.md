# Gateway Service — SmartLogix

API Gateway (BFF — Backend for Frontend) que centraliza el enrutamiento hacia todos los microservicios, valida JWT y aplica Circuit Breaker y Rate Limiting.

## Stack técnico

| Componente | Versión |
|---|---|
| Spring Boot | 4.0.5 |
| Java | 21 |
| Spring Cloud Gateway MVC | 2025.1.1 |
| Resilience4j (Circuit Breaker) | 3.x |
| JJWT | 0.12.x |

## Puerto

`8080` interno → expuesto en `80` vía Docker (`localhost:80`)

## Tabla de enrutamiento

| Prefijo | Destino | Notas |
|---|---|---|
| `/auth/**` | `users-service:8082` | Público (sin JWT en Gateway) |
| `/api/users/**` | `users-service:8082` | Requiere JWT |
| `/api/direcciones,/api/direcciones/**` | `users-service:8082` | Requiere JWT |
| `/api/regiones,/api/regiones/**` | `users-service:8082` | Requiere JWT |
| `/api/comunas,/api/comunas/**` | `users-service:8082` | Requiere JWT |
| `/api/roles/**` | `rol-service:8081` | Requiere JWT |
| `/api/estados/**` | `estado-service:8086` | Requiere JWT |
| `/api/tipos-estado/**` | `estado-service:8086` | Requiere JWT |
| `/api/inventario/**` | `inventario-service:8083` | Requiere JWT |
| `/api/ordenes,/api/ordenes/**` | `orden-service:8084` | Requiere JWT |
| `/api/productos,/api/productos/**` | `producto-service:8085` | GET público, escritura requiere JWT |
| `/api/categorias,/api/categorias/**` | `producto-service:8085` | GET público |

## Rutas públicas (sin JWT requerido)

- `/auth/**` — login, registro, recuperación de clave
- `GET /api/productos/**` — catálogo y fotos de productos
- `GET /api/categorias/**` — listado de categorías
- `/actuator/**`, `/fallback/**`, `/error`

## Filtros y seguridad

- **JwtAuthFilter** — valida y decodifica el JWT Bearer en cada request protegido. Inyecta el contexto de seguridad con el `rolNombre` como `GrantedAuthority`.
- **LoginRateLimitFilter** — limita intentos de login por IP para mitigar ataques de fuerza bruta.
- **Circuit Breaker (Resilience4j)** — cada ruta tiene su propio circuit breaker. Al abrirse, redirige a `/fallback/{servicio}`.

## Variables de entorno

```env
JWT_SECRET=SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters
```

## Ejecutar en Docker

```bash
docker compose build gateway-service
docker compose up -d gateway-service
```

## Patrones aplicados

- **BFF (Backend for Frontend)** — punto único de entrada para el frontend Angular; oculta la topología interna de microservicios.
- **Circuit Breaker** — Resilience4j con ventana deslizante de 20 llamadas, umbral de fallo al 60%.
- **Gateway / Proxy** — enrutamiento dinámico con Spring Cloud Gateway MVC.
- **Rate Limiting** — filtro personalizado para login.
