data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

resource "aws_launch_template" "bff_host" {
  name_prefix   = "${var.project_name}-bff-"
  image_id      = data.aws_ami.al2023.id
  instance_type = var.ec2_instance_type

  iam_instance_profile {
    name = data.aws_iam_instance_profile.lab_instance_profile.name
  }

  vpc_security_group_ids = [aws_security_group.app_sg.id]

  metadata_options {
    http_tokens = "required" # IMDSv2 obligatorio
  }

  user_data = base64encode(<<-EOF
    #!/bin/bash
    set -e
    dnf update -y
    dnf install -y docker jq
    systemctl enable docker
    systemctl start docker
    usermod -aG docker ec2-user
    curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
      -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    export HOME=/root AWS_REGION=us-east-1
    mkdir -p /opt/smartlogix
    cd /opt/smartlogix
    aws s3 cp s3://smartlogix-deploy-temp-427744211591/docker-compose.ec2.yml .
    aws s3 cp s3://smartlogix-deploy-temp-427744211591/fetch-secrets.sh .
    chmod +x fetch-secrets.sh
    export COGNITO_USER_POOL_ID=${aws_cognito_user_pool.pedidos360.id}
    export COGNITO_CLIENT_ID=${aws_cognito_user_pool_client.frontend.id}
    export COGNITO_REGION=us-east-1
    export FRONTEND_URL=https://smart-logix-rho.vercel.app
    export POSTGRES_ENDPOINT=${aws_db_instance.postgres.endpoint}
    export POSTGRES_SECRET_ARN=${aws_db_instance.postgres.master_user_secret[0].secret_arn}
    ./fetch-secrets.sh
    docker-compose -f docker-compose.ec2.yml up -d
  EOF
  )

  tag_specifications {
    resource_type = "instance"
    tags          = { Name = "${var.project_name}-bff-host" }
  }
}

resource "aws_autoscaling_group" "bff" {
  name                = "${var.project_name}-bff-asg"
  desired_capacity    = 1
  min_size            = 1
  max_size            = 1
  vpc_zone_identifier = aws_subnet.private_app[*].id
  target_group_arns   = [aws_lb_target_group.bff.arn]
  # "EC2" en vez de "ELB": con health check de ELB, el ASG reemplazaba la instancia
  # cada ~20 min porque el target group (puerto 8080) tarda en responder mientras
  # bajan las 8 imágenes + se encadenan los healthchecks, y cada instancia nueva
  # empieza sin caché de Docker, así que nunca alcanzaba a terminar antes del
  # siguiente reemplazo (ciclo infinito). Con una sola instancia fija, el chequeo
  # de EC2 (¿la VM está viva?) es suficiente: el ALB igual solo le manda tráfico
  # si el contenedor responde en el target group, healthy o no a nivel de ASG.
  health_check_type = "EC2"

  launch_template {
    id      = aws_launch_template.bff_host.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "${var.project_name}-bff-host"
    propagate_at_launch = true
  }
}