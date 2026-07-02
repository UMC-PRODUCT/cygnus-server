# UMC Product Server Terraform

이 디렉터리는 AWS 인프라를 Terraform으로 import하거나 재구축하기 위한 IaC 베이스다. 현재 단계에서는 `shared`, `dev`, `prod` root의 state 경계를 고정하고, shared root에서 VPC/subnet/route table/security group/ALB/target group/listener를 import할 수 있는 module을 제공한다.

## State Ownership

| Root | State key | 소유 범위 |
| --- | --- | --- |
| `envs/shared` | `umc-product-server/shared/terraform.tfstate` | VPC, subnet, route table, internet gateway, VPC endpoint, security group, ALB, listener/rule, target group, Route53 record |
| `envs/dev` | `umc-product-server/dev/terraform.tfstate` | dev ASG, dev launch template, dev EC2 instance profile, dev IAM role/policy |
| `envs/prod` | `umc-product-server/prod/terraform.tfstate` | prod ASG, prod launch template, prod EC2 instance profile, prod IAM role/policy, RDS instance, DB subnet group |

shared 리소스는 dev/prod root에서 다시 선언하지 않는다. dev/prod는 shared remote state output 또는 data source로만 참조한다.

## Profile Policy

- `umcproduct-readonly`: inventory, discovery, `plan -refresh-only` 확인 전용이다.
- write profile 또는 CI OIDC role: import, 일반 plan, 실제 `terraform apply` 전용이다.
- readonly profile로 `apply` 또는 import를 실행하지 않는다.

`terraform.tfvars.example`에는 secret 값을 넣지 않는다. dev는 기본적으로 홈서버나 로컬의 root-only env file을 사용하고, prod는 JWT key ring, OAuth client, data protection, database secret ARN을 목적별로 분리한다.

## Secret Policy

- 애플리케이션은 secret backend를 직접 알지 않고 환경변수만 읽는다.
- dev 기본값은 `local_env_file`이다. 공유 dev가 필요할 때만 SSM Parameter Store Standard 또는 Secrets Manager를 선택한다.
- prod는 Secrets Manager를 사용하되 하나의 거대한 runtime secret으로 합치지 않는다.
- JWT는 access, refresh, OAuth verification, email verification 용도별 key ring을 분리한다.
- KMS key는 prod 환경 단위로 공유할 수 있지만, JWT signing secret 자체를 용도 간 공유하지 않는다.

## Backend Files

이 저장소에는 실제 backend config를 커밋하지 않고 example만 둔다.

- `backend.shared.example.hcl`
- `backend.dev.example.hcl`
- `backend.prod.example.hcl`

실행자는 운영 계정의 state bucket/table에 맞춰 로컬 또는 CI secret에서 `backend.shared.hcl`, `backend.dev.hcl`, `backend.prod.hcl`을 준비한다.

## Validate

backend 연결 없이 HCL 구조를 확인한다.

```bash
cd infra/terraform/envs/shared
terraform init -backend=false
terraform validate

cd ../dev
terraform init -backend=false
terraform validate

cd ../prod
terraform init -backend=false
terraform validate
```

전체 포맷 확인은 아래 명령을 사용한다.

```bash
terraform fmt -check -recursive infra/terraform
```

## Plan and Apply

실제 plan/apply는 write profile 또는 CI OIDC role로 실행한다. plan 파일을 저장하고 사람이 변경 범위를 확인한 뒤 같은 plan 파일을 apply한다.

```bash
cd infra/terraform/envs/shared
terraform init -backend-config=../../backend.shared.hcl
terraform plan -var-file=terraform.tfvars -out=tfplan
terraform apply tfplan
```

```bash
cd infra/terraform/envs/dev
terraform init -backend-config=../../backend.dev.hcl
terraform plan -var-file=terraform.tfvars -out=tfplan
terraform apply tfplan
```

```bash
cd infra/terraform/envs/prod
terraform init -backend-config=../../backend.prod.hcl
terraform plan -var-file=terraform.tfvars -out=tfplan
terraform apply tfplan
```

plan에서 관리 URL이 `api.university.neordinary.com`, `dev.api.university.neordinary.com` 범위를 벗어나거나 의도하지 않은 destroy/replace가 보이면 적용하지 않는다.

shared ALB import 후 첫 plan에서는 아래 변경이 의도된 것인지 확인한다.

- HTTPS listener 기본 인증서를 `university.neordinary.com` ACM 인증서로 전환
- listener rule host header에서 legacy domain 제거
- listener rule weighted forward에서 legacy IP target group 제거

## Import Safety

기존 리소스는 Terraform이 생성하기 전에 import ownership을 먼저 확정한다.

1. `scripts/aws-infra-inventory.sh`로 현재 리소스 ID와 리스크를 확인한다.
2. owner root의 `imports.tf`에 import block을 추가한다.
3. write profile로 `terraform plan -out=tfplan`을 실행한다.
4. plan에 의도하지 않은 destroy 또는 replacement가 있으면 import address나 config를 수정하고 다시 plan한다.
5. 사람이 리소스 ID, state owner, 변경 범위를 확인한 뒤 `terraform apply tfplan`을 실행한다.

state address를 바꿀 때는 수동 state 조작보다 `moved.tf`의 moved block을 우선한다.
