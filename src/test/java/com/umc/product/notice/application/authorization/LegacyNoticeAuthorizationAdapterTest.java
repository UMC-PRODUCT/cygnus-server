package com.umc.product.notice.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;

@ExtendWith(MockitoExtension.class)
@DisplayName("LegacyNoticeAuthorizationAdapter")
class LegacyNoticeAuthorizationAdapterTest {

    private static final Long SCHOOL_ID = 30L;
    private static final Long OTHER_SCHOOL_ID = 31L;
    private static final Long CHAPTER_ID = 40L;
    private static final Long OTHER_CHAPTER_ID = 41L;

    @Mock
    GetChallengerRoleUseCase challengerRoleUseCase;

    @Test
    @DisplayName("특정 기수 공지 읽기는 기존 호환성을 위해 중앙 총괄단의 다른 기수 역할도 인정한다")
    void central_core_in_other_gisu_can_read_specific_gisu_notice_for_compatibility() {
        LegacyNoticeAuthorizationAdapter sut = new LegacyNoticeAuthorizationAdapter(challengerRoleUseCase);
        NoticeTargetInfo target = new NoticeTargetInfo(10L, null, null, List.of(), NoticeTab.CHALLENGER);
        SubjectAttributes subject = subjectWithRole(new RoleAttribute(
            ChallengerRoleType.CENTRAL_VICE_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            null,
            9L
        ));

        boolean result = decision(sut.evaluate(context(NoticePolicyAction.READ, subject, target)));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("특정 기수 학교 공지 관리는 기존 호환성을 위해 같은 학교 운영진의 다른 기수 역할도 인정한다")
    void school_admin_in_other_gisu_can_check_specific_gisu_school_notice_for_compatibility() {
        LegacyNoticeAuthorizationAdapter sut = new LegacyNoticeAuthorizationAdapter(challengerRoleUseCase);
        NoticeTargetInfo target = new NoticeTargetInfo(10L, null, SCHOOL_ID, List.of(), NoticeTab.CHALLENGER);
        SubjectAttributes subject = subjectWithRole(new RoleAttribute(
            ChallengerRoleType.SCHOOL_PART_LEADER,
            OrganizationType.SCHOOL,
            SCHOOL_ID,
            null,
            9L
        ));

        boolean result = decision(sut.evaluate(context(NoticePolicyAction.CHECK_RECIPIENTS, subject, target)));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("특정 기수 지부 공지 관리는 기존 호환성을 위해 같은 지부장의 다른 기수 역할도 인정한다")
    void chapter_president_in_other_gisu_can_check_specific_gisu_chapter_notice_for_compatibility() {
        LegacyNoticeAuthorizationAdapter sut = new LegacyNoticeAuthorizationAdapter(challengerRoleUseCase);
        NoticeTargetInfo target = new NoticeTargetInfo(10L, CHAPTER_ID, null, List.of(), NoticeTab.CHALLENGER);
        SubjectAttributes subject = subjectWithRole(new RoleAttribute(
            ChallengerRoleType.CHAPTER_PRESIDENT,
            OrganizationType.CHAPTER,
            CHAPTER_ID,
            null,
            9L
        ));

        boolean result = decision(sut.evaluate(context(NoticePolicyAction.CHECK_RECIPIENTS, subject, target)));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("특정 기수 전체 공지 관리는 기존 호환성을 위해 중앙운영진의 다른 기수 역할도 인정한다")
    void central_member_in_other_gisu_can_check_specific_gisu_notice_for_compatibility() {
        LegacyNoticeAuthorizationAdapter sut = new LegacyNoticeAuthorizationAdapter(challengerRoleUseCase);
        NoticeTargetInfo target = new NoticeTargetInfo(10L, null, null, List.of(), NoticeTab.CHALLENGER);
        SubjectAttributes subject = subjectWithRole(new RoleAttribute(
            ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER,
            OrganizationType.CENTRAL,
            null,
            null,
            9L
        ));

        boolean result = decision(sut.evaluate(context(NoticePolicyAction.CHECK_RECIPIENTS, subject, target)));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("다른 학교 운영진은 특정 기수 학교 공지를 관리할 수 없다")
    void school_admin_in_other_school_cannot_check_specific_gisu_school_notice() {
        LegacyNoticeAuthorizationAdapter sut = new LegacyNoticeAuthorizationAdapter(challengerRoleUseCase);
        NoticeTargetInfo target = new NoticeTargetInfo(10L, null, SCHOOL_ID, List.of(), NoticeTab.CHALLENGER);
        SubjectAttributes subject = subjectWithRole(new RoleAttribute(
            ChallengerRoleType.SCHOOL_PART_LEADER,
            OrganizationType.SCHOOL,
            OTHER_SCHOOL_ID,
            null,
            9L
        ));

        boolean result = decision(sut.evaluate(context(NoticePolicyAction.CHECK_RECIPIENTS, subject, target)));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("다른 지부장은 특정 기수 지부 공지를 관리할 수 없다")
    void chapter_president_in_other_chapter_cannot_check_specific_gisu_chapter_notice() {
        LegacyNoticeAuthorizationAdapter sut = new LegacyNoticeAuthorizationAdapter(challengerRoleUseCase);
        NoticeTargetInfo target = new NoticeTargetInfo(10L, CHAPTER_ID, null, List.of(), NoticeTab.CHALLENGER);
        SubjectAttributes subject = subjectWithRole(new RoleAttribute(
            ChallengerRoleType.CHAPTER_PRESIDENT,
            OrganizationType.CHAPTER,
            OTHER_CHAPTER_ID,
            null,
            9L
        ));

        boolean result = decision(sut.evaluate(context(NoticePolicyAction.CHECK_RECIPIENTS, subject, target)));

        assertThat(result).isFalse();
    }

    private SubjectAttributes subjectWithRole(RoleAttribute roleAttribute) {
        return SubjectAttributes.builder()
            .memberId(1L)
            .schoolId(SCHOOL_ID)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of(roleAttribute))
            .systemRoles(Set.of())
            .build();
    }

    private NoticeAuthorizationContext context(
        NoticePolicyAction action,
        SubjectAttributes subject,
        NoticeTargetInfo target
    ) {
        return new NoticeAuthorizationContext(
            action,
            subject.memberId(),
            Optional.of(subject),
            Optional.empty(),
            target,
            null,
            false,
            Instant.parse("2026-07-01T00:00:00Z"));
    }

    private boolean decision(PolicyRolloutEvaluation<Boolean> evaluation) {
        return ((PolicyRolloutEvaluation.Success<Boolean>) evaluation).decision();
    }
}
