# Password de la base de datos Neon (compartida por los 7 servicios con BD — mismo rol
# neondb_owner). Se pasa por variable sensible, NUNCA hardcodeada aquí.
#
# TODO (siguiente paso recomendado, mismo patrón ya aplicado a jwt-secret e internal-service-key):
# migrar esto a un secreto real en Secret Manager (`db-password`) y reemplazar `env_vars.password`
# de cada servicio por un `secret_env_vars` apuntando a ese secreto, en vez de pasarlo como
# variable de Terraform en texto plano en tiempo de apply.
variable "db_password" {
  description = "Password de Neon Postgres (rol neondb_owner) — nunca poner el valor real en un .tfvars versionado"
  type        = string
  sensitive   = true
}

variable "kafka_password" {
  description = "Password SASL de Confluent Cloud — nunca poner el valor real en un .tfvars versionado"
  type        = string
  sensitive   = true
}

variable "kafka_username" {
  description = "Username SASL de Confluent Cloud"
  type        = string
  sensitive   = true
}

locals {
  cors_origins = join(",", var.frontend_urls)

  kafka_env = {
    SPRING_KAFKA_BOOTSTRAP_SERVERS          = var.confluent_bootstrap_servers
    SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL = "SASL_SSL"
    SPRING_KAFKA_PROPERTIES_SASL_MECHANISM    = "PLAIN"
    SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG  = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${var.kafka_username}\" password=\"${var.kafka_password}\";"
  }

  # Un objeto por servicio: nombre de servicio Cloud Run, nombre de la base Neon (null si no
  # tiene BD propia — Gateway no persiste nada), si necesita salir por la VPC para llamar a
  # otro servicio interno, si necesita la clave interna, y sus env vars propias (URLs a otros
  # servicios, config puntual).
  services = {
    gateway = {
      db_name        = null
      ingress        = "INGRESS_TRAFFIC_ALL"
      needs_vpc      = true # llama a los 8 backends internos
      needs_internal_key = false
      extra_env = {
        USERS_SERVICE_URL         = "https://users-service-975241295152.us-central1.run.app"
        ROL_SERVICE_URL           = "https://rol-service-975241295152.us-central1.run.app"
        ESTADO_SERVICE_URL        = "https://estado-service-975241295152.us-central1.run.app"
        INVENTARIO_SERVICE_URL    = "https://inventario-service-975241295152.us-central1.run.app"
        ORDEN_SERVICE_URL         = "https://orden-service-975241295152.us-central1.run.app"
        PRODUCTO_SERVICE_URL      = "https://producto-service-975241295152.us-central1.run.app"
        CONFIGURACION_SERVICE_URL = "https://configuracion-service-975241295152.us-central1.run.app"
      }
    }
    users = {
      db_name        = "users_db"
      ingress        = "INGRESS_TRAFFIC_INTERNAL_ONLY"
      needs_vpc      = true # llama a Rol para asignar rol
      needs_internal_key = false
      extra_env = {
        ROL_SERVICE_URL = "https://rol-service-975241295152.us-central1.run.app"
      }
    }
    rol = {
      db_name        = "rol_db"
      ingress        = "INGRESS_TRAFFIC_INTERNAL_ONLY"
      # TODO: no confirmado en esta sesión si Rol llama directo a otro servicio interno.
      # Revisar con `terraform plan` / RolClient del código si de verdad necesita salir por la VPC.
      needs_vpc      = false
      needs_internal_key = false
      extra_env      = {}
    }
    estado = {
      db_name        = "estado_db"
      ingress        = "INGRESS_TRAFFIC_INTERNAL_ONLY"
      needs_vpc      = false
      needs_internal_key = false
      extra_env      = {}
    }
    inventario = {
      db_name        = "inventario_db"
      ingress        = "INGRESS_TRAFFIC_INTERNAL_ONLY"
      needs_vpc      = true # llama a Producto (decrementar-stock, existeProducto)
      needs_internal_key = true
      extra_env = {
        PRODUCTO_SERVICE_URL = "https://producto-service-975241295152.us-central1.run.app"
      }
    }
    orden = {
      db_name        = "orden_db"
      ingress        = "INGRESS_TRAFFIC_INTERNAL_ONLY"
      needs_vpc      = true # llama a Producto, Users, Estado
      needs_internal_key = true
      extra_env = {
        PRODUCTO_SERVICE_URL = "https://producto-service-975241295152.us-central1.run.app"
        USERS_SERVICE_URL    = "https://users-service-975241295152.us-central1.run.app"
        ESTADO_SERVICE_URL   = "https://estado-service-975241295152.us-central1.run.app"
      }
    }
    producto = {
      db_name        = "producto_db"
      ingress        = "INGRESS_TRAFFIC_INTERNAL_ONLY"
      needs_vpc      = true # necesita salir a Neon/Confluent tras activar VPC egress
      needs_internal_key = true
      extra_env = {
        INVENTARIO_SERVICE_URL = "https://inventario-service-975241295152.us-central1.run.app"
      }
    }
    configuracion = {
      db_name        = "configuracion_db"
      ingress        = "INGRESS_TRAFFIC_INTERNAL_ONLY"
      needs_vpc      = false
      needs_internal_key = false
      extra_env      = {}
    }
  }
}

resource "google_cloud_run_v2_service" "backend" {
  for_each = local.services

  name     = each.key == "gateway" ? "gateway" : "${each.key}-service"
  location = var.region
  ingress  = each.value.ingress

  template {
    containers {
      image = "docker.io/${var.docker_hub_repo}@${lookup(var.image_digests, each.key, "")}"

      ports {
        container_port = 8080
      }

      dynamic "env" {
        for_each = each.value.db_name == null ? {} : {
          SPRING_DATASOURCE_URL      = "jdbc:postgresql://${var.neon_host}/${each.value.db_name}?sslmode=require"
          SPRING_DATASOURCE_USERNAME = "neondb_owner"
          SPRING_JPA_HIBERNATE_DDL_AUTO = "validate" # el código ya espera validate — no volver a poner "update" aquí
        }
        content {
          name  = env.key
          value = env.value
        }
      }

      dynamic "env" {
        for_each = each.value.db_name == null ? {} : { SPRING_DATASOURCE_PASSWORD = var.db_password }
        content {
          name  = env.key
          value = env.value
        }
      }

      dynamic "env" {
        for_each = local.kafka_env
        content {
          name  = env.key
          value = env.value
        }
      }

      dynamic "env" {
        for_each = each.value.extra_env
        content {
          name  = env.key
          value = env.value
        }
      }

      env {
        name  = "SERVER_PORT"
        value = "8080"
      }

      env {
        name  = "CORS_ALLOWED_ORIGINS"
        value = local.cors_origins
      }

      env {
        name = "JWT_SECRET"
        value_source {
          secret_key_ref {
            secret  = data.google_secret_manager_secret.jwt_secret.secret_id
            version = "latest"
          }
        }
      }

      dynamic "env" {
        for_each = each.value.needs_internal_key ? [1] : []
        content {
          name = "INTERNAL_SERVICE_KEY"
          value_source {
            secret_key_ref {
              secret  = data.google_secret_manager_secret.internal_service_key.secret_id
              version = "latest"
            }
          }
        }
      }
    }

    scaling {
      max_instance_count = 3
    }

    dynamic "vpc_access" {
      for_each = each.value.needs_vpc ? [1] : []
      content {
        network_interfaces {
          network    = data.google_compute_network.default.name
          subnetwork = data.google_compute_subnetwork.default.name
        }
        egress = "ALL_TRAFFIC"
      }
    }
  }

  depends_on = [
    google_secret_manager_secret_iam_member.jwt_secret_access,
    google_secret_manager_secret_iam_member.internal_key_access,
    google_compute_router_nat.nat_config,
  ]
}

resource "google_cloud_run_v2_service_iam_member" "public_gateway" {
  name     = google_cloud_run_v2_service.backend["gateway"].name
  location = var.region
  role     = "roles/run.invoker"
  member   = "allUsers"
}
