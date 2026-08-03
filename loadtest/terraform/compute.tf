# 관측 EC2 — 먼저 떠야 SUT 가 OTLP 를 보낼 수 있음 (otel-collector 토큰 공유)
# 앱이 collector 로 보내는 OTLP 요청의 Bearer token. monitoring 과 SUT user-data 에 같은 값이 들어간다.
resource "random_password" "otel_token" {
  length  = 32
  special = false
}

# Grafana 초기 admin password. output 은 sensitive 로만 노출한다.
resource "random_password" "grafana_admin" {
  length  = 20
  special = false
}

resource "aws_instance" "monitoring" {
  ami                    = data.aws_ssm_parameter.monitoring_ami.value
  instance_type          = var.monitoring_instance_type
  subnet_id              = aws_subnet.primary.id
  private_ip             = local.monitoring_private_ip
  iam_instance_profile   = aws_iam_instance_profile.monitoring.name
  vpc_security_group_ids = [aws_security_group.monitoring.id]
  key_name               = var.key_name

  # user-data 를 바꾸면 기존 인스턴스 안에서 스크립트가 재실행되지 않는다.
  # 변경 내용을 확실히 반영하려고 인스턴스를 교체한다.
  user_data_replace_on_change = true

  # monitoring 은 기존 repo 의 infra/monitoring/grafana compose 를 clone 한 뒤,
  # 부하 테스트용 Prometheus scrape/remote-write/postgres_exporter 설정을 덮어쓴다.
  user_data = templatefile("${path.module}/user-data/monitoring.sh.tftpl", {
    git_repo_url           = var.git_repo_url
    git_repo_branch        = var.git_repo_branch
    otel_token             = random_password.otel_token.result
    grafana_admin_password = random_password.grafana_admin.result
    db_host                = aws_db_instance.this.address
    db_port                = aws_db_instance.this.port
    db_name                = var.db_name
    db_username            = var.db_username
    db_password            = random_password.db.result
    sut_private_ip         = local.sut_private_ip
    generator_private_ip   = local.generator_private_ip
  })

  dynamic "credit_specification" {
    # T 계열 EC2 는 CPU credit 고갈이 측정값을 오염시키지 않도록 unlimited 로 둔다.
    for_each = startswith(var.monitoring_instance_type, "t") ? [1] : []
    content { cpu_credits = "unlimited" }
  }

  root_block_device {
    # Loki/Tempo/Prometheus chunk 가 기본 8GB 디스크를 빠르게 채울 수 있어 여유를 둔다.
    volume_size = 60 # tempo/loki chunk 여유
    volume_type = "gp3"
  }

  tags = merge(local.tags, { Name = "${local.name}-monitoring" })
}

# SUT — 앱(dev) + valkey, DB 는 RDS
# SUT 는 측정 대상이므로 여기의 instance_type, Hikari pool, RDS 스펙이 실험 조건이다.
resource "aws_instance" "sut" {
  ami                         = data.aws_ssm_parameter.sut_ami.value
  instance_type               = var.sut_instance_type
  subnet_id                   = aws_subnet.primary.id
  private_ip                  = local.sut_private_ip
  vpc_security_group_ids      = [aws_security_group.sut.id]
  key_name                    = var.key_name
  iam_instance_profile        = aws_iam_instance_profile.sut.name
  user_data_replace_on_change = true

  # 앱 비밀 env 는 로컬 load-test.env 를 base64 로 넘겨 SUT 에서 app.env 로 디코딩한다. ECR 로그인은 SUT EC2 IAM role 로 수행한다.
  # DB/OTLP/Hikari/dev profile 값은 여기서 강제로 덮어써 매 테스트 조건을 고정한다.
  # 이렇게 해야 매 테스트가 같은 DB/관측/풀 크기 조건에서 시작한다.
  user_data = templatefile("${path.module}/user-data/sut.sh.tftpl", {
    app_image           = var.app_image
    registry_server     = local.ecr_registry_server
    app_env_content_b64 = base64encode(local.app_env_content)
    region              = var.aws_region
    db_host             = aws_db_instance.this.address
    db_port             = aws_db_instance.this.port
    db_name             = var.db_name
    db_username         = var.db_username
    db_password         = random_password.db.result
    otel_host           = local.monitoring_private_ip
    otel_token          = random_password.otel_token.result
    hikari_pool         = var.hikari_pool
    tomcat_threads      = var.tomcat_threads
  })

  dynamic "credit_specification" {
    # T 계열 앱 인스턴스는 credit exhausted 상태가 임계점처럼 보일 수 있어 unlimited 로 둔다.
    for_each = startswith(var.sut_instance_type, "t") ? [1] : []
    content { cpu_credits = "unlimited" }
  }

  tags = merge(local.tags, { Name = "${local.name}-sut" })

  # DB 와 collector endpoint 가 준비된 뒤 앱을 올린다. 앱 기동 시 Flyway/OTLP 연결이 바로 필요하다.
  depends_on = [aws_db_instance.this, aws_instance.monitoring]
}

# 생성기 — k6 설치 (실행은 수동)
# generator 는 측정 대상이 아니므로 SUT 보다 넉넉하게 잡아 k6 CPU 가 먼저 병목나지 않게 한다.
resource "aws_instance" "generator" {
  ami                         = data.aws_ssm_parameter.generator_ami.value
  instance_type               = var.generator_instance_type
  subnet_id                   = aws_subnet.primary.id
  private_ip                  = local.generator_private_ip
  vpc_security_group_ids      = [aws_security_group.generator.id]
  key_name                    = var.key_name
  user_data_replace_on_change = true

  # k6 스크립트는 로컬 loadtest/scripts/sync-k6.sh 로 loadtest/k6 를 올리고(부팅 시 clone 하지 않음),
  # 실행 결과는 monitoring Prometheus remote-write endpoint 로 바로 보낸다.
  user_data = templatefile("${path.module}/user-data/generator.sh.tftpl", {
    base_url    = "http://${aws_lb.sut.dns_name}"
    prom_rw_url = "http://${local.monitoring_private_ip}:${local.prometheus_port}/api/v1/write"
  })

  dynamic "credit_specification" {
    # generator 가 T 계열이면 k6 자체가 credit 에 막힐 수 있어 unlimited 로 둔다.
    for_each = startswith(var.generator_instance_type, "t") ? [1] : []
    content { cpu_credits = "unlimited" }
  }

  tags = merge(local.tags, { Name = "${local.name}-generator" })

  # RUN.md 와 run-umc-k6 가 ALB/Prometheus endpoint 를 내장하므로 listener 준비 이후 생성한다.
  depends_on = [aws_lb_listener.http, aws_instance.monitoring]
}
