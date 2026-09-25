resource "google_cloud_run_v2_service" "frontend" {
  name     = "frontend"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    containers {
      image = "docker.io/${var.docker_hub_repo}@${lookup(var.image_digests, "frontend", "")}"

      ports {
        container_port = 8080
      }

      # El servidor Express (server.ts) proxea /api, /auth, /actuator, /fallback hacia este target.
      env {
        name  = "API_URL"
        value = google_cloud_run_v2_service.backend["gateway"].uri
      }
    }

    scaling {
      max_instance_count = 3
    }
  }
}

resource "google_cloud_run_v2_service_iam_member" "public_frontend" {
  name     = google_cloud_run_v2_service.frontend.name
  location = var.region
  role     = "roles/run.invoker"
  member   = "allUsers"
}

output "frontend_url" {
  value = google_cloud_run_v2_service.frontend.uri
}
