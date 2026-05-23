# Users Service — SmartLogix

Microservicio de gestión de usuarios, autenticación JWT y direcciones geográficas.

## Stack técnico

| Componente | Versión |
|---|---|
| Spring Boot | 4.0.5 |
| Java | 21 |
| PostgreSQL | 16 |
| Spring Security + JWT | 6.x / JJWT |
| Spring Cloud (Circuit Breaker) | 2025.1.1 |
| Apache Kafka | 4.x |
| Springdoc OpenAPI | 3.x |

## Puerto

`8082` (interno Docker: `users-service:8082`)

## Endpoints principales

### Autenticación (público)
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/login` | Login → retorna JWT |
| POST | `/auth/register` | Registro de nuevo cliente |
| POST | `/auth/recuperar-clave` | Solicitar recuperación de clave |
| POST | `/auth/validar-identidad` | Validar identidad para cambio de clave |
| POST | `/auth/cambiar-clave` | Cambiar clave con token de recuperación |

### Usuarios (requiere JWT)
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/users/me` | Cualquier autenticado |
| PUT | `/api/users/me` | Cualquier autenticado (actualiza dirección) |
| GET | `/api/users` | admin |
| GET | `/api/users/{id}` | admin |
| PUT | `/api/users/{id}` | admin |
| DELETE | `/api/users/{id}` | admin |
| PATCH | `/api/users/{id}/toggle-activo` | admin |
| POST | `/api/users/{id}/asignar-rol` | admin |

### Geografía (requiere JWT)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/regiones` | Lista todas las regiones |
| GET | `/api/comunas/por-region/{id}` | Comunas de una región |
| POST | `/api/direcciones` | Crear dirección de entrega |
| GET | `/api/direcciones/{id}` | Obtener dirección por ID |

## Modelos de base de datos

```
users          → id, nombre, apellido, rut, correo, clave, cargo, activo, rol_id, rol_nombre, estado_id, estado_nombre, direccion_id
region         → id, nombre
comuna         → id, nombre, region_id
direccion      → id, calle, numero, codigo_postal, comuna_id
```

## Eventos Kafka

| Tópico | Tipo | Descripción |
|---|---|---|
| `user-created-topic` | Producer | Se publica al registrar un usuario |
| `role-assigned-topic` | Consumer | Recibe asignación de rol desde Rol service |
| `estado-assigned-topic` | Consumer | Recibe asignación de estado desde Estado service |

## Variables de entorno

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://users-db:5432/usersdb
SPRING_DATASOURCE_USERNAME=smartlogix
SPRING_DATASOURCE_PASSWORD=smartlogix123
JWT_SECRET=SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

## Ejecutar en Docker

```bash
docker compose build users-service
docker compose up -d users-service
```

## Datos iniciales (DataInitializer)

Al arrancar siembra automáticamente: 3 regiones (Metropolitana, Valparaíso, Biobío), 4 comunas, 3 direcciones y los usuarios base (admin, bodeguero, transportista, cliente).

## Patrones aplicados

- **Factory Method** — `UserFactory`, `EmpleadoFactory`, `ClienteFactory` para crear distintos tipos de usuario.
- **Repository** — `UserRepository`, `RegionRepository`, `ComunaRepository`, `DireccionRepository`.
- **Circuit Breaker** — en los clientes REST (`RolClient`, `EstadoClient`) con Resilience4j.
- **Observer / Event-Driven** — comunicación asíncrona con Kafka hacia Rol y Estado service.
