package com.umc.product.notice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.notice.application.port.in.query.GetNoticeTargetUseCase;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.support.fixture.NoticeUnitFixture;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticePermissionEvaluator 잔여 경계")
class NoticePermissionEvaluatorResidualTest {

    private static final Long NOTICE_ID = 1L;
    private static final Long MEMBER_ID = 10L;
    private static final Long GISU_ID = 20L;
    private static final Long CHAPTER_ID = 30L;
    private static final Long SCHOOL_ID = 40L;

    @Mock
    GetNoticeTargetUseCase getNoticeTargetUseCase;

    @Mock
    LoadNoticePort loadNoticePort;

    @Test
    @DisplayName("지원하지 않는 권한은 평가하지 않고 명시적인 권한 예외를 반환한다")
    void rejects_unsupported_permission() {
        ResourcePermission permission = mock(ResourcePermission.class);
        given(permission.resourceType()).willReturn(ResourceType.NOTICE);
        given(permission.permission()).willReturn(PermissionType.WRITE);

        assertThatThrownBy(() -> sut().evaluate(subject(), permission))
            .isInstanceOf(AuthorizationDomainException.class);
    }

    @Test
    @DisplayName("지원 리소스 타입을 NOTICE로 선언하고 foreign permission은 구현되지 않은 권한으로 거부한다")
    void exposes_supported_type_and_rejects_foreign_permission() {
        assertThat(sut().supportedResourceType()).isEqualTo(ResourceType.NOTICE);
        ResourcePermission permission = mock(ResourcePermission.class);
        given(permission.resourceType()).willReturn(ResourceType.ATTENDANCE);
        given(permission.permission()).willReturn(PermissionType.APPROVE);
        given(permission.getResourceIdAsLong()).willReturn(NOTICE_ID);
        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(null, null, null, List.of(), NoticeTab.CHALLENGER)
        );

        assertThatThrownBy(() -> sut().evaluate(subject(), permission))
            .isInstanceOf(AuthorizationDomainException.class);
    }

    @Test
    @DisplayName("일반 챌린저는 기수·지부·학교·파트가 모두 일치할 때만 읽을 수 있다")
    void challenger_scope_must_match_completely() {
        NoticeTargetInfo target = target(GISU_ID, CHAPTER_ID, SCHOOL_ID,
            List.of(ChallengerPart.SPRINGBOOT), NoticeTab.CHALLENGER);
        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(target);

        assertThat(evaluate(PermissionType.READ, subject(
            List.of(info(GISU_ID, CHAPTER_ID, ChallengerPart.SPRINGBOOT)), List.of(), Set.of(), SCHOOL_ID)))
            .isTrue();
        assertThat(evaluate(PermissionType.READ, subject(
            List.of(info(GISU_ID, CHAPTER_ID, ChallengerPart.WEB)), List.of(), Set.of(), SCHOOL_ID)))
            .isFalse();
    }

    @Test
    @DisplayName("운영진 공지는 역할 하한·기수·학교·담당 파트를 모두 확인한다")
    void staff_notice_checks_role_gisu_school_and_part() {
        NoticeTargetInfo target = target(GISU_ID, null, SCHOOL_ID,
            List.of(ChallengerPart.SPRINGBOOT), NoticeTab.SCHOOL_PART_LEADER);
        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(target);

        assertThat(evaluate(PermissionType.READ, subject(List.of(), List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, ChallengerPart.SPRINGBOOT, GISU_ID)),
            Set.of(), SCHOOL_ID))).isTrue();
        assertThat(evaluate(PermissionType.READ, subject(List.of(), List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, ChallengerPart.WEB, GISU_ID)),
            Set.of(), SCHOOL_ID))).isFalse();
        assertThat(evaluate(PermissionType.READ, subject(List.of(), List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, ChallengerPart.ADMIN, GISU_ID)),
            Set.of(), SCHOOL_ID))).isTrue();
        assertThat(evaluate(PermissionType.READ, subject(List.of(), List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID + 1, ChallengerPart.SPRINGBOOT, GISU_ID)),
            Set.of(), SCHOOL_ID))).isFalse();
        assertThat(evaluate(PermissionType.READ, subject(List.of(), List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, ChallengerPart.SPRINGBOOT, GISU_ID + 1)),
            Set.of(), SCHOOL_ID))).isFalse();

        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(GISU_ID, null, SCHOOL_ID, List.of(), NoticeTab.CENTRAL_MEMBER)
        );
        assertThat(evaluate(PermissionType.READ, subject(List.of(), List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, ChallengerPart.ADMIN, GISU_ID)),
            Set.of(), SCHOOL_ID))).isFalse();
    }

    @Test
    @DisplayName("학교 파트장은 역할의 학교·파트·기수·지부 범위가 모두 맞아야 일반 공지를 읽는다")
    void school_part_leader_scope_is_fail_closed() {
        NoticeTargetInfo target = target(GISU_ID, CHAPTER_ID, SCHOOL_ID,
            List.of(ChallengerPart.SPRINGBOOT), NoticeTab.CHALLENGER);
        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(target);
        GisuChallengerInfo challenger = info(GISU_ID, CHAPTER_ID, ChallengerPart.WEB);

        assertThat(evaluate(PermissionType.READ, subject(List.of(challenger), List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, ChallengerPart.SPRINGBOOT, GISU_ID)),
            Set.of(), SCHOOL_ID))).isTrue();
        assertThat(evaluate(PermissionType.READ, subject(List.of(challenger), List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, null, ChallengerPart.SPRINGBOOT, GISU_ID)),
            Set.of(), SCHOOL_ID))).isFalse();
        assertThat(evaluate(PermissionType.READ, subject(List.of(challenger), List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, null, GISU_ID)),
            Set.of(), SCHOOL_ID))).isFalse();
        assertThat(evaluate(PermissionType.READ, subject(List.of(challenger), List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, ChallengerPart.WEB, GISU_ID)),
            Set.of(), SCHOOL_ID))).isFalse();

        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(GISU_ID, CHAPTER_ID, SCHOOL_ID + 1,
                List.of(ChallengerPart.SPRINGBOOT), NoticeTab.CHALLENGER)
        );
        assertThat(evaluate(PermissionType.READ, subject(List.of(challenger), List.of(
            role(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, ChallengerPart.SPRINGBOOT, GISU_ID)),
            Set.of(), SCHOOL_ID))).isFalse();
    }

    @Test
    @DisplayName("학교 회장단은 같은 학교 또는 같은 기수·지부 범위의 공지만 읽는다")
    void school_core_scope_is_fail_closed() {
        GisuChallengerInfo challenger = info(GISU_ID, CHAPTER_ID, ChallengerPart.WEB);
        RoleAttribute president = role(ChallengerRoleType.SCHOOL_PRESIDENT, SCHOOL_ID, null, GISU_ID);

        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(GISU_ID, CHAPTER_ID, null, List.of(ChallengerPart.SPRINGBOOT), NoticeTab.CHALLENGER));
        assertThat(evaluate(PermissionType.READ,
            subject(List.of(challenger), List.of(president), Set.of(), SCHOOL_ID))).isTrue();

        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(GISU_ID, CHAPTER_ID, SCHOOL_ID + 1, List.of(), NoticeTab.CHALLENGER));
        assertThat(evaluate(PermissionType.READ,
            subject(List.of(challenger), List.of(president), Set.of(), SCHOOL_ID))).isFalse();
        assertThat(evaluate(PermissionType.READ,
            subject(List.of(challenger), List.of(role(ChallengerRoleType.SCHOOL_PRESIDENT, null, null, GISU_ID)),
                Set.of(), SCHOOL_ID))).isFalse();
    }

    @Test
    @DisplayName("지부장은 조직·학교·기수·지부 범위가 불완전하거나 전체 공지이면 역할 우회가 허용되지 않는다")
    void chapter_president_scope_is_fail_closed() {
        RoleAttribute president = role(ChallengerRoleType.CHAPTER_PRESIDENT, CHAPTER_ID, null, GISU_ID);
        GisuChallengerInfo challenger = info(GISU_ID, CHAPTER_ID, ChallengerPart.WEB);

        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(GISU_ID, CHAPTER_ID, SCHOOL_ID, List.of(ChallengerPart.SPRINGBOOT), NoticeTab.CHALLENGER));
        assertThat(evaluate(PermissionType.READ,
            subject(List.of(challenger), List.of(president), Set.of(), SCHOOL_ID))).isTrue();
        assertThat(evaluate(PermissionType.READ,
            subject(List.of(challenger), List.of(president), Set.of(), SCHOOL_ID + 1))).isFalse();
        assertThat(evaluate(PermissionType.READ,
            subject(List.of(challenger), List.of(role(ChallengerRoleType.CHAPTER_PRESIDENT, null, null, GISU_ID)),
                Set.of(), SCHOOL_ID))).isFalse();

        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(GISU_ID, null, null, List.of(ChallengerPart.SPRINGBOOT), NoticeTab.CHALLENGER));
        assertThat(evaluate(PermissionType.READ,
            subject(List.of(challenger), List.of(president), Set.of(), SCHOOL_ID))).isFalse();

        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(GISU_ID, CHAPTER_ID + 1, null,
                List.of(ChallengerPart.SPRINGBOOT), NoticeTab.CHALLENGER)
        );
        assertThat(evaluate(PermissionType.READ,
            subject(List.of(challenger), List.of(president), Set.of(), SCHOOL_ID))).isFalse();
    }

    @Test
    @DisplayName("중앙 운영진은 같은 기수의 일반 공지를 파트와 무관하게 읽는다")
    void central_member_reads_only_matching_gisu() {
        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(GISU_ID, null, null, List.of(ChallengerPart.SPRINGBOOT), NoticeTab.CHALLENGER));

        assertThat(evaluate(PermissionType.READ, subject(List.of(), List.of(
            role(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER, null, null, GISU_ID)),
            Set.of(), null))).isTrue();
        assertThat(evaluate(PermissionType.READ, subject(List.of(), List.of(
            role(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, null, null, GISU_ID + 1)),
            Set.of(), null))).isFalse();

        assertThat(evaluate(PermissionType.READ, subject(List.of(), List.of(
            role(ChallengerRoleType.SCHOOL_ETC_ADMIN, SCHOOL_ID, null, GISU_ID)),
            Set.of(), null))).isFalse();
    }

    @Test
    @DisplayName("수정·삭제는 SUPER_ADMIN 또는 작성자만 허용하고 누락 공지는 명시적으로 실패한다")
    void edit_and_delete_require_super_admin_or_author() {
        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(null, null, null, List.of(), NoticeTab.CHALLENGER));

        assertThat(evaluate(PermissionType.EDIT,
            subject(List.of(), List.of(), Set.of(SystemRoleType.SUPER_ADMIN), null))).isTrue();

        given(loadNoticePort.findNoticeById(NOTICE_ID)).willReturn(Optional.of(NoticeUnitFixture.notice(NOTICE_ID, MEMBER_ID)));
        assertThat(evaluate(PermissionType.DELETE, subject())).isTrue();
        assertThat(evaluate(PermissionType.EDIT,
            subject(List.of(), List.of(), Set.of(), null, MEMBER_ID + 1))).isFalse();

        given(loadNoticePort.findNoticeById(NOTICE_ID)).willReturn(Optional.empty());
        assertThatThrownBy(() -> evaluate(PermissionType.DELETE, subject()))
            .isInstanceOf(NoticeDomainException.class);
    }

    @Test
    @DisplayName("공지 관리 권한은 중앙·학교·지부·전체 공지 범위별로 fail-closed 한다")
    void manage_permission_depends_on_target_scope() {
        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(null, null, SCHOOL_ID, List.of(), NoticeTab.CHALLENGER));
        assertThat(evaluate(PermissionType.CHECK, subject(List.of(), List.of(
            role(ChallengerRoleType.SCHOOL_ETC_ADMIN, SCHOOL_ID, null, GISU_ID)), Set.of(), SCHOOL_ID))).isTrue();

        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(null, CHAPTER_ID, null, List.of(), NoticeTab.CHALLENGER));
        assertThat(evaluate(PermissionType.CHECK, subject(List.of(), List.of(
            role(ChallengerRoleType.CHAPTER_PRESIDENT, CHAPTER_ID, null, GISU_ID)), Set.of(), SCHOOL_ID))).isTrue();

        given(getNoticeTargetUseCase.findByNoticeId(NOTICE_ID)).willReturn(
            target(null, null, null, List.of(), NoticeTab.CHALLENGER));
        assertThat(evaluate(PermissionType.CHECK, subject(List.of(), List.of(
            role(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER, null, null, GISU_ID)), Set.of(), SCHOOL_ID))).isTrue();
        assertThat(evaluate(PermissionType.CHECK, subject())).isFalse();

        assertThat(evaluate(PermissionType.CHECK, subject(List.of(), List.of(
            role(ChallengerRoleType.CENTRAL_PRESIDENT, null, null, GISU_ID)), Set.of(), SCHOOL_ID))).isTrue();
    }

    private NoticePermissionEvaluator sut() {
        return new NoticePermissionEvaluator(getNoticeTargetUseCase, loadNoticePort);
    }

    private boolean evaluate(PermissionType permissionType, SubjectAttributes subject) {
        return sut().evaluate(subject, ResourcePermission.of(ResourceType.NOTICE, NOTICE_ID, permissionType));
    }

    private SubjectAttributes subject() {
        return subject(List.of(), List.of(), Set.of(), SCHOOL_ID);
    }

    private SubjectAttributes subject(List<GisuChallengerInfo> infos, List<RoleAttribute> roles,
                                      Set<SystemRoleType> systemRoles, Long schoolId) {
        return subject(infos, roles, systemRoles, schoolId, MEMBER_ID);
    }

    private SubjectAttributes subject(List<GisuChallengerInfo> infos, List<RoleAttribute> roles,
                                      Set<SystemRoleType> systemRoles, Long schoolId, Long memberId) {
        return SubjectAttributes.builder()
            .memberId(memberId)
            .schoolId(schoolId)
            .gisuChallengerInfos(infos)
            .roleAttributes(roles)
            .systemRoles(systemRoles)
            .build();
    }

    private GisuChallengerInfo info(Long gisuId, Long chapterId, ChallengerPart part) {
        return new GisuChallengerInfo(gisuId, chapterId, part, 99L);
    }

    private RoleAttribute role(ChallengerRoleType type, Long organizationId,
                               ChallengerPart part, Long gisuId) {
        return new RoleAttribute(type, type.organizationType(), organizationId, part, gisuId);
    }

    private NoticeTargetInfo target(Long gisuId, Long chapterId, Long schoolId,
                                    List<ChallengerPart> parts, NoticeTab tab) {
        return new NoticeTargetInfo(gisuId, chapterId, schoolId, parts, tab);
    }
}
