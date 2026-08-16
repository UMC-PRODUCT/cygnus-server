# Authorization 운영 안전 가이드

## 문서 목적

이 문서는 권한 저장소와 `AuthoritySnapshot`을 변경할 때 검토해야 하는 운영 안전장치를 정리한다. 특히 `SUPER_ADMIN`을 `challenger_role`에서 `member_system_role`로 이전하면서 검토했던 rolling deployment 대응과, 현재 운영 환경에 맞춰 선택한 단일 hard cutover의 경계를 기록한다.

현재 구현과 배포 방식이 아래 전제를 벗어나면 이 문서를 기준으로 migration과 cache 전략을 다시 설계해야 한다.

## 현재 운영 전제와 선택

| 항목 | 현재 전제 |
|---|---|
| 애플리케이션 writer | 동시에 실행되는 서버 1대 |
| 데이터베이스 writer | PostgreSQL 인스턴스 1개 |
| 배포 방식 | 구버전 서버와 nginx 중지 -> nginx 없이 신버전 app 시작 및 startup Flyway 실행 -> 검증 후 nginx 시작 |
| 구버전·신버전 중첩 | 없음 |
| legacy 데이터 규모 | 중단 시간 안에 단일 backfill과 constraint 변경이 가능한 수준 |

이 전제에서는 구버전 writer가 migration 도중 `challenger_role.SUPER_ADMIN`을 다시 쓰지 않는다. 따라서 임시 dual-write 객체를 운영하지 않고 [단일 Flyway migration](../../../src/main/resources/db/migration/V2026.07.05.18.50__move_super_admin_to_member_system_role.sql)에서 테이블 생성, backfill, legacy 정리, constraint 강화를 순서대로 수행한다.

현재 `spring.flyway.enabled`는 `true`이므로 별도 migration runner가 없다면 Flyway는 신버전 애플리케이션 startup 중 실행된다. 이때 "migration 후 신버전 시작"은 논리적 순서이며, 실제 운영에서는 migration과 검증이 끝날 때까지 신버전 인스턴스에 traffic을 전달하지 않아야 한다. 현재 CD workflow의 `docker compose up`은 app과 nginx를 함께 시작하고 health gate 없이 종료되므로, 이 migration을 배포할 때는 그대로 실행하지 않는다.

이 선택은 영구적인 아키텍처 제약이 아니라 현재 topology에 대한 운영 결정이다. 서버 수, 배포 방식, writer 종류 또는 rollback 요구사항이 달라지면 아래 안전장치를 다시 검토한다.

### Flyway 파일의 생명주기

이 문서의 단일 migration 단순화는 해당 version이 운영 DB에 적용되기 전에만 가능하다. 한 환경이라도 `flyway_schema_history`에 성공 기록이 생긴 뒤에는 기존 파일을 수정하지 않는다. checksum 불일치와 환경별 schema 차이를 피하기 위해 후속 변경은 반드시 새로운 version의 migration으로 추가한다.

## 현재 유지하는 안전장치

### 데이터 이전과 무결성

- `SELECT DISTINCT`로 여러 기수에 중복된 `SUPER_ADMIN`을 회원별 1건으로 이관하고, `(member_id, role_type)` unique constraint로 DB에서도 중복을 차단한다.
- `ON CONFLICT DO NOTHING`은 backfill statement의 중복 충돌을 막는 보조 장치다. `CREATE TABLE`을 포함한 migration 파일 전체를 재실행 가능하게 만드는 장치는 아니다.
- `challenger`, `member`와 inner join하여 실제 회원에 연결된 역할만 이관한다.
- `member_system_role.member_id`는 `member.id`를 참조하고 `ON DELETE CASCADE`를 사용한다.
- legacy 행을 삭제한 뒤 `challenger_role`과 `challenger_record`의 check constraint를 다시 생성하여 `SUPER_ADMIN` 재삽입을 DB 수준에서 차단한다.
- 일반 ChallengerRole과 ChallengerRecord는 migration 대상에서 제외하여 그대로 보존한다.

### 권한 판정

- `SUPER_ADMIN`은 member 도메인의 system role로만 저장하고, authorization 도메인에서는 기수와 무관한 global override로 평가한다.
- ChallengerRole 기반 권한은 리소스 기수에 맞춰 평가하고, 기존에 의도적으로 기수 무관이었던 정책은 별도 정책 변경 없이 강화하지 않는다.
- 존재하지 않는 회원은 legacy 역할 데이터가 남아 있어도 권한을 부여하지 않는 fail-closed 정책을 사용한다.
- 외부 API의 path, request/response shape, status semantics는 별도 계약 변경이 없는 한 유지한다.
- 의도적 계약 예외로 `SUPER_ADMIN`은 ChallengerRole과 ChallengerRecord의 저장·조회 계약에서 제거한다. 두 command API의 JSON enum 입력에 `SUPER_ADMIN`을 전달하면 request body parsing 단계에서 `400 Bad Request`로 거부한다. 이는 일반 API 하위호환 원칙이 legacy `SUPER_ADMIN` 입력을 보존한다는 뜻이 아니다.

### AuthoritySnapshot cache

- domain entity가 아니라 `AuthoritySnapshotCacheDto`를 JSON으로 직렬화하여 저장한다.
- cache DTO의 `schemaVersion`이 없거나 현재 버전과 다르면 payload를 사용하지 않고 evict 후 DB에서 다시 구성한다.
- cache key의 회원 ID와 payload의 회원 ID가 다르면 동일하게 evict 후 다시 구성한다.
- cache hit에서도 회원 존재 여부를 다시 확인하여 삭제된 회원의 snapshot을 반환하지 않는다.
- 현재 구현된 Challenger, ChallengerRole, ChallengerRecord, 학교·지부 정보 변경과 회원 삭제 경로는 transaction commit 이후 `EvictAuthoritySnapshotCacheUseCase`로 관련 회원 cache를 제거한다.
- [현재 Caffeine cache](../../../src/main/java/com/umc/product/authorization/application/service/AuthorizationService.java)는 인스턴스 로컬이며 TTL은 30초다. 단일 서버에서는 명시적 eviction이 같은 cache 인스턴스에 도달하고, TTL은 stale entry의 수명을 제한하는 보조 장치다.
- 단일 서버에서도 cache miss 요청이 이전 권한을 조회한 뒤 역할 변경 transaction의 eviction보다 늦게 cache에 저장하면 구 snapshot이 다시 들어갈 수 있다. 현재 구조는 이런 경쟁에서 최대 TTL 30초의 stale 권한을 허용한다.
- 실행 중인 서버에서 role 테이블을 직접 SQL로 변경하면 application eviction을 우회한다. 즉시 반영이 필요하면 서버를 재시작하여 local cache를 비우거나 별도 eviction 경로를 실행해야 한다.

새로운 system role 부여·회수 Command UseCase를 추가할 때도 저장 transaction commit 이후 해당 회원의 `AUTHORITY_SNAPSHOT`을 반드시 evict해야 한다.

권한 회수의 즉시성이 필수라면 서버가 1대여도 TTL과 local eviction만 사용하지 않고 회원별 authority version 검증 또는 동등한 stale-write 차단을 도입한다.

## 단순화하면서 제거한 안전장치

| 안전장치 | 보호하려던 상황 | 현재 제거한 이유 |
|---|---|---|
| expand와 contract를 분리한 2개 migration | 구버전과 신버전 schema가 일정 기간 공존 | 배포 중 두 애플리케이션 버전이 동시에 실행되지 않음 |
| 2개 trigger와 3개 function을 이용한 DB dual-write | 구버전이 legacy 역할을 생성·수정·삭제해도 신규 저장소와 동기화 | migration 전에 유일한 구버전 writer를 중지함 |
| contract 직전 최종 catch-up backfill | expand 이후 발생한 legacy write 유실 방지 | expand와 contract 사이에 write 가능한 시간이 없음 |
| 장기 online backfill, batch, checkpoint | 대량 데이터의 lock 시간과 migration timeout 완화 | 현재 데이터 규모가 작고 중단 배포를 수용함 |
| 분산 cache invalidation 또는 authority version | 여러 서버의 로컬 cache를 즉시 무효화 | 애플리케이션 cache 인스턴스가 1개임 |

제거했다는 것은 필요성이 영구히 사라졌다는 뜻이 아니다. 아래 조건 중 하나라도 충족하면 hard cutover 전제를 폐기하고 안전장치를 재설계한다.

## 안전장치를 다시 도입해야 하는 조건

### 여러 애플리케이션 버전이 동시에 실행될 때

rolling, blue-green, canary deployment 또는 무중단 배포를 도입하면 expand -> 동기화 -> contract 순서가 필요하다.

1. 신규 저장소를 먼저 추가하고 구버전 read/write를 유지한다.
2. 기존 데이터를 idempotent하게 backfill한다.
3. 모든 writer가 신규 저장소를 함께 갱신하도록 한다.
4. 구버전 인스턴스와 legacy writer가 0개인지 확인한다.
5. 마지막 catch-up backfill 후 read path를 신규 저장소로 전환한다.
6. rollback window가 끝난 뒤 legacy 데이터와 임시 동기화 객체를 제거한다.

가능하면 application dual-write를 우선 검토한다. 이미 배포된 구버전이 신규 저장소를 알 수 없어 공존 기간을 보호할 수 없다면 임시 DB trigger를 사용할 수 있다. DB trigger를 선택하면 생성·수정·삭제, 부모 Challenger 삭제, 중복 기수 역할을 모두 처리하고 contract migration에서 trigger와 function을 제거하는 절차까지 한 작업으로 관리한다.

### writer가 추가될 때

서버가 1대여도 scheduler, batch, 운영 SQL, 별도 admin 서비스가 legacy 테이블을 쓰면 writer는 1개가 아니다. migration 전 다음 항목을 확인한다.

- `challenger_role` 또는 `challenger_record`를 직접 변경하는 모든 애플리케이션과 스크립트
- 배포 중 계속 실행되는 scheduler와 비동기 consumer
- 외부 도구가 DB에 직접 쓰는 운영 절차
- 장애 복구 시 구버전 바이너리가 자동으로 재기동되는 설정

### cache 인스턴스가 여러 개가 될 때

애플리케이션 서버를 2대 이상 운영하면 현재 local eviction은 다른 인스턴스에 전달되지 않는다. 권한 회수는 보안 민감 작업이므로 30초 TTL만으로 즉시성을 주장해서는 안 된다. 다음 중 하나를 도입한다.

- Redis 같은 공유 cache와 중앙 eviction
- 권한 변경 event를 모든 인스턴스가 소비하는 invalidation 방식
- 회원별 authority version을 DB 또는 token과 비교하여 stale snapshot을 거부하는 방식

도입 후에는 한 인스턴스에서 역할을 회수하고 다른 인스턴스에서 즉시 접근이 거부되는지 통합 테스트한다.

서버가 1대여도 권한 회수 직후의 stale 권한을 허용할 수 없다면 같은 대안을 검토한다.

### 데이터와 DB topology가 커질 때

- backfill 또는 constraint 변경이 허용된 중단 시간을 넘으면 batch backfill과 checkpoint를 분리한다.
- writable DB가 여러 개이거나 shard·multi-region writer를 도입하면 모든 writer의 전환 순서와 replication lag를 고려한다.
- read replica만 추가된 경우 dual-write 요건은 아니지만, 권한 read가 replica를 사용한다면 lag 동안 이전 권한이 보일 수 있는지 확인한다.

### 애플리케이션 단독 rollback이 필요할 때

현재 migration 이후 구버전 애플리케이션은 `member_system_role`을 읽지 못하고, DB constraint 때문에 `challenger_role.SUPER_ADMIN`을 다시 쓸 수도 없다. 따라서 애플리케이션만 구버전으로 되돌리는 rollback은 지원하지 않는다.

구버전 단독 rollback이 운영 요구사항이 되면 legacy read/write를 유지하는 expand-contract 방식으로 전환하고, rollback window가 끝날 때까지 contract migration을 실행하지 않는다.

## 단일 hard cutover 배포 체크리스트

### 배포 전

- [ ] 실제 실행 중인 애플리케이션 writer가 1대인지 확인한다.
- [ ] scheduler, batch, 운영 스크립트를 포함해 legacy 역할을 쓰는 별도 writer가 없는지 확인한다.
- [ ] 구버전 프로세스가 중지된 뒤 자동 재기동되지 않는지 확인한다.
- [ ] `flyway_schema_history`에 `2026.07.05.18.50` 성공 기록이 없는지 확인한다. 이미 적용됐다면 파일을 수정하거나 재실행하지 않는다.
- [ ] DB backup 또는 복구 시점을 확보하고 복원 절차를 확인한다.
- [ ] 실제 DB와 격리 복구 환경에 `postgis` extension binary가 있는지 확인한다. 현재 compose의 `postgres:18.2` image만으로는 신규 DB에서 초기 `CREATE EXTENSION postgis` migration을 실행할 수 없다.
- [ ] cutover 시각을 기록하고, rollback 시 그 이후의 쓰기를 식별할 방법을 준비한다.
- [ ] legacy `challenger_record.SUPER_ADMIN` 이력이 삭제된다는 점과 backup 없이는 복구할 수 없다는 점을 승인한다.
- [ ] 이관 전 `SUPER_ADMIN` 회원 ID 집합과 수를 기록한다.
- [ ] 보존해야 하는 일반 ChallengerRole과 ChallengerRecord 수를 기록한다.
- [ ] 허용 가능한 중단 시간 안에 migration이 끝나는지 staging 또는 동일 규모 데이터로 확인한다.
- [ ] startup Flyway를 사용하면 migration과 DB 검증이 끝날 때까지 신버전 traffic을 차단할 수 있는지 확인한다.

이관 대상 회원 수는 다음 query로 확인할 수 있다.

```sql
SELECT COUNT(DISTINCT c.member_id)
FROM challenger_role cr
JOIN challenger c ON c.id = cr.challenger_id
JOIN member m ON m.id = c.member_id
WHERE cr.role_type = 'SUPER_ADMIN';

SELECT DISTINCT c.member_id
FROM challenger_role cr
JOIN challenger c ON c.id = cr.challenger_id
JOIN member m ON m.id = c.member_id
WHERE cr.role_type = 'SUPER_ADMIN'
ORDER BY c.member_id;

-- migration 전후에 보존되어야 할 일반 역할의 기준값이다.
SELECT COUNT(*)
FROM challenger_role
WHERE role_type IS DISTINCT FROM 'SUPER_ADMIN';

SELECT COUNT(*)
FROM challenger_record
WHERE challenger_role_type IS DISTINCT FROM 'SUPER_ADMIN';

SELECT extname, extversion
FROM pg_extension
WHERE extname = 'postgis';

SELECT postgis_full_version();

SELECT version, success
FROM flyway_schema_history
WHERE version = '2026.07.05.18.50';
```

### 배포 중

현재 `CD [Home Server]` workflow는 `workflow_dispatch`로 수동 실행되지만, 실행하면 app과 nginx를 함께 시작한다. 이 migration 배포에는 해당 workflow를 사용하지 않고 release 담당자 한 명이 아래 수동 절차를 끝까지 책임진다. 현재 compose에서 외부 port를 publish하는 service는 nginx뿐이므로 nginx를 시작하지 않은 상태가 traffic gate다.

1. 같은 환경의 CD workflow가 병렬 실행되지 않는지 확인하고 구버전의 자동 재기동을 중지한다.
2. 서버에서 구버전 app과 nginx를 포함한 compose stack을 내린다.
3. nginx를 제외하고 postgres, valkey, 신버전 app만 시작한다. app startup 과정에서 Flyway가 실행된다.
4. app container의 health가 통과하고 `flyway_schema_history`와 아래 검증 query가 모두 정상인지 확인한다.
5. 보호된 권한 API를 내부 네트워크에서 smoke test한다.
6. 마지막으로 nginx를 시작해 외부 traffic을 허용한다. 이후 배포부터 기존 CD workflow 사용을 재개한다.

현재 배포 서버에서는 다음 순서로 traffic gate를 적용한다. `SERVER_APP_DIRECTORY`와 image tag를 포함한 compose 환경 파일은 자동 CD가 생성하는 값과 동일해야 한다.

```bash
set -Eeuo pipefail

: "${SERVER_APP_DIRECTORY:?SERVER_APP_DIRECTORY is required}"
cd "$SERVER_APP_DIRECTORY"

docker compose pull app
docker compose down
docker compose up -d --scale app=1 postgres valkey app

HEALTH_DEADLINE=$((SECONDS + 180))
until docker compose exec -T app \
  curl --fail --silent http://127.0.0.1:9090/actuator/health >/dev/null; do
  if ((SECONDS >= HEALTH_DEADLINE)); then
    docker compose logs --tail=100 app
    exit 1
  fi
  sleep 5
done

docker compose exec -T postgres sh -ceu \
  'migration_count=$(psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc \
    "SELECT COUNT(*) FROM flyway_schema_history \
     WHERE version = \$\$2026.07.05.18.50\$\$ AND success IS TRUE;"); \
   if [ "$migration_count" != "1" ]; then \
     echo "expected one successful authorization migration, got: $migration_count" >&2; \
     exit 1; \
   fi'
```

예시의 health timeout은 180초다. 운영 기준에 맞게 조정하되 timeout, app 종료, Flyway 실패 또는 성공 history가 정확히 1건이 아니면 명령은 non-zero로 종료되어야 한다. 이때 nginx를 시작하지 않고 app log와 `flyway_schema_history`를 확인한다.

이어서 아래 전체 SQL 결과를 배포 전 기록과 비교하고 내부 네트워크에서 권한 smoke를 수행한다. 최소한 기존 `SUPER_ADMIN`의 global override, 일반 운영진의 기수별 권한, 두 legacy `SUPER_ADMIN` command 입력의 `400 Bad Request`를 확인한다. 결과를 배포 기록에 남기고 release 담당자가 모두 승인한 뒤에만 별도 shell에서 ingress를 연다.

```bash
set -Eeuo pipefail

: "${AUTHORIZATION_CUTOVER_APPROVED:?set to yes after DB comparison and authority smoke}"
if [[ "$AUTHORIZATION_CUTOVER_APPROVED" != "yes" ]]; then
  echo "authorization cutover is not approved" >&2
  exit 1
fi

docker compose up -d nginx
```

nginx 시작 후에는 외부 ingress health와 표준 API smoke를 다시 확인한다. `AUTHORIZATION_CUTOVER_APPROVED`는 자동으로 설정하는 값이 아니라 release 담당자가 DB 비교 결과와 smoke evidence를 확인한 뒤 현재 shell에만 설정하는 수동 gate다.

현재 레포에는 migration 전용 `flywayMigrate` Gradle task가 없다. compose를 사용하지 않는 로컬 또는 별도 runner 환경에서는 표준 배포 환경 변수를 모두 주입한 뒤 다음 명령으로 신버전을 시작하며, 이 startup 과정에서 Flyway가 실행된다. DB 변수만으로는 애플리케이션이 시작되지 않으며 JWT를 포함한 profile별 필수 설정도 필요하다.

```bash
set -Eeuo pipefail

: "${APP_ENV_FILE:?APP_ENV_FILE is required}"
set -a
. "$APP_ENV_FILE"
set +a

: "${SPRING_PROFILES_ACTIVE:?SPRING_PROFILES_ACTIVE is required}"
: "${DATABASE_URL:?DATABASE_URL is required}"
: "${DATABASE_USERNAME:?DATABASE_USERNAME is required}"
: "${DATABASE_PASSWORD:?DATABASE_PASSWORD is required}"
: "${JWT_ACCESS_TOKEN_SECRET:?JWT_ACCESS_TOKEN_SECRET is required}"
: "${JWT_REFRESH_TOKEN_SECRET:?JWT_REFRESH_TOKEN_SECRET is required}"
: "${JWT_OAUTH_VERIFICATION_TOKEN_SECRET:?JWT_OAUTH_VERIFICATION_TOKEN_SECRET is required}"
: "${JWT_EMAIL_VERIFICATION_TOKEN_SECRET:?JWT_EMAIL_VERIFICATION_TOKEN_SECRET is required}"
: "${JWT_SSO_LOGIN_TOKEN_SECRET:?JWT_SSO_LOGIN_TOKEN_SECRET is required}"

./gradlew bootRun
```

`APP_ENV_FILE`은 배포 secret store가 생성한 shell-compatible env file을 가리킨다. [`.env.example`](../../../.env.example)은 필요한 항목을 확인하는 기준일 뿐이며 비밀값이 비어 있으므로 그대로 실행에 사용하지 않는다. 운영 container는 같은 환경 변수를 주입한 상태에서 image의 `java $JAVA_OPTS -jar application.jar` entrypoint를 실행한다. startup 명령은 계속 실행되므로 별도 shell에서 health와 DB를 검증한다.

```bash
set -Eeuo pipefail

curl --fail --silent --show-error http://127.0.0.1:9090/actuator/health

: "${PGSERVICE:?PGSERVICE is required}"
: "${PGSERVICEFILE:?PGSERVICEFILE is required}"
: "${PGPASSFILE:?PGPASSFILE is required}"
MIGRATION_COUNT=$(psql -v ON_ERROR_STOP=1 -Atc \
  'SELECT COUNT(*) FROM flyway_schema_history
   WHERE version = $$2026.07.05.18.50$$ AND success IS TRUE;')
if [[ "$MIGRATION_COUNT" != "1" ]]; then
  echo "expected one successful authorization migration, got: $MIGRATION_COUNT" >&2
  exit 1
fi
```

`DATABASE_URL`은 애플리케이션용 JDBC URL이다. 별도 shell의 `psql`은 secret store가 만든 `PGSERVICEFILE`과 권한이 `0600`인 `PGPASSFILE`을 사용하여 password가 command argument나 shell history에 노출되지 않게 한다. credential 값을 문서 또는 CI log에 직접 기록하지 않는다.

```sql
-- legacy 저장소에는 SUPER_ADMIN이 남아 있지 않아야 한다.
SELECT COUNT(*) FROM challenger_role WHERE role_type = 'SUPER_ADMIN';
SELECT COUNT(*) FROM challenger_record WHERE challenger_role_type = 'SUPER_ADMIN';

-- 이관 전 기록한 회원 수와 회원 ID 집합이 모두 같아야 한다.
SELECT COUNT(DISTINCT member_id)
FROM member_system_role
WHERE role_type = 'SUPER_ADMIN';

SELECT member_id
FROM member_system_role
WHERE role_type = 'SUPER_ADMIN'
ORDER BY member_id;

-- 일반 역할 데이터가 배포 전 확인한 수와 일치해야 한다.
SELECT COUNT(*) FROM challenger_role WHERE role_type IS DISTINCT FROM 'SUPER_ADMIN';
SELECT COUNT(*) FROM challenger_record WHERE challenger_role_type IS DISTINCT FROM 'SUPER_ADMIN';

-- 해당 Flyway version은 성공 기록 1건이어야 한다.
SELECT version, success
FROM flyway_schema_history
WHERE version = '2026.07.05.18.50';
```

### 배포 후

- [ ] 기존 `SUPER_ADMIN` 회원이 Challenger 이력 없이도 보호된 리소스에 접근할 수 있는지 확인한다.
- [ ] 일반 운영진의 기수별 권한 결과가 배포 전과 같은지 확인한다.
- [ ] ChallengerRole과 ChallengerRecord command API에서 `SUPER_ADMIN` 입력이 `400 Bad Request`로 거부되는지 확인한다.
- [ ] 권한 cache의 schema mismatch 또는 역직렬화 실패 log가 반복되지 않는지 확인한다.
- [ ] 신규 권한 판정의 access denied 지표가 비정상적으로 증가하지 않는지 확인한다.

## 실패와 rollback

- migration이 실패하면 traffic을 허용하지 말고 `flyway_schema_history`와 실제 schema 상태를 함께 확인한다.
- migration 성공 후 traffic을 허용하기 전에 문제가 발견되면 모든 writer를 중지한 상태로 현재 DB도 보존하고, backup 복원과 검증을 끝낸 뒤 구버전을 시작한다.
- traffic 허용 후 문제가 발견되면 전체 DB 복원보다 forward fix를 우선한다. DB 복원이 필요하면 먼저 모든 writer를 동결하고 현재 DB snapshot을 보존한다.
- cutover 이후 발생한 다른 도메인의 쓰기를 식별하여 복원 DB에 재적용하거나, 유실을 명시적으로 승인해야 한다. 가능하면 격리된 임시 DB에 backup을 먼저 복원하여 schema와 데이터를 검증한 뒤 전환한다.
- 격리 복구 DB는 source DB와 같은 PostgreSQL major version과 extension binary를 제공해야 한다. 현재 compose의 `postgres:18.2` image에는 PostGIS가 없으므로 fresh restore 검증에는 그대로 사용하지 않는다. PostgreSQL 18과 호환되는 PostGIS image를 pin하거나 PostGIS package가 설치된 별도 복구 image를 사용하고 `SELECT postgis_full_version()`과 전체 Flyway history를 확인한다.
- 구버전으로 rollback할 때 애플리케이션만 되돌리지 않는다. legacy `challenger_record.SUPER_ADMIN`은 migration에서 삭제되므로 backup 없이 완전한 이력 rollback을 보장할 수 없다.

## 향후 변경 체크리스트

### AuthoritySnapshot 구조 변경

- [ ] `AuthoritySnapshotCacheDto.CURRENT_SCHEMA_VERSION`을 증가시킨다.
- [ ] domain과 DTO의 양방향 mapping을 함께 수정한다.
- [ ] 이전·누락·지원하지 않는 version의 payload가 evict 후 재구성되는지 테스트한다.
- [ ] cache key의 회원 ID와 payload 회원 ID 불일치가 fail-closed인지 유지한다.
- [ ] 여러 서버라면 배포 중 구버전과 신버전 cache payload가 섞이는 상황을 검증한다.

### 권한 입력 변경

- [ ] 신규 system role의 소유 도메인과 전역·기수 범위를 먼저 결정한다.
- [ ] 역할 부여·회수 transaction commit 이후 authority snapshot을 evict한다.
- [ ] 직접 SQL 변경을 금지하거나 application cache를 함께 비우는 운영 절차를 둔다.
- [ ] `AuthoritySnapshot`, evaluator, cache DTO, migration constraint를 함께 갱신한다.
- [ ] 기존 API와 권한 결과의 하위호환 테스트를 먼저 고정한다.
- [ ] 이미 적용된 Flyway 파일을 고치지 않고 새로운 version의 migration을 추가한다.

### 필수 검증

- disposable PostgreSQL 또는 staging clone을 가리키도록 `PGSERVICE`, `PGSERVICEFILE`, `PGPASSFILE`을 설정하고 `psql -v ON_ERROR_STOP=1 -f src/main/resources/db/migration/V2026.07.05.18.50__move_super_admin_to_member_system_role.sql`로 migration smoke를 실행한다. PR에는 중복 이관, 회원 ID 집합 일치, 일반 데이터 보존, legacy 재삽입 거부 결과를 남긴다. 이 명령은 Flyway history를 기록하지 않으므로 운영 migration 적용에는 사용하지 않는다.
- [AuthoritySnapshot cache 테스트](../../../src/test/java/com/umc/product/authorization/application/service/AuthorizationServiceCacheTest.java): cache hit, 회원 삭제, schema version, 회원 ID 불일치
- [Cache serializer 테스트](../../../src/test/java/com/umc/product/authorization/application/service/AuthoritySnapshotCacheSerializerTest.java): 직렬화 round trip과 지원하지 않는 version 거부
- [AuthoritySnapshot 도메인 테스트](../../../src/test/java/com/umc/product/authorization/domain/AuthoritySnapshotTest.java): `SUPER_ADMIN` global override와 기수별 역할 정책
- 권한 API 회귀 테스트: endpoint, response shape, status semantics와 대표 리소스 권한 결과
- ChallengerRole과 ChallengerRecord controller 테스트: JSON의 `SUPER_ADMIN` enum 입력이 모두 `400 Bad Request`인지 확인
