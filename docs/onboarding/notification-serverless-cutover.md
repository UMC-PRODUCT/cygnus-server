# Notification serverless cutover

이 문서는 Notification Node.js Lambda stack의 배포, shadow, production 전환, rollback 절차를
정리한다. 코드 기본값은 Java local transport와 비활성 SQS event source mapping이다.

## 1. 배포 전 준비

### GitHub environment 변수

dev/prod environment에 다음 값을 설정한다.

- `AWS_NOTIFICATION_DEPLOY_ROLE_ARN`
- `NOTIFICATION_FIREBASE_SECRET_ID`
- `NOTIFICATION_WEBHOOK_SECRET_ID`
- `NOTIFICATION_EMAIL_FROM_ADDRESS`
- `NOTIFICATION_EMAIL_FROM_NAME`
- `NOTIFICATION_SES_CONFIGURATION_SET`

Firebase secret은 service account JSON, Webhook secret은 `slackUrl`, `discordUrl`,
`telegramBotToken`, `telegramChatId` JSON 필드를 가진다. secret 원문은 로그나 workflow output으로
출력하지 않는다. 두 secret 변수에는 이름이 아니라 ARN을 넣는다. customer-managed KMS key로
secret을 암호화했다면 해당 key의 `kms:Decrypt`도 Lambda 실행 role에 추가한다.

### Java ASG role 권한

- notification EventBridge bus에 `events:PutEvents`
- installation command function에 `lambda:InvokeFunction`
- delivery result queue에 `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:GetQueueAttributes`
- notification KMS key에 `kms:Decrypt`, `kms:GenerateDataKey`

### Java 환경변수

```text
NOTIFICATION_TRANSPORT=local
NOTIFICATION_EVENTBRIDGE_ENABLED=false
NOTIFICATION_LAMBDA_ENABLED=false
NOTIFICATION_RESULT_CONSUMER_ENABLED=false
NOTIFICATION_AWS_REGION=ap-northeast-2
NOTIFICATION_EVENT_BUS_NAME=<SAM output EventBusName>
NOTIFICATION_INSTALLATION_FUNCTION_NAME=<SAM output FcmInstallationFunctionName>
NOTIFICATION_RESULT_QUEUE_URL=<SAM output DeliveryResultQueueUrl>
```

## 2. 최초 stack 배포

CD workflow를 다음 입력으로 실행한다.

```text
environment=dev 또는 prod
delivery_enabled=false
delivery_mode=VALIDATE
```

이 상태에서 EventBridge rule은 queue에 event를 적재할 수 있지만 worker mapping과 token validator
schedule은 비활성이다.

## 3. 앱 선배포와 재등록 관찰

앱은 인증된 실행, 로그인, FCM token refresh마다 installation API를 호출해야 한다.

```text
POST /api/v1/notifications/fcm/installations
DELETE /api/v1/notifications/fcm/installations/{installationId}
```

Java는 JWT에서 `memberId`를 확정하고 installation command Lambda를 IAM으로 호출한다. PostgreSQL
token backfill은 실행하지 않는다.

앱 선배포 후 Java를 다음으로 배포한다.

```text
NOTIFICATION_TRANSPORT=shadow
NOTIFICATION_EVENTBRIDGE_ENABLED=true
NOTIFICATION_LAMBDA_ENABLED=true
NOTIFICATION_RESULT_CONSUMER_ENABLED=false
```

이때 Java provider가 실제 발송을 유지한다. Node worker는 아직 비활성이며 installation만
DynamoDB에 누적된다.

재등록률은 최근 30일 DynamoDB 활성 installation의 고유 member 수를 같은 기간 PostgreSQL 활성
installation의 고유 member 수로 나눠 계산한다. production 전환 조건은 95% 이상이다.

## 4. Shadow 검증

stack을 `delivery_enabled=true`, `delivery_mode=VALIDATE`로 갱신한다.

- FCM은 Firebase `dryRun`
- 이메일은 Handlebars rendering과 만료 검증
- Webhook은 계약 검증만 수행
- 채용 메일 result는 VALIDATE에서 발행하지 않음

다음 조건을 모두 확인한다.

- AsyncAPI/runtime validation 오류 0건
- DLQ visible message 0건
- token 재등록률 95% 이상
- 20,000명, 평균 2 installation 부하에서 queue age p95 60초 이하
- 모든 Java ASG instance가 `local`, `shadow`, `external` event를 이해하는 버전

## 5. Production cutover

1. stack을 `delivery_enabled=false`, `delivery_mode=LIVE`로 배포해 mapping을 중지하고 queue 적재를
   유지한다.
2. 모든 Java ASG instance를 다음 설정으로 교체한다.

```text
NOTIFICATION_TRANSPORT=external
NOTIFICATION_EVENTBRIDGE_ENABLED=true
NOTIFICATION_LAMBDA_ENABLED=true
NOTIFICATION_RESULT_CONSUMER_ENABLED=true
```

3. flag 전파를 확인한 후 stack을 `delivery_enabled=true`, `delivery_mode=LIVE`로 한 번에 갱신한다.
4. FCM, 이메일, Webhook mapping이 모두 활성인지 확인한다.
5. DLQ, oldest message, Lambda error/throttle, FCM failure rate, SES bounce/complaint alarm을 확인한다.

Java provider 구현은 14일 동안 비활성 상태로 보존한다.

## 6. 긴급 rollback

### Lambda 코드 문제

Lambda alias/version을 직전 정상 버전으로 되돌린다. stack 리소스와 queue는 유지한다.

### 전달 경로 문제

1. stack의 `delivery_enabled=false`로 event source mapping을 중지한다.
2. Java를 `NOTIFICATION_TRANSPORT=local`로 되돌린다.
3. `NOTIFICATION_RESULT_CONSUMER_ENABLED=false`로 result polling을 중지한다.
4. 외부 queue 메시지는 삭제하지 않고 별도 격리한다.
5. `NotificationDeliveries` ledger로 완료 event를 제외한 뒤 재처리한다.

DLQ 발생, oldest message 5분 초과, Lambda throttle, FCM 실패율 5% 초과, SES reputation alarm 중
하나라도 발생하면 자동 재처리 전에 원인을 확인한다.

## 7. 검증 명령

```bash
cd services/notification
npm ci
npm run check

cd ../..
sam validate --lint --template-file infra/notification/template.yaml
sam build --template-file infra/notification/template.yaml

./gradlew compileJava compileTestJava
./gradlew test
```
