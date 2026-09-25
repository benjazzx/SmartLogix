# Lambda Authorizer — AWS Cognito

Valida el ID token emitido por Cognito antes de dejar pasar la petición al backend (BFF) a través del API Gateway.

En el flujo de este proyecto, Terraform (`terraform/aws-ep1/cognito.tf`) crea el Cognito User Pool, el App Client y despliega este Lambda automáticamente con las variables de entorno ya resueltas — normalmente no hace falta hacer el `create-function` a mano (ver `terraform/aws-ep1/lambda.tf`). Esta sección queda como referencia si alguna vez lo necesitas desplegar suelto.

## Variables de entorno del Lambda

- `COGNITO_USER_POOL_ID`: ID del User Pool (ej. `us-east-1_xxxxxxxxx`).
- `COGNITO_REGION`: región AWS del User Pool.
- `COGNITO_CLIENT_ID`: Client ID del App Client del frontend — es el `audience` esperado.
- `COGNITO_ISSUER` (opcional): por defecto `https://cognito-idp.<COGNITO_REGION>.amazonaws.com/<COGNITO_USER_POOL_ID>`.
- `COGNITO_JWKS_URI` (opcional): por defecto el endpoint de llaves públicas del User Pool.

## Empaquetar y subir (manual, fuera de Terraform)

```bash
cd lambda-authorizer
npm install --omit=dev
zip -r authorizer.zip index.js node_modules package.json

aws lambda create-function \
  --function-name smartlogix-cognito-authorizer \
  --runtime nodejs20.x \
  --handler index.handler \
  --zip-file fileb://authorizer.zip \
  --role arn:aws:iam::<ACCOUNT_ID>:role/<ROL_CON_LOGS_BASICOS> \
  --environment "Variables={COGNITO_USER_POOL_ID=<pool-id>,COGNITO_REGION=<region>,COGNITO_CLIENT_ID=<client-id>}"
```

## Conectarlo al API Gateway (HTTP API)

```bash
aws apigatewayv2_create-authorizer \
  --api-id <API_ID> \
  --authorizer-type REQUEST \
  --authorizer-payload-format-version 2.0 \
  --identity-source '$request.header.Authorization' \
  --name cognito-jwt-authorizer \
  --authorizer-uri arn:aws:apigateway:<REGION>:lambda:path/2015-03-31/functions/<LAMBDA_ARN>/invocations \
  --authorizer-result-ttl-in-seconds 0
```

No olvides dar permiso a API Gateway para invocar el Lambda (`aws lambda add-permission --action lambda:InvokeFunction --principal apigateway.amazonaws.com ...`).
