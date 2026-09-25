output "service_urls" {
  description = "URL pública de cada Cloud Run (las de ingress interno solo son alcanzables entre servicios de la VPC/proyecto)"
  value = {
    for k, s in google_cloud_run_v2_service.backend : k => s.uri
  }
}

output "nat_config" {
  value = "${google_compute_router.nat_router.name} / ${google_compute_router_nat.nat_config.name}"
}
