# 운영 HTTP 진입점이 ALB 이므로 부하 테스트도 generator -> ALB -> SUT 경로를 사용한다.
# 단일 SUT 한 대만 target 으로 붙여 "단일 앱 + RDS" 스펙 자체는 유지한다.
resource "aws_lb" "sut" {
  name               = "${local.name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = [aws_subnet.primary.id, aws_subnet.public_secondary.id]

  tags = merge(local.tags, { Name = "${local.name}-alb" })
}

resource "aws_lb_target_group" "sut" {
  name        = "${local.name}-sut"
  port        = 8080
  protocol    = "HTTP"
  target_type = "instance"
  vpc_id      = aws_vpc.this.id

  # 실제 요청은 8080 으로 보내되, health 는 actuator management port 로 본다.
  # 앱 컨텍스트/루트 경로가 바뀌어도 ALB health check 가 안정적으로 동작하게 하려는 선택이다.
  health_check {
    enabled             = true
    protocol            = "HTTP"
    port                = "9090"
    path                = "/actuator/health"
    matcher             = "200-399"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = merge(local.tags, { Name = "${local.name}-sut-tg" })
}

resource "aws_lb_target_group_attachment" "sut" {
  target_group_arn = aws_lb_target_group.sut.arn
  target_id        = aws_instance.sut.id
  port             = 8080
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.sut.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.sut.arn
  }
}
