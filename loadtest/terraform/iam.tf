data "aws_iam_policy_document" "sut_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

# SUT 는 현재 AWS 계정 ECR 에서 앱 이미지를 pull 하기 위해 ECR 권한이 필요하다. (load-test 는 ECR 전용)
data "aws_iam_policy_document" "sut_ecr_pull" {
  statement {
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
    ]
    resources = ["arn:aws:ecr:${var.aws_region}:${data.aws_caller_identity.current.account_id}:repository/${var.ecr_repository_name}"]
  }
}

resource "aws_iam_role" "sut" {
  name               = "${local.name}-sut-runtime"
  assume_role_policy = data.aws_iam_policy_document.sut_assume_role.json

  tags = merge(local.tags, { Name = "${local.name}-sut-runtime" })
}

resource "aws_iam_role_policy" "sut_ecr_pull" {
  name   = "${local.name}-sut-ecr-pull"
  role   = aws_iam_role.sut.id
  policy = data.aws_iam_policy_document.sut_ecr_pull.json
}

resource "aws_iam_instance_profile" "sut" {
  name = "${local.name}-sut-runtime"
  role = aws_iam_role.sut.name

  tags = merge(local.tags, { Name = "${local.name}-sut-runtime" })
}

# ── monitoring: Grafana CloudWatch datasource 용 읽기 권한 ─────────────────
# RDS 호스트 레벨(CPU·메모리·디스크·네트워크) 지표는 postgres-exporter 가 못 보고
# CloudWatch 가 원천이다. Grafana 의 cloudwatch datasource(기본 credential chain)가
# 이 instance role 로 조회한다.
data "aws_iam_policy_document" "monitoring_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "monitoring_cloudwatch_read" {
  statement {
    actions = [
      "cloudwatch:GetMetricData",
      "cloudwatch:ListMetrics",
      "cloudwatch:GetMetricStatistics",
      "cloudwatch:DescribeAlarms",
      "ec2:DescribeRegions",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role" "monitoring" {
  name               = "${local.name}-monitoring-runtime"
  assume_role_policy = data.aws_iam_policy_document.monitoring_assume_role.json
  tags               = merge(local.tags, { Name = "${local.name}-monitoring-runtime" })
}

resource "aws_iam_role_policy" "monitoring_cloudwatch_read" {
  name   = "${local.name}-monitoring-cloudwatch-read"
  role   = aws_iam_role.monitoring.id
  policy = data.aws_iam_policy_document.monitoring_cloudwatch_read.json
}

resource "aws_iam_instance_profile" "monitoring" {
  name = "${local.name}-monitoring-runtime"
  role = aws_iam_role.monitoring.name

  tags = merge(local.tags, { Name = "${local.name}-monitoring-runtime" })
}
