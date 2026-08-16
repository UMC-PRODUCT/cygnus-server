output "alb_arn" {
  description = "ALB ARN."
  value       = aws_lb.this.arn
}

output "alb_dns_name" {
  description = "ALB DNS name."
  value       = aws_lb.this.dns_name
}

output "alb_zone_id" {
  description = "ALB canonical hosted zone ID."
  value       = aws_lb.this.zone_id
}

output "http_listener_arn" {
  description = "HTTP listener ARN."
  value       = aws_lb_listener.http.arn
}

output "https_listener_arn" {
  description = "HTTPS listener ARN."
  value       = aws_lb_listener.https.arn
}

output "dev_target_group_arn" {
  description = "Dev EC2 target group ARN."
  value       = aws_lb_target_group.dev_ec2.arn
}

output "prod_target_group_arn" {
  description = "Prod EC2 target group ARN."
  value       = aws_lb_target_group.prod_ec2.arn
}
