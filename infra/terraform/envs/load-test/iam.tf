data "aws_iam_policy_document" "sut_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "sut_ssm_parameter_access" {
  count = length(local.sut_ssm_parameter_arns) > 0 ? 1 : 0

  statement {
    actions = [
      "ssm:GetParameter",
      "ssm:GetParameters",
      "ssm:GetParametersByPath",
    ]
    resources = local.sut_ssm_parameter_arns
  }
}

data "aws_iam_policy_document" "sut_ssm_kms_decrypt" {
  count = var.ssm_kms_key_arn != "" ? 1 : 0

  statement {
    actions   = ["kms:Decrypt"]
    resources = [var.ssm_kms_key_arn]
  }
}

data "aws_iam_policy_document" "sut_ecr_pull" {
  count = var.registry_type == "ecr" ? 1 : 0

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
  count = local.sut_needs_iam_role ? 1 : 0

  name               = "${local.name}-sut-runtime"
  assume_role_policy = data.aws_iam_policy_document.sut_assume_role.json

  tags = merge(local.tags, { Name = "${local.name}-sut-runtime" })
}

resource "aws_iam_role_policy" "sut_ssm_parameter_access" {
  count = length(local.sut_ssm_parameter_arns) > 0 ? 1 : 0

  name   = "${local.name}-sut-ssm-parameter-access"
  role   = aws_iam_role.sut[0].id
  policy = data.aws_iam_policy_document.sut_ssm_parameter_access[0].json
}

resource "aws_iam_role_policy" "sut_ssm_kms_decrypt" {
  count = var.ssm_kms_key_arn != "" ? 1 : 0

  name   = "${local.name}-sut-ssm-kms-decrypt"
  role   = aws_iam_role.sut[0].id
  policy = data.aws_iam_policy_document.sut_ssm_kms_decrypt[0].json
}

resource "aws_iam_role_policy" "sut_ecr_pull" {
  count = var.registry_type == "ecr" ? 1 : 0

  name   = "${local.name}-sut-ecr-pull"
  role   = aws_iam_role.sut[0].id
  policy = data.aws_iam_policy_document.sut_ecr_pull[0].json
}

resource "aws_iam_instance_profile" "sut" {
  count = local.sut_needs_iam_role ? 1 : 0

  name = "${local.name}-sut-runtime"
  role = aws_iam_role.sut[0].name

  tags = merge(local.tags, { Name = "${local.name}-sut-runtime" })
}
