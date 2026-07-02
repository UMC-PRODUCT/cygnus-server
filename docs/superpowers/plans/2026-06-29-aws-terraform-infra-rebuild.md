# AWS Terraform Infra Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `umcproduct-readonly` AWS profile로 현재 UMC Product 서버 인프라를 실사하고, 그 실사 결과를 바탕으로 동일 구조를 Terraform으로 import/재생성할 수 있는 IaC 기반을 만든다. ALB/Route53 관리 대상 URL은 `university.neordinary.com` 계열만 둔다.

**Architecture:** Terraform 코드는 `infra/terraform` 아래에 환경(`envs/dev`, `envs/prod`)과 재사용 모듈(`modules/network`, `modules/security`, `modules/alb`, `modules/compute-asg`, `modules/rds`, `modules/dns`, `modules/iam`)을 분리한다. 1차 목표는 현재 운영 구조를 drift 없이 코드화하는 것이고, 2차 목표는 S3 secret 의존과 공개 SSH/NAT blackhole 같은 보안/운영 리스크를 명시적으로 개선 가능한 형태로 만드는 것이다.

**Tech Stack:** Terraform >= 1.6, AWS Provider 5.x/6.x 호환 설계, AWS CLI, ap-northeast-2, EC2/ASG/ALB/RDS/Route53/ACM/IAM/KMS/Secrets Manager.

---

## 현재 AWS 실사 요약

조회 기준:

```bash
aws sts get-caller-identity --profile umcproduct-readonly --output json
```

확인된 계정과 region:

- Account: `137809407320`
- Principal: `arn:aws:iam::137809407320:user/readonly`
- Region: `ap-northeast-2`

현재 애플리케이션 핵심 VPC:

- VPC: `vpc-0101ab9cb36353cc5`
- CIDR: `10.0.0.0/16`
- Name tag: `umc-prouct-server-vpc`

주요 subnet:

| 용도 | AZ | CIDR | Subnet ID | Name |
|---|---|---:|---|---|
| ALB public | 2a | `10.0.0.0/24` | `subnet-03b5e95c0297bcfeb` | `server-subnet-pub-2a` |
| ALB public | 2b | `10.0.1.0/24` | `subnet-0f578df7c07593340` | `server-subnet-pub-2b` |
| EC2 public app | 2a | `10.0.30.0/24` | `subnet-0f1e6fca9afdf0eb7` | `server-subnet-pub-app-2a` |
| EC2 public app | 2b | `10.0.31.0/24` | `subnet-0694296b62c8dc7e7` | `server-subnet-pub-app-2b` |
| app private | 2a | `10.0.10.0/24` | `subnet-0996146733a8eb8c1` | `server-subnet-pri-app-2a` |
| app private | 2b | `10.0.11.0/24` | `subnet-0f732a7943b67f494` | `server-subnet-pri-app-2b` |
| DB private | 2a | `10.0.20.0/24` | `subnet-0e46fa453b5318c12` | `server-subnet-pri-db-2a` |
| DB private | 2b | `10.0.21.0/24` | `subnet-002bbfa36ff0f7b15` | `server-subnet-pri-db-2b` |
| legacy EC2 public | 2a | `10.0.2.0/24` | `subnet-0caf472e4677bd195` | `server-subnet-pub-ec2-2a` |

현재 발견된 리스크:

- `server-sg-bastion-default`의 SSH ingress에 `0.0.0.0/0`가 포함되어 있다.
- private route table에 `nat-032efecf3692c04cd`로 가는 `0.0.0.0/0` route가 있으나 NAT Gateway 조회 결과는 비어 있고 route 상태는 `blackhole`이다.
- ASG launch template user-data에 S3 secret URI(`s3://product-team-secrets/server/{dev,prod}/.env`)가 포함되어 있다. 이는 GitHub Issue #1073의 Secrets Manager/KMS 전환 계획과 충돌한다.
- ASG EC2 root EBS volume은 `Encrypted=false`로 확인된다.
- RDS `server-rds-prod`는 `DeletionProtection=false`다.

## 범위와 결정

이 계획은 두 결과물을 동시에 만든다.

1. **Parity mode:** 현재 인프라를 Terraform state로 import할 수 있는 코드. 기존 리소스를 망가뜨리지 않는 것이 우선이다.
2. **Rebuild mode:** 새 환경에서 같은 구조를 바로 생성할 수 있는 코드. 기본값은 안전 개선을 반영한다.

기본 결정:

- `umcproduct-readonly`는 실사와 `terraform plan -refresh-only` 검증에만 사용한다.
- 실제 `terraform apply`는 별도 write 권한 profile을 사용한다. 예: `umcproduct-admin` 또는 GitHub Actions OIDC role.
- 같은 VPC, ALB, Route53 record를 둘 이상의 Terraform state가 동시에 소유하지 않는다. dev/prod root를 분리해 구현하는 경우 shared 리소스 소유자를 먼저 정하고 다른 root는 remote state output 또는 data source로 참조한다.
- ASG EC2는 1차 parity에서 현재처럼 public app subnet에 둔다. 별도 hardening task에서 private app subnet + VPC endpoints 또는 NAT로 전환한다.
- S3 secret read 정책은 parity에는 data/import로 남기되, rebuild 기본값은 환경별로 분리한다.
  - dev: 비용 절감을 위해 홈서버/로컬 root-only env file을 기본 secret source로 사용한다. 공유 dev가 필요할 때만 SSM Parameter Store Standard 또는 Secrets Manager를 선택한다.
  - prod: Secrets Manager를 사용하되 하나의 runtime secret으로 합치지 않고 JWT key ring, OAuth client, data protection, database 단위로 분리한다.
- JWT key ring은 저장소 객체 하나 안에 둘 수 있지만, access, refresh, OAuth verification, email verification signing key는 용도별로 분리한다.
- RDS master password는 Terraform state에 평문으로 남기지 않도록 `manage_master_user_password = true` 사용을 목표로 한다. 기존 RDS import 시에는 현 상태를 보존하고 신규 rebuild에서 적용한다.
- ALB listener rule, Route53 record, 운영 예시는 `api.university.neordinary.com`과 `dev.api.university.neordinary.com`만 관리 대상으로 둔다.

## 파일 구조

생성할 파일:

```text
infra/terraform/
  README.md
  versions.tf
  providers.tf
  backend.example.hcl
  modules/
    network/
      main.tf
      variables.tf
      outputs.tf
    security/
      main.tf
      variables.tf
      outputs.tf
    iam/
      main.tf
      variables.tf
      outputs.tf
      user-data/
        asg-app.sh.tftpl
    alb/
      main.tf
      variables.tf
      outputs.tf
    compute-asg/
      main.tf
      variables.tf
      outputs.tf
    rds/
      main.tf
      variables.tf
      outputs.tf
    dns/
      main.tf
      variables.tf
      outputs.tf
  envs/
    shared/
      backend.tf
      main.tf
      variables.tf
      terraform.tfvars.example
      imports.tf
      moved.tf
    dev/
      backend.tf
      main.tf
      variables.tf
      terraform.tfvars.example
      imports.tf
      moved.tf
    prod/
      backend.tf
      main.tf
      variables.tf
      terraform.tfvars.example
      imports.tf
      moved.tf
scripts/
  aws-infra-inventory.sh
docs/infra/
  aws-current-inventory.md
docs/onboarding/project/
  terraform-aws-infra.md
```

책임:

- `scripts/aws-infra-inventory.sh`: `umcproduct-readonly` profile로 현재 리소스 ID/속성을 JSON과 Markdown으로 덤프한다. secret 값과 user-data 원문은 출력하지 않는다.
- `docs/infra/aws-current-inventory.md`: 현재 구조, 보안 리스크, Terraform import 대상 목록을 기록한다.
- `infra/terraform/modules/network`: VPC, subnet, IGW, route table, S3 gateway endpoint.
- `infra/terraform/modules/security`: SG와 rule. SG rule은 독립 resource로 작성해 순환 참조를 줄인다.
- `infra/terraform/modules/iam`: EC2 instance profile, ECR pull policy, Secrets Manager read policy, user-data template.
- `infra/terraform/modules/alb`: ALB, target groups, listeners, listener rules, listener certificates.
- `infra/terraform/modules/compute-asg`: launch template, ASG, ASG target group attachment.
- `infra/terraform/modules/rds`: DB subnet group, PostgreSQL RDS.
- `infra/terraform/modules/dns`: `university.neordinary.com` hosted zone의 Route53 A alias 관리.
- `infra/terraform/envs/shared`: VPC, subnet, SG, ALB, target groups, Route53처럼 dev/prod가 함께 쓰는 리소스의 단일 state 소유자.
- `infra/terraform/envs/dev`: dev ASG와 dev 전용 IAM/policy를 소유하고 shared state output을 참조한다.
- `infra/terraform/envs/prod`: prod ASG, prod 전용 IAM/policy, RDS를 소유하고 shared state output을 참조한다.
- `docs/onboarding/project/terraform-aws-infra.md`: Terraform 파일별 책임, 전체 생성 절차, import 절차, scale-in/out/up/down 운영 절차를 설명한다.

---

### Task 1: AWS Inventory Script

**Files:**
- Create: `scripts/aws-infra-inventory.sh`
- Create: `docs/infra/aws-current-inventory.md`

- [ ] **Step 1: inventory script 초안 작성**

`scripts/aws-infra-inventory.sh`를 생성한다.

```bash
#!/usr/bin/env bash
set -euo pipefail

PROFILE="${AWS_PROFILE:-umcproduct-readonly}"
REGION="${AWS_REGION:-ap-northeast-2}"
OUT_DIR="${1:-docs/infra/generated}"

mkdir -p "${OUT_DIR}"

aws_cli() {
  aws "$@" --profile "${PROFILE}" --region "${REGION}" --output json
}

aws sts get-caller-identity --profile "${PROFILE}" --output json > "${OUT_DIR}/caller-identity.json"

aws_cli ec2 describe-vpcs \
  --query 'Vpcs[].{VpcId:VpcId,Cidr:CidrBlock,IsDefault:IsDefault,State:State,Tags:Tags}' \
  > "${OUT_DIR}/vpcs.json"

aws_cli ec2 describe-subnets \
  --query 'Subnets[].{SubnetId:SubnetId,VpcId:VpcId,Az:AvailabilityZone,Cidr:CidrBlock,MapPublicIpOnLaunch:MapPublicIpOnLaunch,State:State,Tags:Tags}' \
  > "${OUT_DIR}/subnets.json"

aws_cli ec2 describe-security-groups \
  --query 'SecurityGroups[].{GroupId:GroupId,GroupName:GroupName,Description:Description,VpcId:VpcId,Tags:Tags,Ingress:IpPermissions,Egress:IpPermissionsEgress}' \
  > "${OUT_DIR}/security-groups.json"

aws_cli ec2 describe-route-tables \
  --query 'RouteTables[].{RouteTableId:RouteTableId,VpcId:VpcId,Associations:Associations,Routes:Routes,Tags:Tags}' \
  > "${OUT_DIR}/route-tables.json"

aws_cli ec2 describe-internet-gateways \
  --query 'InternetGateways[].{InternetGatewayId:InternetGatewayId,Attachments:Attachments,Tags:Tags}' \
  > "${OUT_DIR}/internet-gateways.json"

aws_cli ec2 describe-vpc-endpoints \
  --query 'VpcEndpoints[].{VpcEndpointId:VpcEndpointId,ServiceName:ServiceName,VpcEndpointType:VpcEndpointType,State:State,VpcId:VpcId,SubnetIds:SubnetIds,RouteTableIds:RouteTableIds,Groups:Groups,PrivateDnsEnabled:PrivateDnsEnabled,Tags:Tags}' \
  > "${OUT_DIR}/vpc-endpoints.json"

aws_cli elbv2 describe-load-balancers \
  --query 'LoadBalancers[].{Name:LoadBalancerName,Arn:LoadBalancerArn,DNS:DNSName,Type:Type,Scheme:Scheme,VpcId:VpcId,Subnets:AvailabilityZones[].SubnetId,SecurityGroups:SecurityGroups,State:State.Code}' \
  > "${OUT_DIR}/load-balancers.json"

aws_cli elbv2 describe-target-groups \
  --query 'TargetGroups[].{Name:TargetGroupName,Arn:TargetGroupArn,VpcId:VpcId,Protocol:Protocol,Port:Port,TargetType:TargetType,HealthCheckProtocol:HealthCheckProtocol,HealthCheckPath:HealthCheckPath,HealthCheckPort:HealthCheckPort,Matcher:Matcher,HealthyThreshold:HealthyThresholdCount,UnhealthyThreshold:UnhealthyThresholdCount,Interval:HealthCheckIntervalSeconds,Timeout:HealthCheckTimeoutSeconds}' \
  > "${OUT_DIR}/target-groups.json"

aws_cli autoscaling describe-auto-scaling-groups \
  --query 'AutoScalingGroups[].{Name:AutoScalingGroupName,LaunchTemplate:LaunchTemplate,VPCZoneIdentifier:VPCZoneIdentifier,Min:MinSize,Max:MaxSize,Desired:DesiredCapacity,TargetGroupARNs:TargetGroupARNs,HealthCheckType:HealthCheckType,HealthCheckGracePeriod:HealthCheckGracePeriod,Tags:Tags}' \
  > "${OUT_DIR}/auto-scaling-groups.json"

aws_cli rds describe-db-instances \
  --query 'DBInstances[].{Identifier:DBInstanceIdentifier,Engine:Engine,EngineVersion:EngineVersion,Class:DBInstanceClass,Status:DBInstanceStatus,MultiAZ:MultiAZ,StorageType:StorageType,AllocatedStorage:AllocatedStorage,VpcSecurityGroups:VpcSecurityGroups[].VpcSecurityGroupId,DBSubnetGroup:DBSubnetGroup.DBSubnetGroupName,Subnets:DBSubnetGroup.Subnets[].SubnetIdentifier,PubliclyAccessible:PubliclyAccessible,DeletionProtection:DeletionProtection,BackupRetentionPeriod:BackupRetentionPeriod,StorageEncrypted:StorageEncrypted,KmsKeyId:KmsKeyId}' \
  > "${OUT_DIR}/rds-instances.json"

echo "Inventory written to ${OUT_DIR}"
```

- [ ] **Step 2: 실행 권한 부여**

Run:

```bash
chmod +x scripts/aws-infra-inventory.sh
```

Expected: exit code `0`.

- [ ] **Step 3: readonly profile로 inventory 실행**

Run:

```bash
AWS_PROFILE=umcproduct-readonly AWS_REGION=ap-northeast-2 scripts/aws-infra-inventory.sh
```

Expected:

```text
Inventory written to docs/infra/generated
```

- [ ] **Step 4: 현재 구조 문서 작성**

`docs/infra/aws-current-inventory.md`를 작성한다. 문서에는 최소한 다음 항목을 포함한다.

```markdown
# AWS Current Inventory

## Account

- Account: `137809407320`
- Region: `ap-northeast-2`
- Read profile: `umcproduct-readonly`

## Core VPC

- VPC ID: `vpc-0101ab9cb36353cc5`
- CIDR: `10.0.0.0/16`
- Name: `umc-prouct-server-vpc`

## Routing Notes

- Public route table `rtb-0339f36c1ea252ba0` routes `0.0.0.0/0` to `igw-00a6087e41662b26b`.
- Private route table `rtb-0f029c27b8898036c` has S3 gateway endpoint `vpce-0f77eae66ff7327cd`.
- Private route table has blackhole NAT route to `nat-032efecf3692c04cd`; NAT Gateway is not present.

## Application Entry

- ALB: `server-alb-default`
- HTTP 80 redirects to HTTPS 443.
- HTTPS listener uses ACM certificate for `university.neordinary.com`.
- Host header rules:
  - dev: `dev.api.university.neordinary.com`
  - prod: `api.university.neordinary.com`

## Compute

- dev ASG: `dev-umc-product-server-asg`, launch template `lt-00d81a2f4497849f8`, version `84`
- prod ASG: `prod-umc-product-server-asg`, launch template `lt-099a89fa42efbbac9`, version `20`
- Both ASGs use `t4g.small`, AMI `ami-0fdcb184d4d869b49`.

## Database

- RDS: `server-rds-prod`
- Engine: PostgreSQL `18.3`
- Class: `db.t4g.small`
- Storage: `20GB gp3`
- Private: `true`
- Backup retention: `7`
- Deletion protection: `false`

## Security Findings

- Bastion SG currently allows SSH from `0.0.0.0/0`; Terraform rebuild default must not preserve this unless explicitly enabled.
- ASG user-data reads S3 secret file; rebuild default should use Secrets Manager from issue #1073.
- EC2 root EBS volumes are unencrypted; rebuild default should enable encryption.
```

- [ ] **Step 5: commit**

Run:

```bash
git add scripts/aws-infra-inventory.sh docs/infra/aws-current-inventory.md
git commit -m "docs: document current aws infrastructure inventory"
```

### Task 2: Terraform Project Skeleton

**Files:**
- Create: `infra/terraform/README.md`
- Create: `infra/terraform/versions.tf`
- Create: `infra/terraform/providers.tf`
- Create: `infra/terraform/backend.example.hcl`
- Create: `infra/terraform/envs/shared/backend.tf`
- Create: `infra/terraform/envs/shared/main.tf`
- Create: `infra/terraform/envs/shared/variables.tf`
- Create: `infra/terraform/envs/shared/terraform.tfvars.example`
- Create: `infra/terraform/envs/shared/imports.tf`
- Create: `infra/terraform/envs/shared/moved.tf`
- Create: `infra/terraform/envs/dev/backend.tf`
- Create: `infra/terraform/envs/dev/main.tf`
- Create: `infra/terraform/envs/dev/variables.tf`
- Create: `infra/terraform/envs/dev/terraform.tfvars.example`
- Create: `infra/terraform/envs/dev/imports.tf`
- Create: `infra/terraform/envs/dev/moved.tf`
- Create: `infra/terraform/envs/prod/backend.tf`
- Create: `infra/terraform/envs/prod/main.tf`
- Create: `infra/terraform/envs/prod/variables.tf`
- Create: `infra/terraform/envs/prod/terraform.tfvars.example`
- Create: `infra/terraform/envs/prod/imports.tf`
- Create: `infra/terraform/envs/prod/moved.tf`

- [ ] **Step 1: root Terraform version 파일 작성**

`infra/terraform/versions.tf`:

```hcl
terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.80, < 7.0"
    }
  }
}
```

- [ ] **Step 2: provider 파일 작성**

`infra/terraform/providers.tf`:

```hcl
provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile

  default_tags {
    tags = {
      Project     = "umc-product-server"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

variable "aws_region" {
  type        = string
  description = "AWS region."
  default     = "ap-northeast-2"
}

variable "aws_profile" {
  type        = string
  description = "AWS CLI profile used by Terraform."
}

variable "environment" {
  type        = string
  description = "Environment name such as dev or prod."
}
```

- [ ] **Step 3: backend example 작성**

`infra/terraform/backend.example.hcl`:

```hcl
bucket         = "umc-product-terraform-state"
key            = "umc-product-server/shared/terraform.tfstate"
region         = "ap-northeast-2"
dynamodb_table = "umc-product-terraform-lock"
encrypt        = true
```

각 root의 remote state key는 반드시 분리한다.

- shared: `umc-product-server/shared/terraform.tfstate`
- dev: `umc-product-server/dev/terraform.tfstate`
- prod: `umc-product-server/prod/terraform.tfstate`

`infra/terraform/envs/{shared,dev,prod}/backend.tf`:

```hcl
terraform {
  backend "s3" {}
}
```

- [ ] **Step 4: README 작성**

`infra/terraform/README.md`:

```markdown
# UMC Product Server Terraform

## Profiles

- `umcproduct-readonly`: discovery, inventory, refresh-only plan.
- write profile: import, normal plan, real `terraform apply`. Do not use readonly for apply.

## Validate Commands

```bash
cd infra/terraform/envs/shared
terraform init -backend=false
terraform validate
```

## Plan and Apply Commands

```bash
cd infra/terraform/envs/shared
terraform init -backend-config=../../backend.shared.hcl
terraform plan -var-file=terraform.tfvars -out=tfplan
terraform apply tfplan
```

## Import Safety

Existing production resources must be imported before Terraform is allowed to manage them.
Run `terraform plan` after every import block and verify no destructive action appears.
```
```

- [ ] **Step 5: env skeleton 작성**

`infra/terraform/envs/shared/main.tf`:

```hcl
module "network" {
  source      = "../../modules/network"
  name_prefix = "server"
  environment = var.environment
  vpc_cidr    = "10.0.0.0/16"
}
```

`infra/terraform/envs/shared/variables.tf`:

```hcl
variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "aws_profile" {
  type    = string
  default = null
}

variable "environment" {
  type    = string
  default = "shared"
}
```

`infra/terraform/envs/shared/terraform.tfvars.example`:

```hcl
aws_region  = "ap-northeast-2"
aws_profile = "umcproduct-admin"
environment = "shared"
```

dev/prod root는 shared 리소스를 다시 선언하지 않는다. compute, env-specific IAM, prod RDS처럼 환경별 리소스만 조립하고 shared output은 remote state 또는 data source로 참조한다.

- [ ] **Step 6: validate**

Run:

```bash
cd infra/terraform/envs/dev
terraform init -backend=false
terraform validate
```

Expected: `Success! The configuration is valid.`

- [ ] **Step 7: commit**

Run:

```bash
git add infra/terraform
git commit -m "chore: scaffold terraform project"
```

### Task 3: Network Module

**Files:**
- Create: `infra/terraform/modules/network/main.tf`
- Create: `infra/terraform/modules/network/variables.tf`
- Create: `infra/terraform/modules/network/outputs.tf`
- Modify: `infra/terraform/envs/shared/main.tf`

- [ ] **Step 1: network variables 작성**

`infra/terraform/modules/network/variables.tf`:

```hcl
variable "name_prefix" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_cidr" {
  type = string
}

variable "subnets" {
  type = map(object({
    cidr                    = string
    availability_zone       = string
    map_public_ip_on_launch = bool
    route_table             = string
  }))
}

variable "enable_s3_gateway_endpoint" {
  type    = bool
  default = true
}
```

- [ ] **Step 2: network resources 작성**

`infra/terraform/modules/network/main.tf`:

```hcl
resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "umc-prouct-server-vpc"
  }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "server-igw"
  }
}

resource "aws_subnet" "this" {
  for_each = var.subnets

  vpc_id                  = aws_vpc.this.id
  cidr_block              = each.value.cidr
  availability_zone       = each.value.availability_zone
  map_public_ip_on_launch = each.value.map_public_ip_on_launch

  tags = {
    Name = each.key
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = {
    Name = "server-rtb-public"
  }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "server-rtb-private"
  }
}

resource "aws_route_table_association" "public" {
  for_each = {
    for name, subnet in var.subnets : name => subnet
    if subnet.route_table == "public"
  }

  subnet_id      = aws_subnet.this[each.key].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  for_each = {
    for name, subnet in var.subnets : name => subnet
    if subnet.route_table == "private"
  }

  subnet_id      = aws_subnet.this[each.key].id
  route_table_id = aws_route_table.private.id
}

resource "aws_vpc_endpoint" "s3" {
  count = var.enable_s3_gateway_endpoint ? 1 : 0

  vpc_id            = aws_vpc.this.id
  service_name      = "com.amazonaws.ap-northeast-2.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = [aws_route_table.private.id]

  tags = {
    Name = "server-vpce-s3"
  }
}
```

주의: 기존 private route table의 blackhole NAT route는 재생성하지 않는다. Rebuild mode에서는 존재하지 않는 NAT로 향하는 route를 만들지 않는다.

- [ ] **Step 3: network outputs 작성**

`infra/terraform/modules/network/outputs.tf`:

```hcl
output "vpc_id" {
  value = aws_vpc.this.id
}

output "subnet_ids" {
  value = { for name, subnet in aws_subnet.this : name => subnet.id }
}

output "public_route_table_id" {
  value = aws_route_table.public.id
}

output "private_route_table_id" {
  value = aws_route_table.private.id
}
```

- [ ] **Step 4: shared env에서 현재 subnet map 연결**

`infra/terraform/envs/shared/main.tf`의 network module에 다음 값을 넣는다. dev/prod root는 이 module을 다시 선언하지 않고 shared state output을 참조한다.

```hcl
module "network" {
  source      = "../../modules/network"
  name_prefix = "server"
  environment = var.environment
  vpc_cidr    = "10.0.0.0/16"

  subnets = {
    server-subnet-pub-2a = {
      cidr                    = "10.0.0.0/24"
      availability_zone       = "ap-northeast-2a"
      map_public_ip_on_launch = true
      route_table             = "public"
    }
    server-subnet-pub-2b = {
      cidr                    = "10.0.1.0/24"
      availability_zone       = "ap-northeast-2b"
      map_public_ip_on_launch = false
      route_table             = "public"
    }
    server-subnet-pub-app-2a = {
      cidr                    = "10.0.30.0/24"
      availability_zone       = "ap-northeast-2a"
      map_public_ip_on_launch = false
      route_table             = "public"
    }
    server-subnet-pub-app-2b = {
      cidr                    = "10.0.31.0/24"
      availability_zone       = "ap-northeast-2b"
      map_public_ip_on_launch = false
      route_table             = "public"
    }
    server-subnet-pri-app-2a = {
      cidr                    = "10.0.10.0/24"
      availability_zone       = "ap-northeast-2a"
      map_public_ip_on_launch = false
      route_table             = "private"
    }
    server-subnet-pri-app-2b = {
      cidr                    = "10.0.11.0/24"
      availability_zone       = "ap-northeast-2b"
      map_public_ip_on_launch = false
      route_table             = "private"
    }
    server-subnet-pri-db-2a = {
      cidr                    = "10.0.20.0/24"
      availability_zone       = "ap-northeast-2a"
      map_public_ip_on_launch = false
      route_table             = "private"
    }
    server-subnet-pri-db-2b = {
      cidr                    = "10.0.21.0/24"
      availability_zone       = "ap-northeast-2b"
      map_public_ip_on_launch = false
      route_table             = "private"
    }
    server-subnet-pub-ec2-2a = {
      cidr                    = "10.0.2.0/24"
      availability_zone       = "ap-northeast-2a"
      map_public_ip_on_launch = true
      route_table             = "public"
    }
  }
}
```

- [ ] **Step 5: validate**

Run:

```bash
cd infra/terraform/envs/dev
terraform fmt -recursive ../../
terraform init -backend=false
terraform validate
```

Expected: validation success.

- [ ] **Step 6: commit**

Run:

```bash
git add infra/terraform
git commit -m "feat: add terraform network module"
```

### Task 4: Security Group Module

**Files:**
- Create: `infra/terraform/modules/security/main.tf`
- Create: `infra/terraform/modules/security/variables.tf`
- Create: `infra/terraform/modules/security/outputs.tf`
- Modify: `infra/terraform/envs/shared/main.tf`

- [ ] **Step 1: variables 작성**

`infra/terraform/modules/security/variables.tf`:

```hcl
variable "vpc_id" {
  type = string
}

variable "allowed_ssh_cidrs" {
  type        = list(string)
  description = "CIDR blocks allowed to SSH into bastion and ASG instances."
  default     = ["116.124.253.97/32"]
}

variable "allow_world_ssh_for_parity" {
  type        = bool
  description = "Set true only for import parity. Rebuild mode must keep this false."
  default     = false
}
```

- [ ] **Step 2: SG resources 작성**

`infra/terraform/modules/security/main.tf`:

```hcl
locals {
  ssh_cidrs = var.allow_world_ssh_for_parity ? distinct(concat(var.allowed_ssh_cidrs, ["0.0.0.0/0"])) : var.allowed_ssh_cidrs
}

resource "aws_security_group" "alb" {
  name        = "server-sg-alb-default"
  description = "ALB default (80, 443)"
  vpc_id      = var.vpc_id

  tags = {
    Name = "server-sg-alb-default"
  }
}

resource "aws_security_group_rule" "alb_http" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  protocol          = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "alb_https" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  protocol          = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "alb_egress_all" {
  type              = "egress"
  security_group_id = aws_security_group.alb.id
  protocol          = "-1"
  from_port         = 0
  to_port           = 0
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group" "asg" {
  name        = "server-sg-ec2-asg-default"
  description = "SG for EC2 in ASG"
  vpc_id      = var.vpc_id

  tags = {
    Name = "server-sg-ec2-asg-default"
  }
}

resource "aws_security_group_rule" "asg_app_from_alb" {
  type                     = "ingress"
  security_group_id        = aws_security_group.asg.id
  protocol                 = "tcp"
  from_port                = 8080
  to_port                  = 8080
  source_security_group_id = aws_security_group.alb.id
  description              = "Service Port from ALB"
}

resource "aws_security_group_rule" "asg_management_from_alb" {
  type                     = "ingress"
  security_group_id        = aws_security_group.asg.id
  protocol                 = "tcp"
  from_port                = 9090
  to_port                  = 9090
  source_security_group_id = aws_security_group.alb.id
  description              = "Management Port from ALB"
}

resource "aws_security_group_rule" "asg_ssh" {
  for_each = toset(local.ssh_cidrs)

  type              = "ingress"
  security_group_id = aws_security_group.asg.id
  protocol          = "tcp"
  from_port         = 22
  to_port           = 22
  cidr_blocks       = [each.value]
}

resource "aws_security_group_rule" "asg_egress_all" {
  type              = "egress"
  security_group_id = aws_security_group.asg.id
  protocol          = "-1"
  from_port         = 0
  to_port           = 0
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group" "db" {
  name        = "server-sg-db-default"
  description = "RDS DB (5432, from app and bastion)"
  vpc_id      = var.vpc_id

  tags = {
    Name = "server-sg-db-default"
  }
}

resource "aws_security_group_rule" "db_from_asg" {
  type                     = "ingress"
  security_group_id        = aws_security_group.db.id
  protocol                 = "tcp"
  from_port                = 5432
  to_port                  = 5432
  source_security_group_id = aws_security_group.asg.id
  description              = "from EC2 ASG"
}

resource "aws_security_group_rule" "db_egress_all" {
  type              = "egress"
  security_group_id = aws_security_group.db.id
  protocol          = "-1"
  from_port         = 0
  to_port           = 0
  cidr_blocks       = ["0.0.0.0/0"]
}
```

추가로 bastion SG, ECS legacy SG, VPC endpoint SG가 필요하면 같은 파일에 별도 resource로 추가한다. 최소 rebuild에는 ALB/ASG/DB SG가 필요하다.

- [ ] **Step 3: outputs 작성**

`infra/terraform/modules/security/outputs.tf`:

```hcl
output "alb_security_group_id" {
  value = aws_security_group.alb.id
}

output "asg_security_group_id" {
  value = aws_security_group.asg.id
}

output "db_security_group_id" {
  value = aws_security_group.db.id
}
```

- [ ] **Step 4: shared env 연결**

`envs/shared/main.tf`에 추가:

```hcl
module "security" {
  source = "../../modules/security"

  vpc_id                     = module.network.vpc_id
  allowed_ssh_cidrs          = ["116.124.253.97/32"]
  allow_world_ssh_for_parity = false
}
```

- [ ] **Step 5: validate 및 commit**

Run:

```bash
cd infra/terraform/envs/dev
terraform fmt -recursive ../../
terraform validate
git add infra/terraform
git commit -m "feat: add terraform security groups"
```

### Task 5: ALB, Target Group, Listener Rules

**Files:**
- Create: `infra/terraform/modules/alb/main.tf`
- Create: `infra/terraform/modules/alb/variables.tf`
- Create: `infra/terraform/modules/alb/outputs.tf`
- Modify: `infra/terraform/envs/shared/main.tf`

- [ ] **Step 1: ALB variables 작성**

`infra/terraform/modules/alb/variables.tf`:

```hcl
variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "alb_security_group_id" {
  type = string
}

variable "certificate_arns" {
  type = list(string)
}

variable "dev_host_headers" {
  type = list(string)
}

variable "prod_host_headers" {
  type = list(string)
}
```

- [ ] **Step 2: ALB resources 작성**

`infra/terraform/modules/alb/main.tf`:

```hcl
resource "aws_lb" "this" {
  name               = "server-alb-default"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [var.alb_security_group_id]
  subnets            = var.public_subnet_ids
}

resource "aws_lb_target_group" "dev_ec2" {
  name        = "server-tg-ec2-dev"
  vpc_id      = var.vpc_id
  protocol    = "HTTP"
  port        = 8080
  target_type = "instance"

  health_check {
    enabled             = true
    protocol            = "HTTP"
    path                = "/actuator/health"
    port                = "9090"
    matcher             = "200"
    healthy_threshold   = 3
    unhealthy_threshold = 10
    interval            = 30
    timeout             = 10
  }
}

resource "aws_lb_target_group" "prod_ec2" {
  name        = "server-tg-ec2-prod"
  vpc_id      = var.vpc_id
  protocol    = "HTTP"
  port        = 8080
  target_type = "instance"

  health_check {
    enabled             = true
    protocol            = "HTTP"
    path                = "/actuator/health"
    port                = "9090"
    matcher             = "200"
    healthy_threshold   = 3
    unhealthy_threshold = 10
    interval            = 30
    timeout             = 10
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  protocol          = "HTTP"
  port              = 80

  default_action {
    type = "redirect"

    redirect {
      protocol    = "HTTPS"
      port        = "443"
      host        = "#{host}"
      path        = "/#{path}"
      query       = "#{query}"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  protocol          = "HTTPS"
  port              = 443
  certificate_arn   = var.certificate_arns[0]
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-Res-PQ-2025-09"

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      message_body = "ALB Route Not Found"
      status_code  = "404"
    }
  }
}

resource "aws_lb_listener_certificate" "additional" {
  for_each = toset(slice(var.certificate_arns, 1, length(var.certificate_arns)))

  listener_arn    = aws_lb_listener.https.arn
  certificate_arn = each.value
}

resource "aws_lb_listener_rule" "dev" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 1

  condition {
    host_header {
      values = var.dev_host_headers
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.dev_ec2.arn
  }
}

resource "aws_lb_listener_rule" "prod" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 2

  condition {
    host_header {
      values = var.prod_host_headers
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.prod_ec2.arn
  }
}
```

현재 listener rule에는 legacy IP target group도 weight `0`으로 남아 있다. Terraform rebuild 기본값은 active EC2 target group만 forward한다. import parity가 필요하면 별도 `legacy_ip_target_groups_enabled` 변수를 추가한다.

- [ ] **Step 3: outputs 작성**

`infra/terraform/modules/alb/outputs.tf`:

```hcl
output "alb_arn" {
  value = aws_lb.this.arn
}

output "alb_dns_name" {
  value = aws_lb.this.dns_name
}

output "alb_zone_id" {
  value = aws_lb.this.zone_id
}

output "dev_target_group_arn" {
  value = aws_lb_target_group.dev_ec2.arn
}

output "prod_target_group_arn" {
  value = aws_lb_target_group.prod_ec2.arn
}
```

- [ ] **Step 4: shared env 연결**

`envs/shared/main.tf`에 추가:

```hcl
module "alb" {
  source = "../../modules/alb"

  vpc_id                = module.network.vpc_id
  public_subnet_ids     = [module.network.subnet_ids["server-subnet-pub-2a"], module.network.subnet_ids["server-subnet-pub-2b"]]
  alb_security_group_id = module.security.alb_security_group_id
  certificate_arns = [
    "arn:aws:acm:ap-northeast-2:137809407320:certificate/0050f1b3-bd9d-4f97-8c3b-83129196de8e"
  ]
  dev_host_headers  = ["dev.api.university.neordinary.com"]
  prod_host_headers = ["api.university.neordinary.com"]
}
```

- [ ] **Step 5: validate 및 commit**

Run:

```bash
cd infra/terraform/envs/dev
terraform fmt -recursive ../../
terraform validate
git add infra/terraform
git commit -m "feat: add terraform alb module"
```

### Task 6: IAM과 ASG User Data

**Files:**
- Create: `infra/terraform/modules/iam/main.tf`
- Create: `infra/terraform/modules/iam/variables.tf`
- Create: `infra/terraform/modules/iam/outputs.tf`
- Create: `infra/terraform/modules/iam/user-data/asg-app.sh.tftpl`
- Modify: `infra/terraform/envs/dev/main.tf`
- Modify: `infra/terraform/envs/prod/main.tf`

- [ ] **Step 1: IAM variables 작성**

`infra/terraform/modules/iam/variables.tf`:

```hcl
variable "environment" {
  type = string
}

variable "ecr_repository_arn" {
  type = string
}

variable "dev_secret_source" {
  type = string

  validation {
    condition     = contains(["local_env_file", "ssm_parameter_store", "secrets_manager"], var.dev_secret_source)
    error_message = "dev_secret_source must be one of local_env_file, ssm_parameter_store, secrets_manager."
  }
}

variable "dev_runtime_env_file_path" {
  type    = string
  default = "/etc/umc-product/dev.env"
}

variable "dev_ssm_parameter_path" {
  type    = string
  default = null
}

variable "dev_runtime_secret_arn" {
  type    = string
  default = null
}

variable "jwt_key_ring_secret_arn" {
  type    = string
  default = null
}

variable "oauth_client_secret_arn" {
  type    = string
  default = null
}

variable "data_protection_secret_arn" {
  type    = string
  default = null
}

variable "database_secret_arn" {
  type    = string
  default = null
}

variable "runtime_secret_kms_key_arn" {
  type    = string
  default = null
}
```

- [ ] **Step 2: IAM resources 작성**

`infra/terraform/modules/iam/main.tf`:

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

resource "aws_iam_role" "app_ec2" {
  name               = "server-ec2"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json
}

data "aws_iam_policy_document" "ecr_pull" {
  statement {
    sid       = "GetEcrAuthToken"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "PullEcrImage"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage"
    ]
    resources = [var.ecr_repository_arn]
  }
}

resource "aws_iam_policy" "ecr_pull" {
  name   = "server-ecr-pull"
  policy = data.aws_iam_policy_document.ecr_pull.json
}

resource "aws_iam_role_policy_attachment" "ecr_pull" {
  role       = aws_iam_role.app_ec2.name
  policy_arn = aws_iam_policy.ecr_pull.arn
}

locals {
  runtime_secret_arns = compact([
    var.dev_runtime_secret_arn,
    var.jwt_key_ring_secret_arn,
    var.oauth_client_secret_arn,
    var.data_protection_secret_arn,
    var.database_secret_arn
  ])
}

data "aws_iam_policy_document" "runtime_secret_read" {
  count = length(local.runtime_secret_arns) > 0 ? 1 : 0

  statement {
    sid       = "ReadRuntimeSecrets"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = local.runtime_secret_arns
  }

  dynamic "statement" {
    for_each = var.runtime_secret_kms_key_arn == null ? [] : [var.runtime_secret_kms_key_arn]

    content {
      sid       = "DecryptRuntimeSecrets"
      actions   = ["kms:Decrypt"]
      resources = [statement.value]
    }
  }
}

resource "aws_iam_policy" "runtime_secret_read" {
  count = length(local.runtime_secret_arns) > 0 ? 1 : 0

  name   = "server-runtime-secret-read-${var.environment}"
  policy = data.aws_iam_policy_document.runtime_secret_read[0].json
}

resource "aws_iam_role_policy_attachment" "runtime_secret_read" {
  count = length(local.runtime_secret_arns) > 0 ? 1 : 0

  role       = aws_iam_role.app_ec2.name
  policy_arn = aws_iam_policy.runtime_secret_read[0].arn
}

resource "aws_iam_instance_profile" "app_ec2" {
  name = "server-ec2"
  role = aws_iam_role.app_ec2.name
}
```

- [ ] **Step 3: secret source 분기 user-data template 작성**

`infra/terraform/modules/iam/user-data/asg-app.sh.tftpl`:

```bash
#!/usr/bin/env bash
set -euo pipefail

exec > >(tee /var/log/umc-product-user-data.log | logger -t user-data -s 2>/dev/console) 2>&1

AWS_REGION="${aws_region}"
AWS_ACCOUNT_ID="${aws_account_id}"
ECR_REPOSITORY="${ecr_repository}"
IMAGE_TAG="${image_tag}"
APP_PORT="${app_port}"
MANAGEMENT_PORT="${management_port}"
SECRET_SOURCE="${secret_source}"
DEV_ENV_FILE="${dev_runtime_env_file_path}"
RUNTIME_SECRET_ID="${runtime_secret_id}"
SSM_PARAMETER_PATH="${ssm_parameter_path}"
SPRING_PROFILE="${spring_profile}"

ECR_REGISTRY="$${AWS_ACCOUNT_ID}.dkr.ecr.$${AWS_REGION}.amazonaws.com"
IMAGE_URI="$${ECR_REGISTRY}/$${ECR_REPOSITORY}:$${IMAGE_TAG}"
APP_DIR="/opt/umc-product"
ENV_DIR="/run/umc-product"

ensure_bootstrap_dependencies() {
  local missing=()
  command -v docker >/dev/null 2>&1 || missing+=("docker")
  docker compose version >/dev/null 2>&1 || missing+=("docker compose plugin")
  command -v aws >/dev/null 2>&1 || missing+=("awscli")
  command -v jq >/dev/null 2>&1 || missing+=("jq")
  command -v curl >/dev/null 2>&1 || missing+=("curl")

  if (( $${#missing[@]} > 0 )); then
    echo "Missing baked AMI dependencies: $${missing[*]}"
    exit 1
  fi
}

ensure_bootstrap_dependencies

systemctl enable docker
systemctl start docker

mkdir -p "$${APP_DIR}" "$${ENV_DIR}"
chmod 700 "$${ENV_DIR}"

case "$${SECRET_SOURCE}" in
  local_env_file)
    echo "Loading runtime env from local file..."
    install -m 600 "$${DEV_ENV_FILE}" "$${ENV_DIR}/app.env"
    ;;
  ssm_parameter_store)
    echo "Loading runtime env from SSM Parameter Store..."
    aws ssm get-parameters-by-path \
      --region "$${AWS_REGION}" \
      --path "$${SSM_PARAMETER_PATH}" \
      --with-decryption \
      --recursive \
      --query 'Parameters[].{Name:Name,Value:Value}' \
      --output json \
      | jq -r '.[] | "\(.Name | split("/")[-1])=\(.Value)"' > "$${ENV_DIR}/app.env"
    ;;
  secrets_manager)
    echo "Loading runtime env from Secrets Manager..."
    aws secretsmanager get-secret-value \
      --region "$${AWS_REGION}" \
      --secret-id "$${RUNTIME_SECRET_ID}" \
      --query SecretString \
      --output text > "$${ENV_DIR}/app.env"
    ;;
  *)
    echo "Unsupported SECRET_SOURCE: $${SECRET_SOURCE}"
    exit 1
    ;;
esac
chmod 600 "$${ENV_DIR}/app.env"

echo "Logging in to ECR..."
aws ecr get-login-password --region "$${AWS_REGION}" \
  | docker login --username AWS --password-stdin "$${ECR_REGISTRY}"

cd "$${APP_DIR}"

cat > docker-compose.yml <<EOF
services:
  app:
    image: $${IMAGE_URI}
    container_name: umc-product-app
    restart: unless-stopped
    env_file:
      - $${ENV_DIR}/app.env
    environment:
      SERVER_PORT: "$${APP_PORT}"
      MANAGEMENT_PORT: "$${MANAGEMENT_PORT}"
      SPRING_PROFILES_ACTIVE: "$${SPRING_PROFILE}"
    ports:
      - "$${APP_PORT}:$${APP_PORT}"
      - "$${MANAGEMENT_PORT}:$${MANAGEMENT_PORT}"
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://localhost:$${MANAGEMENT_PORT}/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s
EOF

docker compose pull
docker compose up -d
```

- [ ] **Step 4: outputs 작성**

`infra/terraform/modules/iam/outputs.tf`:

```hcl
output "app_instance_profile_arn" {
  value = aws_iam_instance_profile.app_ec2.arn
}

output "app_instance_profile_name" {
  value = aws_iam_instance_profile.app_ec2.name
}
```

- [ ] **Step 5: env 연결**

`envs/dev/main.tf`와 `envs/prod/main.tf`에 추가한다. dev는 기본적으로 local env file을 사용하고, prod만 목적별 secret ARN과 KMS ARN을 `terraform.tfvars`에서 받는다.

```hcl
module "iam" {
  source = "../../modules/iam"

  environment        = var.environment
  ecr_repository_arn = "arn:aws:ecr:ap-northeast-2:137809407320:repository/umc-product-server"

  dev_secret_source         = try(var.dev_secret_source, "secrets_manager")
  dev_runtime_env_file_path = try(var.dev_runtime_env_file_path, null)
  dev_ssm_parameter_path    = try(var.dev_ssm_parameter_path, null)
  dev_runtime_secret_arn    = try(var.dev_runtime_secret_arn, null)

  jwt_key_ring_secret_arn    = try(var.jwt_key_ring_secret_arn, null)
  oauth_client_secret_arn    = try(var.oauth_client_secret_arn, null)
  data_protection_secret_arn = try(var.data_protection_secret_arn, null)
  database_secret_arn        = try(var.database_secret_arn, null)
  runtime_secret_kms_key_arn = try(var.runtime_secret_kms_key_arn, null)
}
```

`envs/dev/variables.tf`에는 비용 없는 local env file 기본값을 둔다.

```hcl
variable "dev_secret_source" {
  type        = string
  description = "dev runtime secret source."
  default     = "local_env_file"
}

variable "dev_runtime_env_file_path" {
  type        = string
  description = "root-only env file path on dev/home-server host."
  default     = "/etc/umc-product/dev.env"
}
```

`envs/prod/variables.tf`에는 목적별 ARN을 둔다.

```hcl
variable "jwt_key_ring_secret_arn" { type = string }
variable "oauth_client_secret_arn" { type = string }
variable "data_protection_secret_arn" { type = string }
variable "database_secret_arn" { type = string }
variable "runtime_secret_kms_key_arn" { type = string }
```

- [ ] **Step 6: validate 및 commit**

Run:

```bash
cd infra/terraform/envs/dev
terraform fmt -recursive ../../
terraform validate
git add infra/terraform
git commit -m "feat: add terraform iam and user data"
```

### Task 7: Compute ASG Module

**Files:**
- Create: `infra/terraform/modules/compute-asg/main.tf`
- Create: `infra/terraform/modules/compute-asg/variables.tf`
- Create: `infra/terraform/modules/compute-asg/outputs.tf`
- Modify: `infra/terraform/envs/dev/main.tf`
- Modify: `infra/terraform/envs/prod/main.tf`

- [ ] **Step 1: variables 작성**

`infra/terraform/modules/compute-asg/variables.tf`:

```hcl
variable "environment" {
  type = string
}

variable "name" {
  type = string
}

variable "ami_id" {
  type = string
}

variable "instance_type" {
  type    = string
  default = "t4g.small"
}

variable "key_name" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}

variable "instance_profile_arn" {
  type = string
}

variable "target_group_arns" {
  type = list(string)
}

variable "user_data" {
  type = string
}

variable "desired_capacity" {
  type    = number
  default = 1
}

variable "min_size" {
  type    = number
  default = 1
}

variable "max_size" {
  type    = number
  default = 1
}

variable "health_check_grace_period" {
  type = number
}
```

- [ ] **Step 2: ASG resources 작성**

`infra/terraform/modules/compute-asg/main.tf`:

```hcl
resource "aws_launch_template" "this" {
  name_prefix   = "${var.name}-template-"
  image_id      = var.ami_id
  instance_type = var.instance_type
  key_name      = var.key_name
  user_data     = base64encode(var.user_data)

  iam_instance_profile {
    arn = var.instance_profile_arn
  }

  network_interfaces {
    associate_public_ip_address = true
    security_groups             = [var.security_group_id]
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  block_device_mappings {
    device_name = "/dev/sda1"

    ebs {
      volume_size           = 8
      volume_type           = "gp3"
      encrypted             = true
      delete_on_termination = true
    }
  }
}

resource "aws_autoscaling_group" "this" {
  name                      = var.name
  min_size                  = var.min_size
  max_size                  = var.max_size
  desired_capacity          = var.desired_capacity
  vpc_zone_identifier       = var.subnet_ids
  health_check_type         = "ELB"
  health_check_grace_period = var.health_check_grace_period
  target_group_arns         = var.target_group_arns

  launch_template {
    id      = aws_launch_template.this.id
    version = "$Latest"
  }

  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 100
      instance_warmup        = var.health_check_grace_period
    }
  }

  tag {
    key                 = "Name"
    value               = var.name
    propagate_at_launch = true
  }
}
```

- [ ] **Step 3: outputs 작성**

`infra/terraform/modules/compute-asg/outputs.tf`:

```hcl
output "launch_template_id" {
  value = aws_launch_template.this.id
}

output "autoscaling_group_name" {
  value = aws_autoscaling_group.this.name
}
```

- [ ] **Step 4: env 연결**

dev:

```hcl
data "terraform_remote_state" "shared" {
  backend = "s3"

  config = {
    bucket = var.terraform_state_bucket
    key    = "umc-product-server/shared/terraform.tfstate"
    region = var.aws_region
  }
}

locals {
  aws_account_id = "137809407320"
  ecr_repository = "umc-product-server"
  shared          = data.terraform_remote_state.shared.outputs
}

module "dev_asg" {
  source = "../../modules/compute-asg"

  environment               = "dev"
  name                      = "dev-umc-product-server-asg"
  ami_id                    = "ami-0fdcb184d4d869b49"
  instance_type             = "t4g.small"
  key_name                  = "server-dev-ec2"
  subnet_ids                = [local.shared.subnet_ids["server-subnet-pub-app-2a"], local.shared.subnet_ids["server-subnet-pub-app-2b"]]
  security_group_id         = local.shared.asg_security_group_id
  instance_profile_arn      = module.iam.app_instance_profile_arn
  target_group_arns         = [local.shared.dev_target_group_arn]
  health_check_grace_period = 600

  user_data = templatefile("${path.module}/../../modules/iam/user-data/asg-app.sh.tftpl", {
    aws_region        = var.aws_region
    aws_account_id    = local.aws_account_id
    ecr_repository    = local.ecr_repository
    image_tag         = var.image_tag
    app_port          = "8080"
    management_port   = "9090"
    secret_source     = var.dev_secret_source
    dev_runtime_env_file_path = var.dev_runtime_env_file_path
    ssm_parameter_path = var.dev_ssm_parameter_path
    runtime_secret_id = var.dev_runtime_secret_arn
    spring_profile    = "dev"
  })
}
```

prod는 `name`, `key_name`, `target_group_arns`, `health_check_grace_period = 300`, `spring_profile = "prod"`로 변경하고, `secret_source = "secrets_manager"`와 목적별 prod secret ARN을 IAM module에 전달한다.

- [ ] **Step 5: image tag 변수 추가**

`envs/dev/variables.tf`와 `envs/prod/variables.tf`:

```hcl
variable "image_tag" {
  type        = string
  description = "ECR image tag deployed by ASG user-data."
}

variable "dev_secret_source" {
  type        = string
  description = "Secret source used by dev ASG user-data."
  default     = "local_env_file"
}

variable "dev_runtime_env_file_path" {
  type        = string
  description = "root-only env file path on dev/home-server host."
  default     = "/etc/umc-product/dev.env"
}
```

- [ ] **Step 6: validate 및 commit**

Run:

```bash
cd infra/terraform/envs/dev
terraform fmt -recursive ../../
terraform validate
git add infra/terraform
git commit -m "feat: add terraform asg module"
```

### Task 8: RDS Module

**Files:**
- Create: `infra/terraform/modules/rds/main.tf`
- Create: `infra/terraform/modules/rds/variables.tf`
- Create: `infra/terraform/modules/rds/outputs.tf`
- Modify: `infra/terraform/envs/prod/main.tf`

- [ ] **Step 1: variables 작성**

`infra/terraform/modules/rds/variables.tf`:

```hcl
variable "identifier" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "instance_class" {
  type    = string
  default = "db.t4g.small"
}

variable "engine_version" {
  type    = string
  default = "18.3"
}

variable "allocated_storage" {
  type    = number
  default = 20
}

variable "deletion_protection" {
  type    = bool
  default = true
}
```

- [ ] **Step 2: RDS resources 작성**

`infra/terraform/modules/rds/main.tf`:

```hcl
resource "aws_db_subnet_group" "this" {
  name       = "server-subnet-group-db-default"
  subnet_ids = var.subnet_ids

  tags = {
    Name = "server-subnet-group-db-default"
  }
}

resource "aws_db_instance" "this" {
  identifier = var.identifier

  engine         = "postgres"
  engine_version = var.engine_version
  instance_class = var.instance_class

  allocated_storage = var.allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true

  username                    = "postgres"
  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = var.security_group_ids
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period = 7
  copy_tags_to_snapshot   = true
  deletion_protection     = var.deletion_protection

  performance_insights_enabled = true
  monitoring_interval          = 60
  auto_minor_version_upgrade   = true

  skip_final_snapshot = false
  final_snapshot_identifier = "${var.identifier}-final-${formatdate("YYYYMMDDhhmmss", timestamp())}"

  lifecycle {
    ignore_changes = [
      final_snapshot_identifier
    ]
  }
}
```

기존 `server-rds-prod` import 시에는 기존 master password 관리 방식을 변경하면 교체/수정 영향이 있을 수 있다. import parity에서는 `manage_master_user_password` 전환을 별도 migration PR로 분리한다. 신규 rebuild는 위 설정을 기본으로 사용한다.

- [ ] **Step 3: outputs 작성**

`infra/terraform/modules/rds/outputs.tf`:

```hcl
output "db_instance_identifier" {
  value = aws_db_instance.this.identifier
}

output "db_endpoint" {
  value = aws_db_instance.this.address
}

output "db_port" {
  value = aws_db_instance.this.port
}
```

- [ ] **Step 4: prod env 연결**

`infra/terraform/envs/prod/main.tf`:

```hcl
module "rds" {
  source = "../../modules/rds"

  identifier          = "server-rds-prod"
  subnet_ids          = [local.shared.subnet_ids["server-subnet-pri-db-2a"], local.shared.subnet_ids["server-subnet-pri-db-2b"]]
  security_group_ids  = [local.shared.db_security_group_id]
  instance_class      = "db.t4g.small"
  engine_version      = "18.3"
  allocated_storage   = 20
  deletion_protection = true
}
```

- [ ] **Step 5: validate 및 commit**

Run:

```bash
cd infra/terraform/envs/prod
terraform fmt -recursive ../../
terraform validate
git add infra/terraform
git commit -m "feat: add terraform rds module"
```

### Task 9: Route53 DNS Module

**Files:**
- Create: `infra/terraform/modules/dns/main.tf`
- Create: `infra/terraform/modules/dns/variables.tf`
- Create: `infra/terraform/modules/dns/outputs.tf`
- Modify: `infra/terraform/envs/shared/main.tf`

- [ ] **Step 1: variables 작성**

`infra/terraform/modules/dns/variables.tf`:

```hcl
variable "records" {
  type = map(object({
    zone_id                = string
    name                   = string
    alb_dns_name           = string
    alb_zone_id            = string
    evaluate_target_health = bool
  }))
}
```

- [ ] **Step 2: Route53 resources 작성**

`infra/terraform/modules/dns/main.tf`:

```hcl
resource "aws_route53_record" "alias" {
  for_each = var.records

  zone_id = each.value.zone_id
  name    = each.value.name
  type    = "A"

  alias {
    name                   = each.value.alb_dns_name
    zone_id                = each.value.alb_zone_id
    evaluate_target_health = each.value.evaluate_target_health
  }
}
```

- [ ] **Step 3: outputs 작성**

`infra/terraform/modules/dns/outputs.tf`:

```hcl
output "record_fqdns" {
  value = { for key, record in aws_route53_record.alias : key => record.fqdn }
}
```

- [ ] **Step 4: shared env 연결**

`envs/shared/main.tf`:

```hcl
module "dns" {
  source = "../../modules/dns"

  records = {
    dev_api = {
      zone_id                = "Z027462015TRXMTRN1ZWS"
      name                   = "dev.api.university.neordinary.com"
      alb_dns_name           = module.alb.alb_dns_name
      alb_zone_id            = module.alb.alb_zone_id
      evaluate_target_health = true
    }
    prod_api = {
      zone_id                = "Z027462015TRXMTRN1ZWS"
      name                   = "api.university.neordinary.com"
      alb_dns_name           = module.alb.alb_dns_name
      alb_zone_id            = module.alb.alb_zone_id
      evaluate_target_health = true
    }
  }
}
```

- [ ] **Step 5: validate 및 commit**

Run:

```bash
cd infra/terraform/envs/shared
terraform fmt -recursive ../../
terraform validate
git add infra/terraform
git commit -m "feat: add terraform dns module"
```

### Task 10: Import Blocks for Existing Resources

**Files:**
- Create: `infra/terraform/envs/shared/imports.tf`
- Create: `infra/terraform/envs/dev/imports.tf`
- Create: `infra/terraform/envs/prod/imports.tf`
- Modify: `infra/terraform/README.md`

- [ ] **Step 1: shared import blocks 작성**

`infra/terraform/envs/shared/imports.tf` 예시:

```hcl
import {
  to = module.network.aws_vpc.this
  id = "vpc-0101ab9cb36353cc5"
}

import {
  to = module.alb.aws_lb.this
  id = "arn:aws:elasticloadbalancing:ap-northeast-2:137809407320:loadbalancer/app/server-alb-default/bf4a4964f52710d8"
}

import {
  to = module.alb.aws_lb_target_group.dev_ec2
  id = "arn:aws:elasticloadbalancing:ap-northeast-2:137809407320:targetgroup/server-tg-ec2-dev/a987266b77a658a5"
}

import {
  to = module.alb.aws_lb_target_group.prod_ec2
  id = "arn:aws:elasticloadbalancing:ap-northeast-2:137809407320:targetgroup/server-tg-ec2-prod/635cac1b989d13d1"
}
```

복수 `for_each` 리소스 import는 실제 Terraform address를 확인한 뒤 다음 형식을 사용한다.

```hcl
import {
  to = module.network.aws_subnet.this["server-subnet-pub-2a"]
  id = "subnet-03b5e95c0297bcfeb"
}
```

- [ ] **Step 2: dev import blocks 작성**

`infra/terraform/envs/dev/imports.tf`에 dev ASG import를 추가한다.

```hcl
import {
  to = module.dev_asg.aws_autoscaling_group.this
  id = "dev-umc-product-server-asg"
}
```

- [ ] **Step 3: prod import blocks 작성**

`infra/terraform/envs/prod/imports.tf`에 prod ASG와 RDS import를 추가한다. prod target group은 shared ALB module이 소유하므로 prod state로 import하지 않는다.

```hcl
import {
  to = module.prod_asg.aws_autoscaling_group.this
  id = "prod-umc-product-server-asg"
}

import {
  to = module.rds.aws_db_instance.this
  id = "server-rds-prod"
}
```

- [ ] **Step 4: import 절차 README에 추가**

`infra/terraform/README.md`에 다음 절차를 추가한다.

```markdown
## Import Workflow

1. Use readonly profile for inventory.
2. Keep shared resources in `envs/shared`; never import the same resource into dev/prod state.
3. Use write profile only after import blocks are reviewed.
4. Run:

```bash
cd infra/terraform/envs/shared
terraform init -backend-config=../../backend.shared.hcl
terraform plan -generate-config-out=generated.tf
terraform plan -out=tfplan
terraform apply tfplan

cd ../dev
terraform init -backend-config=../../backend.dev.hcl
terraform plan -out=tfplan
terraform apply tfplan

cd ../prod
terraform init -backend-config=../../backend.prod.hcl
terraform plan -out=tfplan
terraform apply tfplan
```

5. If Terraform shows replacement or deletion of existing prod resources, stop and fix configuration before apply.
```
```

- [ ] **Step 5: validate 및 commit**

Run:

```bash
cd infra/terraform/envs/prod
terraform fmt -recursive ../../
terraform validate
git add infra/terraform
git commit -m "chore: add terraform import plan"
```

### Task 11: CI Validation

**Files:**
- Create: `.github/workflows/terraform-validate.yml`

- [ ] **Step 1: Terraform validation workflow 작성**

`.github/workflows/terraform-validate.yml`:

```yaml
name: Terraform Validate

on:
  pull_request:
    paths:
      - "infra/terraform/**"
      - ".github/workflows/terraform-validate.yml"

jobs:
  validate:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        env: [shared, dev, prod]
    steps:
      - uses: actions/checkout@v4

      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: 1.6.6

      - name: Terraform fmt
        working-directory: infra/terraform
        run: terraform fmt -check -recursive

      - name: Terraform init without backend
        working-directory: infra/terraform/envs/${{ matrix.env }}
        run: terraform init -backend=false

      - name: Terraform validate
        working-directory: infra/terraform/envs/${{ matrix.env }}
        run: terraform validate
```

- [ ] **Step 2: local workflow equivalent 실행**

Run:

```bash
cd infra/terraform
terraform fmt -check -recursive
cd envs/shared && terraform init -backend=false && terraform validate
cd ../dev && terraform init -backend=false && terraform validate
cd ../prod && terraform init -backend=false && terraform validate
```

Expected: both environments validate.

- [ ] **Step 3: commit**

Run:

```bash
git add .github/workflows/terraform-validate.yml
git commit -m "ci: validate terraform configuration"
```

### Task 12: Terraform Onboarding Documentation

**Files:**
- Create: `docs/onboarding/project/terraform-aws-infra.md`
- Modify: `docs/superpowers/plans/2026-06-29-aws-terraform-infra-rebuild.md`

- [ ] **Step 1: URL 관리 범위 고정**

문서 전체에서 운영 예시와 Terraform managed DNS는 다음 두 URL만 사용한다.

- `dev.api.university.neordinary.com`
- `api.university.neordinary.com`

- [ ] **Step 2: Terraform 파일 책임 정리**

`docs/onboarding/project/terraform-aws-infra.md`에 아래 책임을 표로 정리한다.

- root files: `README.md`, `versions.tf`, `providers.tf`, `backend.example.hcl`
- env files: `envs/dev`, `envs/prod`의 `main.tf`, `variables.tf`, `terraform.tfvars.example`, `imports.tf`, `moved.tf`
- modules: `network`, `security`, `iam`, `alb`, `compute-asg`, `rds`, `dns`
- support files: `scripts/aws-infra-inventory.sh`, `docs/infra/aws-current-inventory.md`

- [ ] **Step 3: 운영 절차 정리**

온보딩 문서에 다음 절차를 포함한다.

- 전체 인프라 생성: write profile 또는 CI OIDC role로 `init`, `plan`, `apply`
- 기존 인프라 import: readonly inventory 후 import block 작성, write profile로 import/apply
- scale out/in: ASG `desired_capacity`, `min_size`, `max_size` 변경
- scale up/down: EC2 `instance_type` 변경과 RDS `instance_class` 변경을 분리
- RDS vertical scaling: snapshot, maintenance window, downtime risk 확인
- Secret: `.tfvars`에 secret 값을 저장하지 않는다. dev는 local env file path 또는 선택적 SSM path만 전달하고, prod는 목적별 Secrets Manager ARN만 전달한다.

- [ ] **Step 4: process diagram 추가**

Terraform 운영 흐름을 Mermaid diagram으로 추가한다.

- [ ] **Step 5: 문서 검증**

Run:

```bash
rg -n 'umc[.]it[.]kr|Z0322165[0-9]|0221280[b]' docs/onboarding/project/terraform-aws-infra.md docs/superpowers/plans/2026-06-29-aws-terraform-infra-rebuild.md
```

Expected: 실행 예시와 Terraform managed resource 예시에 legacy URL/zone/certificate가 남아 있지 않다.

### Task 13: Execution Handoff for Subagent-Driven Development

**Files:**
- Modify: `docs/superpowers/plans/2026-06-29-aws-terraform-infra-rebuild.md`

- [ ] **Step 1: implementation order 고정**

Subagent-driven execution order:

1. Task 1 inventory script
2. Task 2 Terraform skeleton
3. Task 3 network
4. Task 4 security
5. Task 5 ALB
6. Task 6 IAM/user-data
7. Task 7 ASG
8. Task 8 RDS
9. Task 9 DNS
10. Task 10 import blocks
11. Task 11 CI
12. Task 12 onboarding documentation

- [ ] **Step 2: review gates 고정**

각 task는 다음 순서로 진행한다.

1. Implementer subagent: 구현, `terraform fmt`, `terraform validate`, self-review
2. Spec reviewer subagent: 이 계획의 task 요구사항과 일치하는지 검토
3. Code quality reviewer subagent: Terraform 모듈 경계, 보안 기본값, state secret 노출 여부 검토
4. reviewer가 지적한 사항을 같은 implementer가 수정
5. re-review 통과 후 다음 task 진행

- [ ] **Step 3: final review 기준**

최종 리뷰에서 반드시 확인한다.

- `umcproduct-readonly`가 apply profile로 쓰이지 않는다.
- `allow_world_ssh_for_parity` 기본값이 `false`다.
- S3 secret read는 rebuild 기본 경로에서 사용하지 않는다.
- RDS 신규 생성 기본값은 encrypted, private, deletion protection enabled다.
- ASG launch template root EBS는 encrypted다.
- Terraform state에 RDS master password가 직접 들어가지 않는다.
- prod import plan에서 destroy/replace가 발생하지 않는다.

## 검증 명령 모음

Inventory:

```bash
AWS_PROFILE=umcproduct-readonly AWS_REGION=ap-northeast-2 scripts/aws-infra-inventory.sh
```

Terraform validate:

```bash
cd infra/terraform
terraform fmt -check -recursive
cd envs/shared
terraform init -backend=false
terraform validate
cd ../dev
terraform init -backend=false
terraform validate
cd ../prod
terraform init -backend=false
terraform validate
```

Readonly refresh-only plan:

```bash
cd infra/terraform/envs/prod
terraform init -backend-config=../../backend.prod.hcl
terraform plan \
  -refresh-only \
  -var-file=terraform.tfvars \
  -var='aws_profile=umcproduct-readonly' \
  -var='jwt_key_ring_secret_arn=arn:aws:secretsmanager:ap-northeast-2:137809407320:secret:placeholder-jwt' \
  -var='oauth_client_secret_arn=arn:aws:secretsmanager:ap-northeast-2:137809407320:secret:placeholder-oauth' \
  -var='data_protection_secret_arn=arn:aws:secretsmanager:ap-northeast-2:137809407320:secret:placeholder-data' \
  -var='database_secret_arn=arn:aws:secretsmanager:ap-northeast-2:137809407320:secret:placeholder-database' \
  -var='runtime_secret_kms_key_arn=arn:aws:kms:ap-northeast-2:137809407320:key/placeholder' \
  -var='image_tag=placeholder'
```

이 명령은 import/state가 없는 초기 상태에서는 유용하지 않다. import 이후 drift 확인 용도로 사용한다.

## 남은 의사결정

다음 항목은 실제 적용 전에 사람이 결정해야 한다.

- Terraform apply용 AWS profile/role 이름.
- Terraform remote state S3 bucket/DynamoDB lock table을 새로 만들지, 기존 bucket을 사용할지.
- 기존 RDS `server-rds-prod`를 Terraform으로 import만 할지, 신규 RDS를 생성하고 snapshot restore/cutover를 할지.
- ASG를 계속 public app subnet에 둘지, private app subnet + VPC endpoints 또는 NAT로 이동할지.
- bastion을 유지할지, SSM Session Manager 전용으로 전환하고 SSH를 닫을지.
- 현재 `umc-prouct-server-vpc` 오타 tag를 보존할지, 새 rebuild부터 `umc-product-server-vpc`로 고칠지.

## Self-Review

- Spec coverage: VPC, subnet, SG, EC2/ASG, ALB, RDS, Route53/ACM, IAM, user-data, import 전략, onboarding 운영 절차를 모두 포함했다.
- Placeholder scan: `placeholder`는 실행 예시에서 의도적으로 사용한 더미 값이며 실제 구현 단계에서는 `terraform.tfvars`로 대체한다. 구현 파일에는 `TODO`를 넣지 않는다.
- Type consistency: module output 이름은 downstream module 입력과 일치한다.
- Risk coverage: readonly/apply profile 분리, S3 secret 제거, blackhole NAT, public SSH, EBS encryption, RDS deletion protection을 명시했다.
