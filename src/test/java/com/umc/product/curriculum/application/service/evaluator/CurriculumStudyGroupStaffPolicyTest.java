package com.umc.product.curriculum.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;

@ExtendWith(MockitoExtension.class)
class CurriculumStudyGroupStaffPolicyTest {

    @Mock
    private GetStudyGroupUseCase getStudyGroupUseCase;
    @Mock
    private GetMemberUseCase getMemberUseCase;
    @InjectMocks
    private CurriculumStudyGroupStaffPolicy policy;

    @Test
    @DisplayName("해당 스터디 그룹 mentor는 관리할 수 있다")
    void mentor_allowed() {
        given(getStudyGroupUseCase.findById(10L)).willReturn(Optional.of(group()));

        boolean allowed = policy.canManage(subject(20L, 100L, List.of(), Set.of()), 10L, 30L, 9L);

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("같은 기수와 학교의 회장단은 관리할 수 있다")
    void sameGisuSchoolCore_allowed() {
        given(getStudyGroupUseCase.findById(10L)).willReturn(Optional.of(group()));
        given(getMemberUseCase.findById(30L)).willReturn(Optional.of(member(30L, 100L)));
        RoleAttribute role = new RoleAttribute(
            ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 100L, null, 9L
        );

        boolean allowed = policy.canManage(subject(21L, 100L, List.of(role), Set.of()), 10L, 30L, 9L);

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("다른 기수의 학교 회장단은 관리할 수 없다")
    void differentGisuSchoolCore_denied() {
        given(getStudyGroupUseCase.findById(10L)).willReturn(Optional.of(group()));
        given(getMemberUseCase.findById(30L)).willReturn(Optional.of(member(30L, 100L)));
        RoleAttribute role = new RoleAttribute(
            ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 100L, null, 8L
        );

        boolean allowed = policy.canManage(subject(21L, 100L, List.of(role), Set.of()), 10L, 30L, 9L);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("다른 그룹 mentor는 관리할 수 없다")
    void differentGroupMentorDenied() {
        given(getStudyGroupUseCase.findById(10L)).willReturn(Optional.of(group()));
        given(getMemberUseCase.findById(30L)).willReturn(Optional.of(member(30L, 100L)));

        boolean allowed = policy.canManage(subject(22L, 100L, List.of(), Set.of()), 10L, 30L, 9L);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("이전 그룹 mentor는 현재 그룹 구성원이 아닌 대상의 워크북을 관리할 수 없다")
    void formerGroupMentorDenied() {
        StudyGroupInfo formerGroup = StudyGroupInfo.create(
            10L, "이전 그룹", 9L, ChallengerPart.SPRINGBOOT, Instant.EPOCH,
            List.of(20L), List.of(31L)
        );
        given(getStudyGroupUseCase.findById(10L)).willReturn(Optional.of(formerGroup));
        given(getMemberUseCase.findById(30L)).willReturn(Optional.of(member(30L, 100L)));

        boolean allowed = policy.canManage(subject(20L, 100L, List.of(), Set.of()), 10L, 30L, 9L);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("다른 학교 회장단은 관리할 수 없다")
    void differentSchoolCoreDenied() {
        given(getStudyGroupUseCase.findById(10L)).willReturn(Optional.of(group()));
        given(getMemberUseCase.findById(30L)).willReturn(Optional.of(member(30L, 100L)));
        RoleAttribute role = new RoleAttribute(
            ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 200L, null, 9L
        );

        boolean allowed = policy.canManage(subject(21L, 200L, List.of(role), Set.of()), 10L, 30L, 9L);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("같은 학교와 기수의 일반 운영진은 관리할 수 없다")
    void generalSchoolAdminDenied() {
        given(getStudyGroupUseCase.findById(10L)).willReturn(Optional.of(group()));
        given(getMemberUseCase.findById(30L)).willReturn(Optional.of(member(30L, 100L)));
        RoleAttribute role = new RoleAttribute(
            ChallengerRoleType.SCHOOL_ETC_ADMIN, OrganizationType.SCHOOL, 100L, null, 9L
        );

        boolean allowed = policy.canManage(subject(21L, 100L, List.of(role), Set.of()), 10L, 30L, 9L);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 그룹과 관계없이 관리할 수 있다")
    void superAdmin_allowed() {
        boolean allowed = policy.canManage(
            subject(99L, null, List.of(), Set.of(SystemRoleType.SUPER_ADMIN)), null, 30L, 9L
        );

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("그룹이 없는 legacy 워크북은 mentor 권한을 추론하지 않는다")
    void legacyWithoutGroup_failsClosedForMentor() {
        given(getMemberUseCase.findById(30L)).willReturn(Optional.of(member(30L, 100L)));

        boolean allowed = policy.canManage(subject(20L, 100L, List.of(), Set.of()), null, 30L, 9L);

        assertThat(allowed).isFalse();
    }

    private StudyGroupInfo group() {
        return StudyGroupInfo.create(
            10L, "그룹", 9L, ChallengerPart.SPRINGBOOT, Instant.EPOCH, List.of(20L), List.of(30L)
        );
    }

    private MemberInfo member(Long memberId, Long schoolId) {
        return MemberInfo.builder().id(memberId).schoolId(schoolId).build();
    }

    private SubjectAttributes subject(
        Long memberId,
        Long schoolId,
        List<RoleAttribute> roles,
        Set<SystemRoleType> systemRoles
    ) {
        return SubjectAttributes.builder()
            .memberId(memberId)
            .schoolId(schoolId)
            .roleAttributes(roles)
            .systemRoles(systemRoles)
            .build();
    }
}
