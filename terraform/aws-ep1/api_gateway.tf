resource "aws_apigatewayv2_api" "pedidos360" {
  name          = "${var.project_name}-api"
  protocol_type = "HTTP"

  # Sin cors_configuration acá a propósito: el BFF y cada microservicio ya manejan
  # su propio CORS (Spring Security .cors()). Si API Gateway TAMBIÉN agrega el header
  # Access-Control-Allow-Origin, la respuesta queda con el header duplicado y el
  # navegador la rechaza (aunque curl/Postman no lo noten, porque ellos no aplican
  # el spec de CORS). Una sola fuente de verdad para estos headers.
}

# VPC Link: el puente privado entre API Gateway (público) y el ALB interno.
# Es lo que permite que el EC2/ALB no necesiten IP pública y aun así reciban tráfico
# del API Gateway — sin esto tendríamos que exponer el ALB a internet.
resource "aws_apigatewayv2_vpc_link" "this" {
  name               = "${var.project_name}-vpc-link"
  security_group_ids = [aws_security_group.alb_sg.id]
  subnet_ids         = aws_subnet.private_app[*].id
}

resource "aws_apigatewayv2_integration" "bff_proxy" {
  api_id                 = aws_apigatewayv2_api.pedidos360.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  connection_type        = "VPC_LINK"
  connection_id           = aws_apigatewayv2_vpc_link.this.id
  integration_uri        = aws_lb_listener.http.arn
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_authorizer" "cognito_jwt" {
  api_id                             = aws_apigatewayv2_api.pedidos360.id
  authorizer_type                    = "REQUEST"
  authorizer_uri                     = aws_lambda_function.cognito_authorizer.invoke_arn
  identity_sources                   = ["$request.header.Authorization"]
  name                               = "cognito-jwt-authorizer"
  authorizer_payload_format_version = "2.0"
  authorizer_result_ttl_in_seconds  = 0 # sin caché: cada request se valida de verdad
  enable_simple_responses            = true
}

resource "aws_apigatewayv2_route" "ordenes" {
  api_id             = aws_apigatewayv2_api.pedidos360.id
  route_key          = "ANY /api/ordenes/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.bff_proxy.id}"
  authorization_type = "CUSTOM"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_jwt.id
}

resource "aws_apigatewayv2_route" "productos" {
  api_id             = aws_apigatewayv2_api.pedidos360.id
  route_key          = "ANY /api/productos/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.bff_proxy.id}"
  authorization_type = "CUSTOM"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_jwt.id
}

# Catch-all para el resto de SmartLogix (Users, Rol, Estado, Inventario, Configuracion,
# /auth) — fuera del caso evaluado, así que NO pasan por el Lambda Authorizer de Cognito.
# Siguen protegidos por el JWT HMAC propio que el Gateway ya valida internamente
# (SecurityConfig.java, sin cambios). API Gateway prioriza las rutas específicas de
# arriba sobre este catch-all, así que /api/ordenes y /api/productos no se ven afectados.
resource "aws_apigatewayv2_route" "resto_smartlogix" {
  api_id    = aws_apigatewayv2_api.pedidos360.id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.bff_proxy.id}"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.pedidos360.id
  name        = "$default"
  auto_deploy = true

  default_route_settings {
    throttling_burst_limit = 50
    throttling_rate_limit  = 25
  }
}
