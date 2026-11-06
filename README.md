# SmartLogix — Backend (Microservicios)

Sistema de gestión logística desarrollado con arquitectura de microservicios. Spring Boot 4.0.5 + Java 21 + PostgreSQL + Apache Kafka.

## Repositorio Frontend

El frontend Angular se encuentra en: [vvillalobos7/SmartLogix](https://github.com/vvillalobos7/SmartLogix)

## Arquitectura

```
                        ┌─────────────────┐
  Angular Frontend ────▶│   API Gateway   │ :80 → :8080
                        │  (BFF + JWT)    │
                        └────────┬────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │              ┌───────┴──────┐               │
          ▼              ▼              ▼                ▼
    ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐
    │  Users   │  │ Producto │  │  Orden   │  │  Inventario  │
    │  :8082   │  │  :8085   │  │  :8084   │  │    :8083     │
    └──────────┘  └──────────┘  └──────────┘  └──────────────┘
          │
    ┌─────┴──────┐
    ▼            ▼
┌───────┐  ┌─────────┐
│  Rol  │  │ Estado  │
│ :8081 │  │  :8086  │
└───────┘  └─────────┘
```

## Microservicios

| Servicio | Puerto | Descripción | README |
|---|---|---|---|
| Gateway | 8080 | BFF — enrutamiento, JWT, Circuit Breaker | [README](Gateway/README.md) |
| Users | 8082 | Usuarios, autenticación, geografía | [README](Users/README.md) |
| Rol | 8081 | Roles y permisos | [README](Rol/README.md) |
| Estado | 8086 | Estados del ciclo de vida de órdenes | [README](Estado/README.md) |
| Inventario | 8083 | Bodegas, pasillos, estantes | [README](Inventario/README.md) |
| Producto | 8085 | Catálogo, imágenes en BD, stock | [README](Producto/README.md) |
| Orden | 8084 | Órdenes, detalles, historial | [README](Orden/README.md) |

## Stack técnico

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.5 |
| Spring Cloud | 2025.1.1 |
| PostgreSQL | 16 |
| Apache Kafka | 4.x |
| Resilience4j | 3.x |
| Docker / Docker Compose | — |

## Mensajería

Conviven dos brokers: **Kafka** para eventos de dominio entre Users, Rol y Estado (más Producto ↔ Inventario),
y **RabbitMQ** para tareas asíncronas de logística y onboarding.

### RabbitMQ (colas con DLQ, ACK manual)

| Routing key | Exchange | Productor | Consumidor | Qué hace el consumidor |
|---|---|---|---|---|
| `producto.creado` | `logistica-exchange` | Producto | Inventario | Elige la ubicación con menos ocupación |
| `producto.ubicacion-asignada` | `logistica-exchange` | Inventario | Producto | Guarda bodega/pasillo/estante (solo si el producto no tenía ubicación) |
| `orden.creada` | `logistica-exchange` | Orden | Inventario | Elige la bodega de despacho con menos ocupación |
| `orden.bodega-asignada` | `logistica-exchange` | Inventario | Orden | Guarda la bodega en `orden.bodega_despacho` |
| `usuario.registrado` | `usuarios-exchange` | Users | Configuracion | Crea la preferencia `onboarding=pendiente` |

Política de los consumidores: ACK solo **después** de confirmar la transacción de base de datos y de publicar el
resultado; mensaje inválido → DLQ; fallo recuperable → un reintento y luego DLQ. Los productores publican **después
del commit** y, si RabbitMQ falla, no abortan la operación de negocio (solo registran el error en el log).

Nombres de colas, exchanges y routing keys: `application.properties` de cada servicio (`rabbitmq.*`); la topología
(Queue/Exchange/Binding) está en la clase `RabbitConfig` de cada servicio.

### Administración de RabbitMQ

`/api/rabbitmq/**` (rol `admin`) se accede por el Gateway; lo implementa el servicio Users.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/rabbitmq/queues`, `/exchanges`, `/bindings` | Lista (vía API de management, puerto 15672) |
| POST | `/api/rabbitmq/queues` | Crea cola (`nombre`, `durable`, `deadLetterExchange`, `deadLetterRoutingKey`, `ttlMs`) |
| DELETE | `/api/rabbitmq/queues/{nombre}` | Elimina cola |
| POST / DELETE | `/api/rabbitmq/exchanges` · `/exchanges/{nombre}` | Crea (`topic`/`direct`/`fanout`) / elimina exchange |
| POST | `/api/rabbitmq/bindings` | Crea binding (cuerpo JSON) |
| DELETE | `/api/rabbitmq/bindings?cola=&exchange=&routingKey=` | Elimina binding (query params) |

Las colas y exchanges que usa la propia aplicación están protegidos contra borrado
(`rabbitmq.admin.protected-names` en Users) y los nombres `amq.*` están reservados.

> **Cambio de esquema:** la tabla `orden` tiene una columna nueva, `bodega_despacho`. El perfil `aws-ep1`/EC2 usa
> `ddl-auto=update` y la crea sola; en una base existente con `ddl-auto=validate` ejecuta antes:
> `ALTER TABLE orden ADD COLUMN bodega_despacho VARCHAR(200);`

## Levantar el proyecto

```bash
# Construir e iniciar todos los servicios
docker compose up -d --build

# Ver logs
docker compose logs -f

# Detener
docker compose down
```

## Credenciales de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Admin | admin@smartlogix.cl | admin123 |
| Bodeguero | bodeguero@smartlogix.cl | bodega123 |
| Transportista | transportista@smartlogix.cl | trans123 |
| Cliente | cliente@smartlogix.cl | cliente123 |

## Flujo de trabajo Git (Git Flow)

```
main      ──●────────────────────────────────────●── (producción)
             \                                  /
develop        ●──●──●──●──●──●──●──●──●──●──●    (integración)
                \__/    \__/    \__/    \__/
              feature  feature  feature  feature   (desarrollo)
```

| Rama | Propósito |
|---|---|
| `main` | Código estable. Solo recibe merges desde `develop` tras revisión. |
| `develop` | Integración continua. Todas las features se fusionan aquí. |
| `feature/*` | Una rama por funcionalidad (ej: `feature/autenticacion-jwt`). |

**Convención de commits:**
```
feat(servicio):  nueva funcionalidad
fix(servicio):   corrección de bug
test(servicio):  pruebas unitarias / integración
ci:              cambios en pipeline GitHub Actions
docs:            documentación
chore:           tareas de mantenimiento (deps, config)
```

## Cobertura de tests (JaCoCo)

El CI ejecuta `mvn verify` en cada microservicio. El reporte HTML se genera en:
```
<Microservicio>/target/site/jacoco/index.html
```

Para ejecutar localmente en cualquier microservicio:
```bash
cd Orden        # o Gateway, Users, Producto, etc.
mvn verify
# Abre: target/site/jacoco/index.html
```

Cobertura mínima exigida: **60%** por microservicio. SonarCloud analiza el servicio Orden.

## CI/CD — GitHub Actions

El pipeline corre automáticamente en cada push a `develop` o `main`:

```
push / PR
    │
    ├── gateway     mvn verify (JaCoCo ≥60%)
    ├── users       mvn verify (JaCoCo ≥60%)
    ├── rol         mvn verify (JaCoCo ≥60%)
    ├── estado      mvn verify (JaCoCo ≥60%)
    ├── inventario  mvn verify (JaCoCo ≥60%)
    ├── producto    mvn verify (JaCoCo ≥60%)
    ├── orden       mvn verify + SonarCloud
    └── config      mvn verify (JaCoCo ≥60%)
            │
            └── quality-gate  (falla si alguno falla)
```

## Patrones de diseño aplicados

- **BFF (Backend for Frontend)** — Gateway centraliza acceso y seguridad
- **Circuit Breaker** — Resilience4j en Gateway y clientes REST
- **Factory Method** — creación de usuarios por tipo (Admin, Empleado, Cliente)
- **Repository** — Spring Data JPA en cada microservicio
- **Observer / Event-Driven** — Apache Kafka para comunicación asíncrona entre servicios
- **DTO** — separación entre modelos de dominio y contratos de API
