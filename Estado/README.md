# Estado Service — SmartLogix

Microservicio de gestión de estados de orden. Define y administra los estados del ciclo de vida de una orden (Pendiente, Procesando, Aprobado, En tránsito, Entregado, Cancelado) y sus tipos.

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

`8086` (interno Docker: `estado-service:8086`)

## Endpoints

### Estados
| Método | Ruta | Rol requerido |
|---|---|---|
| GET | `/api/estados` | Cualquier autenticado |
| GET | `/api/estados/{id}` | Cualquier autenticado |
| POST | `/api/estados` | admin |
| PUT | `/api/estados/{id}` | admin |
| DELETE | `/api/estados/{id}` | admin |

### Tipos de estado
| Método | Ruta | Rol requerido |
|---|---|---|
| GET | `/api/tipos-estado` | Cualquier autenticado |
| POST | `/api/tipos-estado` | admin |

## Modelos de base de datos

```
tipo_de_estado  → id (UUID), nombre, descripcion
estado          → id (UUID), nombre, descripcion, tipo_de_estado_id
```

## Estados sembrados (DataInitializer)

| Nombre | Descripción |
|---|---|
| Pendiente | Orden recibida, esperando procesamiento |
| Procesando | Orden en preparación en bodega |
| Aprobado | Orden aprobada para despacho |
| En tránsito | Orden en camino al cliente |
| Entregado | Orden entregada al cliente |
| Cancelado | Orden cancelada |

## Eventos Kafka

| Tópico | Tipo | Descripción |
|---|---|---|
| `estado-assigned-topic` | Producer | Publica cuando se asigna un estado a un usuario |

## Variables de entorno

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://estado-db:5432/estadodb
SPRING_DATASOURCE_USERNAME=smartlogix
SPRING_DATASOURCE_PASSWORD=smartlogix123
JWT_SECRET=SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

## Ejecutar en Docker

```bash
docker compose build estado-service
docker compose up -d estado-service
```

## Patrones aplicados

- **Repository** — `EstadoRepository`, `TipoDeEstadoRepository`.
- **Observer / Event-Driven** — Kafka producer para notificar cambios de estado al Users service.
- **Circuit Breaker** — aplicado en el Gateway para este servicio.
