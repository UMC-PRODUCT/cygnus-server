output "vpc_id" {
  description = "Shared VPC ID."
  value       = module.network.vpc_id
}

output "public_app_subnet_ids" {
  description = "Public app subnet IDs used by current ASG launch templates."
  value       = module.network.public_app_subnet_ids
}

output "private_app_subnet_ids" {
  description = "Private app subnet IDs for future ASG hardening."
  value       = module.network.private_app_subnet_ids
}

output "private_db_subnet_ids" {
  description = "Private DB subnet IDs."
  value       = module.network.private_db_subnet_ids
}

output "asg_security_group_id" {
  description = "ASG security group ID."
  value       = module.security.asg_security_group_id
}

output "db_security_group_id" {
  description = "DB security group ID."
  value       = module.security.db_security_group_id
}

output "alb_dns_name" {
  description = "ALB DNS name."
  value       = module.alb.alb_dns_name
}

output "alb_zone_id" {
  description = "ALB canonical hosted zone ID."
  value       = module.alb.alb_zone_id
}

output "dev_target_group_arn" {
  description = "Dev EC2 target group ARN."
  value       = module.alb.dev_target_group_arn
}

output "prod_target_group_arn" {
  description = "Prod EC2 target group ARN."
  value       = module.alb.prod_target_group_arn
}
