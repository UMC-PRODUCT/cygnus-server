locals {
  ssh_cidrs = var.allow_world_ssh_for_parity ? distinct(concat(var.allowed_ssh_cidrs, ["0.0.0.0/0"])) : var.allowed_ssh_cidrs
}

resource "aws_security_group" "alb" {
  name        = "server-sg-alb-default"
  description = "ALB default (80, 443)"
  vpc_id      = var.vpc_id

  tags = {
    Name = "server-sg-alb-default"
  }
}

resource "aws_security_group_rule" "alb_ingress_http" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "alb_ingress_https" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "alb_egress_all" {
  type              = "egress"
  security_group_id = aws_security_group.alb.id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group" "asg" {
  name        = "server-sg-ec2-asg-default"
  description = "SG for EC2 in ASG"
  vpc_id      = var.vpc_id

  tags = {
    Name = "server-sg-ec2-asg-default"
  }
}

resource "aws_security_group_rule" "asg_ingress_app" {
  type                     = "ingress"
  security_group_id        = aws_security_group.asg.id
  source_security_group_id = aws_security_group.alb.id
  description              = "Service Port from ALB"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
}

resource "aws_security_group_rule" "asg_ingress_prometheus" {
  type                     = "ingress"
  security_group_id        = aws_security_group.asg.id
  source_security_group_id = aws_security_group.alb.id
  description              = "Management Port from ALB"
  from_port                = 9090
  to_port                  = 9090
  protocol                 = "tcp"
}

resource "aws_security_group_rule" "asg_ingress_ssh" {
  for_each = toset(local.ssh_cidrs)

  type              = "ingress"
  security_group_id = aws_security_group.asg.id
  from_port         = 22
  to_port           = 22
  protocol          = "tcp"
  cidr_blocks       = [each.value]
}

resource "aws_security_group_rule" "asg_egress_all" {
  type              = "egress"
  security_group_id = aws_security_group.asg.id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group" "db" {
  name        = "server-sg-db-default"
  description = "RDS DB (5432, from ECS)"
  vpc_id      = var.vpc_id

  tags = {
    Name = "server-sg-db-default"
  }
}

resource "aws_security_group_rule" "db_ingress_postgresql" {
  type                     = "ingress"
  security_group_id        = aws_security_group.db.id
  source_security_group_id = aws_security_group.asg.id
  description              = "from EC2 ASG"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
}

# 기존 ECS task와 bastion의 DB ingress는 초기 import 범위에서 의도적으로 소유하지 않는다.
# standalone rule만 선언했으므로 Terraform은 미소유 기존 rule을 삭제하지 않는다.
resource "aws_security_group_rule" "db_egress_vpc" {
  type              = "egress"
  security_group_id = aws_security_group.db.id
  description       = "Restrict DB outbound traffic to VPC CIDR"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = [var.vpc_cidr]
}
