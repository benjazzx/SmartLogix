# Orden Service — SmartLogix

Microservicio de gestión de órdenes de compra. Administra el ciclo completo de una orden: creación, historial de estados (`historial_orden`) y detalle de productos (`detalle_orden`).

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

`8084` (interno Docker: `orden-service:8084`)

## Endpoints

### Órdenes
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/ordenes` | admin, bodeguero, transportista |
| GET | `/api/ordenes/mis-ordenes` | cliente (solo sus propias órdenes) |
| GET | `/api/ordenes/{id}` | admin, bodeguero, transportista, cliente |
| POST | `/api/ordenes` | cliente |
| DELETE | `/api/ordenes/{id}` | admin |

### Historial de estado
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/ordenes/{id}/historial` | Lista el historial de cambios de estado |
| POST | `/api/ordenes/{id}/historial` | Agrega un nuevo estado a la orden |

## Modelos de base de datos

```
orden          → id, fecha_orden, user_id, user_nombre, direccion_id, estado_actual, total
detalle_orden  → id, orden_id, producto_id, producto_nombre, cantidad, precio_unitario, subtotal
historial_orden→ id, orden_id, estado_id, estado_nombre, comentario, fecha
```

> - `detalle_orden`: guarda el snapshot de precio y nombre del producto en el momento de la compra, no una FK al producto actual (el producto puede cambiar de precio después).
> - `historial_orden`: registro inmutable de todos los cambios de estado (Pendiente → Procesando → Aprobado → En tránsito → Entregado / Cancelado).

## Flujo de una orden

```
Cliente crea orden (POST /api/ordenes)
  → se crean registros en detalle_orden
  → se registra primer historial_orden con estado "Pendiente"
  → se publica evento Kafka "orden-creada-topic" → Producto service descuenta stock
Admin/Bodeguero avanza estado (POST /api/ordenes/{id}/historial)
  → nuevo registro en historial_orden
  → se actualiza estado_actual en orden
Cliente confirma entrega → último historial_orden con estado "Entregado"
```

## Eventos Kafka

| Tópico | Tipo | Descripción |
|---|---|---|
| `orden-creada-topic` | Producer | Notifica a Producto service para descontar stock |

## Variables de entorno

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://orden-db:5432/ordendb
SPRING_DATASOURCE_USERNAME=smartlogix
SPRING_DATASOURCE_PASSWORD=smartlogix123
JWT_SECRET=SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

## Ejecutar en Docker

```bash
docker compose build orden-service
docker compose up -d orden-service
```

## Patrones aplicados

- **Repository** — `OrdenRepository`, `DetalleOrdenRepository`, `HistorialOrdenRepository`.
- **Factory Method** — `OrdenFactory` para construir una orden con sus detalles e historial inicial.
- **DTO** — `OrdenRequest` / `OrdenResponse` con `DetalleOrdenDTO` y `HistorialOrdenDTO`.
- **Observer / Event-Driven** — Kafka producer al crear una orden para notificar descuento de stock.
- **Circuit Breaker** — aplicado en el Gateway para este servicio.
