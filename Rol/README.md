# Rol Service — SmartLogix

Microservicio de gestión de roles y permisos. Define los roles del sistema (admin, bodeguero, transportista, cliente) y publica eventos Kafka cuando se asigna un rol a un usuario.

## Stack técnico

| Componente | Versión |
|---|---|
| Spring Boot | 4.0.5 |
| Java | 21 |
| PostgreSQL | 16 |
| Spring Data JPA | 6.x |
| Spring Security + JWT | 6.x |
| Apache Kafka | 4.x |
| Springdoc OpenAPI | 3.x |

## Puerto

`8081` (interno Docker: `rol-service:8081`)

## Endpoints

### Roles
| Método | Ruta | Rol requerido |
|---|---|---|
| GET | `/api/roles` | Cualquier autenticado |
| GET | `/api/roles/{id}` | Cualquier autenticado |
| POST | `/api/roles` | admin |
| PUT | `/api/roles/{id}` | admin |
| DELETE | `/api/roles/{id}` | admin |

## Modelos de base de datos

```
rol  → id (UUID), nombre, descripcion
```

## Roles sembrados (DataInitializer)

| Nombre | Descripción |
|---|---|
| `admin` | Acceso total al sistema |
| `bodeguero` | Gestión de productos e inventario |
| `transportista` | Gestión de envíos |
| `cliente` | Realiza pedidos y consulta sus órdenes |

## Eventos Kafka

| Tópico | Tipo | Descripción |
|---|---|---|
| `role-assigned-topic` | Producer | Publica cuando se asigna un rol a un usuario |

## Variables de entorno

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://rol-db:5432/roldb
SPRING_DATASOURCE_USERNAME=smartlogix
SPRING_DATASOURCE_PASSWORD=smartlogix123
JWT_SECRET=SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

## Ejecutar en Docker

```bash
docker compose build rol-service
docker compose up -d rol-service
```

## Patrones aplicados

- **Repository** — `RolRepository`.
- **Observer / Event-Driven** — Kafka producer para notificar asignaciones de rol al Users service.
- **Circuit Breaker** — aplicado en el Gateway para este servicio.
