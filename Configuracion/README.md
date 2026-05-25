# Microservicio Configuracion

Almacena preferencias de usuario (clave-valor) por `userId` en PostgreSQL.  
Permite personalizar la interfaz (ej. tema de color) de forma independiente por cada usuario autenticado.

## Puerto

`8087`

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/config/preferencias` | Obtiene todas las preferencias del usuario autenticado |
| PUT | `/api/config/preferencias/{clave}` | Crea o actualiza una preferencia |

El `userId` se extrae automáticamente del JWT validado por el API Gateway.

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 15+ (la BD `Configuracion` se crea automáticamente al iniciar)

## Ejecución local

```bash
cd Configuracion
mvn spring-boot:run
```

La base de datos `Configuracion` se crea sola en PostgreSQL al arrancar el servicio.

## Ejecución con Docker

```bash
docker-compose up configuracion-service
```

## Variables de entorno

| Variable | Valor por defecto |
|----------|------------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/Configuracion` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` |
| `JWT_SECRET` | `SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters` |

## Pruebas unitarias

```bash
mvn test
```

Cobertura mínima exigida: **60%** (validada con JaCoCo).

## Tecnologías

- Spring Boot 3.3
- Spring Data JPA + PostgreSQL
- Spring Security + JWT
- JUnit 5 + Mockito
- JaCoCo
