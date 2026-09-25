# La red y subred "default" ya existen en el proyecto (creadas por GCP automáticamente).
# Se referencian como data source, no se crean — Terraform no debe intentar gestionar
# la red default de un proyecto existente.
data "google_compute_network" "default" {
  name = var.network_name
}

data "google_compute_subnetwork" "default" {
  name   = var.subnet_name
  region = var.region
}

# Cloud Router + Cloud NAT: necesarios porque los servicios con Direct VPC egress (--vpc-egress=all-traffic)
# enrutan TODO su tráfico saliente por la VPC — incluida la conexión externa a Neon Postgres y a
# Confluent Cloud Kafka. Sin este NAT, esos servicios pierden conectividad a internet apenas se
# les activa el VPC egress (incidente real de esta sesión: producto-service quedó sin poder
# conectar a Neon hasta crear este NAT).
resource "google_compute_router" "nat_router" {
  name    = "nat-router"
  region  = var.region
  network = data.google_compute_network.default.id
}

resource "google_compute_router_nat" "nat_config" {
  name                               = "nat-config"
  router                             = google_compute_router.nat_router.name
  region                             = var.region
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"

  # Asignación dinámica de puertos — el default (64 fijos por VM) se agotó en esta sesión
  # cuando varios servicios compartieron el mismo NAT bajo carga de redeploys simultáneos.
  enable_dynamic_port_allocation = true
  min_ports_per_vm               = 64
  max_ports_per_vm               = 2048

  log_config {
    enable = true
    filter = "ERRORS_ONLY"
  }
}
