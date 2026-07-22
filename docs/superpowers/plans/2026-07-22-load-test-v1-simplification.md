# load-test 환경 v1 단순화 계획

- 작성일: 2026-07-22
- 대상: `loadtest/terraform/`, `loadtest/k6/`, `loadtest/scripts/`, `docs/adr/013`
- 구조 정리(후속): load-test terraform은 배포 env가 아니라 ephemeral 테스트 리그라 `infra/terraform/envs/load-test/` → `loadtest/terraform/`로 이동함. 아래 Phase 본문의 `infra/terraform/envs/load-test` 경로는 이동 전 기준이다.
- 선행 계획: `docs/superpowers/plans/2026-06-28-load-test-terraform.md` (SSM 기반 초기 구현). 본 문서는 그 구조를 v1으로 단순화하는 개정안이다.
- 상태: **확정 대기 (구현 착수 전 리뷰용)**

---

## 1. 배경 / 목표

현재 load-test 환경은 SUT의 런타임 비밀값을 SSM Parameter Store에서 EC2 IAM role로 조회하는 것을 전제로 한다. 이 방식은 테스트를 띄우기 전에 사람이 SSM parameter를 먼저 만들어야 하고, generator user-data가 부팅 중 repo를 clone해 k6 스크립트/시드까지 준비하는 등 user-data가 너무 많은 책임을 진다.

아직 이 부하 테스트 환경의 실제 AWS 비용과 실행 빈도를 모르는 단계다. 따라서 S3 backend + lock, SSM secret 주입까지 한 번에 고정하기보다, **비용과 실행 흐름을 먼저 검증하는 v1**을 목표로 한다.

**v1 목표: "누가 로컬에서 띄워도 같은 순서로 시드 넣고 k6를 돌릴 수 있는 구조."**

---

## 2. 전제 (이번 범위 밖)

- S3 backend + DynamoDB lock은 이번에 하지 않는다.
- 비밀값은 기존처럼 Notion에서 공유하고, 각자 로컬 `infra/terraform/envs/load-test/load-test.env`에 입력한다.
- `terraform.tfvars`에는 secret 원문이 아니라 `app_env_file_path = "./load-test.env"`처럼 로컬 파일 경로만 둔다.
- `terraform.tfvars`, `load-test.env`, `*.tfstate`, `*.auto.tfvars`는 커밋 금지 유지.
- 이 방식은 비밀값이 로컬 Terraform state와 EC2 user-data에 남는다. 운영/장기 환경엔 부적합하지만, **destroy 전제의 ephemeral 부하 테스트 환경**에서는 비용 대비 단순성이 낫다.
- prod/dev의 secret backend 정책은 이 문서 범위가 아니다. **load-test 환경만** 다룬다.
- SSM Parameter Store 조회는 **후속 개선**으로 남긴다.

---

## 3. 확정된 결정 사항

| 항목 | 결정 |
|------|------|
| k6 디렉터리 구조 | **리뷰안 채택** — `loadtest/k6/` 아래 단일 `script.js` entrypoint가 `PROFILE`/`SCENARIO` env로 시나리오 선택. ADR-013의 per-file 구조 기술은 이 구조로 갱신. |
| v1 시딩 방식 | **prepare-data.sh가 데이터 준비의 유일한 소유자.** k6는 시딩하지 않고 `seed.json`을 읽기만 한다. prepare-data.sh는 `SEED_STRATEGY` 스위치 구조로 두고 v1은 `api`만 구현, `sql`(덤프 복원)/`snapshot`은 후속 (아래 4절 근거). |
| 진행 방식 | 본 플랜 문서 확정 → Phase 0(정리/보안)부터 순차 구현. 스트레이 시크릿 정리는 즉시. |

---

## 4. 시딩 전략 조언 (이 프로젝트 구조 기준)

이 레포에는 이미 도메인 가드를 통과하는 잘 만들어진 시딩 API가 있다 (`SeedController`, ADR-017). 엔드포인트에 **명확한 순서 의존성**이 있다:

```
POST /test/seed/members        → POST /test/seed/challengers
  → POST /test/seed/projects (or /projects/scenarios)
  → POST /test/seed/project-applications, /curriculum, /notice
DELETE /test/seed/projects     (gisu 단위 정리)
```

기존 `docs/guides/load-test/k6/data/seed.local.json`(`gisuId`, `chapterId`, `matchingRoundId`, `memberIds`, `targets`)은 명백히 **이 시딩 체인을 실행한 결과를 파일로 굳혀 재사용**하는 산출물이다.

**결론: v1 무게중심은 k6 setup() 직접 시딩이 아니라 `prepare-data.sh`(API 시딩)다.** 근거:

1. **순서 의존성** — members→challengers→projects→applications 다단계를 k6 `setup()`(단일 iteration) 안에 다 넣으면 무겁고, 시딩 실패가 부하 실행 흐름에 섞인다.
2. **재현성/오염 방지** — 시딩은 느리고 비결정적(force 임계값, 풀 부족 시 skip)이다. k6 run마다 setup()으로 재시딩하면 매 실행이 DB에 데이터를 누적해 측정 조건이 오염된다. 한 번 시딩해 `seed.json`으로 굳히고 재사용하는 게 맞다.
3. **정리 API 존재** — `DELETE /test/seed/projects`가 있어 `prepare-data.sh`가 seed(+필요 시 cleanup)까지 관장하면 반복 실행 재현성이 좋아진다.
4. **현행 코드와 정합** — generator의 `run-umc-k6`가 이미 `SEED_FILE=./data/seed.json`을 k6에 넘긴다. `prepare-data.sh`가 seed API 응답에서 이 JSON을 만들고 k6가 읽어 VU에 분배하는 흐름이 지금 코드와 정확히 맞는다.

**k6는 시딩하지 않는다.** 시딩은 오직 prepare-data.sh가 소유하고, k6 `lib/data.js`는 prepare-data.sh가 산출한 `seed.json`을 **읽기만** 한다. `smoke/health-check`처럼 데이터가 거의 필요 없는 시나리오는 seed.json 없이 바로 돈다. (setup()에서 seed API를 호출하는 폴백은 두지 않는다 — 시딩 실패/지연이 부하 실행에 섞이는 것을 막기 위함.)

### 4.1 SeedController의 역할 경계와 대용량 승계

`SeedController`의 가치는 **도메인 가드를 통과한 정확한 baseline**이지 대용량 생성이 아니다. 대용량을 컨트롤러로 밀면 안 되는 이유:

- HTTP 왕복 × N + 엔티티마다 전체 도메인 검증 → O(N)이라 수만~수십만 행에서 매우 느리다.
- 시딩 트래픽이 측정 대상(SUT) 앱을 직접 때려 부하 테스트 조건을 오염시킨다. 시딩과 측정은 분리돼야 한다.

**승계 방식은 "생성기(코드)가 원천, 데이터 파일은 캐시"다.**

> (2026-07-22 개정) 초안의 `pg_dump --data-only` 승계(`SEED_STRATEGY=sql`)는 **폐기**했다.
> 벌크 생성 경로 없이는 10만+ baseline 을 굽는 것 자체가 api 시더로는 비현실적이고,
> 덤프 아티팩트는 캐시일 뿐 원천이 될 수 없어서다. 대체는 아래 `bulk`.

```
[생성] seeder 프로파일 Spring 벌크 시더 — 골격(기수·학교·역할)은 use case 경유,
       대량(멤버·챌린저·공지·상벌점·스케줄)은 JdbcTemplate 배치
       (엔티티가 전부 GenerationType.IDENTITY 라 Hibernate 배치 인서트가 무력화되므로 JPA saveAll 은 배제)
[캐시] RDS snapshot(rds.tf snapshot_identifier) — destroy/apply 사이클을 넘길 때 복원
```

**전략별 동작 계층이 다르다** — prepare-data.sh 과설계를 피하기 위한 핵심 구분:

| 전략 | 계층 | 위치 | v1 |
|------|------|------|----|
| `api` | apply 이후 | `prepare-data.sh` → SeedController | ✅ 구현 |
| `bulk` | apply 이후 | `prepare-data.sh` → SUT EC2 에서 `seeder` 프로파일 컨테이너 1회 실행 | 후속 |
| `snapshot` | **apply 시점** | `rds.tf`의 `snapshot_identifier` (prepare-data.sh 아님) | 후속 |

`api`/`bulk`는 prepare-data.sh의 `SEED_STRATEGY` 스위치로 드롭인 교체 가능. `snapshot`은 RDS 생성 시점 문제라 Terraform(`rds.tf`)에서 다룬다.

---

## 5. 단계별 구현 계획

각 Phase 끝에 verify 게이트를 둔다.

### Phase 0 · 정리 / 보안 (선행, 즉시)

- **스트레이 디렉터리 `infra/terraform/load-test/` 제거.** git-untracked이지만 최상위 `.gitignore`가 `envs/*/terraform.tfvars`만 무시하므로 이 경로의 `terraform.tfvars`(실제 계정ID·개인 공인 IP·Secrets Manager ARN 포함, 게다가 옛 변수명 `app_env_secret_arn`/`registry_credentials_secret_arn` 사용 — SSM 리팩터 이전 잔재)는 무시 대상이 아니다. → 삭제.
- 최상위 `.gitignore`에 재발 방지 규칙 추가: `infra/terraform/load-test/`.

verify:
```bash
test ! -e infra/terraform/load-test           # 스트레이 제거 확인
git status --porcelain | grep -q "load-test/terraform.tfvars" && echo FAIL || echo OK
```

### Phase 1 · Terraform 시크릿 주입 단순화 (리뷰 1·2·4)

`infra/terraform/envs/load-test/`

> **레지스트리: ECR 전용화.** generic/public은 이 팀이 쓰지 않으므로 `registry_type` 추상화를 통째로 제거하고 항상 ECR pull로 고정한다. 나중에 Docker Hub 등이 필요해지면 그때 재도입.

- **variables.tf**
  - 제거: `app_env_ssm_parameter_path`(L173), `registry_credentials_ssm_parameter_name`(L150), `ssm_kms_key_arn`(L189)
  - 제거: `registry_type`(L127), `registry_server`(L138) — ECR 전용화로 불필요
  - 유지: `ecr_repository_name`(IAM pull 정책 스코프에 필요), `app_image`
  - 추가: `app_env_file_path`(기본 `"./load-test.env"`, `fileexists` validation)
  - L165 주석의 `docs/guides/load-test/k6` → `loadtest/k6`로 수정
- **main.tf (locals)**
  - 제거: `normalized_app_env_ssm_parameter_path`, `normalized_registry_credentials_ssm_param_name`, `app_env_ssm_parameter_arns`, `registry_credentials_ssm_parameter_arns`, `sut_ssm_parameter_arns` (L45–54)
  - 제거: `resolved_registry_server`의 조건 분기, `sut_needs_iam_role` — ECR 전용이라 registry_server는 항상 `local.ecr_registry_server`, IAM role은 항상 필요
  - 추가: `app_env_content = file(var.app_env_file_path)`
- **iam.tf**
  - 제거: `sut_ssm_parameter_access`, `sut_ssm_kms_decrypt` (data + role_policy 각 2개)
  - 변경: `sut_ecr_pull` / `aws_iam_role.sut` / `aws_iam_instance_profile.sut`의 `count` 가드 제거 → ECR 전용이라 항상 생성
  - 유지: `sut_assume_role`, `sut_ecr_pull` 정책 내용
- **compute.tf (aws_instance.sut)**
  - 제거: `registry_type`, `registry_credentials_ssm_parameter_name`, `app_env_ssm_parameter_path` template 인자
  - 변경: `registry_server`는 `local.ecr_registry_server` 직접 전달, `iam_instance_profile`는 `aws_iam_instance_profile.sut.name`(try 불필요)
  - 추가: `app_env_content_b64 = base64encode(local.app_env_content)`
- **user-data/sut.sh.tftpl**
  - 제거: `registry_type` 분기 전체(L28–54의 generic/public case) → 항상 ECR 로그인만 남김
  - 제거: app_env SSM 조회 블록(L59–99)
  - 추가: `printf '%s' "${app_env_content_b64}" | base64 -d > app.env`
  - 유지: 이후 `cat >> app.env`의 DB/OTEL/Hikari/dev override는 그대로. **핵심은 뒷부분이 항상 `/opt/umc/app.env`만 바라보게 하는 것.**
- **terraform.tfvars.example**
  - 제거: SSM 관련 라인(L30–42), `registry_type`·`registry_server`·`registry_credentials_ssm_parameter_name`(L26–33)
  - 유지: `app_image`, `ecr_repository_name`
  - 추가: `app_env_file_path = "./load-test.env"`
- **신규: load-test.env.example** — 필요한 키 목록만. 실제 secret 값 금지.
  ```
  JWT_SECRET=<Notion에서 공유>
  OAUTH_CLIENT_SECRET=<Notion에서 공유>
  FIGMA_TOKEN_ENCRYPTION_KEY=<Notion에서 공유>
  # 주의: DB/OTEL/Hikari 값은 넣지 않는다. Terraform이 user-data에서 덮어쓴다.
  ```
- **.gitignore (envs/load-test/)** — `load-test.env` 추가 (`load-test.env.example`은 커밋 유지)

verify:
```bash
terraform -chdir=infra/terraform/envs/load-test fmt -check
terraform -chdir=infra/terraform/envs/load-test validate
```

### Phase 2 · user-data 책임 축소 (리뷰 3)

`user-data/generator.sh.tftpl`

- 유지: Docker 설치, k6 설치, node-exporter 실행, `/home/ec2-user/k6` 디렉터리 생성, `/usr/local/bin/run-umc-k6` 생성
- 제거: repo clone(L43–46), k6 복사, seed.example.json 복사(L51–55)
- `run-umc-k6`: 2-arg(`profile rate`) → **4-arg(`profile scenario rate duration`)**, 넘기는 env: `BASE_URL/PROFILE/SCENARIO/RATE/DURATION`
- compute.tf L111 주석의 `docs/guides/load-test/k6` → `loadtest/k6` 수정

verify:
```bash
bash -n infra/terraform/envs/load-test/user-data/*.sh.tftpl   # 템플릿 문법(간이)
```

### Phase 3 · k6 하니스 신규 작성 + `loadtest/k6/` 배치 (리뷰 6) — 최대 작업

> 주의: 현재 `docs/guides/load-test/k6/`에는 `data/seed.local.json`만 있고 `script.js`·scenarios·lib이 **없다**. 이동이 아니라 신규 작성이다.

```
loadtest/k6/
├── script.js                 # entrypoint. PROFILE/SCENARIO로 options·시나리오 선택
├── config/profiles.js        # smoke/load/stress/soak별 options·thresholds·기본 RATE/DURATION
├── lib/
│   ├── auth.js               # 인증 필요한 시나리오용 선택 helper
│   ├── data.js               # seed.json 로드 전용 (k6는 시딩하지 않음)
│   ├── http.js               # 공통 headers·checks·error tagging
│   └── metrics.js            # custom metrics·scenario tags
├── scenarios/
│   ├── smoke/{health-check.js, project-read.js}
│   ├── load/{project-read.js, application-submit.js}
│   ├── stress/project-read.js
│   └── soak/project-read.js
└── data/seed.json            # (gitignore) — docs/guides/load-test/k6/data/seed.local.json 이관
```

- seed 데이터 이관 후 `docs/guides/load-test/k6/` 정리
- ADR-013을 리뷰안 구조로 갱신 (per-file → script.js entrypoint + PROFILE/SCENARIO, `loadtest/` → `loadtest/k6/`)

verify:
```bash
test -f loadtest/k6/script.js
! rg -n "docs/guides/load-test/k6" infra/terraform scripts loadtest
```

### Phase 4 · 실행 스크립트 (리뷰 5)

`loadtest/scripts/`

- **sync-k6.sh** (로컬) — `terraform output -raw generator_public_ip`로 `loadtest/k6/`(script.js, config/, lib/, scenarios/)를 generator `/home/ec2-user/k6/`로 rsync
- **run-k6.sh** (로컬) — sync-k6 호출 후 SSH로 `run-umc-k6 <profile> <scenario> <rate> <duration>` 실행
- **prepare-data.sh** (로컬) — **데이터 준비의 유일한 소유자.** `SEED_STRATEGY`(기본 `api`) 스위치로 방식을 고른다. v1은 `api`만 구현.
  - `api`: **도메인별 얇은 스텝**으로 seed API를 순서 호출 → 반환 ID를 조립해 `seed.json` 산출
    ```
    seed_members → seed_challengers → seed_projects(/scenarios)
      → seed_applications → seed_curriculum → seed_notice
      → assemble seed.json   # ID 핸드오프 조립은 한 곳에 모아 계약 안정
    ```
    - **각 스텝은 얇게**: "어느 엔드포인트를, 어떤 순서로, 어떤 ID를 다음으로 넘기는지"만 담는다. **도메인 규칙은 SeedController/SeedService(Java)에 남긴다.** 스텝에 도메인 지식을 넣으면 이중 관리가 되므로 금지.
    - 변경 지점 분리: API 계약 변경 → 해당 도메인 스텝만 수정 / 도메인 규칙 변경 → Java만 (스텝 대개 무변경).
  - `bulk`(후속): SUT EC2 에서 앱 이미지를 `seeder` 프로파일로 1회 실행해 대규모 적재 (4.1절 개정 참조). ~~`sql`(pg_dump 승계)~~ 는 폐기.
  - `snapshot`은 여기 아님 — RDS 생성 시점이라 `rds.tf`의 `snapshot_identifier`로 다룸(후속).
  - 주의: 6개 도메인을 bash로 오케스트레이션하면 JSON 스레딩/에러 처리가 지저분해질 수 있다. v1은 bash + `jq`로 충분하되, ID 전달이 복잡해지면 작은 스크립트 언어 이관을 후속으로 연다.

verify:
```bash
bash -n loadtest/scripts/*.sh
```

### Phase 5 · outputs 보강 + 문서 (리뷰 7·8)

- **outputs.tf**: `generator_public_ip`(raw, 스크립트 파싱용) 추가. 기존 `generator_ssh`는 유지. `next_steps`의 `run-umc-k6 smoke 1` → 4-arg 예시로 갱신
- `docs/loadtest/`는 실행 결과·분석 문서 전용으로 정리(ADR-013 방침)

verify:
```bash
terraform -chdir=infra/terraform/envs/load-test validate
```

---

## 6. 최종 검증 기준 (merge 전 최소)

```bash
terraform -chdir=infra/terraform/envs/load-test fmt -check
terraform -chdir=infra/terraform/envs/load-test validate
bash -n loadtest/scripts/*.sh
! rg -n "docs/guides/load-test/k6" infra/terraform scripts loadtest
```

실제 AWS 검증(비용 발생, 선택):
```bash
terraform -chdir=infra/terraform/envs/load-test apply
loadtest/scripts/prepare-data.sh
loadtest/scripts/run-k6.sh smoke health-check 1
terraform -chdir=infra/terraform/envs/load-test destroy   # plan -destroy로 대상 확인 후
```

---

## 7. 리스크 / 롤백

- **비밀값이 state·user-data에 남음** — 의도된 트레이드오프(ephemeral + destroy 전제). 운영 전환 시 SSM 런타임 조회(현행 PR 방식)로 복귀.
- **IAM 축소** — SSM/KMS 권한 제거로 SUT가 SSM 조회 불가. 후속 SSM 재도입 시 `app_env_source = "local_file" | "ssm_parameter_store"` 선택 변수로 app.env 생성부만 교체.
- **롤백** — 본 변경은 Git 커밋 단위로 되돌리기 가능. state를 건드리는 apply/destroy는 별도 승인 하에서만.

## 8. 미결 / 후속

- SSM Parameter Store 재도입(후속 PR)
- S3 backend + DynamoDB/lockfile(후속)
- 대량 데이터 승계(4.1절 개정): `seeder` 프로파일 Spring 벌크 시더(`SEED_STRATEGY=bulk`) + RDS snapshot 캐시. ~~pg_dump sql 전략~~ 은 폐기.
- ~~generic registry 인증 지원 여부~~ → **결정: ECR 전용화(generic/public 제거).** 필요 시 후속 재도입.
