# Load Test Terraform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `terraform apply` 한 번으로 부하 테스트 전용 SUT, RDS, 부하 생성기, 관측 스택을 만들고 `terraform destroy`로 통째 정리한다.

**Architecture:** 단일 VPC 안에 public EC2 3대(SUT, load generator, monitoring)와 private RDS PostgreSQL을 둔다. EC2는 인터넷에서 이미지를 pull해야 하므로 public subnet에 두되, Security Group으로 admin CIDR과 인스턴스 간 통신만 허용한다. 앱은 GHCR/Docker Hub 이미지를 `docker compose pull && up -d`로 띄우고, 메트릭/트레이스/로그는 부하 테스트 전용 monitoring EC2의 otel-collector로 보낸다.

**Tech Stack:** Terraform AWS provider, AWS EC2, RDS PostgreSQL `db.t4g.small`, Secrets Manager, S3 bootstrap artifact bucket, Docker Compose, k6, Prometheus, Grafana, Tempo, Loki, OpenTelemetry Collector, node-exporter, postgres_exporter.

---

## 전제와 결정

- 기준 문서: `docs/guides/load-test/issues.md`
- SUT는 `dev` profile로 띄운다. `/test/token/access`와 seed API가 dev 전용이기 때문이다.
- 현재 CD는 `.github/workflows/cd.yml` 기준으로 SSH + `docker compose pull` 방식이며, 로그인 대상은 코드상 GHCR이다. Terraform은 registry를 변수화해서 GHCR/Docker Hub 둘 다 가능하게 둔다.
- `docker/develop/docker-compose.yml`은 local Postgres에 의존하므로 그대로 쓰지 않는다. 부하 테스트용 SUT compose는 app + nginx + valkey + node-exporter만 두고 DB는 RDS endpoint를 주입한다.
- `infra/monitoring/grafana/docker-compose.yml`은 재사용하되, 부하 테스트용 `.env`, Prometheus config, postgres_exporter, remote-write receiver 설정만 Terraform bootstrap에서 덮어쓴다.
- RDS `db.t4g.small`의 CPU credit unlimited 설정은 Terraform AWS provider에서 명시 필드가 없을 수 있다. 구현 시 `aws rds describe-db-instances`로 실제 credit mode 노출 여부를 확인하고, 명시 불가하면 문서에 "RDS T-class unlimited/default 확인 절차"를 남긴다.
- Terraform state에는 RDS password, random token 등 민감 값이 들어갈 수 있다. v1은 local state + `.gitignore`로 유출을 막고, 협업 운영이 필요해지면 S3 backend + lock으로 바꾼다.

## ASCII 구조도

```text
Internet / Admin CIDR
        |
        | 3000 Grafana, 9090 Prometheus, 8080/9090 smoke, 22 optional
        v
+----------------------------------------------------------------------------------+
| VPC 10.42.0.0/16                                                                 |
|                                                                                  |
|  Public subnet A 10.42.1.0/24                         Private subnets            |
|                                                        10.42.11.0/24, 10.42.12.0 |
|                                                                                  |
|  +--------------------------+        HTTP :8080        +-----------------------+  |
|  | Loadgen EC2              |-------------------------> | SUT EC2               |  |
|  | 10.42.1.30               |                           | 10.42.1.20            |  |
|  |                          |                           |                       |  |
|  | - k6                     |                           | - nginx :8080         |  |
|  | - k6 seed/script         |                           | - app dev :9090       |  |
|  | - node-exporter :9100    |                           | - valkey              |  |
|  +------------+-------------+                           | - node-exporter :9100 |  |
|               |                                         +-----+------------+----+  |
|               |                                               |            |       |
|               | k6 remote-write :9090                         |            |       |
|               v                                               |            |       |
|  +--------------------------+                                  |            |       |
|  | Monitoring EC2           |<---------------------------------+            |       |
|  | 10.42.1.10               |        OTLP :4318/4317                        |       |
|  |                          |        app metrics/traces/logs                |       |
|  | - otel-collector :4318   |                                               |       |
|  | - prometheus :9090       |                                               |       |
|  | - grafana :3000          |                                               |       |
|  | - tempo                  |                                               |       |
|  | - loki                   |                                               |       |
|  | - postgres-exporter:9187 |------------------------------+                |       |
|  | - node-exporter :9100    |     postgres exporter :5432  |                |       |
|  +------------+-------------+                              |                |       |
|               |                                            |                |       |
|               | scrape :9100                               v                v       |
|               +-----------------------> SUT node-exporter  +-----------------------+ |
|               +-----------------------> Loadgen node       | RDS PostgreSQL        | |
|                                                            | db.t4g.small :5432    | |
|                                                            +-----------------------+ |
|                                                                                  |
+----------------------------------------------------------------------------------+
```

흐름 요약:

- `Loadgen -> SUT`: k6가 실제 사용자 트래픽을 `8080`으로 밀어 넣는다.
- `Loadgen -> Monitoring`: k6 실행 결과를 Prometheus remote-write `9090`으로 보낸다.
- `SUT -> Monitoring`: 앱 메트릭, 트레이스, 로그를 OTLP `4318/4317`로 보낸다.
- `Monitoring -> SUT/Loadgen`: Prometheus가 각 EC2의 node-exporter `9100`을 scrape한다.
- `Monitoring -> RDS`: `postgres_exporter`가 RDS `5432`에 붙고, Prometheus가 exporter `9187`을 scrape한다.

## 파일 구조

- Create: `infra/load-test/versions.tf`
  - Terraform/provider 버전과 archive/random provider 선언.
- Create: `infra/load-test/variables.tf`
  - region, AZ, instance type, registry image, admin CIDR, app secret ARN, DB 설정 변수.
- Create: `infra/load-test/locals.tf`
  - name prefix, common tags, generated env 값을 모은다.
- Create: `infra/load-test/network.tf`
  - VPC, public/private subnets, route table, internet gateway.
- Create: `infra/load-test/security-groups.tf`
  - SUT/loadgen/monitoring/RDS SG와 최소 인바운드 규칙.
- Create: `infra/load-test/iam.tf`
  - EC2 instance profile, SSM 권한, S3 artifact read, Secrets Manager read.
- Create: `infra/load-test/artifacts.tf`
  - monitoring/k6/nginx bootstrap artifact를 S3에 업로드.
- Create: `infra/load-test/rds.tf`
  - RDS PostgreSQL `db.t4g.small`, subnet group, parameter group.
- Create: `infra/load-test/ec2.tf`
  - SUT/loadgen/monitoring EC2와 cloud-init 연결.
- Create: `infra/load-test/outputs.tf`
  - Grafana URL, SUT URL, RDS endpoint, SSM target names.
- Create: `infra/load-test/templates/sut-cloud-init.yaml.tftpl`
  - Docker 설치, app env 병합, SUT compose 생성, app 기동.
- Create: `infra/load-test/templates/sut-compose.yml.tftpl`
  - nginx + app + valkey + node-exporter.
- Create: `infra/load-test/templates/monitoring-cloud-init.yaml.tftpl`
  - monitoring artifact 압축 해제, `.env` 생성, Prometheus config 덮어쓰기, compose 기동.
- Create: `infra/load-test/templates/prometheus-load-test.yml.tftpl`
  - internal monitoring scrape + SUT/loadgen node-exporter + postgres_exporter scrape.
- Create: `infra/load-test/templates/monitoring-compose.override.yml.tftpl`
  - Prometheus remote-write receiver enable, postgres_exporter 추가.
- Create: `infra/load-test/templates/loadgen-cloud-init.yaml.tftpl`
  - k6 설치, k6 script/data 배치, 실행 helper 생성, node-exporter 기동.
- Create: `infra/load-test/templates/run-k6.sh.tftpl`
  - `K6_OUT=experimental-prometheus-rw` 실행 래퍼.
- Create: `infra/load-test/terraform.tfvars.example`
  - 커밋 가능한 예시 값.
- Create: `infra/load-test/README.md`
  - apply/destroy/검증 절차.
- Modify: `.gitignore`
  - Terraform local state, tfvars, `.terraform/` 제외.

## Security Group Matrix

| 대상 | 포트 | 허용 원본 | 이유 |
| --- | --- | --- | --- |
| SUT | 8080 | loadgen SG, admin CIDR | k6 요청과 smoke 확인 |
| SUT | 9090 | monitoring SG, admin CIDR | health/actuator 확인 |
| SUT | 9100 | monitoring SG | SUT host node-exporter scrape |
| loadgen | 22 | admin CIDR | 직접 접속이 필요할 때만 |
| loadgen | 9100 | monitoring SG | 생성기가 병목인지 확인 |
| monitoring | 3000 | admin CIDR | Grafana |
| monitoring | 9090 | loadgen SG, admin CIDR | k6 remote-write, Prometheus 확인 |
| monitoring | 4318 | SUT SG | 앱 OTLP HTTP push |
| monitoring | 4317 | SUT SG | 앱 OTLP gRPC 사용 시 대비 |
| RDS | 5432 | SUT SG, monitoring SG | 앱 DB 접속, postgres_exporter |

## Task 1: Terraform 스캐폴드

**Files:**
- Create: `infra/load-test/versions.tf`
- Create: `infra/load-test/variables.tf`
- Create: `infra/load-test/locals.tf`
- Create: `infra/load-test/terraform.tfvars.example`
- Modify: `.gitignore`

- [ ] **Step 1: Terraform 디렉터리 생성**

Run:

```bash
mkdir -p infra/load-test/templates
```

Expected: `infra/load-test/templates` 디렉터리가 생긴다.

- [ ] **Step 2: provider 버전 파일 작성**

Write `infra/load-test/versions.tf`:

```hcl
terraform {
  required_version = ">= 1.8.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.4"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "aws" {
  region = var.aws_region
}
```

- [ ] **Step 3: 변수 파일 작성**

Write `infra/load-test/variables.tf` with these variables:

```hcl
variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "primary_az" {
  type    = string
  default = "ap-northeast-2a"
}

variable "secondary_az" {
  type    = string
  default = "ap-northeast-2c"
}

variable "admin_cidr" {
  type        = string
  description = "관리자 PC의 public CIDR. 예: 203.0.113.10/32"
}

variable "key_name" {
  type        = string
  default     = null
  description = "SSH key pair name. SSM만 쓸 경우 null."
}

variable "registry_server" {
  type    = string
  default = "ghcr.io"
}

variable "image_name" {
  type        = string
  description = "예: ghcr.io/<owner>/<image>"
}

variable "image_tag" {
  type        = string
  description = "부하 테스트에 사용할 앱 이미지 태그"
}

variable "registry_secret_arn" {
  type        = string
  default     = null
  description = "private registry 로그인 정보 JSON secret ARN. public image면 null."
}

variable "app_env_secret_arn" {
  type        = string
  description = "앱 필수 런타임 env를 담은 Secrets Manager secret ARN"
}

variable "sut_instance_type" {
  type    = string
  default = "t3.medium"
}

variable "loadgen_instance_type" {
  type    = string
  default = "c7i.large"
}

variable "monitoring_instance_type" {
  type    = string
  default = "t3.large"
}

variable "db_name" {
  type    = string
  default = "umc_product"
}

variable "db_username" {
  type    = string
  default = "umc_product"
}

variable "db_engine_version" {
  type    = string
  default = "18"
}
```

- [ ] **Step 4: locals 작성**

Write `infra/load-test/locals.tf`:

```hcl
locals {
  name_prefix = "umc-load-test"

  common_tags = {
    Project     = "umc-product"
    Environment = "load-test"
    ManagedBy   = "terraform"
    Ephemeral   = "true"
  }

  app_port        = 8080
  management_port = 9090
  node_port       = 9100
  otlp_http_port  = 4318
  otlp_grpc_port  = 4317
}
```

- [ ] **Step 5: tfvars 예시 작성**

Write `infra/load-test/terraform.tfvars.example`:

```hcl
admin_cidr        = "203.0.113.10/32"
image_name        = "ghcr.io/umc-product/umc-product-server"
image_tag         = "develop-20260628"
app_env_secret_arn = "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:umc-load-test/app-env"

# private registry인 경우만 사용
# registry_secret_arn = "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:umc-load-test/registry"
```

- [ ] **Step 6: Git ignore 보강**

Append to `.gitignore`:

```gitignore
# Terraform
**/.terraform/
*.tfstate
*.tfstate.*
*.tfvars
!*.tfvars.example
crash.log
crash.*.log
```

- [ ] **Step 7: 포맷 검증**

Run:

```bash
terraform -chdir=infra/load-test fmt
```

Expected: command exits 0.

## Task 2: 네트워크와 Security Group

**Files:**
- Create: `infra/load-test/network.tf`
- Create: `infra/load-test/security-groups.tf`

- [ ] **Step 1: VPC/Subnet 작성**

Write `infra/load-test/network.tf` with one VPC, two public subnets, two private subnets, internet gateway, and a public route table. Use:

```hcl
resource "aws_vpc" "this" {
  cidr_block           = "10.42.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags                 = merge(local.common_tags, { Name = "${local.name_prefix}-vpc" })
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id
  tags   = merge(local.common_tags, { Name = "${local.name_prefix}-igw" })
}

resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = "10.42.1.0/24"
  availability_zone       = var.primary_az
  map_public_ip_on_launch = true
  tags                    = merge(local.common_tags, { Name = "${local.name_prefix}-public-a" })
}

resource "aws_subnet" "public_b" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = "10.42.2.0/24"
  availability_zone       = var.secondary_az
  map_public_ip_on_launch = true
  tags                    = merge(local.common_tags, { Name = "${local.name_prefix}-public-b" })
}

resource "aws_subnet" "private_a" {
  vpc_id            = aws_vpc.this.id
  cidr_block        = "10.42.11.0/24"
  availability_zone = var.primary_az
  tags              = merge(local.common_tags, { Name = "${local.name_prefix}-private-a" })
}

resource "aws_subnet" "private_b" {
  vpc_id            = aws_vpc.this.id
  cidr_block        = "10.42.12.0/24"
  availability_zone = var.secondary_az
  tags              = merge(local.common_tags, { Name = "${local.name_prefix}-private-b" })
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }
  tags = merge(local.common_tags, { Name = "${local.name_prefix}-public-rt" })
}
```

Add route table associations for both public subnets.

- [ ] **Step 2: SG 파일 작성**

Write `infra/load-test/security-groups.tf` using separate SGs:

```hcl
resource "aws_security_group" "sut" {
  name        = "${local.name_prefix}-sut"
  description = "Load test SUT"
  vpc_id      = aws_vpc.this.id
  tags        = merge(local.common_tags, { Name = "${local.name_prefix}-sut-sg" })
}

resource "aws_security_group" "loadgen" {
  name        = "${local.name_prefix}-loadgen"
  description = "Load generator"
  vpc_id      = aws_vpc.this.id
  tags        = merge(local.common_tags, { Name = "${local.name_prefix}-loadgen-sg" })
}

resource "aws_security_group" "monitoring" {
  name        = "${local.name_prefix}-monitoring"
  description = "Ephemeral monitoring stack"
  vpc_id      = aws_vpc.this.id
  tags        = merge(local.common_tags, { Name = "${local.name_prefix}-monitoring-sg" })
}

resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds"
  description = "Load test RDS"
  vpc_id      = aws_vpc.this.id
  tags        = merge(local.common_tags, { Name = "${local.name_prefix}-rds-sg" })
}
```

Add `aws_vpc_security_group_ingress_rule` and `aws_vpc_security_group_egress_rule` resources matching the matrix above. Use allow-all egress for EC2 so Docker/GHCR/Grafana packages can be installed.

- [ ] **Step 3: Terraform validate**

Run:

```bash
terraform -chdir=infra/load-test init
terraform -chdir=infra/load-test validate
```

Expected: `Success! The configuration is valid.`

## Task 3: IAM과 bootstrap artifact

**Files:**
- Create: `infra/load-test/iam.tf`
- Create: `infra/load-test/artifacts.tf`

- [ ] **Step 1: EC2 IAM role 작성**

Write `infra/load-test/iam.tf` with:

- `aws_iam_role.ec2`
- `aws_iam_instance_profile.ec2`
- managed policy attachment `AmazonSSMManagedInstanceCore`
- inline policy allowing:
  - `s3:GetObject` on artifact bucket objects
  - `secretsmanager:GetSecretValue` on `var.app_env_secret_arn`
  - `secretsmanager:GetSecretValue` on `var.registry_secret_arn` when not null

Use this trust policy:

```hcl
data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}
```

- [ ] **Step 2: artifact bucket 작성**

Write `infra/load-test/artifacts.tf` with:

- `random_id.artifact_suffix`
- `aws_s3_bucket.artifacts`
- private ACL ownership controls
- server-side encryption
- public access block
- lifecycle rule expiring objects after 7 days

Bucket name format:

```hcl
bucket = "${local.name_prefix}-artifacts-${random_id.artifact_suffix.hex}"
```

- [ ] **Step 3: monitoring artifact 업로드**

In `artifacts.tf`, add `data.archive_file.monitoring` using `source_dir = "${path.module}/../../infra/monitoring/grafana"`, `output_path = "${path.module}/.terraform/monitoring.zip"`, and excludes:

```hcl
excludes = [
  ".env",
  ".env.*",
  ".claude",
  "**/.DS_Store"
]
```

Upload it as:

```hcl
resource "aws_s3_object" "monitoring_artifact" {
  bucket = aws_s3_bucket.artifacts.id
  key    = "monitoring-${data.archive_file.monitoring.output_md5}.zip"
  source = data.archive_file.monitoring.output_path
  etag   = data.archive_file.monitoring.output_md5
}
```

- [ ] **Step 4: k6 artifact 업로드**

Add `data.archive_file.k6` for `docs/guides/load-test/k6` with `output_path = "${path.module}/.terraform/k6.zip"` and upload it to the same bucket. Exclude generated `data/seed.json` so real test data is copied manually or created by Phase 03.

Excludes:

```hcl
excludes = [
  "data/seed.json",
  "**/.DS_Store"
]
```

- [ ] **Step 5: validate**

Run:

```bash
terraform -chdir=infra/load-test fmt
terraform -chdir=infra/load-test validate
```

Expected: command exits 0.

## Task 4: RDS PostgreSQL

**Files:**
- Create: `infra/load-test/rds.tf`

- [ ] **Step 1: DB password와 subnet group 작성**

Write:

```hcl
resource "random_password" "db" {
  length  = 32
  special = false
}

resource "aws_db_subnet_group" "this" {
  name       = "${local.name_prefix}-db-subnet-group"
  subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_b.id]
  tags       = merge(local.common_tags, { Name = "${local.name_prefix}-db-subnet-group" })
}
```

- [ ] **Step 2: RDS instance 작성**

Write:

```hcl
resource "aws_db_instance" "postgres" {
  identifier             = "${local.name_prefix}-postgres"
  engine                 = "postgres"
  engine_version         = var.db_engine_version
  instance_class         = "db.t4g.small"
  allocated_storage      = 20
  max_allocated_storage  = 100
  storage_type           = "gp3"
  db_name                = var.db_name
  username               = var.db_username
  password               = random_password.db.result
  port                   = 5432
  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  multi_az               = false
  deletion_protection    = false
  skip_final_snapshot    = true
  backup_retention_period = 0
  apply_immediately      = true
  tags                   = merge(local.common_tags, { Name = "${local.name_prefix}-postgres" })
}
```

- [ ] **Step 3: RDS version 확인 명령 문서화**

Add this command to the implementation notes when writing `infra/load-test/README.md`:

```bash
aws rds describe-db-engine-versions \
  --engine postgres \
  --engine-version 18 \
  --region ap-northeast-2 \
  --query 'DBEngineVersions[].EngineVersion'
```

Expected: the selected `db_engine_version` is present.

## Task 5: SUT EC2와 app compose

**Files:**
- Create: `infra/load-test/templates/sut-cloud-init.yaml.tftpl`
- Create: `infra/load-test/templates/sut-compose.yml.tftpl`
- Modify: `infra/load-test/ec2.tf`

- [ ] **Step 1: SUT compose template 작성**

Write `infra/load-test/templates/sut-compose.yml.tftpl`:

```yaml
services:
  nginx:
    image: nginx:1.31.1
    container_name: umc-load-test-nginx
    restart: unless-stopped
    depends_on:
      app:
        condition: service_started
    ports:
      - "8080:80"
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro

  app:
    image: ${image_name}:${image_tag}
    container_name: umc-load-test-app
    restart: unless-stopped
    depends_on:
      valkey:
        condition: service_healthy
    env_file:
      - app.env
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SERVER_PORT: "8080"
      MANAGEMENT_PORT: "9090"
      DATABASE_URL: jdbc:postgresql://${db_host}:5432/${db_name}
      DATABASE_USERNAME: ${db_username}
      DATABASE_PASSWORD: ${db_password}
      HIKARI_MAX_POOL_SIZE: "4"
      HIKARI_MIN_IDLE: "4"
      APP_SEED_ENABLED: "true"
      OTEL_URL: http://${monitoring_private_ip}:4318
      OTEL_AUTH_HEADER: "Bearer ${otel_ingest_token}"
    ports:
      - "9090:9090"
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://localhost:9090/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 30
      start_period: 60s

  valkey:
    image: valkey/valkey:9.1
    container_name: umc-load-test-valkey
    restart: unless-stopped
    command: ["valkey-server", "--appendonly", "yes"]
    volumes:
      - valkey-data:/data
    healthcheck:
      test: ["CMD", "valkey-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 10

  node-exporter:
    image: prom/node-exporter:v1.11.1
    container_name: umc-load-test-sut-node-exporter
    restart: unless-stopped
    command:
      - --path.rootfs=/host
      - --collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)
    pid: host
    volumes:
      - /:/host:ro,rslave
    ports:
      - "9100:9100"

volumes:
  valkey-data:
```

- [ ] **Step 2: SUT cloud-init 작성**

Write `infra/load-test/templates/sut-cloud-init.yaml.tftpl` to:

1. install Docker, Docker Compose plugin, AWS CLI, unzip, curl
2. fetch `app_env_secret_arn` into `/opt/umc-product/app.env`
3. append Terraform-generated load-test overrides to `app.env`
4. login to registry only if `registry_secret_arn` is set
5. write `docker-compose.yml` from the template
6. copy `docker/develop/nginx.conf` content or write an equivalent nginx config
7. run `docker compose pull && docker compose up -d`
8. wait until `http://localhost:9090/actuator/health` returns 2xx

Use shell guard:

```bash
set -euo pipefail
exec > >(tee /var/log/umc-load-test-sut-user-data.log | logger -t user-data -s 2>/dev/console) 2>&1
```

- [ ] **Step 3: SUT EC2 작성**

In `infra/load-test/ec2.tf`, add Amazon Linux 2023 AMI lookup and SUT instance:

```hcl
data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
}

resource "random_password" "otel_ingest" {
  length  = 32
  special = false
}

resource "aws_instance" "sut" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.sut_instance_type
  subnet_id                   = aws_subnet.public_a.id
  vpc_security_group_ids      = [aws_security_group.sut.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.ec2.name
  key_name                    = var.key_name
  user_data_replace_on_change = true
  user_data                   = templatefile("${path.module}/templates/sut-cloud-init.yaml.tftpl", {
    image_name            = var.image_name
    image_tag             = var.image_tag
    registry_server       = var.registry_server
    registry_secret_arn   = var.registry_secret_arn
    app_env_secret_arn    = var.app_env_secret_arn
    db_host               = aws_db_instance.postgres.address
    db_name               = var.db_name
    db_username           = var.db_username
    db_password           = random_password.db.result
    monitoring_private_ip = aws_instance.monitoring.private_ip
    otel_ingest_token     = random_password.otel_ingest.result
  })
  tags = merge(local.common_tags, { Name = "${local.name_prefix}-sut" })
}
```

Note: this creates a dependency from SUT to monitoring. That is acceptable because app boot needs the collector endpoint.

- [ ] **Step 4: SUT smoke 검증 명령 준비**

After apply, run from local:

```bash
curl -f http://$(terraform -chdir=infra/load-test output -raw sut_public_ip):8080/nginx-health
curl -f http://$(terraform -chdir=infra/load-test output -raw sut_public_ip):9090/actuator/health
```

Expected: both commands exit 0.

## Task 6: Monitoring EC2와 전용 관측 스택

**Files:**
- Create: `infra/load-test/templates/monitoring-cloud-init.yaml.tftpl`
- Create: `infra/load-test/templates/prometheus-load-test.yml.tftpl`
- Create: `infra/load-test/templates/monitoring-compose.override.yml.tftpl`
- Modify: `infra/load-test/ec2.tf`

- [ ] **Step 1: Prometheus config template 작성**

Write `infra/load-test/templates/prometheus-load-test.yml.tftpl`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: umc-product-load-test

scrape_configs:
  - job_name: prometheus
    static_configs:
      - targets: ["localhost:9090"]

  - job_name: node-exporter
    static_configs:
      - targets: ["node-exporter:9100"]
        labels:
          role: monitoring
      - targets: ["${sut_private_ip}:9100"]
        labels:
          role: sut
      - targets: ["${loadgen_private_ip}:9100"]
        labels:
          role: loadgen

  - job_name: postgres-exporter
    static_configs:
      - targets: ["postgres-exporter:9187"]
        labels:
          role: rds

  - job_name: alertmanager
    static_configs:
      - targets: ["alertmanager:9093"]

  - job_name: grafana
    static_configs:
      - targets: ["grafana:3000"]

  - job_name: loki
    static_configs:
      - targets: ["loki:3100"]

  - job_name: otel-collector
    static_configs:
      - targets: ["otel-collector:8888"]

alerting:
  alertmanagers:
    - static_configs:
        - targets: ["alertmanager:9093"]

rule_files:
  - /etc/prometheus/rules/*.yml
```

- [ ] **Step 2: monitoring override 작성**

Write `infra/load-test/templates/monitoring-compose.override.yml.tftpl`:

```yaml
services:
  prometheus:
    command:
      - --config.file=/etc/prometheus/prometheus.yml
      - --storage.tsdb.path=/prometheus
      - --storage.tsdb.retention.time=3d
      - --web.enable-otlp-receiver
      - --web.enable-remote-write-receiver
      - --web.enable-lifecycle

  postgres-exporter:
    image: prometheuscommunity/postgres-exporter:v0.17.1
    container_name: umc-load-test-postgres-exporter
    environment:
      DATA_SOURCE_URI: "${db_host}:5432/${db_name}?sslmode=require"
      DATA_SOURCE_USER: "${db_username}"
      DATA_SOURCE_PASS: "${db_password}"
    networks: [monitoring]
    restart: unless-stopped
```

- [ ] **Step 3: monitoring cloud-init 작성**

Write `infra/load-test/templates/monitoring-cloud-init.yaml.tftpl` to:

1. install Docker, Docker Compose plugin, AWS CLI, unzip, curl
2. download `s3://${artifact_bucket}/${monitoring_artifact_key}`
3. unzip into `/opt/umc-monitoring`
4. write `.env` with:

```dotenv
MONITORING_BIND_ADDRESS=0.0.0.0
OTEL_BIND_ADDRESS=0.0.0.0
GRAFANA_PORT=3000
PROMETHEUS_PORT=9090
LOKI_PORT=3100
TEMPO_HTTP_PORT=3200
TEMPO_OTLP_GRPC_PORT=4317
TEMPO_OTLP_HTTP_PORT=4318
OTEL_OTLP_GRPC_PORT=4317
OTEL_OTLP_HTTP_PORT=4318
OTEL_INGEST_TOKEN=${otel_ingest_token}
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=${grafana_admin_password}
DISCORD_ALERT_WEBHOOK=http://127.0.0.1:65535/load-test-alert-disabled
AWS_REGION=${aws_region}
```

5. overwrite `config/prometheus/prometheus.yml` with rendered template
6. write `docker-compose.load-test.override.yml`
7. run `docker compose -f docker-compose.yml -f docker-compose.load-test.override.yml --profile linux up -d`
8. wait for Grafana and Prometheus health

- [ ] **Step 4: monitoring EC2 작성**

In `infra/load-test/ec2.tf`, add:

```hcl
resource "random_password" "grafana_admin" {
  length  = 20
  special = false
}

resource "aws_instance" "monitoring" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.monitoring_instance_type
  subnet_id                   = aws_subnet.public_a.id
  vpc_security_group_ids      = [aws_security_group.monitoring.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.ec2.name
  key_name                    = var.key_name
  user_data_replace_on_change = true
  user_data                   = templatefile("${path.module}/templates/monitoring-cloud-init.yaml.tftpl", {
    aws_region              = var.aws_region
    artifact_bucket         = aws_s3_bucket.artifacts.id
    monitoring_artifact_key = aws_s3_object.monitoring_artifact.key
    otel_ingest_token       = random_password.otel_ingest.result
    grafana_admin_password  = random_password.grafana_admin.result
    sut_private_ip          = aws_instance.sut.private_ip
    loadgen_private_ip      = aws_instance.loadgen.private_ip
    db_host                 = aws_db_instance.postgres.address
    db_name                 = var.db_name
    db_username             = var.db_username
    db_password             = random_password.db.result
  })
  tags = merge(local.common_tags, { Name = "${local.name_prefix}-monitoring" })
}
```

Cycle warning: the snippet above references SUT/loadgen IPs while SUT references monitoring IP. Avoid a Terraform dependency cycle by using fixed private IPs:

```hcl
private_ip = "10.42.1.10" # monitoring
private_ip = "10.42.1.20" # sut
private_ip = "10.42.1.30" # loadgen
```

Set these static IPs in all three `aws_instance` resources and pass static locals into templates instead of cross-referencing instances.

- [ ] **Step 5: monitoring smoke 검증**

After apply:

```bash
curl -f http://$(terraform -chdir=infra/load-test output -raw grafana_public_ip):3000/api/health
curl -f http://$(terraform -chdir=infra/load-test output -raw grafana_public_ip):9090/-/ready
```

Expected: both commands exit 0.

## Task 7: Load generator EC2와 k6 runner

**Files:**
- Create: `infra/load-test/templates/loadgen-cloud-init.yaml.tftpl`
- Create: `infra/load-test/templates/run-k6.sh.tftpl`
- Modify: `infra/load-test/ec2.tf`

- [ ] **Step 1: run-k6 helper 작성**

Write `infra/load-test/templates/run-k6.sh.tftpl`:

```bash
#!/usr/bin/env bash
set -euo pipefail

PROFILE="${1:-smoke}"
RATE="${2:-50}"

cd /opt/umc-load-test/k6

export K6_OUT=experimental-prometheus-rw
export K6_PROMETHEUS_RW_SERVER_URL="http://${monitoring_private_ip}:9090/api/v1/write"
export K6_PROMETHEUS_RW_TREND_STATS="p(50),p(90),p(95),p(99),min,max,avg"

k6 run \
  -e BASE_URL="http://${sut_private_ip}:8080" \
  -e PROFILE="${PROFILE}" \
  -e RATE="${RATE}" \
  -e SEED_FILE="./data/seed.json" \
  script.js
```

- [ ] **Step 2: loadgen cloud-init 작성**

Write `infra/load-test/templates/loadgen-cloud-init.yaml.tftpl` to:

1. install Docker, AWS CLI, unzip, curl, gnupg
2. install k6 from Grafana package repository
3. download k6 artifact from S3 to `/opt/umc-load-test/k6`
4. copy `data/seed.example.json` to `data/seed.json` for the initial smoke file
5. write executable `/usr/local/bin/run-umc-k6`
6. run node-exporter container on port 9100

Use:

```bash
docker run -d \
  --name umc-load-test-loadgen-node-exporter \
  --restart unless-stopped \
  --pid host \
  -p 9100:9100 \
  -v /:/host:ro,rslave \
  prom/node-exporter:v1.11.1 \
  --path.rootfs=/host \
  '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
```

- [ ] **Step 3: loadgen EC2 작성**

In `infra/load-test/ec2.tf`, add loadgen instance with static private IP `10.42.1.30` and user data from `loadgen-cloud-init.yaml.tftpl`.

- [ ] **Step 4: k6 smoke 실행**

After Phase 03 seed file is copied, run on loadgen:

```bash
run-umc-k6 smoke 1
```

Expected:

- k6 checks pass
- Prometheus receives `k6_http_reqs`
- Grafana can show k6 RPS and latency on the same time range as app metrics

## Task 8: Outputs와 README

**Files:**
- Create: `infra/load-test/outputs.tf`
- Create: `infra/load-test/README.md`

- [ ] **Step 1: outputs 작성**

Write `infra/load-test/outputs.tf`:

```hcl
output "sut_public_ip" {
  value = aws_instance.sut.public_ip
}

output "sut_private_ip" {
  value = aws_instance.sut.private_ip
}

output "loadgen_public_ip" {
  value = aws_instance.loadgen.public_ip
}

output "loadgen_private_ip" {
  value = aws_instance.loadgen.private_ip
}

output "grafana_public_ip" {
  value = aws_instance.monitoring.public_ip
}

output "grafana_url" {
  value = "http://${aws_instance.monitoring.public_ip}:3000"
}

output "prometheus_url" {
  value = "http://${aws_instance.monitoring.public_ip}:9090"
}

output "grafana_admin_user" {
  value = "admin"
}

output "grafana_admin_password" {
  value     = random_password.grafana_admin.result
  sensitive = true
}

output "rds_endpoint" {
  value = aws_db_instance.postgres.address
}
```

- [ ] **Step 2: README 작성**

Write `infra/load-test/README.md` with these sections:

- Prerequisites:
  - AWS credentials
  - app image already pushed
  - app env secret exists
  - admin CIDR known
- Apply:

```bash
cd infra/load-test
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
terraform apply
```

- Smoke:

```bash
curl -f "$(terraform output -raw grafana_url)/api/health"
curl -f "http://$(terraform output -raw sut_public_ip):9090/actuator/health"
```

- k6:

```bash
ssh ec2-user@$(terraform output -raw loadgen_public_ip)
sudo cp /opt/umc-load-test/k6/data/seed.example.json /opt/umc-load-test/k6/data/seed.json
run-umc-k6 smoke 1
```

- Destroy:

```bash
terraform destroy
```

- Cost warning:
  - RDS, EC2, EBS, public IPv4, data transfer cost accrue until destroy.

- [ ] **Step 3: final validation**

Run:

```bash
terraform -chdir=infra/load-test fmt
terraform -chdir=infra/load-test validate
```

Expected: command exits 0.

## Task 9: Phase 01 완료 게이트

**Files:**
- No new files.

- [ ] **Step 1: Apply**

Run:

```bash
terraform -chdir=infra/load-test apply
```

Expected:

- SUT EC2 running
- loadgen EC2 running
- monitoring EC2 running
- RDS available
- user-data logs end without failure

- [ ] **Step 2: SUT request**

Run:

```bash
curl -f "http://$(terraform -chdir=infra/load-test output -raw sut_public_ip):8080/nginx-health"
curl -f "http://$(terraform -chdir=infra/load-test output -raw sut_public_ip):9090/actuator/health"
```

Expected: both return 2xx.

- [ ] **Step 3: Grafana/Prometheus 확인**

Run:

```bash
curl -f "$(terraform -chdir=infra/load-test output -raw grafana_url)/api/health"
curl -f "$(terraform -chdir=infra/load-test output -raw prometheus_url)/api/v1/query?query=up"
```

Expected:

- Grafana health is OK
- Prometheus `up` contains `node-exporter`, `otel-collector`, `postgres-exporter`

- [ ] **Step 4: 앱 메트릭 확인**

Run:

```bash
curl -G "$(terraform -chdir=infra/load-test output -raw prometheus_url)/api/v1/query" \
  --data-urlencode 'query=hikaricp_connections_pending'
```

Expected: app starts publishing HikariCP metrics after the first scrape/export interval.

- [ ] **Step 5: destroy 검증**

Run:

```bash
terraform -chdir=infra/load-test destroy
```

Expected: EC2, RDS, S3 artifact bucket objects, SG, VPC are removed.

## 다음 Phase 연결

- Phase 02:
  - `infra/monitoring/grafana/config/grafana/dashboards/load-test.json` 추가.
  - Prometheus remote-write receiver와 `postgres_exporter`는 이 Terraform plan에서 이미 bootstrap하므로 대시보드와 쿼리 검증에 집중한다.
- Phase 03:
  - SeedController로 데이터를 만든 뒤 `/opt/umc-load-test/k6/data/seed.json`을 loadgen에 배치한다.
  - RDS snapshot 생성/복원 절차를 `infra/load-test/README.md`에 추가한다.
- Phase 04:
  - `docs/guides/load-test/k6/script.js` smoke 보정 후 artifact 재업로드가 필요하므로 `terraform apply`를 다시 실행하거나 loadgen에 스크립트를 직접 갱신한다.

## Self Review

- Spec coverage:
  - SUT + RDS `db.t4g.small` + Hikari pool 4: Task 4, Task 5.
  - 별도 load generator EC2: Task 7.
  - 별도 monitoring EC2 + otel-collector: Task 6.
  - app/k6/postgres metrics path: Task 6, Task 7, Phase 02 연결.
  - `destroy` cleanup: Task 9.
- Placeholder scan:
  - 구현자가 값을 채워야 하는 항목은 `terraform.tfvars.example`과 Secrets Manager ARN처럼 환경 고유 값뿐이다.
  - 파일별 책임과 검증 명령을 모두 명시했다.
- Risk:
  - app image architecture가 x86_64를 지원해야 한다. 지원하지 않으면 AMI filter와 EC2 instance type을 arm64/t4g 계열로 바꾼다.
  - app 필수 env가 secret에 빠지면 SUT가 boot 실패한다. user-data log와 app container log로 확인한다.
  - static private IP를 쓰는 이유는 Terraform dependency cycle을 피하기 위해서다. CIDR 충돌이 생기면 locals로 IP를 한 곳에서 바꾼다.
