# Runbook EP1 DSY1107 — Entra ID + AWS (nivel sobresaliente)

Orden de ejecución: **Entra ID primero** (AWS necesita sus valores), después AWS. Cada punto marca qué exige la rúbrica en el tramo 100% para no perder puntos por detalles menores.

## 1. Microsoft Entra ID (hacerlo antes que nada en AWS)

1. **Tenant**: usa uno existente o crea uno nuevo en [entra.microsoft.com](https://entra.microsoft.com) (cuenta gratuita de Azure alcanza).
2. **App Registration — API** (`pedidos360-api`):
   - Application ID URI: `api://<client-id-api>` (Expose an API).
   - Scope: `access_as_user`, admin+user consent.
   - **App roles** (Manifest o App roles blade), `allowedMemberTypes: ["User"]`:
     - `Admin`, `Operador`, `Cliente` (deben llamarse exactamente así, en mayúscula inicial, porque el BFF va a mapear ese string literal a `ROLE_*`).
3. **App Registration — Frontend** (`pedidos360-frontend`):
   - Plataforma: **Single-page application** (NO "Web" — si la eliges mal, MSAL no usa Authorization Code + PKCE automáticamente y pierdes el ítem del 15% de la rúbrica).
   - Redirect URIs: `http://localhost:4200` (dev) + la URL real del frontend en prod.
   - En **API permissions**: agrega el scope `access_as_user` de `pedidos360-api`, y presiona **Grant admin consent**.
4. **Usuarios de prueba + roles**: crea al menos 3 usuarios (uno por rol). Ve a **Enterprise Applications → pedidos360-api → Users and groups** y asígnale a cada usuario su App Role (Admin/Operador/Cliente). Sin este paso el token no trae el claim `roles` y todo el sistema de autorización queda vacío.
5. **Anota estos 4 valores** (los vas a necesitar en AWS y en el frontend):
   - `TENANT_ID`
   - `CLIENT_ID_FRONTEND` (App Registration frontend)
   - `CLIENT_ID_API` (App Registration API — es el `audience` que valida el Lambda y el BFF)
   - `AUTHORITY` = `https://login.microsoftonline.com/<TENANT_ID>`

## 2. AWS — vía Terraform (nivel profesional, no clicks sueltos en la consola)

Toda la infraestructura AWS (VPC propia con subredes públicas/privadas, ALB interno, EC2 en Auto Scaling Group sin IP pública ni SSH abierto, RDS Oracle, Lambda Authorizer, API Gateway con VPC Link, S3+CloudFront para el frontend) está definida en `terraform/aws-ep1/`. Sigue el `README.md` de esa carpeta:

```bash
cd terraform/aws-ep1
cp terraform.tfvars.example terraform.tfvars
# completa entra_tenant_id, entra_audience, internal_service_key, oracle_password_orden/producto
terraform init
terraform apply
```

Puntos clave que resuelve automáticamente (y por qué le suman a la nota):
- **API Gateway es el único punto público real** — el ALB y el EC2 están en subredes privadas, solo alcanzables vía VPC Link. Antes tenía el EC2 con IP pública en el puerto 8080, lo cual violaba literalmente el requisito *"AWS API Gateway como único punto público de entrada al backend"* de la sección 4 de la guía.
- **CORS con origen exacto** (el dominio real de CloudFront, no `*`) — resuelto automáticamente por Terraform, sin que tengas que copiar/pegar URLs a mano.
- **Sin SSH abierto**: acceso al EC2 vía SSM Session Manager (`aws ssm start-session --target <id>`), no hay puerto 22 expuesto ni key pair que perder.
- **RDS Oracle** en vez de una Autonomous DB externa en OCI — todo queda dentro de tu cuenta AWS.

Después del `apply`, sigue los pasos de "Después del apply" en `terraform/aws-ep1/README.md`: crear los 2 schemas de Oracle a mano (Terraform no corre DDL con las credenciales maestras, por seguridad), subir el build de Angular a S3, y desplegar el `docker-compose.ec2.yml` en el EC2 vía SSM.

El Lambda Authorizer que ya está en `lambda-authorizer/` se empaqueta y despliega automáticamente como parte del `terraform apply` — no hace falta crearlo a mano (solo corre `npm install --omit=dev` ahí antes, una vez).

## 3. Pruebas que pide la rúbrica (sección 11 de la guía, ítem de 15% "Evidencia el funcionamiento")

Con Postman o `curl`, documenta (captura de pantalla) cada caso:

| Caso | Cómo | Esperado |
|---|---|---|
| Sin token | `curl https://<api-id>.execute-api.<region>.amazonaws.com/api/ordenes` | `401` |
| Token inválido/expirado | Bearer con un JWT vencido o alterado | `401` |
| Token válido, rol correcto | Login MSAL como Admin/Operador, llamar `/api/ordenes` | `200` + JSON |
| Token válido, sin permiso | Login como Cliente, intentar una acción solo-Admin | `403` |

## Estado del código

Ya hecho:
- BFF (`Gateway`): `EntraSecurityConfig` (perfil `aws-ep1`) valida issuer/audience/firma del JWT de Entra ID y traduce sus roles (Admin/Operador/Cliente) al vocabulario interno (admin/bodeguero/cliente). `GatewayRouteConfig` reenvía la identidad ya resuelta a Orden/Producto vía headers internos.
- `Orden`/`Producto`: `BffTrustFilter` confía en esos headers (protegidos por `X-Internal-Key`) sin tener que entender JWT de Entra. `application-aws-ep1.properties` + driver `ojdbc11` listos para Oracle.
- `terraform/aws-ep1/`: toda la infraestructura AWS.

Pendiente:
- Frontend: instalar y configurar MSAL con los 4 valores del paso 1 (`entra_tenant_id`, `entra_audience`, etc.).
- `docker-compose.ec2.yml`: el compose reducido (bff+orden+producto) que corre en el EC2, leyendo los secrets desde Secrets Manager.
- Crear los 2 schemas de Oracle a mano en el RDS (paso manual, ver README de terraform).
