output "alb_security_group_id" {
  description = "ALB security group ID."
  value       = aws_security_group.alb.id
}

output "asg_security_group_id" {
  description = "ASG security group ID."
  value       = aws_security_group.asg.id
}

output "db_security_group_id" {
  description = "DB security group ID."
  value       = aws_security_group.db.id
}
