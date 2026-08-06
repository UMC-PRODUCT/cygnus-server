# EC2 Image Builder로 UMC PRODUCT 서버 AMI Bake 하기

## 시작 상황

UMC PRODUCT 서버를 EC2에서 Docker 컨테이너로 실행하려고 했어요. 처음에는 인스턴스 부팅 시 `user-data`에서 필요한 패키지를 설치하고, ECR에서 이미지를 받아 서버를 띄우는 방식이었어요.

대략 이런 흐름이었어요.

```text
EC2 부팅
-> user-data 실행
-> apt update
-> docker, docker compose, awscli 설치
-> ECR login
-> docker pull
-> docker run
```

그런데 부팅 로그에서 Ubuntu package repository에 접근하지 못하는 문제가 먼저 발생했어요.

```text
Cannot initiate the connection to ports.ubuntu.com:80
Network is unreachable
Could not connect to ports.ubuntu.com:80
connection timed out
```

처음에는 Security Group outbound를 열어뒀는데 왜 못 나가나 싶었어요. 하지만 이 문제는 Security Group만의 문제가 아니었어요.

## Security Group outbound만으로는 인터넷이 되지 않아요

AWS에서 Security Group outbound는 "이 트래픽을 내보내도 된다"는 허용 규칙이에요. 하지만 실제로 인터넷까지 가는 경로는 Route Table, Internet Gateway, NAT Gateway가 만들어줘야 해요.

즉, outbound 80/443을 열어도 subnet route table에 인터넷 경로가 없으면 외부로 나갈 수 없어요.

private subnet에서 인터넷 outbound를 하려면 보통 이런 구성이 필요해요.

```text
Private subnet
-> route table: 0.0.0.0/0 -> NAT Gateway

NAT Gateway
-> public subnet에 위치
-> Elastic IP 보유

Public subnet
-> route table: 0.0.0.0/0 -> Internet Gateway
```

반대로 public subnet을 쓴다면 NAT Gateway가 아니라 Internet Gateway를 통해 나가요.

```text
Public subnet
-> route table: 0.0.0.0/0 -> Internet Gateway
-> EC2에 public IPv4 필요
```

여기서 중요한 점은 public subnet에 있다고 해서 EC2가 자동으로 인터넷을 쓸 수 있는 게 아니라는 점이에요. EC2에 public IPv4가 붙어 있어야 해요. public subnet에 있고 route table에 Internet Gateway가 있어도, public IPv4가 없으면 EC2의 private IP는 인터넷에서 라우팅되지 않아요.

## ALB inbound와 EC2 outbound는 별개의 문제예요

서비스 인스턴스는 ALB 뒤에 있었고, inbound는 ALB에서 들어오는 것만 허용하는 구조였어요. 이 구조 자체는 맞아요. 운영 EC2를 직접 인터넷에 노출하지 않는 건 좋은 방향이에요.

하지만 ALB는 외부 요청을 EC2로 들여보내는 경로예요. EC2가 Ubuntu repository, AWS Systems Manager, ECR, AWS CLI 다운로드 서버로 나가는 트래픽은 ALB를 타지 않아요.

그래서 "inbound는 ALB만 허용한다"와 "EC2가 외부로 apt update를 할 수 있다"는 완전히 별개의 조건이에요.

## 네트워크가 뚫리자 다음 문제는 패키지명이었어요

이후 네트워크 경로가 해결되자 `apt update`는 성공했어요.

```text
Fetched 45.7 MB in 20s
Reading package lists...
```

하지만 다음 단계에서 다시 실패했어요.

```text
E: Unable to locate package docker-compose-plugin
E: Package 'awscli' has no installation candidate
```

이건 네트워크 문제가 아니라 Ubuntu 24.04 arm64에서 패키지 선택이 잘못된 문제였어요.

`docker-compose-plugin`은 Docker 공식 apt repository를 추가했을 때 사용하는 이름이에요. Ubuntu 기본 repository만 쓸 거면 `docker-compose-v2`를 사용하는 게 맞아요.

`awscli`도 Ubuntu apt package로 설치하기보다 AWS CLI v2 공식 installer를 쓰는 편이 안정적이에요. 특히 Image Builder에서 AMI bake를 할 때는 설치 방법이 명확하고 재현 가능해야 해요.

그래서 설치 전략은 이렇게 바꿨어요.

```text
docker.io: Ubuntu apt repository에서 설치
docker-compose-v2: Ubuntu apt repository에서 설치
awscli: AWS CLI v2 공식 zip installer로 설치
```

## 왜 AMI bake로 방향을 바꿨나요

처음 구조처럼 부팅 시점마다 `apt install`을 하면 매번 외부 repository 상태, 네트워크 상태, 패키지 후보 상태에 영향을 받아요.

운영 서버 부팅 과정은 가능하면 짧고 단순해야 해요. 부팅 시점에는 "이미 준비된 런타임 위에서 애플리케이션을 실행"하는 정도만 남기는 게 좋아요.

그래서 다음 작업들은 AMI에 미리 bake하기로 했어요.

```text
AMI에 bake할 것:
- docker.io
- docker-compose-v2
- AWS CLI v2
- curl
- unzip
- jq
- ca-certificates
- docker service enable
```

반대로 아래는 AMI에 bake하지 않는 게 좋아요.

```text
AMI에 bake하지 않을 것:
- DB password
- JWT secret
- ECR login token
- 특정 환경의 secret
- 자주 바뀌는 애플리케이션 image tag
```

AMI는 런타임 기반을 고정하고, 환경별 설정과 secret은 launch time에 주입하는 쪽이 안전해요.

## Image Builder 설정들이 각각 하는 일

EC2 Image Builder에는 설정 항목이 많아요. 처음 보면 Component, Recipe, Infrastructure Configuration, Distribution Settings가 전부 비슷해 보여요. 하지만 역할을 나누면 꽤 명확해요.

```text
Component
-> AMI 안에서 실행할 설치/설정 작업

Recipe
-> 어떤 base AMI에 어떤 Component들을 적용할지 정의

Infrastructure Configuration
-> AMI를 만들 임시 EC2를 어디에, 어떤 권한과 네트워크로 띄울지 정의

Distribution Settings
-> 완성된 AMI를 어떤 이름으로, 어느 리전에, 누구에게 배포할지 정의

Pipeline
-> 위 설정들을 묶어서 실제 빌드를 실행하거나 예약
```

짧게 정리하면 이렇게 볼 수 있어요.

```text
Recipe = 어떤 AMI를 만들지
Component = AMI 안에 뭘 설치할지
Infrastructure Configuration = 그 AMI를 어디서 만들지
Distribution Settings = 만든 AMI를 어디에 등록할지
Pipeline = 이 과정을 언제 실행할지
```

## Component는 설치 스크립트 묶음이에요

UMC PRODUCT 서버용 Component는 Docker, Compose v2, AWS CLI v2를 설치하고 검증하는 역할이에요.

예시는 아래와 같아요.

```yaml
name: install-umc-product-runtime
description: Install runtime packages for UMC PRODUCT server
schemaVersion: 1.0

phases:
  - name: build
    steps:
      - name: InstallRuntimePackages
        action: ExecuteBash
        inputs:
          commands:
            - set -euxo pipefail
            - apt-get update
            - DEBIAN_FRONTEND=noninteractive apt-get install -y curl unzip ca-certificates gnupg jq docker.io docker-compose-v2
            - systemctl enable docker
            - systemctl start docker

      - name: InstallAwsCliV2
        action: ExecuteBash
        inputs:
          commands:
            - set -euxo pipefail
            - ARCH="$(uname -m)"
            - |
              case "$ARCH" in
                aarch64|arm64)
                  AWSCLI_ARCH="aarch64"
                  ;;
                x86_64|amd64)
                  AWSCLI_ARCH="x86_64"
                  ;;
                *)
                  echo "Unsupported architecture: $ARCH"
                  exit 1
                  ;;
              esac
            - curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-${AWSCLI_ARCH}.zip" -o /tmp/awscliv2.zip
            - unzip -q /tmp/awscliv2.zip -d /tmp
            - /tmp/aws/install --bin-dir /usr/local/bin --install-dir /usr/local/aws-cli --update
            - rm -rf /tmp/aws /tmp/awscliv2.zip

      - name: CleanupApt
        action: ExecuteBash
        inputs:
          commands:
            - set -euxo pipefail
            - apt-get clean
            - rm -rf /var/lib/apt/lists/*

  - name: test
    steps:
      - name: VerifyRuntime
        action: ExecuteBash
        inputs:
          commands:
            - set -euxo pipefail
            - docker --version
            - docker compose version
            - aws --version
            - systemctl is-enabled docker
```

여기서 일부러 `awscli`를 apt로 설치하지 않았어요. 앞에서 확인한 것처럼 Ubuntu 24.04 arm64 환경에서 apt 후보가 없을 수 있기 때문이에요.

## Recipe는 base AMI와 Component를 묶는 설계도예요

Recipe에서는 Ubuntu 24.04 LTS arm64 base AMI를 선택하고, 위에서 만든 Component를 붙이면 돼요.

현재 로그가 `ubuntu-ports`와 `arm64`를 가리키고 있었기 때문에 Graviton 계열 인스턴스를 쓰는 상황으로 봤어요. 이 경우 세 가지가 맞아야 해요.

```text
Base AMI: Ubuntu 24.04 arm64
Image Builder build instance: t4g.small, t4g.medium 같은 arm64
실제 운영 instance: t4g, c7g, m7g 같은 arm64
```

Infrastructure Configuration에서 지정하는 EC2 instance type은 실제 서비스를 실행할 instance type과 직접적인 관계는 없어요. 다만 CPU architecture는 맞아야 해요.

예를 들어 Image Builder는 `t4g.small`로 bake하고, 실제 운영은 `t4g.medium`이나 `c7g.large`로 띄워도 돼요. 둘 다 arm64라면 괜찮아요.

## Infrastructure Configuration이 가장 헷갈렸어요

이번 과정에서 가장 많은 시간을 쓴 부분이 Infrastructure Configuration이었어요. 이름만 보면 최종 운영 인프라 설정처럼 보이지만, 실제로는 AMI를 만들기 위해 잠깐 뜨는 build/test EC2의 실행 환경이에요.

여기서 정하는 것은 이런 것들이에요.

```text
- Build instance type
- IAM instance profile
- VPC
- Subnet
- Security Group
- Key pair
- S3 log 위치
- 실패 시 임시 인스턴스를 유지할지 여부
```

즉, 이 설정에서 고른 VPC/Subnet/Security Group은 완성된 AMI를 나중에 실행할 운영 EC2의 네트워크 설정이 아니에요. Image Builder가 AMI를 만드는 동안 사용할 임시 EC2의 네트워크 설정이에요.

## SSM InvalidInstanceId 문제가 발생했어요

Image Builder pipeline을 실행했더니 build instance 자체는 뜨고 EC2 status check도 통과했어요.

```text
Instance status 'reachability' is passed
Action successful for step LaunchBuildInstance
```

하지만 바로 다음 단계에서 실패했어요.

```text
Sending command to instance to run
Error while calling ssm:SendCommand: InvalidInstanceId
```

이 로그의 의미는 EC2가 부팅되긴 했지만, Image Builder가 SSM Run Command를 보낼 수 없다는 뜻이에요.

Image Builder는 build instance에 SSH로 접속해서 명령을 실행하지 않아요. build instance 안의 SSM Agent가 AWS Systems Manager에 등록되고, Image Builder가 SSM SendCommand로 Component를 실행하는 구조예요.

그래서 `InvalidInstanceId`는 대체로 이런 상태를 의미해요.

```text
EC2 instance status check는 passed
하지만 SSM managed node로 등록되지 않음
그래서 Image Builder가 명령을 보낼 수 없음
```

Fleet Manager 화면에 해당 instance가 보이지 않는 것도 같은 의미예요. Fleet Manager는 Systems Manager가 관리 가능한 EC2 목록을 보여주는 화면이에요. 거기에 안 보인다는 건 SSM Agent가 AWS Systems Manager에 등록하지 못했다는 뜻이에요.

## IAM Role은 맞았고, 네트워크가 더 의심스러웠어요

Image Builder instance profile에는 필요한 정책들이 붙어 있었어요.

```text
EC2InstanceProfileForImageBuilder
AmazonSSMManagedInstanceCore
AmazonEC2ContainerRegistryReadOnly
S3 log용 inline policy
```

이 조합은 Image Builder build instance용으로 정상 범위예요.

물론 role trust relationship은 `ec2.amazonaws.com`이 assume할 수 있어야 해요.

```json
{
  "Effect": "Allow",
  "Principal": {
    "Service": "ec2.amazonaws.com"
  },
  "Action": "sts:AssumeRole"
}
```

정책이 맞는데도 Fleet Manager에 안 보인다면, 다음으로 볼 것은 네트워크예요.

SSM은 AWS가 EC2로 inbound 접속하는 구조가 아니에요. EC2 안의 SSM Agent가 AWS Systems Manager endpoint로 outbound HTTPS 연결을 맺는 구조예요.

```text
EC2 안의 SSM Agent
-> outbound HTTPS 443
-> AWS Systems Manager endpoint
-> 명령 수신 및 결과 업로드
```

그래서 build instance가 SSM endpoint로 나갈 수 있어야 해요.

## public subnet을 쓰면 public IPv4가 필요해요

처음에는 "SSM이 접근하는데 왜 public IP가 필요하지?"라는 의문이 있었어요.

답은 SSM의 연결 방향 때문이에요. SSM은 inbound로 EC2에 들어오는 방식이 아니라, EC2 안의 agent가 AWS 쪽으로 outbound 연결을 여는 방식이에요.

public subnet에서 이 outbound 인터넷 연결이 되려면 아래 조건이 필요해요.

```text
Public subnet route table:
0.0.0.0/0 -> Internet Gateway

EC2:
public IPv4 할당

Security Group outbound:
TCP 443 -> 0.0.0.0/0
TCP 80 -> 0.0.0.0/0
```

public subnet에 있더라도 public IPv4가 없으면 private IP만 가진 EC2가 Internet Gateway를 통해 인터넷으로 나갈 수 없어요.

문제는 Image Builder Infrastructure Configuration에는 build instance에 public IPv4를 강제로 붙이는 옵션이 사실상 없다는 점이에요. `subnetId`, `securityGroupIds`, `instanceProfileName`, `instanceTypes` 같은 설정은 있지만, EC2 launch wizard처럼 `Associate public IP`를 직접 고르는 옵션은 없어요.

그래서 public subnet 방식을 쓰려면 subnet 자체의 `Auto-assign public IPv4` 설정에 의존해야 해요.

## subnet 설정을 전역으로 바꾸고 싶지 않다면 build 전용 subnet이 좋아요

기존 public subnet의 `Auto-assign public IPv4`를 켜는 건 부담스러울 수 있어요. 그 subnet에 앞으로 뜨는 다른 인스턴스에도 영향을 줄 수 있기 때문이에요.

그래서 더 깔끔한 선택지는 Image Builder 전용 public subnet을 새로 만드는 거예요.

```text
VPC: 기존 dev VPC
Subnet: public-image-builder-subnet
Route table: 0.0.0.0/0 -> Internet Gateway
Subnet setting: Auto-assign public IPv4 ON
```

그리고 Infrastructure Configuration에서 이 subnet을 선택해요.

Security Group은 최소 이렇게 잡으면 돼요.

```text
Inbound:
- 없음

Outbound:
- TCP 80 -> 0.0.0.0/0
- TCP 443 -> 0.0.0.0/0
```

디버깅을 위해 SSH를 열고 싶다면 일시적으로만 이렇게 추가해요.

```text
Inbound:
- TCP 22 -> 내 공인 IP/32
```

절대 `22 -> 0.0.0.0/0`로 열지 않는 게 좋아요.

## NAT Gateway를 쓰는 private subnet이 운영적으로는 더 안정적이에요

public subnet 전용으로 해결할 수도 있지만, 운영 관점에서는 private subnet + NAT Gateway가 더 예측 가능해요.

이 경우 build instance에는 public IPv4가 없어도 돼요.

```text
Image Builder build instance
-> private subnet
-> route table: 0.0.0.0/0 -> NAT Gateway
-> NAT Gateway public IP
-> Internet Gateway
-> AWS APIs / Ubuntu repo / AWS CLI download
```

단, NAT Gateway는 자동으로 켜지는 리소스가 아니에요. 별도로 만들어야 하고, private subnet의 route table이 NAT Gateway를 바라보도록 설정해야 해요.

NAT 방식의 조건은 아래와 같아요.

```text
1. Public subnet에 NAT Gateway 생성
2. NAT Gateway에 Elastic IP 연결
3. NAT Gateway가 있는 public subnet route table에 0.0.0.0/0 -> Internet Gateway
4. Image Builder용 private subnet route table에 0.0.0.0/0 -> NAT Gateway
5. Security Group outbound 80/443 허용
```

이렇게 하면 public IP 자동 할당 여부와 상관없이 SSM, apt, curl, ECR 접근이 가능해져요.

## VPC Endpoint만으로는 이번 목적에 부족해요

SSM 등록만 해결하려면 VPC Interface Endpoint를 만들 수도 있어요.

필요한 endpoint는 보통 아래 세 개예요.

```text
com.amazonaws.ap-northeast-2.ssm
com.amazonaws.ap-northeast-2.ssmmessages
com.amazonaws.ap-northeast-2.ec2messages
```

하지만 이번 Component는 `apt update`와 AWS CLI zip 다운로드도 해야 해요. 이 트래픽은 Ubuntu repository와 `awscli.amazonaws.com`으로 나가야 해요.

그래서 SSM endpoint만 만들어서는 Image Builder 전체가 성공하지 않을 가능성이 높아요. 결국 public internet outbound 또는 NAT Gateway가 필요해요.

## 최종 선택지는 세 가지예요

고민한 선택지를 정리하면 아래와 같아요.

### 옵션 1. 기존 public subnet의 auto-assign public IPv4를 켜기

가장 빠른 방법이에요.

```text
VPC > Subnets > 기존 public subnet
-> Actions
-> Edit subnet settings
-> Enable auto-assign public IPv4 address
```

장점은 빠르다는 거예요. 단점은 해당 subnet에 앞으로 뜨는 다른 인스턴스에도 영향을 줄 수 있다는 점이에요.

### 옵션 2. Image Builder 전용 public subnet 만들기

기존 subnet 설정을 건드리기 싫다면 이 방법이 좋아요.

```text
새 public subnet 생성
-> route table: 0.0.0.0/0 -> Internet Gateway
-> auto-assign public IPv4 ON
-> Image Builder Infrastructure Configuration에서 이 subnet 선택
```

Image Builder만을 위한 subnet이라 영향 범위가 작아요. 이번처럼 빠르게 AMI bake를 성공시키기에는 좋은 선택이에요.

### 옵션 3. NAT Gateway가 붙은 private subnet 사용하기

운영적으로 가장 정석적인 방법이에요.

```text
Image Builder build instance는 private subnet
-> route table: 0.0.0.0/0 -> NAT Gateway
```

public IPv4가 필요 없고, build instance가 인터넷에 직접 노출되지 않아요. 단점은 NAT Gateway 비용이 들고, NAT/route table 구성을 먼저 해야 한다는 점이에요.

## Infrastructure Configuration에서 추천하는 설정

최종적으로는 Infrastructure Configuration을 이렇게 잡는 게 좋아요.

```text
Instance type:
- arm64라면 t4g.small 또는 t4g.medium

IAM instance profile:
- EC2InstanceProfileForImageBuilder
- AmazonSSMManagedInstanceCore
- 필요한 경우 AmazonEC2ContainerRegistryReadOnly

Subnet:
- 추천: NAT Gateway가 연결된 private subnet
- 대안: auto-assign public IPv4가 켜진 Image Builder 전용 public subnet

Security Group:
- inbound 없음
- outbound 80/443 허용

Terminate instance on failure:
- 초기 디버깅 중에는 OFF

S3 logs:
- 가능하면 설정

Key pair:
- 디버깅이 필요하면 설정
```

실패 시 인스턴스를 지우지 않게 해두면, build instance에 직접 들어가 로그를 볼 수 있어요.

확인할 로그 위치는 아래예요.

```bash
sudo less /var/log/cloud-init-output.log
sudo less /var/log/cloud-init.log
sudo ls -al /var/log/amazon/imagebuilder/
sudo journalctl -u snap.amazon-ssm-agent.amazon-ssm-agent --no-pager
sudo journalctl -u amazon-ssm-agent --no-pager
```

APT 실패를 볼 때는 아래도 유용해요.

```bash
sudo less /var/log/apt/history.log
sudo less /var/log/apt/term.log
sudo less /var/log/dpkg.log
```

## Bake된 AMI를 쓸 때 user-data는 줄여요

AMI에 Docker, Compose, AWS CLI가 이미 들어가면 user-data는 훨씬 단순해져요.

예시는 아래처럼 애플리케이션 실행에만 집중하면 돼요.

```bash
#!/bin/bash
set -euxo pipefail

REGION="ap-northeast-2"
ACCOUNT_ID="<AWS_ACCOUNT_ID>"
IMAGE="${ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com/umc-product-server:development-04b5f64"

aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"

docker pull "$IMAGE"

docker rm -f umc-product-server || true

docker run -d \
  --name umc-product-server \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  "$IMAGE"
```

여기서도 image tag는 가능하면 고정 문자열로 AMI에 굽지 않고, Launch Template version, SSM Parameter, 배포 파이프라인에서 주입하는 편이 좋아요.

## 최종 체크리스트

AMI bake가 실패할 때는 아래 순서로 보면 좋아요.

```text
1. Image Builder build instance가 실제로 떴는가
2. EC2 status check가 passed인가
3. Systems Manager Fleet Manager에 managed node로 보이는가
4. 안 보인다면 IAM role, trust relationship, SSM outbound 경로를 본다
5. public subnet이라면 public IPv4가 붙었는지 본다
6. private subnet이라면 0.0.0.0/0 -> NAT Gateway route가 있는지 본다
7. Security Group outbound 443이 열려 있는지 본다
8. apt를 쓴다면 outbound 80도 열려 있는지 본다
9. Component에서 Ubuntu 기본 repo 패키지명과 Docker 공식 repo 패키지명을 섞지 않았는지 본다
10. awscli는 apt가 아니라 AWS CLI v2 공식 installer를 사용한다
```

이번 문제의 핵심은 두 가지였어요.

```text
첫 번째 문제:
EC2가 Ubuntu repository로 나갈 네트워크 경로가 없었어요.

두 번째 문제:
네트워크가 해결된 뒤에는 Ubuntu 24.04 arm64에서 awscli/docker-compose-plugin 패키지 설치 방식이 맞지 않았어요.

세 번째 문제:
Image Builder build instance가 SSM managed node로 등록되지 못해 SendCommand InvalidInstanceId가 발생했어요.
```

결국 Image Builder를 성공시키려면 AMI 안의 설치 스크립트만 잘 쓰는 것으로는 부족해요. Image Builder가 띄우는 임시 EC2가 SSM, AWS APIs, Ubuntu repository, AWS CLI 다운로드 서버까지 나갈 수 있는 네트워크를 가져야 해요.

그래서 최종 권장 구성은 아래예요.

```text
운영적으로 추천:
Image Builder 전용 private subnet
-> 0.0.0.0/0 -> NAT Gateway
-> outbound 80/443 허용
-> IAM instance profile에 Image Builder + SSM 권한

빠른 성공을 위한 대안:
Image Builder 전용 public subnet
-> 0.0.0.0/0 -> Internet Gateway
-> auto-assign public IPv4 ON
-> outbound 80/443 허용
```

이 구성이 잡히면 부팅 시점의 `user-data`는 패키지 설치가 아니라 애플리케이션 실행만 담당하게 돼요. 그만큼 서버 부팅 실패 지점이 줄고, 배포 과정도 예측 가능해져요.
