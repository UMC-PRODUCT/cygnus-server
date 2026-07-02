variable "aws_region" {
  type        = string
  description = "AWS region."
  default     = "ap-northeast-2"
}

variable "aws_profile" {
  type        = string
  description = "Terraform 실행에 사용할 AWS CLI profile. null이면 AWS provider 기본 credential chain을 사용한다."
  default     = null
  nullable    = true
}

variable "environment" {
  type        = string
  description = "환경 이름."
  default     = "shared"
}

variable "hosted_zone_id" {
  type        = string
  description = "university.neordinary.com Route53 hosted zone ID."
  default     = null
  nullable    = true
}

variable "acm_certificate_arn" {
  type        = string
  description = "shared HTTPS listener에서 사용할 ACM certificate ARN."
  default     = "arn:aws:acm:ap-northeast-2:137809407320:certificate/0050f1b3-bd9d-4f97-8c3b-83129196de8e"
  nullable    = false

  validation {
    condition     = can(regex("^arn:aws:acm:ap-northeast-2:[0-9]{12}:certificate/[0-9a-f-]+$", var.acm_certificate_arn))
    error_message = "acm_certificate_arn must be a valid ACM certificate ARN in ap-northeast-2 region."
  }
}
