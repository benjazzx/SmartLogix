#!/bin/bash
# Corre esto en el EC2 (vía SSM Session Manager) antes de `docker compose up`.
# Lee los secrets de AWS Secrets Manager (usando el IAM role de la instancia,
# sin access keys de por medio) y genera el .env que consume docker-compose.ec2.yml.
#
# Requisitos en el EC2: aws-cli (ya instalado por Terraform) y jq.
#   sudo dnf install -y jq

set -euo pipefail

PROJECT_NAME="${PROJECT_NAME:-SmartLogix}"
REGION="${AWS_REGION:-us-east-1}"
ENV_FILE="${ENV_FILE:-.env}"

get_secret() {
  aws secretsmanager get-secret-value --region "$REGION" --secret-id "$1" --query SecretString --output text
}

ORACLE_ORDEN=$(get_secret "${PROJECT_NAME}/oracle/orden")
ORACLE_PRODUCTO=$(get_secret "${PROJECT_NAME}/oracle/producto")
CONFLUENT=$(get_secret "${PROJECT_NAME}/confluent")
INTERNAL_KEY=$(get_secret "${PROJECT_NAME}/internal-service-key")
LEGACY_JWT=$(get_secret "${PROJECT_NAME}/legacy-jwt-secret")
SEED_PASSWORDS=$(get_secret "${PROJECT_NAME}/seed-passwords")

# La contraseña del RDS Postgres la genera y gestiona AWS directamente (manage_master_user_password) —
# se lee del secret nativo que crea RDS, no de uno propio. El ARN/endpoint viene de terraform output.
POSTGRES_SECRET_ARN=${POSTGRES_SECRET_ARN:?falta exportar POSTGRES_SECRET_ARN (terraform output postgres_master_user_secret_arn)}
POSTGRES=$(aws secretsmanager get-secret-value --region "$REGION" --secret-id "$POSTGRES_SECRET_ARN" --query SecretString --output text)

cat > "$ENV_FILE" <<EOF
JWT_SECRET=${LEGACY_JWT}
INTERNAL_SERVICE_KEY=${INTERNAL_KEY}

COGNITO_USER_POOL_ID=${COGNITO_USER_POOL_ID:?falta exportar COGNITO_USER_POOL_ID (terraform output cognito_user_pool_id)}
COGNITO_REGION=${COGNITO_REGION:-$REGION}
COGNITO_CLIENT_ID=${COGNITO_CLIENT_ID:?falta exportar COGNITO_CLIENT_ID (terraform output cognito_client_id)}
FRONTEND_URL=${FRONTEND_URL:?falta exportar FRONTEND_URL (terraform output frontend_url)}
POSTGRES_ENDPOINT=${POSTGRES_ENDPOINT:?falta exportar POSTGRES_ENDPOINT (terraform output postgres_endpoint)}

ORDEN_ORACLE_JDBC_URL=$(echo "$ORACLE_ORDEN" | jq -r .jdbc_url)
ORDEN_ORACLE_USERNAME=$(echo "$ORACLE_ORDEN" | jq -r .username)
ORDEN_ORACLE_PASSWORD=$(echo "$ORACLE_ORDEN" | jq -r .password)

PRODUCTO_ORACLE_JDBC_URL=$(echo "$ORACLE_PRODUCTO" | jq -r .jdbc_url)
PRODUCTO_ORACLE_USERNAME=$(echo "$ORACLE_PRODUCTO" | jq -r .username)
PRODUCTO_ORACLE_PASSWORD=$(echo "$ORACLE_PRODUCTO" | jq -r .password)

CONFLUENT_BOOTSTRAP_SERVERS=$(echo "$CONFLUENT" | jq -r .bootstrap_servers)
CONFLUENT_API_KEY=$(echo "$CONFLUENT" | jq -r .api_key)
CONFLUENT_API_SECRET=$(echo "$CONFLUENT" | jq -r .api_secret)

POSTGRES_USERNAME=$(echo "$POSTGRES" | jq -r .username)
POSTGRES_PASSWORD=$(echo "$POSTGRES" | jq -r .password)

SEED_ADMIN_PASSWORD=$(echo "$SEED_PASSWORDS" | jq -r .admin)
SEED_BODEGUERO_PASSWORD=$(echo "$SEED_PASSWORDS" | jq -r .bodeguero)
SEED_TRANSPORTISTA_PASSWORD=$(echo "$SEED_PASSWORDS" | jq -r .transportista)
SEED_CLIENTE_PASSWORD=$(echo "$SEED_PASSWORDS" | jq -r .cliente)
POSTGRES_USERS_URL=jdbc:postgresql://${POSTGRES_ENDPOINT}/Users
POSTGRES_ROL_URL=jdbc:postgresql://${POSTGRES_ENDPOINT}/Rol
POSTGRES_ESTADO_URL=jdbc:postgresql://${POSTGRES_ENDPOINT}/Estado
POSTGRES_INVENTARIO_URL=jdbc:postgresql://${POSTGRES_ENDPOINT}/Inventario
POSTGRES_CONFIGURACION_URL=jdbc:postgresql://${POSTGRES_ENDPOINT}/Configuracion
EOF

chmod 600 "$ENV_FILE"
echo "Listo: $ENV_FILE generado. Ahora corre: docker compose -f docker-compose.ec2.yml up -d"
