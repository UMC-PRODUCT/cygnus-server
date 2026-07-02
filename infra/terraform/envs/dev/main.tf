# dev root 소유 범위:
# - dev Auto Scaling group과 launch template
# - dev EC2 instance profile, IAM role, IAM policy attachment
#
# shared 리소스는 shared remote state 또는 data source로만 읽는다.
# 후속 ASG/IAM task에서 shared output을 연결한다.

locals {
  environment = "dev"
  app_host    = "dev.api.university.neordinary.com"
}

# data "terraform_remote_state" "shared" {
#   backend = "s3"
#
#   config = merge(
#     {
#       bucket = var.shared_state_bucket
#       key    = var.shared_state_key
#       region = var.shared_state_region
#     },
#     var.shared_state_profile == null ? {} : { profile = var.shared_state_profile }
#   )
# }
#
# module "dev_iam" {
#   source = "../../modules/iam"
#
#   environment         = local.environment
#   dev_secret_source   = var.dev_secret_source
#   runtime_env_file    = var.dev_runtime_env_file_path
#   ssm_parameter_path  = var.dev_ssm_parameter_path
#   dev_runtime_secret_arn     = var.dev_runtime_secret_arn
#   dev_runtime_secret_kms_arn = var.dev_runtime_secret_kms_key_arn
# }
#
# module "dev_asg" {
#   source      = "../../modules/compute-asg"
#   environment = local.environment
#   image_tag   = var.image_tag
# }
