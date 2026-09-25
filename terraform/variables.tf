variable "project_id" {
  description = "Proyecto de GCP"
  type        = string
  default     = "river-dynamo-502903-m6"
}

variable "region" {
  description = "Región de despliegue"
  type        = string
  default     = "us-central1"
}

variable "network_name" {
  description = "Red VPC usada por Cloud Run (Direct VPC egress)"
  type        = string
  default     = "default"
}

variable "subnet_name" {
  description = "Subred usada por Cloud Run (Direct VPC egress)"
  type        = string
  default     = "default"
}

variable "docker_hub_repo" {
  description = "Repositorio de Docker Hub donde se publican las imágenes"
  type        = string
  default     = "contenedorbenjaxio/smartlogix"
}

variable "frontend_urls" {
  description = "Orígenes permitidos para CORS (frontend, en sus dos formatos de URL de Cloud Run)"
  type        = list(string)
  default = [
    "https://frontend-975241295152.us-central1.run.app",
    "https://frontend-evboa4g2fa-uc.a.run.app",
  ]
}

variable "neon_host" {
  description = "Host del proyecto Neon (Postgres externo)"
  type        = string
  default     = "ep-round-cloud-axaigu2j.c-4.us-east-2.aws.neon.tech"
}

variable "confluent_bootstrap_servers" {
  description = "Bootstrap servers de Confluent Cloud (Kafka externo)"
  type        = string
  default     = "pkc-619z3.us-east1.gcp.confluent.cloud:9092"
}

# Imagen por servicio — se pasa por -var en cada apply desde CI, apuntando al digest recién publicado.
# NUNCA usar el tag mutable (":servicio") como referencia real de despliegue: Cloud Run cachea el
# digest resuelto por mirror.gcr.io y puede quedar sirviendo una imagen vieja aunque se haga push
# de una nueva con el mismo tag. Ver docs/incidentes de esta sesión.
variable "image_digests" {
  description = "Digest sha256 de la imagen actualmente desplegada por servicio"
  type        = map(string)
  default = {
    gateway        = "" # completar con el digest real antes del primer apply
    users          = ""
    rol            = ""
    estado         = ""
    inventario     = ""
    orden          = ""
    producto       = ""
    configuracion  = ""
    frontend       = ""
  }
}
