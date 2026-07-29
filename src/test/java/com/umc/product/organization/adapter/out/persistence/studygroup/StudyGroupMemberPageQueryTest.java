package com.umc.product.organization.adapter.out.persistence.studygroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberPageInfo;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.StudyGroup;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(StudyGroupQueryRepository.class)
@DisplayName("스터디원 커서 페이지네이션 조회")
class StudyGroupMemberPageQueryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    StudyGroupQueryRepository sut;

    private Long gisuId;

    @BeforeEach
    void persistGisu() {
        // study_group.gisu_id 에 FK 가 걸려 있어 기수를 먼저 만들어야 그룹을 넣을 수 있다.
        // uq_gisu_active 로 활성 기수는 전역에 하나뿐이라 비활성으로 만든다. 조회 쿼리는 활성 여부를 보지 않는다.
        Gisu gisu = Gisu.create(
            99L, Instant.parse("2026-03-01T00:00:00Z"), Instant.parse("2026-08-31T00:00:00Z"), false
        );
        em.persist(gisu);
        em.flush();
        gisuId = gisu.getId();
    }

    @Test
    @DisplayName("커서로 이어 조회하면 중복·누락 없이 전체 스터디원을 훑는다")
    void cursorPagingCoversEveryMemberExactlyOnce() {
        StudyGroup group = persistGroup("SpringBoot 스터디", ChallengerPart.SPRINGBOOT, 1L, 2L, 3L, 4L, 5L);
        Set<Long> groupIds = Set.of(group.getId());

        List<Long> collected = new ArrayList<>();
        Long cursor = null;
        // size 2 로 끝까지 넘긴다. 무한 루프 방지를 위해 페이지 수 상한을 둔다.
        for (int page = 0; page < 10; page++) {
            List<StudyGroupMemberPageInfo> rows = sut.findStudyGroupMemberPage(groupIds, cursor, 2);
            if (rows.isEmpty()) {
                break;
            }
            rows.forEach(row -> collected.add(row.memberId()));
            cursor = rows.get(rows.size() - 1).studyGroupMemberId();
        }

        assertThat(collected).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    @DisplayName("커서보다 id가 큰 행만 반환한다")
    void returnsRowsAfterCursorOnly() {
        StudyGroup group = persistGroup("SpringBoot 스터디", ChallengerPart.SPRINGBOOT, 1L, 2L, 3L);
        Set<Long> groupIds = Set.of(group.getId());

        List<StudyGroupMemberPageInfo> firstPage = sut.findStudyGroupMemberPage(groupIds, null, 2);
        Long cursor = firstPage.get(1).studyGroupMemberId();

        List<StudyGroupMemberPageInfo> secondPage = sut.findStudyGroupMemberPage(groupIds, cursor, 2);

        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.get(0).studyGroupMemberId()).isGreaterThan(cursor);
        assertThat(secondPage.get(0).memberId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("hasNext 판별을 위해 size+1 로 요청하면 한 건 더 돌려준다")
    void honoursRequestedSize() {
        StudyGroup group = persistGroup("SpringBoot 스터디", ChallengerPart.SPRINGBOOT, 1L, 2L, 3L);

        assertThat(sut.findStudyGroupMemberPage(Set.of(group.getId()), null, 3)).hasSize(3);
        assertThat(sut.findStudyGroupMemberPage(Set.of(group.getId()), null, 2 + 1)).hasSize(3);
    }

    @Test
    @DisplayName("마지막 페이지 다음은 빈 목록이다")
    void lastPageIsFollowedByEmptyPage() {
        StudyGroup group = persistGroup("SpringBoot 스터디", ChallengerPart.SPRINGBOOT, 1L, 2L);

        List<StudyGroupMemberPageInfo> page = sut.findStudyGroupMemberPage(Set.of(group.getId()), null, 2);
        Long cursor = page.get(page.size() - 1).studyGroupMemberId();

        assertThat(sut.findStudyGroupMemberPage(Set.of(group.getId()), cursor, 2)).isEmpty();
    }

    @Test
    @DisplayName("그룹 ID 집합 밖의 스터디원은 섞이지 않는다")
    void excludesMembersOfOtherGroups() {
        StudyGroup visible = persistGroup("보이는 그룹", ChallengerPart.SPRINGBOOT, 1L, 2L);
        persistGroup("안 보이는 그룹", ChallengerPart.IOS, 3L, 4L);

        List<StudyGroupMemberPageInfo> rows = sut.findStudyGroupMemberPage(Set.of(visible.getId()), null, 20);

        assertThat(rows).extracting(StudyGroupMemberPageInfo::memberId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("여러 그룹을 함께 조회하면 그룹 이름과 파트가 행마다 따라온다")
    void carriesGroupContextPerRow() {
        StudyGroup springGroup = persistGroup("SpringBoot 스터디", ChallengerPart.SPRINGBOOT, 1L);
        StudyGroup iosGroup = persistGroup("iOS 스터디", ChallengerPart.IOS, 2L);

        List<StudyGroupMemberPageInfo> rows = sut.findStudyGroupMemberPage(
            Set.of(springGroup.getId(), iosGroup.getId()), null, 20
        );

        assertThat(rows).extracting(
            StudyGroupMemberPageInfo::memberId,
            StudyGroupMemberPageInfo::studyGroupName,
            StudyGroupMemberPageInfo::part
        ).containsExactly(
            tuple(1L, "SpringBoot 스터디", ChallengerPart.SPRINGBOOT),
            tuple(2L, "iOS 스터디", ChallengerPart.IOS)
        );
    }

    @Test
    @DisplayName("그룹 ID 집합이 비면 조회하지 않고 빈 목록을 반환한다")
    void emptyGroupIdsReturnEmpty() {
        persistGroup("SpringBoot 스터디", ChallengerPart.SPRINGBOOT, 1L);

        assertThat(sut.findStudyGroupMemberPage(Set.of(), null, 20)).isEmpty();
    }

    /**
     * {@code Set.of()} 는 순회 순서가 실행마다 달라져 study_group_member 의 삽입 순서(= id 순서)가 흔들린다. 커서 순서를 검증하는 테스트이므로
     * {@link LinkedHashSet} 으로 순서를 고정한다.
     */
    private StudyGroup persistGroup(String name, ChallengerPart part, Long... memberIds) {
        StudyGroup group = StudyGroup.create(
            name, gisuId, part,
            new LinkedHashSet<>(List.of(memberIds)),
            Set.of(999L)
        );
        em.persist(group);
        em.flush();
        em.clear();
        return group;
    }
}
