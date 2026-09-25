# Los secretos ya existen en Secret Manager (creados manualmente esta sesión). Se referencian
# como data source — Terraform NUNCA debe crear ni sobrescribir el valor de un secreto real
# desde código versionado, eso anularía el propósito de tenerlos fuera del repo.
data "google_secret_manager_secret" "jwt_secret" {
  secret_id = "jwt-secret"
}

data "google_secret_manager_secret" "internal_service_key" {
  secret_id = "internal-service-key"
}

# Cuenta de servicio que usan los Cloud Run para leer los secretos de arriba.
data "google_project" "current" {
  project_id = var.project_id
}

locals {
  runtime_service_account = "${data.google_project.current.number}-compute@developer.gserviceaccount.com"
}

resource "google_secret_manager_secret_iam_member" "jwt_secret_access" {
  secret_id = data.google_secret_manager_secret.jwt_secret.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${local.runtime_service_account}"
}

resource "google_secret_manager_secret_iam_member" "internal_key_access" {
  secret_id = data.google_secret_manager_secret.internal_service_key.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${local.runtime_service_account}"
}
