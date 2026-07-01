# Producto Service — SmartLogix

Microservicio de catálogo de productos. Gestiona productos, categorías, stock e imágenes almacenadas en base de datos (PostgreSQL `bytea`).

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

`8085` (interno Docker: `producto-service:8085`)

## Endpoints

### Productos
| Método | Ruta | Rol requerido |
|---|---|---|
| GET | `/api/productos` | Público |
| GET | `/api/productos/{id}` | Público |
| GET | `/api/productos/por-pais?pais={pais}` | Público |
| POST | `/api/productos` | admin, bodeguero |
| PUT | `/api/productos/{id}` | admin, bodeguero |
| PATCH | `/api/productos/{id}/toggle-activo` | admin, bodeguero |
| DELETE | `/api/productos/{id}` | admin, bodeguero |
| PATCH | `/api/productos/{id}/decrementar-stock` | Interno (sin JWT) |

### Imágenes
| Método | Ruta | Rol requerido |
|---|---|---|
| POST | `/api/productos/{id}/foto` | admin, bodeguero |
| GET | `/api/productos/{id}/foto` | Público |
| DELETE | `/api/productos/{id}/foto` | admin, bodeguero |

### Categorías
| Método | Ruta | Rol requerido |
|---|---|---|
| GET | `/api/categorias` | Público |
| GET | `/api/categorias/{id}` | Público |
| POST | `/api/categorias` | admin, bodeguero |
| PUT | `/api/categorias/{id}` | admin, bodeguero |
| DELETE | `/api/categorias/{id}` | admin |

## Modelos de base de datos

```
producto   → id (UUID), nombre, descripcion, precio, stock, activo, pais,
             categoria_id, id_bodega, id_pasillo, id_estante,
             imagen_data (bytea), imagen_tipo, fecha_creacion, fecha_actualizacion
categoria  → id (UUID), nombre, descripcion
```

> Las imágenes se almacenan directamente en PostgreSQL como `bytea` (no en el filesystem), garantizando persistencia entre reinicios de contenedor.

## Eventos Kafka

| Tópico | Tipo | Descripción |
|---|---|---|
| `stock-decremented-topic` | Producer | Notifica descuento de stock al Inventario service |
| `orden-creada-topic` | Consumer | Recibe nueva orden para descontar stock |

## Variables de entorno

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://producto-db:5432/productodb
SPRING_DATASOURCE_USERNAME=smartlogix
SPRING_DATASOURCE_PASSWORD=smartlogix123
JWT_SECRET=SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

## Ejecutar en Docker

```bash
docker compose build producto-service
docker compose up -d producto-service
```

## Patrones aplicados

- **Repository** — `ProductoRepository` (con queries por país y bodega), `CategoriaRepository`.
- **DTO** — `ProductoRequestDTO` / `ProductoResponseDTO` para separar modelo interno de API pública.
- **Observer / Event-Driven** — Kafka consumer para recibir órdenes y producer para notificar descuento de stock.
- **Circuit Breaker** — aplicado en el Gateway para este servicio.
