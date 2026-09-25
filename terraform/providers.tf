terraform {
  required_version = ">= 1.5"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }

  # Estado remoto recomendado antes de correr `terraform apply` en equipo — un bucket GCS dedicado,
  # no el estado local. Descomenta y ajusta el nombre del bucket una vez creado:
  # backend "gcs" {
  #   bucket = "smartlogix-terraform-state"
  #   prefix = "prod"
  # }
}

provider "google" {
  project = var.project_id
  region  = var.region
}
