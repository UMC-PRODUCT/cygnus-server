package com.umc.product.organization.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.organization.domain.StudyGroupSchedule;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({
    StudyGroupSchedulePersistenceAdapter.class
})
class StudyGroupSchedulePersistenceAdapterTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    StudyGroupSchedulePersistenceAdapter sut;

    @Test
    void 회장단_시야의_스터디_그룹들에_등록된_일정ID들을_batch로_가져온다() {
        // given — Schedule 팀이 호출하는 시나리오.
        //   회장단 사용자에게 보이는 그룹 2개 (시야 안):
        //     - schoolMemberGroup: 교내 멤버가 *멤버* 로 등록된 그룹 → 일정 100, 101 등록
        //     - schoolMentorGroup: 교내 멤버가 *멘토* 로 등록된 그룹 → 일정 200 등록 (V1 확장 케이스)
        //   회장단 시야 밖 그룹 1개:
        //     - outOfScopeGroup: 교내 인원 없음, 일정 999 등록되어 있어도 결과에 포함되면 안 됨
        Long schoolMemberGroup = 1L;
        Long schoolMentorGroup = 2L;
        Long outOfScopeGroup = 99L;

        persistLegacyMapping(schoolMemberGroup, 100L);
        persistLegacyMapping(schoolMemberGroup, 101L);
        persistLegacyMapping(schoolMentorGroup, 200L);
        persistLegacyMapping(outOfScopeGroup, 999L);
        em.flush();
        em.clear();

        // when — Schedule 팀이 "회장단 시야의 visibleGroupIds" 를 넘김
        Set<Long> visibleScheduleIds = sut.findScheduleIdsByStudyGroupIds(
            List.of(schoolMemberGroup, schoolMentorGroup)
        );

        // then — 시야 안 그룹들의 일정만 반환. 시야 밖은 제외.
        assertThat(visibleScheduleIds)
            .containsExactlyInAnyOrder(100L, 101L, 200L)
            .doesNotContain(999L);
    }

    @Test
    void 파트장_시야_그룹의_일정ID들을_가져온다() {
        // given — 파트장은 본인이 멘토인 그룹만 보임.
        //   대게 그룹 수가 작으므로 한 그룹의 여러 일정 케이스가 흔함.
        Long myMentorGroup = 1L;

        persistLegacyMapping(myMentorGroup, 100L);
        persistLegacyMapping(myMentorGroup, 101L);
        persistLegacyMapping(myMentorGroup, 102L);
        em.flush();
        em.clear();

        // when
        Set<Long> visibleScheduleIds = sut.findScheduleIdsByStudyGroupIds(List.of(myMentorGroup));

        // then
        assertThat(visibleScheduleIds).containsExactlyInAnyOrder(100L, 101L, 102L);
    }

    @Test
    void 스터디_그룹은_있지만_등록된_일정이_없으면_빈_결과() {
        // given — 신규 생성된 그룹처럼 아직 schedule 매핑이 없는 경우.
        //   호출 자체는 정상이고 그저 결과가 비어있어야 함 (예외 던지면 안 됨).
        Long groupWithNoSchedule = 99999L;

        // when
        Set<Long> result = sut.findScheduleIdsByStudyGroupIds(List.of(groupWithNoSchedule));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 권한_없는_사용자는_visible_그룹이_없어_빈_입력_DB_호출_없이_즉시_종료() {
        // given — Schedule 팀 흐름:
        //   resolveOrganizationRoleScopes(memberId) → 회장단/파트장 모두 아님 → 빈 scopes
        //   getStudyGroupUseCase.findStudyGroupIds(빈_scopes, gisuId) → 빈 Set
        //   ↓ 이게 우리한테 그대로 넘어옴
        //   findScheduleIdsByStudyGroupIds(emptyCollection) → 빈 Set 즉시 반환 (풀스캔 위험 차단)

        // when
        Set<Long> result = sut.findScheduleIdsByStudyGroupIds(List.of());

        // then
        assertThat(result).isEmpty();
    }

    // ========== Helper Methods ==========

    private void persistLegacyMapping(Long studyGroupId, Long scheduleId) {
        StudyGroupSchedule entity = StudyGroupSchedule.builder()
            .studyGroupId(studyGroupId)
            .scheduleId(scheduleId)
            .build();
        em.persist(entity);
    }

}
