# prod root 소유 범위:
# - prod Auto Scaling group과 launch template
# - prod EC2 instance profile, IAM role, IAM policy attachment
# - prod RDS instance와 DB subnet group
#
# shared 리소스는 shared remote state 또는 data source로만 읽는다.
# 후속 ASG/IAM/RDS task에서 shared output을 연결한다.

locals {
  environment = "prod"
  app_host    = "api.university.neordinary.com"
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
# module "prod_iam" {
#   source = "../../modules/iam"
#
#   environment                = local.environment
#   jwt_key_ring_secret_arn    = var.jwt_key_ring_secret_arn
#   oauth_client_secret_arn    = var.oauth_client_secret_arn
#   data_protection_secret_arn = var.data_protection_secret_arn
#   database_secret_arn        = var.database_secret_arn
#   runtime_secret_kms_key_arn = var.runtime_secret_kms_key_arn
# }
#
# module "prod_asg" {
#   source      = "../../modules/compute-asg"
#   environment = local.environment
#   image_tag   = var.image_tag
# }
#
# module "prod_rds" {
#   source      = "../../modules/rds"
#   environment = local.environment
# }
