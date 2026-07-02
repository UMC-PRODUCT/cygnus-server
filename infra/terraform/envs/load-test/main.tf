# 부하 테스트 전용 ephemeral 환경 (Phase 01)
# 3-tier: 생성기(k6) → SUT(앱+RDS) → 관측(otel-collector+prom/tempo/loki/grafana)
# apply 로 테스트 환경을 만들고, 테스트가 끝나면 destroy 로 비용/데이터를 통째 정리한다.

data "aws_caller_identity" "current" {}

# EC2 AMI: 역할별 instance type 이 서로 다른 CPU architecture 일 수 있다.
# 예: prod-like SUT 는 t4g.small(arm64), generator 는 c5.large(x86_64).
# AMI 를 하나로 공유하면 한쪽이 부팅되지 않으므로 역할별 SSM parameter 를 분리한다.
data "aws_ssm_parameter" "monitoring_ami" {
  name = var.monitoring_ami_ssm_parameter
}

data "aws_ssm_parameter" "sut_ami" {
  name = var.sut_ami_ssm_parameter
}

data "aws_ssm_parameter" "generator_ami" {
  name = var.generator_ami_ssm_parameter
}

locals {
  # 모든 리소스 이름 prefix. AWS 콘솔에서 ephemeral 부하 테스트 리소스를 쉽게 찾기 위한 값이다.
  name = "umc-loadtest"

  # 비용/정리 대상을 식별하기 위한 공통 tag. destroy 전에도 콘솔에서 Ephemeral=true 로 검색 가능하다.
  tags = {
    Project   = "umc-product"
    Purpose   = "load-test"
    ManagedBy = "terraform"
    Ephemeral = "true"
  }

  # Prometheus scrape target 과 app OTLP endpoint 를 user-data 렌더링 시점에 알아야 한다.
  # Terraform 리소스 간 private_ip 참조로 엮으면 monitoring ↔ SUT/generator 순환 의존이 생기므로 고정 IP 를 쓴다.
  monitoring_private_ip = "10.20.1.10"
  sut_private_ip        = "10.20.1.20"
  generator_private_ip  = "10.20.1.30"

  # 기존 monitoring compose 의 host port 매핑을 그대로 따른다.
  # 포트를 바꾸면 security group, outputs, user-data health check 를 함께 바꿔야 한다.
  grafana_port    = 13000
  prometheus_port = 19090

  normalized_app_env_ssm_parameter_path          = trim(var.app_env_ssm_parameter_path, "/")
  normalized_registry_credentials_ssm_param_name = trim(var.registry_credentials_ssm_parameter_name, "/")
  app_env_ssm_parameter_arns = var.app_env_ssm_parameter_path != "" ? [
    "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/${local.normalized_app_env_ssm_parameter_path}",
    "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/${local.normalized_app_env_ssm_parameter_path}/*",
  ] : []
  registry_credentials_ssm_parameter_arns = var.registry_credentials_ssm_parameter_name != "" ? [
    "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/${local.normalized_registry_credentials_ssm_param_name}",
  ] : []
  sut_ssm_parameter_arns = concat(local.app_env_ssm_parameter_arns, local.registry_credentials_ssm_parameter_arns)

  # 개인 계정에서 ECR 을 쓰는 경우 registry_server 를 비워두면 현재 AWS 계정의 ECR registry 를 사용한다.
  ecr_registry_server      = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com"
  resolved_registry_server = var.registry_server != "" ? var.registry_server : local.ecr_registry_server

  # SUT 가 SSM/KMS 또는 ECR 중 하나라도 런타임 AWS API 를 호출하면 instance profile 이 필요하다.
  sut_needs_iam_role = length(local.sut_ssm_parameter_arns) > 0 || var.ssm_kms_key_arn != "" || var.registry_type == "ecr"
}
