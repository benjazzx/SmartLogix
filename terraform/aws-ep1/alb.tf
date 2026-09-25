# ALB interno (sin IP pública) — el único que puede llegar a él es el VPC Link de
# API Gateway. Así el API Gateway sigue siendo el único punto público real de entrada.
resource "aws_lb" "internal" {
  name               = "${var.project_name}-alb"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = aws_subnet.private_app[*].id

  tags = { Name = "${var.project_name}-alb" }
}

resource "aws_lb_target_group" "bff" {
  name        = "${var.project_name}-bff-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "instance"

  health_check {
    path                = "/actuator/health"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 15
    timeout             = 5
  }

  tags = { Name = "${var.project_name}-bff-tg" }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.internal.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.bff.arn
  }
}
