package com.umc.product.project.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.graphql.ResponseError;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectMemberInfo;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

final class ProjectGraphQlAuthorizationFixtures {

    static final long REQUESTER_ID = 999L;
    static final long PROJECT_ID = 42L;
    static final long APPLICATION_ID = 1_000L;
    static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private ProjectGraphQlAuthorizationFixtures() {
    }

    static String rootProjectQuery() {
        return """
            query {
              project(id: 42) {
                productOwner { memberId }
                coProductOwners { memberId }
              }
            }
            """;
    }

    static String projectMembersQuery() {
        return """
            query {
              project(id: 42) {
                members {
                  member { memberId }
                }
              }
            }
            """;
    }

    static String transitiveResolversQuery() {
        return """
            query {
              project(id: 42) {
                productOwner { memberId }
                coProductOwners { memberId }
                members {
                  member { memberId }
                  application { applicationId }
                }
              }
            }
            """;
    }

    static void assertForbidden(List<ResponseError> errors, String path) {
        assertThat(errors).hasSize(1);
        if (path != null) {
            assertThat(errors.getFirst().getPath()).isEqualTo(path);
        }
        assertThat(errors.getFirst().getExtensions())
            .containsEntry("code", CommonErrorCode.FORBIDDEN.getCode());
    }

    static ProjectInfo projectInfo() {
        return ProjectInfo.builder()
            .id(PROJECT_ID)
            .status(ProjectStatus.IN_PROGRESS)
            .name("Policy Pilot")
            .gisuId(1L)
            .chapterId(7L)
            .productOwnerMemberId(100L)
            .coProductOwnerMemberIds(List.of(101L))
            .partQuotas(List.of())
            .createdAt(EVALUATED_AT)
            .build();
    }

    static ProjectMemberInfo projectMemberInfo() {
        return ProjectMemberInfo.builder()
            .projectMemberId(10L)
            .projectId(PROJECT_ID)
            .applicationId(APPLICATION_ID)
            .memberId(200L)
            .part(ChallengerPart.WEB)
            .isLeader(false)
            .status(ProjectMemberStatus.ACTIVE)
            .build();
    }

    static MemberInfo memberInfo(long id, String nickname) {
        return MemberInfo.builder()
            .id(id)
            .name(nickname)
            .nickname(nickname)
            .schoolName("UMC")
            .build();
    }

    static SubjectAttributes subject(Instant evaluatedAt) {
        SubjectPolicyFacts facts = new SubjectPolicyFacts(evaluatedAt, List.of(), List.of(), Map.of());
        return facts.toSubjectAttributes(REQUESTER_ID, 1L);
    }

    static ResourcePermission projectPermission() {
        return ResourcePermission.of(ResourceType.PROJECT, PROJECT_ID, PermissionType.READ);
    }

    static ResourcePermission applicationPermission() {
        return ResourcePermission.of(ResourceType.PROJECT_APPLICATION, APPLICATION_ID, PermissionType.READ);
    }
}
