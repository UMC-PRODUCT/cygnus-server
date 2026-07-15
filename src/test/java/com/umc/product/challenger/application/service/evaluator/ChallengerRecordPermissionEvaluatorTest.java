package com.umc.product.challenger.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

@DisplayName("ChallengerRecordPermissionEvaluator")
class ChallengerRecordPermissionEvaluatorTest {

    private final ChallengerRecordPermissionEvaluator sut = new ChallengerRecordPermissionEvaluator();

    @Test
    @DisplayName("supportedResourceType은 CHALLENGER_RECORD를 반환한다")
    void supportedResourceType은_CHALLENGER_RECORD를_반환한다() {
        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.CHALLENGER_RECORD);
    }

    // === READ: 단건 조회 - 교내 회장단 이상 ===

    @Test
    @DisplayName("학교 회장(SCHOOL_PRESIDENT)은 단건 조회(READ) 권한을 통과한다")
    void 학교_회장_READ_허용() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.SCHOOL_PRESIDENT));

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    @Test
    @DisplayName("학교 부회장(SCHOOL_VICE_PRESIDENT)은 단건 조회(READ) 권한을 통과한다")
    void 학교_부회장_READ_허용() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.SCHOOL_VICE_PRESIDENT));

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    @Test
    @DisplayName("총괄(CENTRAL_PRESIDENT)은 단건 조회(READ) 권한을 거부한다 - isAtLeastSchoolCore는 교내 역할만 포함")
    void 총괄_READ_거부() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.CENTRAL_PRESIDENT));

        assertThat(sut.evaluate(subject, readPermission())).isFalse();
    }

    @Test
    @DisplayName("교내 파트장(SCHOOL_PART_LEADER)은 단건 조회(READ) 권한을 거부한다")
    void 교내_파트장_READ_거부() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.SCHOOL_PART_LEADER));

        assertThat(sut.evaluate(subject, readPermission())).isFalse();
    }

    // === MANAGE: 목록/통계 조회 - 총괄단 이상 ===

    @Test
    @DisplayName("총괄(CENTRAL_PRESIDENT)은 목록/통계 조회(MANAGE) 권한을 통과한다")
    void 총괄_MANAGE_허용() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.CENTRAL_PRESIDENT));

        assertThat(sut.evaluate(subject, managePermission())).isTrue();
    }

    @Test
    @DisplayName("부총괄(CENTRAL_VICE_PRESIDENT)은 목록/통계 조회(MANAGE) 권한을 통과한다")
    void 부총괄_MANAGE_허용() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.CENTRAL_VICE_PRESIDENT));

        assertThat(sut.evaluate(subject, managePermission())).isTrue();
    }

    @Test
    @DisplayName("학교 회장(SCHOOL_PRESIDENT)은 목록/통계 조회(MANAGE) 권한을 거부한다")
    void 학교_회장_MANAGE_거부() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.SCHOOL_PRESIDENT));

        assertThat(sut.evaluate(subject, managePermission())).isFalse();
    }

    @Test
    @DisplayName("중앙운영사무국 운영국원(CENTRAL_OPERATING_TEAM_MEMBER)은 목록/통계 조회(MANAGE) 권한을 거부한다")
    void 중앙_운영국원_MANAGE_거부() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER));

        assertThat(sut.evaluate(subject, managePermission())).isFalse();
    }

    // === WRITE / DELETE: 총괄단 이상 ===

    @Test
    @DisplayName("총괄(CENTRAL_PRESIDENT)은 생성(WRITE)/삭제(DELETE) 권한을 통과한다")
    void 총괄_WRITE_DELETE_허용() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.CENTRAL_PRESIDENT));

        assertThat(sut.evaluate(subject, permission(PermissionType.WRITE))).isTrue();
        assertThat(sut.evaluate(subject, permission(PermissionType.DELETE))).isTrue();
    }

    @Test
    @DisplayName("학교 회장(SCHOOL_PRESIDENT)은 생성(WRITE)/삭제(DELETE) 권한을 거부한다")
    void 학교_회장_WRITE_DELETE_거부() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.SCHOOL_PRESIDENT));

        assertThat(sut.evaluate(subject, permission(PermissionType.WRITE))).isFalse();
        assertThat(sut.evaluate(subject, permission(PermissionType.DELETE))).isFalse();
    }

    // === 공통 케이스 ===

    @Test
    @DisplayName("SUPER_ADMIN은 READ/MANAGE/WRITE/DELETE 모든 권한을 통과한다")
    void SUPER_ADMIN_모든_권한_허용() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.SUPER_ADMIN));

        assertThat(sut.evaluate(subject, permission(PermissionType.READ))).isTrue();
        assertThat(sut.evaluate(subject, permission(PermissionType.MANAGE))).isTrue();
        assertThat(sut.evaluate(subject, permission(PermissionType.WRITE))).isTrue();
        assertThat(sut.evaluate(subject, permission(PermissionType.DELETE))).isTrue();
    }

    @Test
    @DisplayName("역할이 없는 사용자는 모든 권한을 거부한다")
    void 역할_없음_모든_권한_거부() {
        SubjectAttributes subject = subjectWithRoles();

        assertThat(sut.evaluate(subject, permission(PermissionType.READ))).isFalse();
        assertThat(sut.evaluate(subject, permission(PermissionType.MANAGE))).isFalse();
    }

    private ResourcePermission readPermission() {
        return permission(PermissionType.READ);
    }

    private ResourcePermission managePermission() {
        return permission(PermissionType.MANAGE);
    }

    private ResourcePermission permission(PermissionType permissionType) {
        return ResourcePermission.ofType(ResourceType.CHALLENGER_RECORD, permissionType);
    }

    private SubjectAttributes subjectWithRoles(RoleAttribute... roles) {
        return SubjectAttributes.builder()
            .memberId(1L)
            .schoolId(1L)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of(roles))
            .build();
    }

    private RoleAttribute roleOf(ChallengerRoleType roleType) {
        return new RoleAttribute(
            roleType,
            OrganizationType.CENTRAL,
            null,
            null,
            1L
        );
    }
}
