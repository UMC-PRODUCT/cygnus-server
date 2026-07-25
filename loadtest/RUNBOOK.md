# 부하 테스트 실행 절차서 (Runbook)

시나리오를 돌리는 날, 위에서부터 순서대로 따라간다. 전부 **repo 루트**에서 실행.
개념·시딩 전략·확장법은 `loadtest/README.md`, 전략 배경은 `docs/adr/013` 참조.

---

## 0. 사전 준비

### 최초 1회 (이미 했다면 건너뜀)

```bash
cp loadtest/terraform/terraform.tfvars.example loadtest/terraform/terraform.tfvars
cp loadtest/terraform/load-test.env.example    loadtest/terraform/load-test.env
```

- `terraform.tfvars`: `aws_profile`, `key_name`, `allowed_cidr`(내 IP/32), `app_image` 채우기
- `load-test.env`: 앱 부팅 secret 채우기 (`src/main/resources/.env.local` 기반. 단 DB/OTEL/HIKARI/프로파일 키는 넣지 말 것 — user-data 가 덮어씀)
- ECR repo·EC2 키페어가 계정에 존재해야 함

### 매 세션 시작할 때

```bash
# 1) AWS 세션 확인 — 계정 ID 나오면 OK, "session expired" 면 aws login
aws sts get-caller-identity

# 2) IP 바뀌었으면(카페/집 이동) tfvars 의 allowed_cidr 갱신 후 apply 때 반영됨
curl ifconfig.me

# 3) SSH 키 (run-k6 / bulk 시딩 / DataGrip 터널 공통)
export SSH_KEY=~/.ssh/umc-loadtest.pem
```

---

## 1. 리그 올리기 (공통)

```bash
# 앱 코드가 바뀌었을 때만 — 이미지 재빌드 + ECR push
loadtest/scripts/push-app-image.sh

# 인프라 생성 (최초엔 init 먼저)
terraform -chdir=loadtest/terraform init      # 최초 1회
terraform -chdir=loadtest/terraform apply     # yes

# 앱 기동 대기 (2~3분) 후 health 확인 — {"status":"UP"} 나와야 다음 진행
curl $(terraform -chdir=loadtest/terraform output -raw sut_app_url)/actuator/health
```

---

## 2-A. smoke 경로 (소규모 검증 — 시나리오 개발·회귀 확인용)

시딩은 **api**(기본, 30명). 매 run 신선한 데이터, 수십 초면 끝난다.

```bash
# 시딩 → loadtest/k6/data/seed.json 산출
loadtest/scripts/prepare-data.sh
#   출력 확인: member 30명 / 스케줄 4개 / 공지 5건 / 상벌점 60건 — 실패 로그 없어야 함

# 부하 (smoke = rate 1, 1분)
loadtest/scripts/run-k6.sh smoke health-check 1 1m   # 리그 자체 확인 (시딩 불필요)
loadtest/scripts/run-k6.sh smoke home         1 1m   # 시나리오 검증 — 에러율 0% 확인
```

시딩 규모 조절: `SEED_MEMBER_COUNT=100 loadtest/scripts/prepare-data.sh`

## 2-B. bulk 경로 (대규모 본 측정 — 10만+ 행)

시딩은 **bulk**(seeder 프로파일 컨테이너, JDBC 배치). **전제 2가지**:
① app 컨테이너가 떠 있어야 함(1의 health UP) ② **빈 DB** — api/bulk 시딩이 이미 들어간 DB엔 재실행 불가(email unique 로 의도적 실패)

```bash
# (1) 소규모로 경로 검증 — 1000명, ~1분. 처음이거나 스키마 바뀐 뒤엔 반드시
BULK_MEMBER_COUNT=1000 SEED_STRATEGY=bulk loadtest/scripts/prepare-data.sh
#   출력 확인: [bulk-remote] image=... network=... → bulk seed completed → wrote seed.json

# (2) DB 리셋 — 검증용 데이터를 지우고 깨끗한 조건에서 본 시딩
terraform -chdir=loadtest/terraform destroy   # RDS 포함 통째 리셋
terraform -chdir=loadtest/terraform apply     # 새 빈 RDS
# (앱 health UP 다시 확인 후)

# (3) 본 시딩 — 10만 명 (수 분)
SEED_STRATEGY=bulk loadtest/scripts/prepare-data.sh

# (4) 본 측정
loadtest/scripts/run-k6.sh smoke home 1   1m     # 먼저 스모크로 seed.json 검증
loadtest/scripts/run-k6.sh load  home 300 10m    # 본 부하 (고정 도착률)
loadtest/scripts/run-k6.sh breakpoint home 500 10m        # 포화점 탐색: 1→500 req/s 점증
#   → Grafana ① 행 "포화점 탐색" 패널에서 TPS 평탄화+응답시간 급등 교차점 = 한계 처리량
loadtest/scripts/run-k6.sh spike home 300 2m              # 이벤트 폭주: 10초 급등→2분 유지→30초 회복
#   → breakpoint 로 찾은 한계의 60~80% rate 로 급등을 버티는지 + 회복되는지 관찰
loadtest/scripts/run-k6.sh stress project-read 1000 20m   # 필요 시
```

조절 옵션: `BULK_MEMBER_COUNT`(기본 100000) · `BULK_RANDOM_SEED`(기본 42 — 같으면 같은 데이터, 실행 간 비교 가능)

같은 대규모 데이터를 자주 반복한다면: 본 시딩 후 `aws rds create-db-snapshot` 으로 구워 tfvars `db_snapshot_identifier` 에 지정 → 이후 apply 는 시딩 없이 즉시 복원 (README "시딩" 절 참조)

---

## 2.5 결과 기록 (실행 전에 시작)

```bash
loadtest/scripts/new-run.sh home-breakpoint-500   # runs/<날짜>-<이름>/summary.md 스캐폴드
export RUN_DIR=docs/loadtest/runs/<방금 생성된 디렉터리>   # 이후 run-k6.sh 결과가 여기 자동 저장
```

실행 **전**에 만들어 가설부터 적는다. `RUN_DIR` 를 설정하면 run-k6.sh 가 콘솔 로그(.log)와
k6 요약(.summary.json)을 실행마다 이 디렉터리에 자동 저장한다 — destroy 해도 결과가 남는다.
끝나면 summary.md 에 결론·Grafana 스크린샷을 채운다. 규칙: `docs/loadtest/README.md`.

## 3. 결과 보기

> 대시보드에서 병목을 판정하는 법(증상 조합 매트릭스)은 `docs/loadtest/diagnosis.md`.

```bash
# Grafana — 브라우저 바로 접속 (ID: admin)
terraform -chdir=loadtest/terraform output -raw grafana_url
terraform -chdir=loadtest/terraform output -raw grafana_admin_password

# DB (DataGrip) — RDS 는 private 라 SSH 터널 필요. 값 3개 조회:
terraform -chdir=loadtest/terraform output -raw sut_public_ip   # 터널 호스트 (user: ec2-user, key: pem)
terraform -chdir=loadtest/terraform output -raw rds_endpoint    # General 의 Host (:5432 떼고) / DB umc_product / user postgres
terraform -chdir=loadtest/terraform output -raw db_password
```

실행 결과·분석은 `docs/loadtest/` 아래에 실행 단위로 남긴다 (가설·환경·결론 포함).

---

## 4. 내리기 (⚠️ 비용 — 세션 끝나면 반드시)

```bash
terraform -chdir=loadtest/terraform destroy
```

데이터도 같이 사라지지만 시더가 결정적(seed 고정)이라 언제든 같은 조건으로 재현된다.

---

## 트러블슈팅

| 증상 | 조치 |
|------|------|
| apply 중 SG `Character sets beyond ASCII` | AWS SG description 은 ASCII 만 허용 — tf 의 한글 description 을 영문으로 |
| `session expired` | `aws login` 재실행 |
| SSH/Grafana 접속 불가 | IP 바뀜 → tfvars `allowed_cidr` 갱신 후 apply. pem 권한은 `chmod 400` |
| health UP 안 뜸 | 이미지 push 누락(`push-app-image.sh`) 또는 load-test.env 필수 키 누락 → SUT 에서 `sudo docker compose logs app` |
| bulk: `app 컨테이너가 실행 중이 아닙니다` | 앱 부팅 대기 후 재실행 (health UP 먼저) |
| bulk: email unique 에러 | 빈 DB 아님 → destroy/apply 로 리셋 후 재시딩 |
| k6 `seed.json 없음` | `prepare-data.sh` 먼저 실행 |
| 시나리오 추가하고 싶다 | `loadtest/README.md` "확장 — 새 시나리오 추가" |
