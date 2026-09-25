output "api_gateway_url" {
  description = "URL pública del API Gateway — esta es la que va en environment.apiGateway del frontend"
  value       = aws_apigatewayv2_api.pedidos360.api_endpoint
}

output "frontend_url" {
  description = "URL del frontend (Vercel) que quedó configurada en Cognito/CORS — confírmala contra tu deploy real"
  value       = var.frontend_url
}

output "rds_endpoint" {
  description = "Host:puerto del RDS Oracle"
  value       = aws_db_instance.oracle.endpoint
}

output "rds_master_user_secret_arn" {
  description = "Secret de Secrets Manager con la contraseña del usuario maestro (admin_smartlogix), gestionado por AWS"
  value       = aws_db_instance.oracle.master_user_secret[0].secret_arn
}

output "postgres_endpoint" {
  description = "Host:puerto del RDS Postgres (Users/Rol/Estado/Inventario/Configuracion)"
  value       = aws_db_instance.postgres.endpoint
}

output "postgres_master_user_secret_arn" {
  description = "Secret de Secrets Manager con la contraseña del usuario maestro (smartlogix_admin), gestionado por AWS"
  value       = aws_db_instance.postgres.master_user_secret[0].secret_arn
}

output "lambda_authorizer_arn" {
  value = aws_lambda_function.cognito_authorizer.arn
}

output "cognito_user_pool_id" {
  value = aws_cognito_user_pool.pedidos360.id
}

output "cognito_client_id" {
  description = "Client ID del App Client del frontend — va en environment.cognito.clientId"
  value       = aws_cognito_user_pool_client.frontend.id
}

output "cognito_hosted_ui_domain" {
  description = "Dominio del Hosted UI de Cognito (login/logout) — va en environment.cognito.domain"
  value       = "https://${aws_cognito_user_pool_domain.pedidos360.domain}.auth.${var.aws_region}.amazoncognito.com"
}

output "cognito_test_users" {
  value = "admin@smartlogix.cl (ADMIN) / bodeguero@smartlogix.cl (OPERADOR) / cliente@smartlogix.cl (CLIENTE) — password: la que pusiste en cognito_test_users_password"
}

output "internal_alb_dns" {
  description = "DNS interno del ALB (no accesible desde internet, solo referencia/debug)"
  value       = aws_lb.internal.dns_name
}

output "asg_name" {
  description = "Auto Scaling Group del EC2 — usa esto para encontrar la instancia y conectarte por SSM"
  value       = aws_autoscaling_group.bff.name
}

output "secrets_arns" {
  value = {
    oracle_orden          = aws_secretsmanager_secret.oracle_orden.arn
    oracle_producto        = aws_secretsmanager_secret.oracle_producto.arn
    internal_service_key  = aws_secretsmanager_secret.internal_service_key.arn
  }
}
