-- form_response 동시 update/submit/delete 방어용 낙관적 락 컬럼 추가.
-- JPA @Version 이 flush 시 UPDATE ... WHERE version = ? 로 CAS 를 수행한다.
--
-- 채택 근거: 폼 응답 처리 트랜잭션이 파일 저장소 등 외부 호출을 포함해 pessimistic
-- lock(SELECT ... FOR UPDATE) 은 락 유지 시간이 예측 불가하다. 예상 동시성도 낮아
-- 재시도 비용보다 낙관적 락의 단순성이 이득.
--
-- 락 영향: 상수 DEFAULT 를 지정한 ADD COLUMN 은 PostgreSQL 11+ 에서 metadata-only
-- 변경이라 테이블 재작성이 없고 짧은 exclusive lock 만 잡는다. CONCURRENTLY 는
-- ALTER TABLE ADD COLUMN 에 적용 불가하므로 별도 스텝으로 나누지 않는다.
--
-- 롤백 주의: 이 컬럼을 다시 DROP 하면 낙관적 락이 사라져, 롤백 이후 동일 행에
-- 진행 중인 동시 write 가 발생하면 조용히 lost update 로 이어질 수 있다. 롤백은
-- 배포 직후 트래픽이 없는 상황에서만 수행.
ALTER TABLE form_response
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
