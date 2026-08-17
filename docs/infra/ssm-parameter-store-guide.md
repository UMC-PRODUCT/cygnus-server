# SSM Parameter Store 환경변수 관리 가이드

서버 환경변수(비밀값 포함)는 AWS SSM Parameter Store 에서 관리한다.
배포 시 인스턴스가 부팅 시점에 파라미터를 내려받아 `.env` 를 구성하므로,
**환경변수 변경 = SSM 파라미터 변경 + 재배포(또는 인스턴스 교체)** 이다.

## 파라미터 이름 규칙

```text
/umc-product/<projectName>/<environment>/<KEY>

예)
/umc-product/cygnus-server/prod/DATABASE_URL
/umc-product/cygnus-server/alpha/JWT_ACCESS_TOKEN_SECRET
```

- `<projectName>`: 파라미터를 소유한 프로젝트. 이 저장소(API 서버)는 `cygnus-server` 를 사용한다.
- `<environment>`: `prod` | `alpha`
- `<KEY>`: `.env.example` 의 키 이름 그대로 (UPPER_SNAKE_CASE)
- 타입은 **항상 `SecureString`** 으로 통일한다. 비밀값 여부를 키마다 판단하지 않는다.
- 어떤 키가 존재하는지의 canonical 목록은 루트 [`.env.example`](../../.env.example) 이다.
  새 환경변수를 추가할 때는 `.env.example` 과 `application.yml` 을 먼저 갱신한다.

## 사전 준비 (최초 1회)

1. AWS CLI v2 설치

   ```bash
   # macOS
   brew install awscli

   # 확인
   aws --version
   ```

2. 서버팀 리더에게 IAM 자격증명(Access Key)을 발급받아 프로필로 등록

   ```bash
   aws configure --profile umc
   # AWS Access Key ID / Secret Access Key 입력
   # Default region name: ap-northeast-2
   # Default output format: json
   ```

3. 접근 확인

   ```bash
   aws ssm describe-parameters \
     --parameter-filters "Key=Path,Values=/umc-product/cygnus-server/alpha/" \
     --profile umc --region ap-northeast-2 \
     --query 'Parameters[*].Name' --output table
   ```

발급되는 IAM 정책은 `/umc-product/*` 경로 하위로 제한되어 있다
(`ssm:GetParameter*`, `ssm:PutParameter`, `ssm:DeleteParameter`, `ssm:GetParameterHistory` + `kms:Decrypt`).
prod 쓰기 권한은 서버팀 리더 승인 후 부여된다.

## 자주 쓰는 명령

아래 명령은 모두 `--profile umc --region ap-northeast-2` 를 붙인다고 가정한다.
(매번 붙이기 귀찮으면 `export AWS_PROFILE=umc AWS_DEFAULT_REGION=ap-northeast-2`)

### 목록 조회

```bash
aws ssm get-parameters-by-path \
  --path "/umc-product/cygnus-server/alpha/" --recursive \
  --query 'Parameters[*].Name' --output table
```

### 값 조회 (복호화 포함)

```bash
aws ssm get-parameter \
  --name "/umc-product/cygnus-server/alpha/DATABASE_URL" \
  --with-decryption \
  --query 'Parameter.Value' --output text
```

### 값 등록 / 수정

```bash
aws ssm put-parameter \
  --name "/umc-product/cygnus-server/alpha/LLM_PROVIDER" \
  --type SecureString \
  --tier Intelligent-Tiering \
  --value "google-genai" \
  --overwrite
```

- `--tier Intelligent-Tiering`: 4KB 이하는 무료 Standard, 초과분만 Advanced(유료, $0.05/월)로 자동 저장된다.
- `--overwrite` 를 빼면 이미 존재하는 키에서 에러가 난다 (신규 생성 시 실수 방지용으로 활용 가능).

### 변경 이력 조회

```bash
aws ssm get-parameter-history \
  --name "/umc-product/cygnus-server/alpha/LLM_PROVIDER" --with-decryption \
  --query 'Parameters[*].[Version,LastModifiedDate,LastModifiedUser]' --output table
```

모든 변경은 버전으로 남고 수정자가 기록되므로, 값이 갑자기 바뀌었을 때 이력부터 확인한다.

### 삭제

```bash
aws ssm delete-parameter --name "/umc-product/cygnus-server/alpha/OLD_KEY"
```

삭제는 복구되지 않는다. prod 파라미터 삭제는 반드시 서버팀 리더와 상의 후 진행한다.

### 로컬에서 alpha 환경으로 서버 실행 (파일 생성 없음, 권장)

`.env` 파일을 만들지 않고 실행할 프로세스에만 환경변수를 주입한다.
값이 디스크에 남지 않으므로 유출 위험이 가장 적다.

```bash
# alpha 환경변수를 주입해 bootRun (가장 흔한 사용법)
AWS_PROFILE=umc ./scripts/run-alpha.sh

# 디버거 attach 등 gradle 인자 전달
AWS_PROFILE=umc ./scripts/run-alpha.sh --debug-jvm

# bootRun 대신 임의 명령 실행
./scripts/run-alpha.sh -- java -jar build/libs/app.jar

# 특정 값만 로컬에서 덮어쓰기 (env 가 SSM 주입값 이후에 적용됨)
./scripts/run-alpha.sh -- env APP_SEED_ENABLED=false ./gradlew bootRun
```

alpha 는 공유 환경이므로 시딩·삭제 등 파괴적인 작업은 주의한다.
다른 환경/프로젝트가 필요하면 범용 스크립트를 직접 쓴다:

```bash
./scripts/with-ssm-env.sh prod cygnus-server -- java -jar build/libs/app.jar
```

IntelliJ 에서 실행하고 싶다면 Run Configuration 을 Gradle 이 아닌
"Shell Script" 로 만들어 위 명령을 지정하면 된다.
값이 바뀌었는데 반영이 안 되는 것 같으면 `./gradlew --stop` 후 재실행한다 (데몬 env 캐시).

### 환경 전체를 .env 로 내려받기 (로컬 디버깅용)

```bash
aws ssm get-parameters-by-path \
  --path "/umc-product/cygnus-server/alpha/" --recursive --with-decryption \
  --query "Parameters[*].[Name,Value]" --output text \
  | while IFS=$'\t' read -r name value; do
      printf '%s=%s\n' "${name##*/}" "${value}"
    done > .env.alpha
chmod 600 .env.alpha
```

## .env 파일 일괄 업로드

키가 많을 때는 스크립트를 사용한다. `.env` 형식 파일을 통째로 업로드한다.

```bash
# 미리보기 (업로드 없음)
DRY_RUN=1 ./scripts/upload-env-to-ssm.sh .env.alpha alpha

# 실제 업로드 (확인 프롬프트 후 진행)
AWS_PROFILE=umc ./scripts/upload-env-to-ssm.sh .env.alpha alpha
```

- 빈 값 항목은 자동으로 건너뛴다 (SSM 은 빈 문자열을 허용하지 않는다).
- 4KB 를 넘는 값에는 Advanced tier 과금 경고를 미리 표시한다.
- 기존 파라미터는 덮어쓰므로, 실행 전 DRY_RUN 으로 대상 목록을 확인하는 습관을 들인다.

## 주의사항

- **빈 값은 저장할 수 없다.** "값 없음"이 필요하면 파라미터를 만들지 않는다
  (`application.yml` 기본값이 적용된다).
- **멀티라인 값 금지.** `APPLE_PRIVATE_KEY`, `FIREBASE_CONFIGURATION` 처럼 개행이 포함된 값은
  base64 로 인코딩해 한 줄로 저장하거나, CloudFront private key 처럼 파라미터 이름만 env 로 넘기고
  앱이 SDK 로 직접 조회하는 방식(`CDN_PRIVATE_KEY_PARAMETER_NAME` 패턴)을 사용한다.
- **조회한 비밀값을 Slack/Discord/PR 에 붙여넣지 않는다.** 로컬에 내려받은 `.env.*` 파일은
  `.gitignore` 대상인지 확인하고, 사용 후 삭제한다.
- **prod 값 변경은 즉시 반영되지 않는다.** 실행 중인 인스턴스는 부팅 시점의 값을 사용하므로,
  변경 후 재배포(인스턴스 refresh)가 필요하다.
- 4KB 초과 값은 Advanced tier 로 저장되어 파라미터당 월 $0.05 가 과금된다.
  큰 JSON 은 가급적 base64 압축 전에 필요한 필드만 남기는 것을 검토한다.
- **`FLYWAY_ENABLED` 는 prod/alpha 모두 `false` 로 유지한다.** 배포가 DB 스키마를 변경하지 않도록
  하는 스위치이며, 마이그레이션은 `DB Migrate [Flyway]` 워크플로우로 따로 실행한다.
  `JPA_DDL_AUTO` 는 기본값 `validate` 를 유지한다 (DB 를 변경하지 않는 검증이고, 마이그레이션 누락 시
  부팅을 실패시켜 배포를 막아준다). 자세한 내용은 [Flyway 마이그레이션 런북](./flyway-migration-runbook.md).

## 콘솔 UI

CLI 대신 웹 콘솔에서도 조회/수정할 수 있다:
[AWS Console → Systems Manager → Parameter Store](https://ap-northeast-2.console.aws.amazon.com/systems-manager/parameters?region=ap-northeast-2)
(경로 필터에 `/umc-product/` 입력)
