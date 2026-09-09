# Flyway 마이그레이션 런북 (배포와 분리 실행)

배포 환경(prod/alpha)에서는 **애플리케이션 배포가 DB 스키마를 변경하지 않는다.**
스키마 변경은 `DB Migrate [Flyway]` 워크플로우를 수동 실행(workflow_dispatch)해서만 수행한다.

## 왜 분리했나

- 배포(ASG instance refresh)와 스키마 변경이 한 트랜잭션처럼 묶여 있으면, 롤백 시 코드만 되돌아가고
  스키마는 되돌아가지 않는다. 실행 시점을 사람이 정할 수 있어야 한다.
- 부팅마다 수십 개 마이그레이션의 checksum 을 비교하는 비용도 사라진다.

## 구성

| 스위치 | 값 | 위치 | 효과 |
|--------|-----|------|------|
| `FLYWAY_ENABLED` | `false` | SSM (prod/alpha) | 부팅 시 자동 migrate 비활성화 |
| `FLYWAY_ENABLED` | `true` (기본값) | 로컬/테스트 | 지금까지와 동일하게 자동 migrate |
| `JPA_DDL_AUTO` | `validate` (기본값) | 전 환경 | 엔티티-스키마 대조 검증 (**DB 변경 없음**) |

`ddl-auto: validate` 는 스키마 메타데이터를 읽어 엔티티와 대조만 하는 읽기 전용 검증이다.
DB 를 실제로 변경하는 것은 Flyway `migrate` 와 `ddl-auto` 의 `create`/`update` 계열뿐이다.

그래서 `validate` 는 끄지 않고 **안전장치로 유지한다.** 마이그레이션을 빠뜨린 채 신버전을 배포하면
부팅이 실패하고, ASG instance refresh 는 health check 실패 시 교체를 중단하므로 구버전이 그대로
살아남는다 — DB 는 아무것도 바뀌지 않은 상태로.

`JPA_DDL_AUTO=none` 은 expand-contract 중간 단계처럼 "스키마가 아직 안 맞아도 일단 떠야 하는"
상황에서만 일시적으로 쓴다. 상시로 꺼두면 부팅은 되는데 런타임 쿼리가 터지는 더 나쁜 실패 모드가 된다.
`create`/`create-drop`/`update` 는 어떤 배포 환경에도 절대 주입하지 않는다.

### SSM 설정 (환경별 1회)

```bash
for ENV in prod alpha; do
  aws ssm put-parameter \
    --name "/umc-product/cygnus-server/${ENV}/FLYWAY_ENABLED" \
    --type SecureString --tier Intelligent-Tiering \
    --value "false" --overwrite
done
```

값 반영은 재배포(인스턴스 교체) 시점부터다. 자세한 SSM 사용법은
[SSM Parameter Store 가이드](./ssm-parameter-store-guide.md) 참고.

## 표준 절차

새 마이그레이션이 포함된 변경을 배포할 때:

1. **PR 머지** — 마이그레이션 SQL 이 default 브랜치에 들어간다.
2. **`DB Migrate [Flyway]` 실행 (`command: info`)** — 미적용(Pending) 목록을 눈으로 확인한다.
3. **`DB Migrate [Flyway]` 실행 (`command: migrate`)** — 스키마를 먼저 올린다.
4. **`CD [ASG]` 실행** — 새 코드를 배포한다. 부팅 시 `validate` 가 통과해야 한다.

> 순서 원칙: **스키마 먼저, 코드 나중.** 컬럼/테이블 추가 같은 additive 변경은 구버전 코드와
> 공존하므로 이 순서가 안전하다. 컬럼 삭제/제약 강화 같은 destructive 변경은
> expand-contract 로 나눠서 "① 추가 마이그레이션 → ② 코드 배포 → ③ 제거 마이그레이션"
> 세 단계로 진행한다. 한 번에 하지 않는다.

## 워크플로우 입력

`Actions → DB Migrate [Flyway] → Run workflow`

| 입력 | 설명 |
|------|------|
| `environment` | `Development`(→ SSM `alpha`) / `Production`(→ SSM `prod`) |
| `command` | `info` 조회 / `validate` 검증 / `migrate` 적용 / `repair` 히스토리 복구 |
| `confirm` | Production 에 `migrate`/`repair` 할 때만 필요. `Production` 을 그대로 입력 |
| `out_of_order` | 이미 적용된 버전보다 낮은 신규 마이그레이션 허용 (기본 false) |
| `baseline_on_migrate` | 히스토리 테이블이 없는 신규 DB 초기화 전용 (기본 false) |

동작 방식:

- 실행 전/후로 `flyway info` 를 찍어 Step Summary 에 접이식 블록으로 남긴다.
- DB 접속 정보는 SSM(`DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD`)에서 읽고
  전부 마스킹한다. 마이그레이션 SQL 은 체크아웃한 `src/main/resources/db/migration` 을 그대로 마운트한다.
- `clean` 은 `FLYWAY_CLEAN_DISABLED=true` 로 아예 봉인되어 있다. 선택지에도 없다.
- `concurrency` 로 환경별 직렬화되어 같은 DB 에 두 실행이 동시에 붙지 않는다.
- Flyway CLI 는 `flyway/flyway:11-alpine` 을 쓴다. 앱이 쓰는 flyway-core(Spring Boot 3.5.x → 11.x)와
  major 를 맞춘 것이므로, Boot 를 올려 flyway major 가 바뀌면 `vars.FLYWAY_IMAGE` 도 함께 올린다.

## 명령별 주의

- **`validate`**: 미적용(Pending) 마이그레이션이 있으면 실패한다. "DB 가 이 커밋 기준으로 최신인가"를
  묻는 용도라서 의도된 동작이다. 단순 현황 조회는 `info` 를 쓴다.
- **`repair`**: `flyway_schema_history` 의 checksum/실패 기록만 재작성한다. **스키마 자체는 고치지 않는다.**
  실패한 마이그레이션이 DDL 을 일부만 적용했다면, repair 전에 DB 실제 상태부터 확인해야 한다.
- **`baseline_on_migrate`**: 기존 스키마가 있는 DB 에서 켜면 baseline 이전 마이그레이션이 통째로
  "적용된 것으로 간주"되어 건너뛰어진다. 신규 DB 초기화 외에는 쓰지 않는다.

## 실패 시

1. Step Summary 의 `flyway info` 출력에서 어느 버전에서 멈췄는지 확인한다.
2. `flyway_schema_history` 의 마지막 행이 `success=false` 면, PostgreSQL 은 DDL 이 트랜잭션 안에서
   롤백되므로 대개 해당 마이그레이션은 통째로 되돌아가 있다. SQL 을 고쳐 새 버전으로 다시 올리는 것이
   원칙이고, 이미 배포된 파일을 수정하는 것은 금지다
   ([migration 규약](../../src/main/resources/db/migration/AGENTS.md)).
3. 실패 행이 남아 재실행이 막히면 `repair` 로 실패 기록을 정리한 뒤 재시도한다.
4. 앱은 아직 구버전이 떠 있는 상태이므로 서비스는 살아 있다. 서두르지 않아도 된다.

## 네트워크 경로

이 워크플로우는 GitHub-hosted 러너에서 RDS 5432 로 직접 접속한다. RDS 보안 그룹이 러너 IP 를
허용하지 않으면 `flyway info` 단계에서 접속 실패로 끝난다. 선택지는 셋이다:

1. RDS 를 접속 가능한 상태로 두고 IP 제한 + 강한 자격증명으로 통제 (현재 개발자 로컬 실행
   `./scripts/run-alpha.sh` 가 의존하는 경로와 동일)
2. self-hosted 러너를 VPC 안에 두고 `runs-on` 을 교체
3. SSM Run Command 로 VPC 내 인스턴스에서 Flyway 컨테이너를 실행하도록 워크플로우를 변경

1번이 안 되는 환경이라면 2번을 먼저 검토한다. 워크플로우 구조는 그대로 두고 `runs-on` 한 줄만 바꾸면 된다.
