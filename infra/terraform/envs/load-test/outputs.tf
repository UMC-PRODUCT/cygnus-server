# output 은 apply 직후 smoke/debug 에 필요한 접속 정보만 노출한다.
# 비밀번호류는 sensitive 로 표시되지만 state 에는 남으므로 state 파일 관리는 별도로 주의한다.
output "sut_public_ip" {
  description = "SUT 공인 IP. 앱 트래픽은 ALB 를 사용하고, 이 값은 SSH/debug 용도다."
  value       = aws_instance.sut.public_ip
}

output "sut_app_url" {
  description = "운영과 같은 ALB 기반 앱 엔드포인트. k6 와 smoke curl 이 이 URL 을 사용한다."
  value       = "http://${aws_lb.sut.dns_name}"
}

output "sut_private_ip" {
  description = "SUT private IP. Prometheus scrape/debug 확인용이며 k6 트래픽은 ALB 로 보낸다."
  value       = aws_instance.sut.private_ip
}

output "alb_dns_name" {
  description = "SUT 앞단 ALB DNS name"
  value       = aws_lb.sut.dns_name
}

output "grafana_url" {
  description = "관측 Grafana (allowed_cidr 에서만 접근)"
  value       = "http://${aws_instance.monitoring.public_ip}:${local.grafana_port}"
}

output "prometheus_url" {
  description = "관측 Prometheus (allowed_cidr 에서만 접근)"
  value       = "http://${aws_instance.monitoring.public_ip}:${local.prometheus_port}"
}

output "grafana_admin_user" {
  value = "admin"
}

output "grafana_admin_password" {
  description = "Grafana 초기 admin password. `terraform output -raw grafana_admin_password` 로 조회한다."
  value       = random_password.grafana_admin.result
  sensitive   = true
}

output "generator_ssh" {
  description = "생성기 SSH (여기서 k6 실행)"
  value       = "ssh ec2-user@${aws_instance.generator.public_ip}"
}

output "rds_endpoint" {
  description = "RDS endpoint. 앱과 postgres_exporter 가 같은 DB 를 바라보는지 확인할 때 사용한다."
  value       = aws_db_instance.this.endpoint
}

output "db_password" {
  description = "RDS 비밀번호 (랜덤 생성)"
  value       = random_password.db.result
  sensitive   = true
}

output "next_steps" {
  description = "apply 직후 사람이 실행할 최소 검증 순서"
  value       = <<-EOT
    1) ALB 앱 스모크: curl http://${aws_lb.sut.dns_name}
       SUT 헬스 직접 확인: curl http://${aws_instance.sut.public_ip}:9090/actuator/health
    2) Grafana: http://${aws_instance.monitoring.public_ip}:${local.grafana_port}
       비밀번호: terraform output -raw grafana_admin_password
    3) 생성기 접속 후 k6 실행:
       ssh ec2-user@${aws_instance.generator.public_ip}
       run-umc-k6 smoke 1
    4) 끝나면: terraform destroy
  EOT
}
