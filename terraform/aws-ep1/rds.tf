resource "aws_db_subnet_group" "oracle" {
  name       = "${lower(var.project_name)}-db-subnet-group"
  subnet_ids = aws_subnet.private_db[*].id
  tags       = { Name = "${var.project_name}-db-subnet-group" }
}

resource "aws_db_instance" "oracle" {
  identifier     = "${lower(var.project_name)}-oracle"
  engine         = "oracle-se2"
  instance_class = var.rds_instance_class
  license_model  = "license-included"
  db_name        = "ORCL"

  allocated_storage = 20
  storage_type       = "gp3"

  db_subnet_group_name   = aws_db_subnet_group.oracle.name
  vpc_security_group_ids = [aws_security_group.db_sg.id]

  username = "admin_smartlogix"
  # AWS genera y rota la contraseña del usuario maestro en Secrets Manager —
  # no queda en ningún .tfvars ni en el state en texto plano.
  manage_master_user_password = true

  publicly_accessible = false
  multi_az             = false
  skip_final_snapshot  = true
  apply_immediately    = true

  tags = { Name = "${var.project_name}-oracle" }
}

# Postgres para Users/Rol/Estado/Inventario/Configuracion — reemplaza a Neon (se acabó
# la cuota mensual gratuita). Cero cambios de JPA/Hibernate: siguen siendo Postgres,
# solo cambia el host. Un único usuario maestro para las 5 bases, igual que ya hacía
# el docker-compose local (mismo patrón, ahora contra RDS en vez de un contenedor).
resource "aws_db_instance" "postgres" {
  identifier     = "${lower(var.project_name)}-postgres"
  engine         = "postgres"
  engine_version = "16"
  instance_class = "db.t3.micro"
  db_name        = "Users" # las otras 4 bases se crean a mano tras el apply (ver README)

  allocated_storage = 20
  storage_type       = "gp3"

  db_subnet_group_name   = aws_db_subnet_group.oracle.name
  vpc_security_group_ids = [aws_security_group.db_sg.id]

  username                    = "smartlogix_admin"
  manage_master_user_password = true

  publicly_accessible = false
  multi_az             = false
  skip_final_snapshot  = true
  apply_immediately    = true

  tags = { Name = "${var.project_name}-postgres" }
}
