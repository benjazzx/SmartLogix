# Terraform — AWS para EP1/EP2 DSY1107 (Pedidos360)

Despliega el SmartLogix completo (8 microservicios) en AWS — no solo Orden/Producto.
Solo esos dos son parte del caso evaluado (por eso son los únicos en RDS Oracle);
Users, Rol, Estado, Inventario y Configuracion se suman para tener el sistema entero
corriendo acá también, en un RDS Postgres propio (antes era Neon, pero se acabó la
cuota gratuita mensual — mismo motor, cero cambios de código).

Arquitectura 100% AWS, sin salir a otro proveedor cloud:

```
Internet
   │
   ▼
CloudFront (S3 privado detrás, vía OAC) ── sirve el Angular (frontend)
   │
   ▼ (llamadas a la API)
API Gateway HTTP API ── único punto público de entrada al backend
   │  (Lambda Authorizer valida el ID token de Cognito primero)
   ▼
VPC Link ── puente privado, el ALB no tiene IP pública
   │
   ▼
ALB interno (subredes privadas)
   │
   ▼
EC2 (Auto Scaling Group, subred privada, sin SSH abierto — acceso por SSM)
   corriendo docker compose: los 8 microservicios de SmartLogix
   │
   ▼
RDS Oracle (Orden/Producto) + RDS Postgres (Users/Rol/Estado/Inventario/Configuracion)
   ambos en subredes privadas, solo alcanzables desde el EC2 de la app
```

Todo detrás de NAT Gateway para salida a internet (pull de imágenes, Confluent Cloud) sin exponer nada innecesariamente.

**IDaaS: AWS Cognito, no Microsoft Entra ID.** La guía de la EP1 nombra Entra ID/MSAL específicamente, pero Microsoft le agregó un requisito de cuenta de facturación (tarjeta) al tenant gratuito este año, lo cual lo hizo impracticable sin pagar. Cognito cumple la misma función arquitectónica (OAuth2/OIDC, Hosted UI con Authorization Code + PKCE, JWT con roles) sin ese bloqueo — es 100% AWS y Terraform lo crea automáticamente, sin ningún paso manual en un portal externo. Avísale a tu profesor de este cambio y el motivo.

## Requisitos previos

1. AWS CLI configurado (`aws configure`).
2. `cd ../../lambda-authorizer && npm install --omit=dev` — Terraform empaqueta esa carpeta tal cual está en disco.

Cognito (tenant, usuarios de prueba, roles) lo crea Terraform mismo — no hay ningún paso manual previo en un portal externo.

## Uso

```bash
cp terraform.tfvars.example terraform.tfvars
# editar terraform.tfvars con tus valores reales

terraform init
terraform plan
terraform apply
```

El RDS Oracle tarda bastante en provisionar (15-20 min aprox.) — es normal, no te quedes reintentando.

## Después del apply

### 1. Crear los schemas de aplicación en el RDS

Terraform crea el RDS y el usuario maestro (`admin_smartlogix`, contraseña autogestionada por AWS en Secrets Manager — `terraform output rds_master_user_secret_arn` te da el ARN para leerla), pero **no** corre DDL con esas credenciales por seguridad. Conéctate tú con un cliente Oracle (SQL Developer, SQLcl, o `sqlplus`) a `terraform output rds_endpoint` y ejecuta:

```sql
CREATE USER orden_user IDENTIFIED BY "la-misma-password-que-pusiste-en-oracle_password_orden";
GRANT CONNECT, RESOURCE, DBA TO orden_user;
ALTER USER orden_user QUOTA UNLIMITED ON USERS;

CREATE USER producto_user IDENTIFIED BY "la-misma-password-que-pusiste-en-oracle_password_producto";
GRANT CONNECT, RESOURCE, DBA TO producto_user;
ALTER USER producto_user QUOTA UNLIMITED ON USERS;
```

(Para conectarte necesitas estar dentro de la VPC — vía un EC2 con SSM en la misma red, o un VPN/bastion. El RDS es privado a propósito.)

### 1b. Crear las 4 bases restantes en el RDS Postgres

El RDS Postgres (`aws_db_instance.postgres`) arranca con una sola base (`Users`) — las otras 4 se crean a mano, igual que en el docker-compose local de siempre (un solo usuario maestro para las 5). Conéctate con `psql` (o cualquier cliente Postgres) a `terraform output postgres_endpoint`, con el usuario `smartlogix_admin` (contraseña en `terraform output postgres_master_user_secret_arn`), y ejecuta:

```sql
CREATE DATABASE "Rol";
CREATE DATABASE "Estado";
CREATE DATABASE "Inventario";
CREATE DATABASE "Configuracion";
```

### 2. Subir el frontend a S3

```bash
cd ../../SmartLogix-main/SmartLogix-main/SmartLogix   # o donde tengas el proyecto Angular
npm run build
aws s3 sync dist/SmartLogix/browser s3://$(terraform -chdir=../../terraform/aws-ep1 output -raw frontend_bucket_name) --delete
aws cloudfront create-invalidation --distribution-id $(terraform -chdir=../../terraform/aws-ep1 output -raw cloudfront_distribution_id) --paths "/*"
```

### 3. Desplegar el BFF/Orden/Producto en el EC2

El Auto Scaling Group crea la instancia sola. Conéctate vía SSM (nada de SSH) y trae el `docker-compose.ec2.yml` + `fetch-secrets.sh` (están en la raíz del repo y en esta carpeta respectivamente):

```bash
aws ssm start-session --target <instance-id-del-ASG>

# dentro de la sesión SSM:
sudo dnf install -y jq git
git clone <tu-repo> app && cd app
export COGNITO_USER_POOL_ID=$(terraform -chdir=terraform/aws-ep1 output -raw cognito_user_pool_id)
export COGNITO_CLIENT_ID=$(terraform -chdir=terraform/aws-ep1 output -raw cognito_client_id)
export FRONTEND_URL=$(terraform -chdir=terraform/aws-ep1 output -raw frontend_url)
export POSTGRES_ENDPOINT=$(terraform -chdir=terraform/aws-ep1 output -raw postgres_endpoint)
export POSTGRES_SECRET_ARN=$(terraform -chdir=terraform/aws-ep1 output -raw postgres_master_user_secret_arn)
bash terraform/aws-ep1/fetch-secrets.sh
docker compose -f docker-compose.ec2.yml --env-file .env up -d
```

### 4. Usuarios de prueba

Ya existen (Terraform los crea): `admin@pedidos360.test` (grupo ADMIN), `operador@pedidos360.test` (OPERADOR), `cliente@pedidos360.test` (CLIENTE) — todos con la password de `cognito_test_users_password`. `terraform output cognito_hosted_ui_domain` te da el dominio del Hosted UI para probar el login manualmente antes de conectar el frontend.

## Destruir todo al terminar la evaluación

```bash
terraform destroy
```

(NAT Gateway y RDS son los que más cobran por hora — no los dejes corriendo sin necesidad.)
