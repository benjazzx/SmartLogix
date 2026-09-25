# Secrets Manager: nada de credenciales en texto plano en el docker-compose del EC2.
# El EC2 las lee en runtime vía su IAM role (sin access keys hardcodeadas).
# Ojo: estos son los usuarios/schemas de aplicación (orden_user, producto_user) dentro
# del RDS Oracle — hay que crearlos a mano una vez el RDS esté arriba (ver README:
# no es seguro dejar que Terraform corra DDL con las credenciales maestras).

resource "aws_secretsmanager_secret" "oracle_orden" {
  name = "${var.project_name}/oracle/orden"
}

resource "aws_secretsmanager_secret_version" "oracle_orden" {
  secret_id = aws_secretsmanager_secret.oracle_orden.id
  secret_string = jsonencode({
    jdbc_url = "jdbc:oracle:thin:@${aws_db_instance.oracle.address}:${aws_db_instance.oracle.port}/${aws_db_instance.oracle.db_name}"
    username = var.oracle_username_orden
    password = var.oracle_password_orden
  })
}

resource "aws_secretsmanager_secret" "oracle_producto" {
  name = "${var.project_name}/oracle/producto"
}

resource "aws_secretsmanager_secret_version" "oracle_producto" {
  secret_id = aws_secretsmanager_secret.oracle_producto.id
  secret_string = jsonencode({
    jdbc_url = "jdbc:oracle:thin:@${aws_db_instance.oracle.address}:${aws_db_instance.oracle.port}/${aws_db_instance.oracle.db_name}"
    username = var.oracle_username_producto
    password = var.oracle_password_producto
  })
}

resource "aws_secretsmanager_secret" "internal_service_key" {
  name = "${var.project_name}/internal-service-key"
}

resource "aws_secretsmanager_secret_version" "internal_service_key" {
  secret_id     = aws_secretsmanager_secret.internal_service_key.id
  secret_string = var.internal_service_key
}

resource "aws_secretsmanager_secret" "confluent" {
  name = "${var.project_name}/confluent"
}

resource "aws_secretsmanager_secret_version" "confluent" {
  secret_id = aws_secretsmanager_secret.confluent.id
  secret_string = jsonencode({
    bootstrap_servers = var.confluent_bootstrap_servers
    api_key            = var.confluent_api_key
    api_secret          = var.confluent_api_secret
  })
}

resource "aws_secretsmanager_secret" "legacy_jwt_secret" {
  name = "${var.project_name}/legacy-jwt-secret"
}

resource "aws_secretsmanager_secret_version" "legacy_jwt_secret" {
  secret_id     = aws_secretsmanager_secret.legacy_jwt_secret.id
  secret_string = var.legacy_jwt_secret
}

resource "aws_secretsmanager_secret" "seed_passwords" {
  name = "${var.project_name}/seed-passwords"
}

resource "aws_secretsmanager_secret_version" "seed_passwords" {
  secret_id = aws_secretsmanager_secret.seed_passwords.id
  secret_string = jsonencode({
    admin         = var.seed_admin_password
    bodeguero     = var.seed_bodeguero_password
    transportista = var.seed_transportista_password
    cliente       = var.seed_cliente_password
  })
}

# Users/Rol/Estado/Inventario/Configuracion no son parte del caso evaluado (solo
# Orden/Producto lo son) — se suman igual porque el usuario quiere el SmartLogix
# completo corriendo en AWS. Antes apuntaban a Neon (se acabó la cuota gratuita);
# ahora es RDS Postgres (aws_db_instance.postgres, rds.tf) — cero migración de
# JPA/Hibernate, solo cambia el host. La contraseña la gestiona AWS directamente
# (manage_master_user_password), fetch-secrets.sh la lee de ese secret nativo,
# no hace falta un secret propio acá.
