package com.umc.product.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

/**
 * Aggregate root {@link StudyGroup} 의 도메인 invariant 단위 테스트.
 * <p>
 * Spring/DB 없이 순수 도메인 객체로 비즈니스 규칙을 검증한다.
 */
class StudyGroupTest {

    @Test
    void 기본트랙_스터디는_파트없이_생성된다() {
        // when
        StudyGroup group = StudyGroup.create("웹 트랙", 1L, null, ChallengerTrack.WEB_PRODUCT_ENGINEER,
            Set.of(1L), Set.of(2L));

        // then
        assertThat(group.getPart()).isNull();
        assertThat(group.getTrack()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }

    @Test
    void 파트와_트랙을_함께_지정할_수_없다() {
        // when & then
        assertThatThrownBy(() -> StudyGroup.create("웹 트랙", 1L, ChallengerPart.WEB,
            ChallengerTrack.WEB_PRODUCT_ENGINEER, Set.of(1L), Set.of(2L)))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_LEARNING_TYPE_INVALID);
    }

    @Test
    void 플러스트랙_스터디를_생성할_수_없다() {
        // when & then
        assertThatThrownBy(() -> StudyGroup.create("인프라", 1L, null,
            ChallengerTrack.INFRA_PLUS, Set.of(1L), Set.of(2L)))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_LEARNING_TYPE_INVALID);
    }

    @Test
    void 트랙스터디를_파트스터디로_변경할_수_없다() {
        // given
        StudyGroup group = StudyGroup.create("웹 트랙", 1L, null, ChallengerTrack.WEB_PRODUCT_ENGINEER,
            Set.of(1L), Set.of(2L));

        // when & then
        assertThatThrownBy(() -> group.updatePart(ChallengerPart.WEB))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_LEARNING_TYPE_INVALID);
    }

    @Test
    void create_정상_입력이면_StudyGroup_생성() {
        // when
        StudyGroup group = StudyGroup.create("스프링 스터디", 1L, ChallengerPart.SPRINGBOOT);

        // then
        assertThat(group.getName()).isEqualTo("스프링 스터디");
        assertThat(group.getGisuId()).isEqualTo(1L);
        assertThat(group.getPart()).isEqualTo(ChallengerPart.SPRINGBOOT);
        assertThat(group.getMembers()).isEmpty();
        assertThat(group.getMentors()).isEmpty();
    }

    @Test
    void create_name_이_blank면_STUDY_GROUP_NAME_REQUIRED() {
        assertThatThrownBy(() -> StudyGroup.create("  ", 1L, ChallengerPart.SPRINGBOOT))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.STUDY_GROUP_NAME_REQUIRED);
    }

    @Test
    void create_gisuId_가_null이면_GISU_REQUIRED() {
        assertThatThrownBy(() -> StudyGroup.create("스터디", null, ChallengerPart.SPRINGBOOT))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.GISU_REQUIRED);
    }

    @Test
    void create_part_가_null이면_PART_REQUIRED() {
        assertThatThrownBy(() -> StudyGroup.create("스터디", 1L, null))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.PART_REQUIRED);
    }

    @Test
    void addMember_정상이면_members에_추가된다() {
        // given
        StudyGroup group = StudyGroup.create("g", 1L, ChallengerPart.SPRINGBOOT);

        // when
        group.addMember(100L);
        group.addMember(101L);

        // then
        assertThat(group.getMembers())
            .extracting(StudyGroupMember::getMemberId)
            .containsExactly(100L, 101L);
    }

    @Test
    void addMember_이미_존재하는_memberId면_STUDY_GROUP_MEMBER_DUPLICATED() {
        // given
        StudyGroup group = StudyGroup.create("g", 1L, ChallengerPart.SPRINGBOOT);
        group.addMember(100L);

        // when & then
        assertThatThrownBy(() -> group.addMember(100L))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_DUPLICATED);
    }

    @Test
    void assignMentor_이미_존재하는_mentorId면_STUDY_GROUP_MENTOR_DUPLICATED() {
        // given
        StudyGroup group = StudyGroup.create("g", 1L, ChallengerPart.SPRINGBOOT);
        group.assignMentor(10L);

        // when & then
        assertThatThrownBy(() -> group.assignMentor(10L))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MENTOR_DUPLICATED);
    }

    @Test
    void removeMember_존재하지_않는_memberId면_STUDY_GROUP_MEMBER_NOT_FOUND() {
        // given
        StudyGroup group = StudyGroup.create("g", 1L, ChallengerPart.SPRINGBOOT);
        group.addMembers(Set.of(100L, 101L));

        // when & then
        assertThatThrownBy(() -> group.removeMember(999L))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_NOT_FOUND);
    }

    @Test
    void removeMember_마지막_멤버를_제거하면_STUDY_GROUP_MEMBER_REQUIRED() {
        // given — 멤버 1명 그룹. 마지막 멤버는 도메인 invariant 상 삭제 불가.
        StudyGroup group = StudyGroup.create("g", 1L, ChallengerPart.SPRINGBOOT);
        group.addMember(100L);

        // when & then
        assertThatThrownBy(() -> group.removeMember(100L))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_REQUIRED);
    }

    @Test
    void removeMember_2명_이상일_때는_정상_제거() {
        // given
        StudyGroup group = StudyGroup.create("g", 1L, ChallengerPart.SPRINGBOOT);
        group.addMembers(Set.of(100L, 101L));

        // when
        group.removeMember(100L);

        // then
        assertThat(group.getMembers())
            .extracting(StudyGroupMember::getMemberId)
            .containsExactly(101L);
    }

    @Test
    void removeMentor_마지막_멘토를_제거하면_STUDY_GROUP_MENTOR_REQUIRED() {
        // given
        StudyGroup group = StudyGroup.create("g", 1L, ChallengerPart.SPRINGBOOT);
        group.assignMentor(10L);

        // when & then
        assertThatThrownBy(() -> group.removeMentor(10L))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MENTOR_REQUIRED);
    }

    @Test
    void addMembers_빈_Set이면_STUDY_GROUP_MEMBER_REQUIRED() {
        // given
        StudyGroup group = StudyGroup.create("g", 1L, ChallengerPart.SPRINGBOOT);

        // when & then
        assertThatThrownBy(() -> group.addMembers(Set.of()))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_REQUIRED);
    }
}
