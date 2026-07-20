package com.umc.product.organization.domain;

import static com.umc.product.support.fixture.OrganizationUnitFixture.기수;
import static com.umc.product.support.fixture.OrganizationUnitFixture.리더십;
import static com.umc.product.support.fixture.OrganizationUnitFixture.스쿼드;
import static com.umc.product.support.fixture.OrganizationUnitFixture.지부;
import static com.umc.product.support.fixture.OrganizationUnitFixture.프로덕트_멤버;
import static com.umc.product.support.fixture.OrganizationUnitFixture.프로덕트_챕터;
import static com.umc.product.support.fixture.OrganizationUnitFixture.학교;
import static com.umc.product.support.fixture.OrganizationUnitFixture.활동_기간;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.enums.UmcProductSquadRole;
import com.umc.product.organization.domain.vo.GisuPeriod;
import com.umc.product.organization.domain.vo.UmcProductDatePeriod;
import com.umc.product.organization.exception.OrganizationDomainException;

@DisplayName("Organization 도메인 불변식 잔여 경로")
class OrganizationDomainResidualTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);

    @Test
    @DisplayName("지부·학교 소속·스터디 구성원은 모든 필수 연관과 식별자를 검증한다")
    void 조직_구조의_필수값을_검증한다() {
        var gisu = 기수(1L, 9L, true);
        var chapter = 지부(2L, gisu, "서울");
        var school = 학교(3L, "테스트대학교");
        var studyGroup = StudyGroup.create("스터디", 1L, ChallengerPart.SPRINGBOOT);

        assertThatThrownBy(() -> Chapter.create(null, "서울"))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> Chapter.create(gisu, " "))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> ChapterSchool.create(null, school))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> ChapterSchool.create(chapter, null))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> StudyGroupMember.create(null, 10L))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> StudyGroupMember.create(studyGroup, null))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> StudyGroupMentor.create(null, 10L))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> StudyGroupMentor.create(studyGroup, null))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> studyGroup.assignMentors(Set.of()))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> UmcProductMember.create(null, null, null))
            .isInstanceOf(OrganizationDomainException.class);
    }

    @Test
    @DisplayName("기수와 프로덕트 기간은 null·역전·비인접 날짜 경계를 안전하게 처리한다")
    void 기간_경계를_검증한다() {
        Instant startAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant endAt = Instant.parse("2026-12-31T23:59:59Z");
        var period = UmcProductDatePeriod.of(START, END);

        assertThatThrownBy(() -> GisuPeriod.of(null, endAt))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> GisuPeriod.of(startAt, null))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> GisuPeriod.of(endAt, startAt))
            .isInstanceOf(OrganizationDomainException.class);
        assertThat(period.overlaps(null)).isFalse();
        assertThat(period.isAdjacentTo(null)).isFalse();
        assertThat(period.isActiveOn(null)).isFalse();
    }

    @Test
    @DisplayName("프로덕트 조직의 상태 변경과 선택 입력을 실제 엔티티에 반영한다")
    void 프로덕트_조직의_상태를_변경한다() {
        var chapter = 프로덕트_챕터(1L);
        chapter.deactivate();
        assertThat(chapter.isActive()).isFalse();
        chapter.activate();
        assertThat(chapter.isActive()).isTrue();

        var squad = 스쿼드(2L, START, END);
        squad.update(null, null, " 변경 ", null, END, 9, false);

        assertThat(squad.getDescription()).isEqualTo("변경");
        assertThat(squad.getSortOrder()).isEqualTo(9);
        assertThat(squad.isActive()).isFalse();
        assertThatThrownBy(() -> squad.update(null, " ", null, null, END, null, null))
            .isInstanceOf(OrganizationDomainException.class);
    }

    @Test
    @DisplayName("프로덕트 소속 엔티티는 누락된 상위 활동·역할·직책을 각각 거부한다")
    void 프로덕트_소속의_필수값을_검증한다() {
        var member = 프로덕트_멤버(1L, 10L, null);
        var activity = 활동_기간(2L, member, START, END);
        var chapter = 프로덕트_챕터(3L);
        var squad = 스쿼드(4L, START, END);

        assertThatThrownBy(() -> UmcProductMemberActivityPeriod.create(null, START, END))
            .isInstanceOf(OrganizationDomainException.class);
        assertThat(activity.overlaps(null)).isFalse();
        assertThat(activity.isAdjacentTo(null)).isFalse();

        assertThatThrownBy(() -> UmcProductChapterMembership.create(
            null, chapter, UmcProductPosition.SERVER_DEVELOPER, null, null, START, END
        )).isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> UmcProductChapterMembership.create(
            activity, null, UmcProductPosition.SERVER_DEVELOPER, null, null, START, END
        )).isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> UmcProductChapterMembership.create(
            activity, chapter, null, null, null, START, END
        )).isInstanceOf(OrganizationDomainException.class);

        assertThatThrownBy(() -> UmcProductLeadership.create(
            null, UmcProductLeadershipRole.UMC_PRODUCT_LEAD, START, END
        )).isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> UmcProductLeadership.create(activity, null, START, END))
            .isInstanceOf(OrganizationDomainException.class);

        assertThatThrownBy(() -> participant(
            null, activity, UmcProductSquadRole.MEMBER, UmcProductPosition.SERVER_DEVELOPER
        )).isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> participant(
            squad, null, UmcProductSquadRole.MEMBER, UmcProductPosition.SERVER_DEVELOPER
        )).isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> participant(
            squad, activity, null, UmcProductPosition.SERVER_DEVELOPER
        )).isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> participant(
            squad, activity, UmcProductSquadRole.MEMBER, null
        )).isInstanceOf(OrganizationDomainException.class);

        assertThat(리더십(5L, activity, START, END).isActiveOn(START)).isTrue();
    }

    private UmcProductSquadParticipant participant(
        UmcProductSquad squad,
        UmcProductMemberActivityPeriod activity,
        UmcProductSquadRole role,
        UmcProductPosition position
    ) {
        return UmcProductSquadParticipant.create(
            squad, activity, role, position, null, null, START, END
        );
    }
}
