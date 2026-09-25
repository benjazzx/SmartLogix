# Empaqueta automáticamente ../../lambda-authorizer (código ya escrito a mano, sin node_modules
# porque Terraform no corre npm install — hay que haberlo corrido una vez antes de `terraform apply`,
# ver README.md de esa carpeta).
data "archive_file" "authorizer_zip" {
  type        = "zip"
  source_dir  = "${path.module}/../../lambda-authorizer"
  output_path = "${path.module}/build/authorizer.zip"
  excludes    = ["README.md"]
}

resource "aws_lambda_function" "cognito_authorizer" {
  function_name    = "${var.project_name}-cognito-authorizer"
  role              = data.aws_iam_role.lab_role.arn
  handler           = "index.handler"
  runtime           = "nodejs20.x"
  filename          = data.archive_file.authorizer_zip.output_path
  source_code_hash  = data.archive_file.authorizer_zip.output_base64sha256
  timeout           = 5
  memory_size       = 128

  environment {
    variables = {
      COGNITO_USER_POOL_ID = aws_cognito_user_pool.pedidos360.id
      COGNITO_REGION        = var.aws_region
      COGNITO_CLIENT_ID     = aws_cognito_user_pool_client.frontend.id
    }
  }
}

resource "aws_lambda_permission" "apigw_invoke" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.cognito_authorizer.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.pedidos360.execution_arn}/*/*"
}
