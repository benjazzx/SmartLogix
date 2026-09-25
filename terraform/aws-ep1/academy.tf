# AWS Academy Learner Lab bloquea iam:CreateRole/iam:CreateInstanceProfile para las
# cuentas de estudiante (SCP de la organización) — por eso no se pueden crear roles
# propios como en una cuenta normal. En su lugar, todo lo que necesita un rol de IAM
# (EC2, Lambda) reusa el LabRole/LabInstanceProfile que Academy ya deja pre-creados en
# cada lab, y que ya vienen con permisos suficientes para los servicios que usa este
# proyecto (Secrets Manager, SSM, logs, etc.) — no se les puede ni hace falta adjuntar
# políticas nuevas.
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

data "aws_iam_instance_profile" "lab_instance_profile" {
  name = "LabInstanceProfile"
}
