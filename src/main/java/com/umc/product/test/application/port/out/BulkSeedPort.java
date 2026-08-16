package com.umc.product.test.application.port.out;

import java.util.List;

import com.umc.product.test.application.port.out.dto.BulkSeedBaseIds;
import com.umc.product.test.application.port.out.dto.SeedChallengerPointRow;
import com.umc.product.test.application.port.out.dto.SeedChallengerRow;
import com.umc.product.test.application.port.out.dto.SeedMemberRow;
import com.umc.product.test.application.port.out.dto.SeedNoticeRow;
import com.umc.product.test.application.port.out.dto.SeedScheduleParticipantRow;
import com.umc.product.test.application.port.out.dto.SeedScheduleRow;

/**
 * 벌크 시딩 전용 out port. 도메인 가드를 거치지 않고 JDBC 배치로 직접 적재한다.
 * 부하 테스트 리그 전용 — 운영 코드가 이 port 를 사용해서는 안 된다.
 */
public interface BulkSeedPort {

    /** 명시적 id 부여의 기준점. 각 테이블의 현재 max(id) 를 반환한다. */
    BulkSeedBaseIds currentMaxIds();

    void insertMembers(List<SeedMemberRow> rows);

    void insertChallengers(List<SeedChallengerRow> rows);

    void insertChallengerPoints(List<SeedChallengerPointRow> rows);

    void insertSchedules(List<SeedScheduleRow> rows);

    void insertScheduleParticipants(List<SeedScheduleParticipantRow> rows);

    /** notice 와 함께 CHALLENGER 탭 GLOBAL(notice_target) 행을 같이 적재한다. */
    void insertNotices(List<SeedNoticeRow> rows, long targetGisuId);

    /** 명시적 id 적재 후 identity 시퀀스 동기화 + 통계 갱신(ANALYZE). 반드시 마지막에 1회 호출한다. */
    void finalizeBulkLoad();
}
