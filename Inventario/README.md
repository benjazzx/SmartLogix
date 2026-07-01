# Inventario Service — SmartLogix

Microservicio de gestión de inventario físico. Administra bodegas, pasillos, estantes y la relación estante-pasillo (`est_pasi`) que define la ubicación física de cada producto.

## Stack técnico

| Componente | Versión |
|---|---|
| Spring Boot | 4.0.5 |
| Java | 21 |
| PostgreSQL | 16 |
| Spring Data JPA / Hibernate | 6.x |
| Spring Security + JWT | 6.x |
| Apache Kafka | 4.x |
| Springdoc OpenAPI | 3.x |

## Puerto

`8083` (interno Docker: `inventario-service:8083`)

## Endpoints

### Bodegas
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/inventario/bodegas` | admin, bodeguero |
| GET | `/api/inventario/bodegas/{id}` | admin, bodeguero |
| POST | `/api/inventario/bodegas` | admin |
| PUT | `/api/inventario/bodegas/{id}` | admin |
| DELETE | `/api/inventario/bodegas/{id}` | admin |

### Pasillos
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/inventario/pasillos` | admin, bodeguero |
| GET | `/api/inventario/pasillos/por-bodega/{bodegaId}` | admin, bodeguero |
| POST | `/api/inventario/pasillos` | admin, bodeguero |
| PUT | `/api/inventario/pasillos/{id}` | admin, bodeguero |
| DELETE | `/api/inventario/pasillos/{id}` | admin |

### Estantes
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/inventario/estantes` | admin, bodeguero |
| GET | `/api/inventario/estantes/por-pasillo/{pasilloId}` | admin, bodeguero |
| POST | `/api/inventario/estantes` | admin, bodeguero |
| PUT | `/api/inventario/estantes/{id}` | admin, bodeguero |
| DELETE | `/api/inventario/estantes/{id}` | admin |

### Relación Estante-Pasillo (`est_pasi`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/inventario/est-pasi` | Lista todas las asignaciones estante↔pasillo |
| POST | `/api/inventario/est-pasi` | Asigna un estante a un pasillo |
| DELETE | `/api/inventario/est-pasi/{id}` | Elimina la asignación |

## Modelos de base de datos

```
bodega    → id_bodega, nombre, direccion, ciudad, pais, capacidad_total, activa
pasillo   → id_pasillo, codigo, descripcion, numero_orden, activo, id_bodega
estante   → id_estante, codigo, descripcion, num_niveles, capacidad_por_nivel, capacidad_total, activo, id_pasillo
est_pasi  → id, id_estante, id_pasillo  ← relación N:M estante ↔ pasillo
```

> `est_pasi` permite que un mismo estante sea accesible desde múltiples pasillos (útil en bodegas con pasillos doble faz).

## Eventos Kafka

| Tópico | Tipo | Descripción |
|---|---|---|
| `stock-decremented-topic` | Consumer | Recibe notificación de Producto para actualizar stock disponible en bodega |

## Variables de entorno

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://inventario-db:5432/inventariodb
SPRING_DATASOURCE_USERNAME=smartlogix
SPRING_DATASOURCE_PASSWORD=smartlogix123
JWT_SECRET=SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

## Ejecutar en Docker

```bash
docker compose build inventario-service
docker compose up -d inventario-service
```

## Patrones aplicados

- **Repository** — `BodegaRepository`, `PasilloRepository`, `EstanteRepository`, `EstPasiRepository`.
- **DTO** — Request/Response DTOs para cada entidad.
- **Observer / Event-Driven** — Kafka consumer que recibe descuentos de stock desde Producto service.
- **Circuit Breaker** — aplicado en el Gateway para este servicio.
