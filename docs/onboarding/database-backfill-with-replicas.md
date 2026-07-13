# Replica 환경의 Database Backfill 운영 가이드

## 목적

이 문서는 운영 중인 PostgreSQL 데이터베이스에서 기존 데이터를 대량으로 보정하는 backfill을 안전하게 수행하기 위한 기준을 정리한다. 특히 physical 또는 logical replica가 있을 때 발생할 수 있는 replication lag, WAL 누적, standby query conflict, failover 문제를 중점적으로 다룬다.

Index 생성은 기존 row를 변경하는 일반적인 data backfill과는 다르다. 하지만 기존 데이터를 전체 스캔하고 I/O, CPU, WAL, lock에 영향을 준다는 점에서 동일한 운영 절차와 관측 기준을 적용한다.

## 핵심 원칙

Backfill의 WAL 생성 속도가 가장 느린 필수 replica의 WAL replay 속도를 지속적으로 넘지 않게 한다.

다음 원칙을 기본값으로 사용한다.

- 전체 row를 하나의 transaction에서 변경하지 않는다.
- Primary key 기반 keyset pagination으로 작은 batch를 처리한다.
- 각 batch는 독립적으로 commit하고 재실행 가능하게 만든다.
- 처리 속도는 primary 처리량이 아니라 replica replay 상태를 기준으로 조절한다.
- 실행 전에 중단 조건과 복구 절차를 정한다.
- Schema 변경은 expand, backfill, contract 순서로 배포한다.
- 모든 replica가 따라잡기 전에는 신규 column이나 상태로 read 경로를 완전히 전환하지 않는다.

## 주요 위험

| 위험 | Primary 영향 | Replica 영향 |
|---|---|---|
| 큰 단일 transaction | Lock 장기 보유, rollback 비용 증가, vacuum 지연 | 변경 가시성이 늦어지고 lag가 한꺼번에 증가할 수 있다. |
| 과도한 batch 속도 | CPU, I/O, WAL 생성량 급증 | WAL 수신, flush, replay가 밀린다. |
| 대량 `UPDATE` | Dead tuple과 autovacuum 부하 발생 | Vacuum WAL과 standby 장기 조회가 충돌할 수 있다. |
| Indexed column 변경 | Row마다 index 갱신이 발생하고 HOT update가 제한된다. | 추가 WAL과 replay I/O가 발생한다. |
| Replication slot | Replica가 소비하지 못한 WAL을 계속 보관한다. | Replica 장애가 primary 디스크 고갈로 전파될 수 있다. |
| Synchronous replica | Commit이 replica 응답을 기다린다. | 느린 replica가 primary transaction latency를 직접 높인다. |
| Logical replica | 변경 row마다 logical decoding과 전송 비용이 발생한다. | Schema 또는 constraint 불일치가 replication 중단으로 이어질 수 있다. |

## 실행 전 확인

### Replication 구조 확인

다음 항목을 먼저 확인한다.

- Replica가 physical replication인지 logical replication인지 확인한다.
- Replica별 `sync_state`가 `sync`, `quorum`, `potential`, `async` 중 무엇인지 확인한다.
- Cascading replica가 있다면 primary에서 직접 보이지 않는 downstream replica까지 확인한다.
- Replication slot 사용 여부와 WAL 보존 한도를 확인한다.
- Read traffic을 처리하는 replica와 failover 전용 replica를 구분한다.
- Logical subscriber를 failover 대상으로 사용하는 경우 sequence 동기화 방법을 확인한다.

### 기준값과 중단 조건 정의

Backfill 시작 전에 평시 기준값을 기록한다.

- Primary API DB latency와 transaction latency
- Primary와 replica의 CPU, disk IOPS, disk queue
- WAL 생성 속도와 replica replay 속도
- Replica별 LSN byte gap과 시간 기준 lag
- `pg_wal` 및 전체 disk 여유 공간
- Table의 `n_dead_tup`, autovacuum 실행 상태
- Standby query cancellation 횟수

환경마다 허용 가능한 값이 다르므로 고정된 임계값을 공통 규칙으로 사용하지 않는다. 서비스의 replica staleness SLO와 disk 용량을 기준으로 다음 중단 조건을 사전에 정한다.

- Replica가 `streaming` 상태에서 이탈한다.
- Replay gap이 정해진 byte 또는 시간 기준을 넘는다.
- Primary 또는 replica DB latency가 허용치를 넘는다.
- Replication slot이 보관한 WAL이 disk 안전 범위를 넘는다.
- Autovacuum이 dead tuple 증가 속도를 따라가지 못한다.
- Replica의 query cancellation이 급증한다.

조건을 넘으면 batch 크기만 줄이는 데 그치지 않고 작업을 일시 중지해 replica가 따라잡도록 한다.

### Backfill 대상과 side effect 확인

다음 항목을 확인한다.

- Update 대상 column에 연결된 index가 있는지 확인한다.
- Trigger, audit log, domain event 또는 외부 연동이 row마다 실행되는지 확인한다.
- Foreign key 검사에 필요한 referenced index가 존재하는지 확인한다.
- ORM callback이나 애플리케이션 이벤트를 거치지 않고 직접 SQL을 실행해도 되는지 확인한다.
- Backfill 중 발생하는 신규 write가 새 column도 함께 채우는지 확인한다.
- 중단 후 재실행했을 때 동일한 결과가 나오는지 확인한다.

## Schema 변경 순서

운영 중 schema와 데이터를 함께 변경할 때는 다음 순서를 사용한다.

1. Nullable column 또는 호환 가능한 신규 schema를 추가한다.
2. 애플리케이션이 신규 write부터 기존 값과 새 값을 함께 기록하도록 배포한다.
3. 기존 row를 batch backfill한다.
4. Primary와 모든 replica의 backfill 및 replication 완료를 확인한다.
5. 애플리케이션 read 경로를 신규 column으로 전환한다.
6. Constraint를 검증한다.
7. 이전 column과 호환 로직은 별도 배포에서 제거한다.

PostgreSQL은 non-volatile constant default를 metadata로 처리할 수 있어 table rewrite 없이 column을 빠르게 추가할 수 있다. 반면 `clock_timestamp()` 같은 volatile default, 일부 generated column, identity column 또는 type 변경은 table과 index 전체 rewrite를 유발할 수 있으므로 실행 계획을 별도로 확인한다.

기존 데이터 검증이 필요한 constraint는 최초 DDL과 validation을 분리한다.

```sql
ALTER TABLE target_table
    ADD CONSTRAINT target_new_column_nn
    CHECK (new_column IS NOT NULL) NOT VALID;

ALTER TABLE target_table
    VALIDATE CONSTRAINT target_new_column_nn;
```

`NOT VALID` 상태에서도 신규 `INSERT`와 `UPDATE`에는 constraint가 적용된다. 이후 `VALIDATE CONSTRAINT`가 기존 row를 스캔한다.

## Batch 실행 방식

### Keyset 기반 처리

`OFFSET` 기반 pagination은 뒤쪽 batch로 갈수록 스캔 비용이 커지고 concurrent write에 취약하므로 사용하지 않는다. Primary key 또는 변경되지 않는 unique key를 기준으로 처리한다.

```sql
WITH target AS (
    SELECT id
    FROM target_table
    WHERE id > :last_id
      AND new_column IS NULL
    ORDER BY id
    LIMIT :batch_size
)
UPDATE target_table AS t
SET new_column = :backfill_value
FROM target
WHERE t.id = target.id
RETURNING t.id;
```

각 batch에서 반환된 가장 큰 `id`를 checkpoint로 저장한다. `new_column IS NULL`처럼 이미 처리된 row를 제외하는 조건을 함께 사용해 동일 batch가 다시 실행돼도 안전하게 만든다.

### Transaction 크기

- 각 batch를 별도 transaction으로 실행한다.
- 처음에는 worker 하나와 작은 batch로 시작한다.
- Batch 처리 시간, WAL 증가량, replay gap을 보고 크기를 점진적으로 조절한다.
- 여러 worker를 병렬 실행하면 WAL 생성과 lock 경쟁도 함께 증가하므로 기본값으로 사용하지 않는다.
- Synchronous replica가 있으면 각 batch commit마다 replica 응답을 기다린다는 점을 반영한다.

필요하면 batch transaction에 제한 시간을 적용한다.

```sql
SET LOCAL lock_timeout = '1s';
SET LOCAL statement_timeout = '30s';
```

시간 값은 예시이며 실제 값은 평시 query latency와 batch 목표 시간에 맞춘다. Timeout이 발생한 batch는 전체 rollback한 뒤 크기를 줄여 재시도한다.

### Indexed column 처리

Update 대상 column이 index에 포함되면 각 row마다 heap과 index가 함께 갱신되고 HOT update를 사용하지 못할 수 있다. 새 column에 대한 신규 index라면 일반적으로 다음 순서를 우선 검토한다.

1. 신규 write가 새 column을 기록하도록 한다.
2. 기존 row를 backfill한다.
3. Index를 생성한다.
4. `ANALYZE`를 실행한다.

다만 backfill query 자체에 해당 index가 반드시 필요하거나 backfill 중 uniqueness를 강제해야 한다면 index를 먼저 생성할 수 있다. 기존 서비스 query가 사용 중인 index는 backfill 속도를 위해 임의로 제거하지 않는다.

## Physical replica 유의사항

Physical replica는 primary의 SQL을 다시 실행하지 않고 WAL을 재생한다. 대량 backfill이 primary에서 생성한 WAL은 replica에서도 유사한 I/O 작업을 발생시킨다.

### Replica lag 관측

Primary에서 다음 query로 직접 연결된 replica 상태를 확인한다.

```sql
SELECT
    application_name,
    state,
    sync_state,
    pg_size_pretty(
        pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn)
    ) AS replay_gap,
    write_lag,
    flush_lag,
    replay_lag
FROM pg_stat_replication;
```

`write_lag`, `flush_lag`, `replay_lag`는 최근 WAL 처리 지연을 보여주지만 replica가 따라잡는 데 걸릴 예상 시간은 아니다. Replica가 완전히 따라잡고 WAL 활동이 없으면 이전 값이 잠시 남아 있거나 `NULL`이 될 수 있다. 시간 값과 LSN byte gap을 함께 관측한다.

### Standby query conflict

Backfill `UPDATE`는 dead tuple을 만든다. Primary의 vacuum cleanup WAL을 replica가 적용할 때 standby의 장기 조회가 과거 row version을 참조하고 있으면 다음 중 하나가 발생한다.

- WAL replay가 대기하면서 replica lag가 증가한다.
- `max_standby_streaming_delay`를 넘으면 standby query가 취소된다.
- `hot_standby_feedback=true`이면 cleanup conflict는 줄지만 primary가 dead tuple을 제거하지 못해 table bloat가 증가할 수 있다.

Replica에서 conflict 발생 현황을 확인한다.

```sql
SELECT *
FROM pg_stat_database_conflicts;
```

HA 전용 replica는 replay 지연을 짧게 유지하는 것이 우선이고, 장기 분석 query가 필요한 replica는 별도로 분리하는 것이 안전하다.

### Synchronous replica

Synchronous replication에서는 commit이 설정된 단계까지 replica 응답을 기다린다.

- `synchronous_commit=remote_write`: Replica OS write까지 기다린다.
- `synchronous_commit=on`: Replica WAL flush까지 기다린다.
- `synchronous_commit=remote_apply`: Replica replay와 query 가시성까지 기다린다.

Replica가 느려지면 backfill뿐 아니라 일반 application transaction의 commit latency와 lock 보유 시간도 증가할 수 있다. Backfill 때문에 durability 정책을 임의로 낮추기보다 batch 크기와 실행 속도를 먼저 조절한다.

### Replication slot과 WAL disk

Replica가 replication slot을 사용하면 아직 소비되지 않은 WAL이 primary에 보관된다.

```sql
SELECT
    slot_name,
    slot_type,
    active,
    wal_status,
    pg_size_pretty(
        pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)
    ) AS retained_wal,
    pg_size_pretty(safe_wal_size) AS safe_wal_size
FROM pg_replication_slots;
```

`safe_wal_size` 등 일부 column은 PostgreSQL 버전과 설정에 따라 사용할 수 없을 수 있다.

`max_slot_wal_keep_size=-1`이면 replication slot이 WAL을 제한 없이 보관할 수 있어 primary의 `pg_wal` disk를 채울 수 있다. 반대로 한도를 너무 작게 잡으면 replica가 따라잡기 전에 필요한 WAL이 제거되어 replica를 다시 구성해야 할 수 있다. Backfill 전에 실제 WAL 생성량과 replica 장애 시 복구 시간을 기준으로 안전 범위를 정한다.

## Logical replica 유의사항

Logical replication에서는 DDL이 자동으로 복제되지 않는다. Subscriber schema가 publisher의 신규 데이터를 수용하지 못하면 replication apply가 중단될 수 있다.

다음 순서를 사용한다.

1. 호환 가능한 additive schema를 subscriber에 먼저 적용한다.
2. Publisher schema와 application write를 변경한다.
3. Backfill을 실행한다.
4. Subscriber apply 상태와 데이터 정합성을 확인한다.
5. 호환성 제거 DDL은 모든 subscriber 전환 후 적용한다.

추가로 다음 항목을 확인한다.

- `UPDATE`와 `DELETE`를 복제하는 table에 primary key 또는 적절한 replica identity가 있어야 한다.
- `REPLICA IDENTITY FULL`은 전체 row를 식별에 사용하므로 대량 backfill에서 비효율적일 수 있다.
- Subscriber의 constraint, 권한, row-level security 위반은 replication을 중단시킬 수 있다.
- Sequence 값은 logical replication으로 자동 동기화되지 않는다.
- Subscriber를 failover 대상으로 사용한다면 승격 전에 sequence 값을 별도로 동기화해야 한다.
- Index DDL도 자동 복제되지 않으므로 subscriber에 별도로 생성해야 한다.

## Failover와 작업 중복 방지

Backfill 도중 failover가 발생할 수 있으므로 작업을 재개 가능하고 중복 실행에 안전하게 만든다.

- Backfill은 read endpoint가 아닌 현재 primary write endpoint에 연결한다.
- 실행 전 현재 연결이 primary인지 확인한다.

```sql
SELECT pg_is_in_recovery();
```

결과가 `false`인 서버에서만 backfill을 실행한다.

- Advisory lock 또는 job lease를 사용해 active worker를 하나로 제한한다.
- Checkpoint를 process memory가 아니라 DB에 영속화한다.
- Connection 종료와 failover 후 lock이 해제되더라도 새 primary에서 checkpoint부터 재개할 수 있게 한다.
- 동일 row를 다시 처리해도 같은 결과가 되도록 update 조건을 설계한다.
- Failover 직후에는 replica 승격과 routing 안정화를 확인한 뒤 작업을 재개한다.

## 완료 조건

Primary의 마지막 row를 처리한 시점만으로 완료 처리하지 않는다. 다음 조건을 모두 확인한다.

1. Primary에서 대상 row가 모두 처리됐다.
2. 누락 조건 query 결과가 0건이다.
3. 모든 필수 replica가 `streaming` 상태다.
4. Replica의 LSN gap이 평시 수준으로 돌아왔다.
5. Replica에서도 신규 값이 조회된다.
6. Logical subscriber에 apply error가 없다.
7. Table의 dead tuple과 autovacuum 상태가 안정적이다.
8. `ANALYZE target_table`로 planner 통계를 갱신했다.
9. Constraint validation이 완료됐다.
10. Application read 경로 전환 후 error rate와 DB latency가 정상이다.

대량 변경 후에는 데이터 분포가 달라질 수 있으므로 `ANALYZE`를 명시적으로 실행한다. 일반 `VACUUM`은 읽기와 쓰기를 병행할 수 있지만 `VACUUM FULL`은 table rewrite와 `ACCESS EXCLUSIVE` lock이 필요하므로 backfill 후 습관적으로 실행하지 않는다.

## Index DDL 선택 기준

일반 index 생성은 table write를 차단할 수 있지만 하나의 transaction으로 빠르게 실행되고 실패 시 rollback된다.

```sql
CREATE INDEX idx_target ON target_table (target_column);
```

`CREATE INDEX CONCURRENTLY`는 index 생성 중 `INSERT`, `UPDATE`, `DELETE`를 허용한다.

```sql
CREATE INDEX CONCURRENTLY idx_target
    ON target_table (target_column);
```

대신 다음 비용이 있다.

- Table을 여러 단계로 스캔해 일반 생성보다 오래 걸릴 수 있다.
- 추가 CPU와 I/O를 사용한다.
- 기존 장기 transaction이 종료되기를 기다릴 수 있다.
- PostgreSQL transaction block 안에서 실행할 수 없다.
- 중간 실패 시 invalid index가 남을 수 있다.
- Flyway에서는 migration별 non-transactional 실행 설정이 필요하다.

다음 조건을 모두 만족할 때 concurrent index DDL을 우선 검토한다.

- Table이 충분히 크다.
- 운영 중 write가 계속 발생한다.
- Index 생성 시간 동안 write 중단을 허용할 수 없다.
- Non-transactional DDL 실패와 재실행 절차가 준비돼 있다.

Table이 비어 있거나 해당 기능이 비활성화돼 write가 발생하지 않는다면 일반 transactional index DDL을 사용한다.

## Event Outbox 적용

현재 Event Outbox는 `app.event-outbox.enabled=true`가 기본값이므로 `OutboxDomainEventPublisher`와 poller가 활성화된다. 회귀 시에는 `EVENT_OUTBOX_ENABLED=false`로 Spring local event publisher에 롤백할 수 있다.

Outbox가 활성화되기 전에는 write가 발생하지 않았으므로 polling index 변경에는 일반 transactional index DDL을 사용한다. 활성 배포는 다음 순서로 진행한다.

1. Partial index migration을 먼저 배포한다.
2. Physical replica의 migration replay 또는 logical subscriber의 별도 index 생성을 확인한다.
3. 모든 replica가 따라잡고 DB 지표가 안정적인지 확인한다.
4. 통합 환경에서 Outbox write와 polling을 검증한다.
5. 기본 활성화 상태로 애플리케이션을 시작한다.
6. Pending 적체, FAILED row, polling lag, replica lag와 WAL 증가량을 함께 관측한다.

빈 Outbox table에 index를 먼저 생성하면 별도 data backfill 없이 가장 안전하게 활성화를 시작할 수 있다.

## 실행 체크리스트

### 시작 전

- [ ] Physical 또는 logical replication 구성을 확인했다.
- [ ] Synchronous replica와 failover 대상 replica를 확인했다.
- [ ] 평시 replica lag, WAL 생성량, DB latency를 기록했다.
- [ ] 중단 임계값과 담당자를 정했다.
- [ ] Batch query가 idempotent하고 checkpoint로 재개 가능하다.
- [ ] 신규 write가 backfill 대상 column을 함께 기록한다.
- [ ] Trigger, event, audit side effect를 확인했다.
- [ ] Replication slot의 WAL 보존량과 disk 여유 공간을 확인했다.

### 실행 중

- [ ] 작은 batch와 worker 하나로 시작했다.
- [ ] LSN byte gap과 시간 기준 lag를 함께 관측한다.
- [ ] Primary API latency와 lock wait를 관측한다.
- [ ] Replica query cancellation을 관측한다.
- [ ] Dead tuple과 autovacuum 상태를 관측한다.
- [ ] 임계값을 넘으면 즉시 일시 중지한다.

### 완료 후

- [ ] Primary 누락 row가 0건이다.
- [ ] 모든 필수 replica가 평시 lag로 복구됐다.
- [ ] Logical subscriber apply error가 없다.
- [ ] Primary와 replica에서 표본 데이터가 일치한다.
- [ ] `ANALYZE`를 실행했다.
- [ ] Constraint validation을 완료했다.
- [ ] Application read 전환 후 error와 latency가 정상이다.

## 참고 자료

- [PostgreSQL: Hot Standby](https://www.postgresql.org/docs/current/hot-standby.html)
- [PostgreSQL: Log-Shipping Standby Servers](https://www.postgresql.org/docs/current/warm-standby.html)
- [PostgreSQL: pg_stat_replication](https://www.postgresql.org/docs/current/monitoring-stats.html)
- [PostgreSQL: Logical Replication Restrictions](https://www.postgresql.org/docs/current/logical-replication-restrictions.html)
- [PostgreSQL: Publication and Replica Identity](https://www.postgresql.org/docs/current/logical-replication-publication.html)
- [PostgreSQL: ALTER TABLE](https://www.postgresql.org/docs/current/sql-altertable.html)
- [PostgreSQL: CREATE INDEX](https://www.postgresql.org/docs/current/sql-createindex.html)
- [PostgreSQL: Populating a Database](https://www.postgresql.org/docs/current/populate.html)
- [PostgreSQL: VACUUM](https://www.postgresql.org/docs/current/sql-vacuum.html)
