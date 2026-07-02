# AWS 현재 인프라 Baseline

이 문서는 Terraform import와 재구축 작업을 위한 사람이 읽는 기준선이다. 기준 리전은 `ap-northeast-2`, 실사 profile은 `umcproduct-readonly`이며, Terraform 관리 URL은 아래 두 개로 제한한다.

- `api.university.neordinary.com`
- `dev.api.university.neordinary.com`

## Account

- Account: `137809407320`
- Principal: `arn:aws:iam::137809407320:user/readonly`
- Region: `ap-northeast-2`
- Read profile: `umcproduct-readonly`

## VPC와 Subnet

- VPC ID: `vpc-0101ab9cb36353cc5`
- CIDR: `10.0.0.0/16`
- Name tag: `umc-prouct-server-vpc`

| 용도 | AZ | CIDR | Subnet ID | Name |
| --- | --- | --- | --- | --- |
| ALB public | `ap-northeast-2a` | `10.0.0.0/24` | `subnet-03b5e95c0297bcfeb` | `server-subnet-pub-2a` |
| ALB public | `ap-northeast-2b` | `10.0.1.0/24` | `subnet-0f578df7c07593340` | `server-subnet-pub-2b` |
| EC2 public app | `ap-northeast-2a` | `10.0.30.0/24` | `subnet-0f1e6fca9afdf0eb7` | `server-subnet-pub-app-2a` |
| EC2 public app | `ap-northeast-2b` | `10.0.31.0/24` | `subnet-0694296b62c8dc7e7` | `server-subnet-pub-app-2b` |
| app private | `ap-northeast-2a` | `10.0.10.0/24` | `subnet-0996146733a8eb8c1` | `server-subnet-pri-app-2a` |
| app private | `ap-northeast-2b` | `10.0.11.0/24` | `subnet-0f732a7943b67f494` | `server-subnet-pri-app-2b` |
| DB private | `ap-northeast-2a` | `10.0.20.0/24` | `subnet-0e46fa453b5318c12` | `server-subnet-pri-db-2a` |
| DB private | `ap-northeast-2b` | `10.0.21.0/24` | `subnet-002bbfa36ff0f7b15` | `server-subnet-pri-db-2b` |
| legacy EC2 public | `ap-northeast-2a` | `10.0.2.0/24` | `subnet-0caf472e4677bd195` | `server-subnet-pub-ec2-2a` |

## Routing Risk

- Public route table `rtb-0339f36c1ea252ba0`는 `0.0.0.0/0`를 `igw-00a6087e41662b26b`로 라우팅한다.
- Private route table `rtb-0f029c27b8898036c`에는 S3 gateway endpoint `vpce-0f77eae66ff7327cd`가 연결되어 있다.
- Private route table에는 `nat-032efecf3692c04cd`로 향하는 `0.0.0.0/0` route가 있으나, NAT Gateway가 존재하지 않아 `blackhole` 상태로 확인되었다.
- Terraform rebuild 기본값은 존재하지 않는 NAT Gateway route를 재생성하지 않아야 한다. private app subnet으로 ASG를 옮기려면 NAT Gateway 또는 필요한 VPC endpoint 설계를 먼저 확정해야 한다.

## Security Group Risk

- `server-sg-bastion-default`는 SSH ingress에 `0.0.0.0/0`를 포함한다.
- Rebuild 기본값은 공개 SSH를 허용하지 않아야 하며, parity import에서만 명시적 변수로 보존 여부를 판단한다.
- ALB, ASG, DB security group은 shared state가 소유하고, dev/prod root는 ID를 참조만 해야 한다.
- Security group rule은 추후 module 구현 시 독립 리소스로 관리해 순환 참조와 drift를 줄인다.

## ALB URL Policy

- ALB: `server-alb-default`
- HTTP `80` listener는 HTTPS `443`으로 redirect한다.
- 현재 HTTPS listener에는 legacy 기본 인증서와 `university.neordinary.com` 계열 ACM certificate가 함께 연결되어 있다.
- Terraform 관리 목표값은 `university.neordinary.com` 계열 ACM certificate만 기본 인증서로 사용한다.
- Terraform이 관리하는 host header rule과 Route53 alias record는 아래 두 URL만 포함한다.

| 환경 | 관리 URL | Target group |
| --- | --- | --- |
| dev | `dev.api.university.neordinary.com` | `server-tg-ec2-dev` |
| prod | `api.university.neordinary.com` | `server-tg-ec2-prod` |

초기 import 후 첫 plan에서 HTTPS 기본 인증서 변경, listener rule의 legacy host header 제거, weight `0` legacy IP target group 제거가 표시될 수 있다. 이는 관리 URL을 `university.neordinary.com` 계열로 제한하기 위한 의도된 변경이지만, 적용 전 실제 트래픽 의존성이 없는지 확인해야 한다.

## ASG

| 환경 | ASG | Launch template | Version | Instance | AMI |
| --- | --- | --- | ---: | --- | --- |
| dev | `dev-umc-product-server-asg` | `lt-00d81a2f4497849f8` | `84` | `t4g.small` | `ami-0fdcb184d4d869b49` |
| prod | `prod-umc-product-server-asg` | `lt-099a89fa42efbbac9` | `20` | `t4g.small` | `ami-0fdcb184d4d869b49` |

- 두 ASG는 현재 public app subnet을 사용한다.
- Launch template user-data 원문은 문서나 inventory 출력에 저장하지 않는다. 존재 여부와 metadata만 기록한다.
- EC2 root EBS volume은 `Encrypted=false`로 확인되었으므로 rebuild 기본값은 암호화를 활성화해야 한다.

## RDS

- DB instance: `server-rds-prod`
- Engine: PostgreSQL `18.3`
- Class: `db.t4g.small`
- Storage: `20GB gp3`
- Publicly accessible: `false`
- Backup retention: `7`
- Storage encrypted: `true`
- Deletion protection: `false`

운영 DB는 prod root가 소유한다. 신규 rebuild에서는 deletion protection을 기본 활성화하고, master password가 Terraform state에 평문으로 남지 않도록 RDS 관리형 password 또는 Secrets Manager 연계를 우선한다.

## IAM과 Secret Risk

- 현재 ASG launch template user-data에는 S3 secret 파일을 읽는 흐름이 포함된 것으로 확인되었다.
- Terraform rebuild 기본값은 Secrets Manager ARN/ID와 KMS key ARN만 입력받고, secret 값을 `.tfvars`, user-data 출력, 문서에 저장하지 않는다.
- EC2 instance role은 ECR pull, 필요한 Secrets Manager secret, 필요한 KMS decrypt 범위만 허용해야 한다.
- IAM policy document 원문은 inventory 산출물에 저장하지 않고 role, instance profile, attached policy ARN, inline policy name metadata만 기록한다.

## Terraform Import Ownership

| State root | 소유 리소스 | 비고 |
| --- | --- | --- |
| shared | VPC, subnet, route table, internet gateway, S3 gateway endpoint, security group, ALB, listener, listener rule, target group, Route53 record | dev/prod 공용 네트워크와 진입점을 단일 state가 소유한다. |
| dev | dev ASG, dev launch template, dev EC2 instance profile, dev IAM role/policy attachment | shared 리소스는 remote state output 또는 data source로만 참조한다. |
| prod | prod ASG, prod launch template, prod EC2 instance profile, prod IAM role/policy attachment, RDS instance, DB subnet group | RDS 변경은 plan에서 replacement 여부를 별도 검토한 뒤 적용한다. |

같은 AWS 리소스를 둘 이상의 state가 동시에 소유하면 안 된다. import block을 작성할 때는 먼저 owner state를 확정하고, 다른 root에서는 data source 또는 remote state 참조만 사용한다.
