# SSM 기반 Secret 관리

## 원칙

- 애플리케이션은 secret backend를 직접 알지 않고 환경변수만 읽는다.
- secret 값은 GitHub repository secret, S3 `.env`, Git tracked file에 저장하지 않는다.
- dev/prod 런타임 환경변수는 AWS Systems Manager Parameter Store의 `SecureString`으로 저장한다.
- Parameter Store 값은 배포 시점 또는 로컬 개발자가 필요할 때 dotenv 파일로 렌더링한다.
- multiline secret은 dotenv 파일 안정성을 위해 escaped text 또는 base64 단일 라인으로 저장한다.

## Parameter path 규칙

환경별 path는 아래처럼 둔다.

```text
/umc-product/dev/runtime
/umc-product/prod/runtime
```

parameter leaf name은 애플리케이션 환경변수명과 동일해야 한다.

```text
/umc-product/dev/runtime/JWT_ACCESS_TOKEN_SECRET
/umc-product/dev/runtime/JWT_REFRESH_TOKEN_SECRET
/umc-product/dev/runtime/S3_ACCESS_KEY_ID
/umc-product/dev/runtime/S3_SECRET_ACCESS_KEY
```

렌더링 결과:

```dotenv
JWT_ACCESS_TOKEN_SECRET=...
JWT_REFRESH_TOKEN_SECRET=...
S3_ACCESS_KEY_ID=...
S3_SECRET_ACCESS_KEY=...
```

## 로컬 사용법

팀원은 secret 값이 바뀔 때마다 파일을 전달받지 않고 SSM에서 다시 내려받는다.

```bash
AWS_PROFILE=umcproduct-readonly AWS_REGION=ap-northeast-2 \
  scripts/download-ssm-env.sh \
  --path /umc-product/dev/runtime \
  --output .env.local \
  --overwrite
```

`SPRING_PROFILES_ACTIVE=local`과 같이 로컬에서만 다른 값이 필요하면 별도 `.env.override`를 두거나 실행 명령에서 오버라이드한다. SSM 값 자체에는 공용 dev 런타임 값을 둔다.

## GitHub Actions 배포

`cd-dev-home.yml`과 `cd-asg.yml`은 `vars.SSM_PARAMETER_PATH`를 읽는다.

필수 repository variable:

```text
SSM_PARAMETER_PATH=/umc-product/dev/runtime
```

GitHub Actions AWS credential에는 아래 권한이 필요하다.

```json
{
  "Effect": "Allow",
  "Action": [
    "ssm:GetParametersByPath"
  ],
  "Resource": "arn:aws:ssm:ap-northeast-2:137809407320:parameter/umc-product/dev/runtime/*"
}
```

SecureString이 customer managed KMS key를 사용하면 `kms:Decrypt`도 필요하다.

```json
{
  "Effect": "Allow",
  "Action": [
    "kms:Decrypt"
  ],
  "Resource": "arn:aws:kms:ap-northeast-2:137809407320:key/REPLACE_WITH_KEY_ID"
}
```

## ASG 배포

ASG user-data도 같은 `SSM_PARAMETER_PATH`를 받아 instance 내부에서 `.env`를 생성한다. EC2 instance profile에는 해당 path의 `ssm:GetParametersByPath`와 필요한 `kms:Decrypt` 권한을 부여한다.

## S3 access key 처리

`S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY` 환경변수는 애플리케이션 설정 이름으로 유지한다. 값은 SSM Parameter Store에 저장하고 배포 시 dotenv로 렌더링한다. 장기적으로는 EC2/ECS role 기반 접근으로 전환해 access key 자체를 제거한다.
