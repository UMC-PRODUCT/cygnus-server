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
