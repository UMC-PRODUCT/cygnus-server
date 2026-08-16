locals {
  health_check = {
    enabled             = true
    path                = "/actuator/health"
    port                = "9090"
    protocol            = "HTTP"
    matcher             = "200"
    healthy_threshold   = 3
    unhealthy_threshold = 10
    interval            = 30
    timeout             = 10
  }
}

resource "aws_lb" "this" {
  name                       = "server-alb-default"
  load_balancer_type         = "application"
  internal                   = false
  security_groups            = [var.alb_security_group_id]
  subnets                    = var.public_subnet_ids
  idle_timeout               = var.idle_timeout_seconds
  enable_deletion_protection = var.enable_deletion_protection
  enable_http2               = true
}

resource "aws_lb_target_group" "dev_ec2" {
  name        = "server-tg-ec2-dev"
  vpc_id      = var.vpc_id
  protocol    = "HTTP"
  port        = 8080
  target_type = "instance"

  health_check {
    enabled             = local.health_check.enabled
    path                = local.health_check.path
    port                = local.health_check.port
    protocol            = local.health_check.protocol
    matcher             = local.health_check.matcher
    healthy_threshold   = local.health_check.healthy_threshold
    unhealthy_threshold = local.health_check.unhealthy_threshold
    interval            = local.health_check.interval
    timeout             = local.health_check.timeout
  }
}

resource "aws_lb_target_group" "prod_ec2" {
  name        = "server-tg-ec2-prod"
  vpc_id      = var.vpc_id
  protocol    = "HTTP"
  port        = 8080
  target_type = "instance"

  health_check {
    enabled             = local.health_check.enabled
    path                = local.health_check.path
    port                = local.health_check.port
    protocol            = local.health_check.protocol
    matcher             = local.health_check.matcher
    healthy_threshold   = local.health_check.healthy_threshold
    unhealthy_threshold = local.health_check.unhealthy_threshold
    interval            = local.health_check.interval
    timeout             = local.health_check.timeout
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  protocol          = "HTTP"
  port              = 80

  default_action {
    type = "redirect"

    redirect {
      protocol    = "HTTPS"
      port        = "443"
      host        = "#{host}"
      path        = "/#{path}"
      query       = "#{query}"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  protocol          = "HTTPS"
  port              = 443
  certificate_arn   = var.certificate_arn
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-Res-PQ-2025-09"

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      message_body = "ALB Route Not Found"
      status_code  = "404"
    }
  }
}

resource "aws_lb_listener_rule" "dev" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 1

  condition {
    host_header {
      values = sort(tolist(var.dev_host_headers))
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.dev_ec2.arn
  }
}

resource "aws_lb_listener_rule" "prod" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 2

  condition {
    host_header {
      values = sort(tolist(var.prod_host_headers))
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.prod_ec2.arn
  }
}
