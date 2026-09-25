variable "aws_region" {
  description = "Región AWS donde se despliega todo"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefijo para nombrar los recursos"
  type        = string
  default     = "SmartLogix"
}

variable "ec2_instance_type" {
  description = "Tamaño de la instancia EC2 que corre BFF + Orden + Producto vía docker compose"
  type        = string
  default     = "t3.small"
}

variable "rds_instance_class" {
  description = "Clase de instancia para RDS Oracle (t3.micro no soporta el motor Oracle)"
  type        = string
  default     = "db.t3.medium"
}

variable "cognito_test_users_password" {
  description = "Password para los 3 usuarios de prueba (admin/operador/cliente) que crea Terraform en Cognito"
  type        = string
  sensitive   = true
}

# El frontend se despliega en Vercel (CloudFront no está disponible en AWS Academy
# Learner Lab), así que esta URL no la genera Terraform — se completa después del
# primer deploy a Vercel con la URL real que te den (ej. https://tu-app.vercel.app).
variable "frontend_url" {
  description = "URL pública del frontend en Vercel — se usa para el callback/logout de Cognito y el CORS del API Gateway"
  type        = string
  default     = "http://localhost:4200"
}

variable "internal_service_key" {
  description = "Secreto compartido X-Internal-Key entre BFF y los microservicios"
  type        = string
  sensitive   = true
}

# Credenciales de los schemas de aplicación dentro del RDS Oracle (admin_smartlogix es
# el usuario maestro, gestionado por AWS — estos son los que usan Orden y Producto).
# Hay que crear estos usuarios a mano una vez el RDS esté arriba (ver README).
variable "oracle_username_orden" {
  type    = string
  default = "orden_user"
}

variable "oracle_password_orden" {
  type      = string
  sensitive = true
}

variable "oracle_username_producto" {
  type    = string
  default = "producto_user"
}

variable "oracle_password_producto" {
  type      = string
  sensitive = true
}

# Confluent Cloud (Kafka externo, ya existente — mismo cluster que usa el despliegue GCP)
variable "confluent_bootstrap_servers" {
  type = string
}

variable "confluent_api_key" {
  type      = string
  sensitive = true
}

variable "confluent_api_secret" {
  type      = string
  sensitive = true
}

# JWT HMAC legado — Gateway lo sigue exigiendo para las rutas que no son de Cognito
# (Users/Rol/etc., fuera del alcance de este EC2, pero la property es obligatoria al
# arrancar). No se usa para nada del flujo evaluado; puede ser cualquier string largo.
variable "legacy_jwt_secret" {
  type      = string
  sensitive = true
}

# Contraseñas de las 4 cuentas semilla que crea Users al arrancar en perfil "dev"
# (admin/bodeguero/transportista/cliente @smartlogix.cl) — sin esto la base de Users
# queda vacía y el login legado siempre cae al modo mock del frontend.
variable "seed_admin_password" {
  type      = string
  sensitive = true
}

variable "seed_bodeguero_password" {
  type      = string
  sensitive = true
}

variable "seed_transportista_password" {
  type      = string
  sensitive = true
}

variable "seed_cliente_password" {
  type      = string
  sensitive = true
}
