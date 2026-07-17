package com.umc.product.project.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.CurrentMemberSecurityConfig;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationDetailUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.GetProjectMemberUseCase;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.in.query.dto.GetProjectApplicationDetailQuery;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationViewStatus;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectMemberInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPartQuotaInfo;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

@GraphQlTest(ProjectGraphQlController.class)
@Import({GraphQlRuntimeWiringConfig.class, GraphQlExceptionAdvice.class, CurrentMemberSecurityConfig.class})
@DisplayName("ProjectGraphQlController")
class ProjectGraphQlControllerTest {

    private static final Long REQUESTER_ID = 999L;
    private static final Long PROJECT_ID = 42L;
    private static final Long APPLICATION_ID = 1000L;

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetProjectUseCase getProjectUseCase;

    @MockitoBean
    SearchProjectUseCase searchProjectUseCase;

    @MockitoBean
    GetProjectMemberUseCase getProjectMemberUseCase;

    @MockitoBean
    GetProjectApplicationFormUseCase getProjectApplicationFormUseCase;

    @MockitoBean
    GetProjectApplicationDetailUseCase getProjectApplicationDetailUseCase;

    @MockitoBean
    GetMemberUseCase getMemberUseCase;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(REQUESTER_ID), null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("project 단건 조회는 PROJECT READ 권한을 먼저 검사한다")
    void project_단건_조회는_PROJECT_READ_권한을_먼저_검사한다() {
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());

        graphQlTester.document("""
                query {
                  project(id: 42) {
                    id
                    name
                    status
                  }
                }
                """)
            .execute()
            .path("project.id").entity(String.class).isEqualTo("42")
            .path("project.name").entity(String.class).isEqualTo("Triple");

        InOrder inOrder = inOrder(checkPermissionUseCase, getProjectUseCase);
        inOrder.verify(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, projectReadPermission(PROJECT_ID));
        inOrder.verify(getProjectUseCase).getById(PROJECT_ID);
    }

    @Test
    @DisplayName("project 권한이 거부되면 프로젝트 usecase를 호출하지 않는다")
    void project_권한이_거부되면_프로젝트_usecase를_호출하지_않는다() {
        willThrow(new AccessDeniedException("프로젝트를 볼 권한이 없어요."))
            .given(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, projectReadPermission(PROJECT_ID));

        graphQlTester.document("""
                query {
                  project(id: 42) {
                    id
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "project", CommonErrorCode.FORBIDDEN));

        then(getProjectUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("projects page size가 100을 넘으면 BAD_REQUEST GraphQL error를 반환한다")
    void projects_page_size가_100을_넘으면_BAD_REQUEST_GraphQL_error를_반환한다() {
        graphQlTester.document("""
                query {
                  projects(input: { gisuId: 1 }, page: { size: 101 }) {
                    totalElements
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "projects", CommonErrorCode.BAD_REQUEST));

        then(searchProjectUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Project.members는 nested field에서도 PROJECT READ 권한을 검사한다")
    void Project_members는_nested_field에서도_PROJECT_READ_권한을_검사한다() {
        SubjectAttributes subject = subject();
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, projectReadPermission(PROJECT_ID))).willReturn(true);
        given(getProjectMemberUseCase.listByProjectIds(List.of(PROJECT_ID))).willReturn(Map.of(
            PROJECT_ID,
            List.of(projectMemberInfo(APPLICATION_ID))
        ));

        graphQlTester.document("""
                query {
                  project(id: 42) {
                    members {
                      projectMemberId
                      projectId
                      part
                      leader
                    }
                  }
                }
                """)
            .execute()
            .path("project.members[0].projectMemberId").entity(String.class).isEqualTo("10")
            .path("project.members[0].projectId").entity(String.class).isEqualTo("42")
            .path("project.members[0].part").entity(String.class).isEqualTo("WEB")
            .path("project.members[0].leader").entity(Boolean.class).isEqualTo(false);

        then(checkPermissionUseCase).should().loadSubject(REQUESTER_ID);
        then(checkPermissionUseCase).should().check(subject, projectReadPermission(PROJECT_ID));
        then(getProjectMemberUseCase).should().listByProjectIds(List.of(PROJECT_ID));
        then(getProjectApplicationFormUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Project.applicationForm은 PROJECT READ 권한을 검사하고 질문과 옵션을 응답한다")
    void Project_applicationForm은_PROJECT_READ_권한을_검사하고_질문과_옵션을_응답한다() {
        SubjectAttributes subject = subject();
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, projectReadPermission(PROJECT_ID))).willReturn(true);
        given(getProjectApplicationFormUseCase.findAllByProjectIds(List.of(PROJECT_ID), REQUESTER_ID))
            .willReturn(Map.of(PROJECT_ID, applicationFormInfo()));

        graphQlTester.document("""
                query {
                  project(id: 42) {
                    applicationForm {
                      applicationFormId
                      title
                      sections {
                        sectionId
                        type
                        questions {
                          questionId
                          type
                          required
                          options {
                            optionId
                            content
                            other
                          }
                        }
                      }
                    }
                  }
                }
                """)
            .execute()
            .path("project.applicationForm.applicationFormId").entity(String.class).isEqualTo("500")
            .path("project.applicationForm.sections[0].sectionId").entity(String.class).isEqualTo("600")
            .path("project.applicationForm.sections[0].type").entity(String.class).isEqualTo("COMMON")
            .path("project.applicationForm.sections[0].questions[0].questionId").entity(String.class).isEqualTo("700")
            .path("project.applicationForm.sections[0].questions[0].required").entity(Boolean.class).isEqualTo(true)
            .path("project.applicationForm.sections[0].questions[0].options[0].optionId").entity(String.class)
            .isEqualTo("800")
            .path("project.applicationForm.sections[0].questions[0].options[0].other").entity(Boolean.class)
            .isEqualTo(false);

        then(checkPermissionUseCase).should().check(subject, projectReadPermission(PROJECT_ID));
        then(getProjectApplicationFormUseCase).should().findAllByProjectIds(List.of(PROJECT_ID), REQUESTER_ID);
    }

    @Test
    @DisplayName("ProjectMember.application 권한이 없으면 지원서 상세를 조회하지 않고 null로 반환한다")
    void ProjectMember_application_권한이_없으면_지원서_상세를_조회하지_않고_null로_반환한다() {
        SubjectAttributes subject = subject();
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, projectReadPermission(PROJECT_ID))).willReturn(true);
        given(checkPermissionUseCase.check(subject, applicationReadPermission(APPLICATION_ID))).willReturn(false);
        given(getProjectMemberUseCase.listByProjectIds(List.of(PROJECT_ID))).willReturn(Map.of(
            PROJECT_ID,
            List.of(projectMemberInfo(APPLICATION_ID))
        ));

        graphQlTester.document("""
                query {
                  project(id: 42) {
                    members {
                      application {
                        applicationId
                      }
                    }
                  }
                }
                """)
            .execute()
            .path("project.members[0].application").valueIsNull();

        then(getProjectApplicationDetailUseCase).should(never()).batchGetDetails(any());
    }

    @Test
    @DisplayName("ProjectMember.application 권한이 있으면 지원서 상세를 조회한다")
    void ProjectMember_application_권한이_있으면_지원서_상세를_조회한다() {
        SubjectAttributes subject = subject();
        given(getProjectUseCase.getById(PROJECT_ID)).willReturn(projectInfo());
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, projectReadPermission(PROJECT_ID))).willReturn(true);
        given(checkPermissionUseCase.check(subject, applicationReadPermission(APPLICATION_ID))).willReturn(true);
        given(getProjectMemberUseCase.listByProjectIds(List.of(PROJECT_ID))).willReturn(Map.of(
            PROJECT_ID,
            List.of(projectMemberInfo(APPLICATION_ID))
        ));
        given(getProjectApplicationDetailUseCase.batchGetDetails(any()))
            .willReturn(Map.of(APPLICATION_ID, applicationDetailInfo()));

        graphQlTester.document("""
                query {
                  project(id: 42) {
                    members {
                      application {
                        applicationId
                        applicantPart
                        status
                        applicant {
                          memberId
                          part
                        }
                      }
                    }
                  }
                }
                """)
            .execute()
            .path("project.members[0].application.applicationId").entity(String.class).isEqualTo("1000")
            .path("project.members[0].application.applicantPart").entity(String.class).isEqualTo("WEB")
            .path("project.members[0].application.status").entity(String.class).isEqualTo("SUBMITTED")
            .path("project.members[0].application.applicant.memberId").entity(String.class).isEqualTo("200")
            .path("project.members[0].application.applicant.part").entity(String.class).isEqualTo("WEB");

        then(getProjectApplicationDetailUseCase).should().batchGetDetails(any());
    }

    private void assertCommonError(
        List<org.springframework.graphql.ResponseError> errors,
        String path,
        CommonErrorCode code
    ) {
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getPath()).isEqualTo(path);
        assertThat(errors.get(0).getExtensions()).containsEntry("code", code.getCode());
    }

    private ProjectInfo projectInfo() {
        return ProjectInfo.builder()
            .id(PROJECT_ID)
            .status(ProjectStatus.IN_PROGRESS)
            .name("Triple")
            .description("프로젝트 설명")
            .thumbnailImageUrl("https://cdn.example.com/thumb.png")
            .logoImageUrl("https://cdn.example.com/logo.png")
            .externalLink("https://example.com")
            .gisuId(1L)
            .chapterId(7L)
            .productOwnerMemberId(100L)
            .coProductOwnerMemberIds(List.of())
            .partQuotas(List.of(ProjectPartQuotaInfo.of(ChallengerPart.WEB, 3L, 1L)))
            .createdAt(Instant.parse("2026-06-01T00:00:00Z"))
            .updatedAt(Instant.parse("2026-06-02T00:00:00Z"))
            .build();
    }

    private ProjectMemberInfo projectMemberInfo(Long applicationId) {
        return ProjectMemberInfo.builder()
            .projectMemberId(10L)
            .projectId(PROJECT_ID)
            .applicationId(applicationId)
            .memberId(200L)
            .part(ChallengerPart.WEB)
            .isLeader(false)
            .description("Backend")
            .decidedAt(Instant.parse("2026-06-03T00:00:00Z"))
            .status(ProjectMemberStatus.ACTIVE)
            .build();
    }

    private ProjectApplicationDetailInfo applicationDetailInfo() {
        return ProjectApplicationDetailInfo.builder()
            .applicationId(APPLICATION_ID)
            .applicantMemberId(200L)
            .applicantPart(ChallengerPart.WEB)
            .matchingRoundId(300L)
            .matchingRoundType(MatchingType.PLAN_DEVELOPER)
            .matchingRoundPhase(MatchingPhase.FIRST)
            .status(ProjectApplicationViewStatus.SUBMITTED)
            .submittedAt(Instant.parse("2026-06-04T00:00:00Z"))
            .statusChangedAt(Instant.parse("2026-06-05T00:00:00Z"))
            .build();
    }

    private ApplicationFormInfo applicationFormInfo() {
        return ApplicationFormInfo.builder()
            .projectId(PROJECT_ID)
            .applicationFormId(500L)
            .title("Triple 지원서")
            .description("지원서 설명")
            .sections(List.of(ApplicationFormInfo.SectionInfo.builder()
                .sectionId(600L)
                .type(FormSectionType.COMMON)
                .allowedParts(Set.of())
                .title("공통")
                .description(null)
                .orderNo(1L)
                .questions(List.of(ApplicationFormInfo.QuestionInfo.builder()
                    .questionId(700L)
                    .type(QuestionType.RADIO)
                    .title("선호")
                    .description(null)
                    .isRequired(true)
                    .orderNo(1L)
                    .options(List.of(ApplicationFormInfo.OptionInfo.builder()
                        .optionId(800L)
                        .content("Spring")
                        .orderNo(1L)
                        .isOther(false)
                        .build()))
                    .build()))
                .build()))
            .build();
    }

    private GetProjectApplicationDetailQuery applicationDetailQuery() {
        return GetProjectApplicationDetailQuery.builder()
            .projectId(PROJECT_ID)
            .applicationId(APPLICATION_ID)
            .requesterMemberId(REQUESTER_ID)
            .build();
    }

    private SubjectAttributes subject() {
        return SubjectAttributes.builder()
            .memberId(REQUESTER_ID)
            .schoolId(1L)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of())
            .build();
    }

    private ResourcePermission projectReadPermission(Long projectId) {
        return ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);
    }

    private ResourcePermission applicationReadPermission(Long applicationId) {
        return ResourcePermission.of(ResourceType.PROJECT_APPLICATION, applicationId, PermissionType.READ);
    }
}
