# Cognito reemplaza a Microsoft Entra ID como IDaaS — misma función arquitectónica
# (OAuth2/OIDC, Hosted UI con Authorization Code + PKCE, JWT con roles), 100% AWS,
# sin depender de un tenant externo ni de una tarjeta de crédito. Todo se provisiona
# acá mismo, incluidos los usuarios de prueba — no hay pasos manuales en un portal.

data "aws_caller_identity" "current" {}

resource "aws_cognito_user_pool" "pedidos360" {
  name = "${var.project_name}-users"

  username_attributes = ["email"]

  password_policy {
    minimum_length    = 8
    require_lowercase = true
    require_uppercase = true
    require_numbers   = true
    require_symbols   = true
  }

  auto_verified_attributes = ["email"]

  admin_create_user_config {
    allow_admin_create_user_only = true # sin autoregistro — coincide con "no se exige autoregistro" de la guía
  }

  tags = { Name = "${var.project_name}-users" }
}

resource "aws_cognito_user_pool_domain" "pedidos360" {
  domain       = "${lower(var.project_name)}-${data.aws_caller_identity.current.account_id}"
  user_pool_id = aws_cognito_user_pool.pedidos360.id
}

# Resource server: define el scope que la API espera en el token.
resource "aws_cognito_resource_server" "api" {
  identifier   = "https://api.${lower(var.project_name)}.internal"
  name         = "${var.project_name}-api"
  user_pool_id = aws_cognito_user_pool.pedidos360.id

  scope {
    scope_name        = "access"
    scope_description = "Acceso a la API de pedidos/catalogo"
  }
}

# App Client del frontend (Angular SPA) — público, sin secreto, Authorization Code + PKCE.
resource "aws_cognito_user_pool_client" "frontend" {
  name         = "${var.project_name}-frontend"
  user_pool_id = aws_cognito_user_pool.pedidos360.id

  generate_secret = false # obligatorio para un cliente público SPA (no puede guardar secretos)

  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"] # Authorization Code (+ PKCE, automático para clientes públicos)
  allowed_oauth_scopes                 = ["openid", "email", "profile", "${aws_cognito_resource_server.api.identifier}/access"]

  supported_identity_providers = ["COGNITO"]

  # El frontend vive en Vercel (var.frontend_url) — localhost:4200 siempre se deja
  # habilitado también, para poder probar en desarrollo sin tener que redeployar.
  # distinct() evita un duplicado mientras var.frontend_url siga en su default (localhost).
  callback_urls = distinct([var.frontend_url, "http://localhost:4200"])
  logout_urls   = distinct([var.frontend_url, "http://localhost:4200"])

  explicit_auth_flows = ["ALLOW_REFRESH_TOKEN_AUTH", "ALLOW_USER_SRP_AUTH"]

  access_token_validity  = 60
  id_token_validity      = 60
  refresh_token_validity = 1
  token_validity_units {
    access_token  = "minutes"
    id_token      = "minutes"
    refresh_token = "days"
  }

  prevent_user_existence_errors = "ENABLED"
}

# Grupos = roles de Pedidos360. El nombre del grupo es el que llega en el claim
# "cognito:groups" del ID token — el BFF los traduce a admin/bodeguero/cliente.
resource "aws_cognito_user_group" "admin" {
  name         = "ADMIN"
  user_pool_id = aws_cognito_user_pool.pedidos360.id
  precedence   = 1
}

resource "aws_cognito_user_group" "operador" {
  name         = "OPERADOR"
  user_pool_id = aws_cognito_user_pool.pedidos360.id
  precedence   = 2
}

resource "aws_cognito_user_group" "cliente" {
  name         = "CLIENTE"
  user_pool_id = aws_cognito_user_pool.pedidos360.id
  precedence   = 3
}

# Usuarios de prueba — "deben existir previamente en el tenant" (no autoregistro).
resource "aws_cognito_user" "admin" {
  user_pool_id = aws_cognito_user_pool.pedidos360.id
  username     = "admin@smartlogix.cl"
  attributes = {
    email          = "admin@smartlogix.cl"
    email_verified = true
  }
  password       = var.cognito_test_users_password
  message_action = "SUPPRESS" # no manda correo de invitación (no hay buzón real detrás)
}

resource "aws_cognito_user_in_group" "admin" {
  user_pool_id = aws_cognito_user_pool.pedidos360.id
  username     = aws_cognito_user.admin.username
  group_name   = aws_cognito_user_group.admin.name
}

resource "aws_cognito_user" "operador" {
  user_pool_id = aws_cognito_user_pool.pedidos360.id
  username     = "bodeguero@smartlogix.cl"
  attributes = {
    email          = "bodeguero@smartlogix.cl"
    email_verified = true
  }
  password       = var.cognito_test_users_password
  message_action = "SUPPRESS"
}

resource "aws_cognito_user_in_group" "operador" {
  user_pool_id = aws_cognito_user_pool.pedidos360.id
  username     = aws_cognito_user.operador.username
  group_name   = aws_cognito_user_group.operador.name
}

resource "aws_cognito_user" "cliente" {
  user_pool_id = aws_cognito_user_pool.pedidos360.id
  username     = "cliente@smartlogix.cl"
  attributes = {
    email          = "cliente@smartlogix.cl"
    email_verified = true
  }
  password       = var.cognito_test_users_password
  message_action = "SUPPRESS"
}

resource "aws_cognito_user_in_group" "cliente" {
  user_pool_id = aws_cognito_user_pool.pedidos360.id
  username     = aws_cognito_user.cliente.username
  group_name   = aws_cognito_user_group.cliente.name
}
