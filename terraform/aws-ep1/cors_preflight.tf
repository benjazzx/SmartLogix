# Verificacion previa CORS (OPTIONS) sin autorizador: el navegador no puede enviar el token
# en el preflight, asi que estas rutas van directo al BFF, que responde con las cabeceras CORS.
resource "aws_apigatewayv2_route" "ordenes_preflight" {
  api_id             = aws_apigatewayv2_api.pedidos360.id
  route_key          = "OPTIONS /api/ordenes/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.bff_proxy.id}"
  authorization_type = "NONE"
}

resource "aws_apigatewayv2_route" "productos_preflight" {
  api_id             = aws_apigatewayv2_api.pedidos360.id
  route_key          = "OPTIONS /api/productos/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.bff_proxy.id}"
  authorization_type = "NONE"
}