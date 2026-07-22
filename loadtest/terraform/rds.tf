# RDS 는 DB subnet group 에 최소 2개 AZ 의 subnet 이 필요하다.
# 실제 DB 인스턴스는 availability_zone 으로 primary AZ 에 고정해 SUT 와 같은 AZ 에 둔다.
resource "aws_db_subnet_group" "this" {
  name       = "${local.name}-db"
  subnet_ids = [aws_subnet.rds_primary.id, aws_subnet.rds_secondary.id]
  tags       = merge(local.tags, { Name = "${local.name}-db" })
}

resource "random_password" "db" {
  length  = 24
  special = false # 일부 클라이언트 URL 인코딩 이슈 회피
}

# 측정 대상 DB. 부하 테스트 후 destroy 로 제거되는 ephemeral 인스턴스다.
resource "aws_db_instance" "this" {
  identifier     = "${local.name}-pg"
  engine         = "postgres"
  engine_version = var.db_engine_version
  instance_class = var.db_instance_class

  # gp3 는 기본 IOPS/throughput 이 명시적이라 재측정 시 스토리지 조건을 비교하기 쉽다.
  allocated_storage = var.db_allocated_storage
  storage_type      = "gp3"

  # RDS 인스턴스를 만들 때 앱이 접속할 PostgreSQL database 까지 같이 만든다.
  # 이 값이 없으면 RDS 인스턴스만 생기고 DATABASE_URL 의 umc_product DB 가 없어 앱/Flyway 가 실패한다.
  db_name  = var.db_name
  username = var.db_username
  password = random_password.db.result

  availability_zone      = var.az # SUT 와 같은 AZ
  multi_az               = false  # 임계점 탐색용 단일 DB 스펙을 고정한다.
  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  # ephemeral: 테스트 데이터는 재생성/스냅샷 복원 대상으로 보고, destroy 를 막지 않는다.
  skip_final_snapshot = true
  deletion_protection = false
  apply_immediately   = true

  tags = merge(local.tags, { Name = "${local.name}-pg" })

  # ⚠️ 크레딧 모드(unlimited): aws_db_instance 에 직접 인자가 없다.
  #   db.t4g 는 RDS 기본이 unlimited 인 경우가 많으나, 생성 후
  #   `aws rds describe-db-instances` 로 ProcessorFeatures/credit 확인하고
  #   필요하면 콘솔/CLI(modify-db-instance)로 unlimited 설정할 것.
  # ⚠️ PostGIS: 앱 Flyway 마이그레이션이 CREATE EXTENSION 으로 만든다(별도 작업 불필요).
}
