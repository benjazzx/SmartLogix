# Bucket temporal para que el EC2 descargue docker-compose.ec2.yml y fetch-secrets.sh
# en su arranque (user_data, ver ec2.tf).
#
# El bucket NO se gestiona como "resource" de Terraform: el provider de AWS, al leer
# cualquier aws_s3_bucket tras crearlo, llama automaticamente a
# GetBucketObjectLockConfiguration — y el SCP de AWS Academy deniega esa llamada
# explicitamente, haciendo fallar el apply aunque el bucket se cree bien. Por eso el
# bucket se crea una sola vez a mano (aws s3 mb, ver README) y aqui solo se referencia
# su nombre (calculado igual que antes, con el Account ID de la cuenta activa) para
# subir los dos archivos via aws_s3_object, que no dispara esa llamada bloqueada.
locals {
  deploy_bucket_name = "${lower(var.project_name)}-deploy-temp-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_object" "docker_compose_ec2" {
  bucket = local.deploy_bucket_name
  key    = "docker-compose.ec2.yml"
  source = "${path.module}/../../docker-compose.ec2.yml"
  etag   = filemd5("${path.module}/../../docker-compose.ec2.yml")
}

resource "aws_s3_object" "fetch_secrets" {
  bucket = local.deploy_bucket_name
  key    = "fetch-secrets.sh"
  source = "${path.module}/fetch-secrets.sh"
  etag   = filemd5("${path.module}/fetch-secrets.sh")
}
