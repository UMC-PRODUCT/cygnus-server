package com.umc.product.feedback.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateAdminUseCase;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateDetailInfo;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateSummaryInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.feedback.domain.exception.FeedbackDomainException;
import com.umc.product.feedback.domain.exception.FeedbackErrorCode;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.security.MemberPrincipal;

@GraphQlTest(UserFeedbackTemplateGraphQlController.class)
@Import({GraphQlRuntimeWiringConfig.class, GraphQlExceptionAdvice.class})
@DisplayName("UserFeedbackTemplateGraphQlController")
class UserFeedbackTemplateGraphQlControllerTest {

    private static final Long REQUESTER_ID = 999L;
    private static final Long TEMPLATE_ID = 42L;
    private static final Long INACTIVE_TEMPLATE_ID = 43L;
    private static final Instant CREATED_AT = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-02T00:00:00Z");

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetUserFeedbackTemplateAdminUseCase getUserFeedbackTemplateAdminUseCase;

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
    @DisplayName("목록 입력이 null이면 세 필터를 모두 null로 전달하고 권한을 먼저 검사한다")
    void 목록_입력이_null이면_세_필터를_모두_null로_전달하고_권한을_먼저_검사한다() {
        given(getUserFeedbackTemplateAdminUseCase.listTemplates(null, null, null))
            .willReturn(List.of(summaryInfo()));

        graphQlTester.document("""
                query {
                  userFeedbackTemplates {
                    templateId
                    context
                    targetType
                    active
                    formId
                    title
                    createdAt
                    updatedAt
                  }
                }
                """)
            .execute()
            .path("userFeedbackTemplates[0].templateId").entity(String.class).isEqualTo("42")
            .path("userFeedbackTemplates[0].context").entity(String.class).isEqualTo("APPLICATION_SUBMITTED")
            .path("userFeedbackTemplates[0].targetType").entity(String.class).isEqualTo("ADMIN")
            .path("userFeedbackTemplates[0].active").entity(Boolean.class).isEqualTo(true)
            .path("userFeedbackTemplates[0].formId").entity(String.class).isEqualTo("100")
            .path("userFeedbackTemplates[0].title").entity(String.class).isEqualTo("피드백 폼")
            .path("userFeedbackTemplates[0].createdAt").entity(String.class).isEqualTo(CREATED_AT.toString())
            .path("userFeedbackTemplates[0].updatedAt").entity(String.class).isEqualTo(UPDATED_AT.toString());

        var order = inOrder(checkPermissionUseCase, getUserFeedbackTemplateAdminUseCase);
        order.verify(checkPermissionUseCase).checkOrThrow(REQUESTER_ID, feedbackReadPermission());
        order.verify(getUserFeedbackTemplateAdminUseCase).listTemplates(null, null, null);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filteredListInputs")
    @DisplayName("목록 입력의 각 필터 조합을 application usecase에 그대로 전달한다")
    void 목록_입력의_각_필터_조합을_application_usecase에_그대로_전달한다(
        String input,
        UserFeedbackContext context,
        UserFeedbackTargetType targetType,
        Boolean active
    ) {
        given(getUserFeedbackTemplateAdminUseCase.listTemplates(context, targetType, active))
            .willReturn(List.of(summaryInfo()));

        graphQlTester.document("""
                query {
                  userFeedbackTemplates(input: { %s }) {
                    templateId
                  }
                }
                """.formatted(input))
            .execute()
            .path("userFeedbackTemplates[0].templateId").entity(String.class).isEqualTo("42");

        var order = inOrder(checkPermissionUseCase, getUserFeedbackTemplateAdminUseCase);
        order.verify(checkPermissionUseCase).checkOrThrow(REQUESTER_ID, feedbackReadPermission());
        order.verify(getUserFeedbackTemplateAdminUseCase).listTemplates(context, targetType, active);
    }

    private static Stream<Arguments> filteredListInputs() {
        return Stream.of(
            Arguments.of(
                "context: APPLICATION_SUBMITTED",
                UserFeedbackContext.APPLICATION_SUBMITTED,
                null,
                null
            ),
            Arguments.of(
                "targetType: NEW_CHALLENGER",
                null,
                UserFeedbackTargetType.NEW_CHALLENGER,
                null
            ),
            Arguments.of("active: true", null, null, true),
            Arguments.of(
                "context: MATCHING_COMPLETED, targetType: EXPERIENCED_CHALLENGER",
                UserFeedbackContext.MATCHING_COMPLETED,
                UserFeedbackTargetType.EXPERIENCED_CHALLENGER,
                null
            ),
            Arguments.of(
                "context: APPLICATION_MONITORING, active: false",
                UserFeedbackContext.APPLICATION_MONITORING,
                null,
                false
            ),
            Arguments.of(
                "targetType: ADMIN, active: true",
                null,
                UserFeedbackTargetType.ADMIN,
                true
            ),
            Arguments.of(
                "context: APPLICATION_SUBMITTED, targetType: ADMIN, active: false",
                UserFeedbackContext.APPLICATION_SUBMITTED,
                UserFeedbackTargetType.ADMIN,
                false
            )
        );
    }

    @Test
    @DisplayName("상세 조회는 권한을 먼저 검사하고 폼 메타데이터와 섹션·질문·옵션을 정확히 응답한다")
    void 상세_조회는_권한을_먼저_검사하고_폼_메타데이터와_섹션_질문_옵션을_정확히_응답한다() {
        given(getUserFeedbackTemplateAdminUseCase.getTemplate(TEMPLATE_ID)).willReturn(detailInfo(true));

        graphQlTester.document("""
                query {
                  userFeedbackTemplate(id: 42) {
                    templateId
                    context
                    targetType
                    active
                    createdAt
                    updatedAt
                    form {
                      formId
                      title
                      description
                      status
                      anonymous
                      allowDuplicateResponses
                      sections {
                        sectionId
                        title
                        description
                        orderNo
                        questions {
                          questionId
                          type
                          title
                          description
                          required
                          orderNo
                          options {
                            optionId
                            content
                            orderNo
                            other
                          }
                        }
                      }
                    }
                  }
                }
                """)
            .execute()
            .path("userFeedbackTemplate.templateId").entity(String.class).isEqualTo("42")
            .path("userFeedbackTemplate.context").entity(String.class).isEqualTo("APPLICATION_SUBMITTED")
            .path("userFeedbackTemplate.targetType").entity(String.class).isEqualTo("ADMIN")
            .path("userFeedbackTemplate.active").entity(Boolean.class).isEqualTo(true)
            .path("userFeedbackTemplate.createdAt").entity(String.class).isEqualTo(CREATED_AT.toString())
            .path("userFeedbackTemplate.updatedAt").entity(String.class).isEqualTo(UPDATED_AT.toString())
            .path("userFeedbackTemplate.form.formId").entity(String.class).isEqualTo("100")
            .path("userFeedbackTemplate.form.title").entity(String.class).isEqualTo("피드백 폼")
            .path("userFeedbackTemplate.form.description").entity(String.class).isEqualTo("폼 설명")
            .path("userFeedbackTemplate.form.status").entity(String.class).isEqualTo("PUBLISHED")
            .path("userFeedbackTemplate.form.anonymous").entity(Boolean.class).isEqualTo(true)
            .path("userFeedbackTemplate.form.allowDuplicateResponses").entity(Boolean.class).isEqualTo(false)
            .path("userFeedbackTemplate.form.sections[0].sectionId").entity(String.class).isEqualTo("200")
            .path("userFeedbackTemplate.form.sections[0].title").entity(String.class).isEqualTo("공통 질문")
            .path("userFeedbackTemplate.form.sections[0].description").entity(String.class).isEqualTo("섹션 설명")
            .path("userFeedbackTemplate.form.sections[0].orderNo").entity(Integer.class).isEqualTo(1)
            .path("userFeedbackTemplate.form.sections[0].questions[0].questionId")
            .entity(String.class).isEqualTo("300")
            .path("userFeedbackTemplate.form.sections[0].questions[0].type")
            .entity(String.class).isEqualTo("RADIO")
            .path("userFeedbackTemplate.form.sections[0].questions[0].title")
            .entity(String.class).isEqualTo("선호 색상")
            .path("userFeedbackTemplate.form.sections[0].questions[0].description")
            .entity(String.class).isEqualTo("하나를 선택하세요")
            .path("userFeedbackTemplate.form.sections[0].questions[0].required")
            .entity(Boolean.class).isEqualTo(true)
            .path("userFeedbackTemplate.form.sections[0].questions[0].orderNo")
            .entity(Integer.class).isEqualTo(1)
            .path("userFeedbackTemplate.form.sections[0].questions[0].options[0].optionId")
            .entity(String.class).isEqualTo("400")
            .path("userFeedbackTemplate.form.sections[0].questions[0].options[0].content")
            .entity(String.class).isEqualTo("파랑")
            .path("userFeedbackTemplate.form.sections[0].questions[0].options[0].orderNo")
            .entity(Integer.class).isEqualTo(1)
            .path("userFeedbackTemplate.form.sections[0].questions[0].options[0].other")
            .entity(Boolean.class).isEqualTo(false);

        var order = inOrder(checkPermissionUseCase, getUserFeedbackTemplateAdminUseCase);
        order.verify(checkPermissionUseCase).checkOrThrow(REQUESTER_ID, feedbackReadPermission());
        order.verify(getUserFeedbackTemplateAdminUseCase).getTemplate(TEMPLATE_ID);
    }

    @Test
    @DisplayName("비활성 템플릿 상세 조회는 FEEDBACK READ 권한으로 계속 읽을 수 있다")
    void 비활성_템플릿_상세_조회는_FEEDBACK_READ_권한으로_계속_읽을_수_있다() {
        given(getUserFeedbackTemplateAdminUseCase.getTemplate(INACTIVE_TEMPLATE_ID))
            .willReturn(detailInfo(INACTIVE_TEMPLATE_ID, false));

        graphQlTester.document("""
                query {
                  userFeedbackTemplate(id: 43) {
                    templateId
                    active
                    form {
                      formId
                    }
                  }
                }
                """)
            .execute()
            .path("userFeedbackTemplate.templateId").entity(String.class).isEqualTo("43")
            .path("userFeedbackTemplate.active").entity(Boolean.class).isEqualTo(false)
            .path("userFeedbackTemplate.form.formId").entity(String.class).isEqualTo("100");

        var order = inOrder(checkPermissionUseCase, getUserFeedbackTemplateAdminUseCase);
        order.verify(checkPermissionUseCase).checkOrThrow(REQUESTER_ID, feedbackReadPermission());
        order.verify(getUserFeedbackTemplateAdminUseCase).getTemplate(INACTIVE_TEMPLATE_ID);
    }

    @Test
    @DisplayName("로그인하지 않은 목록 조회는 권한과 admin usecase보다 먼저 거부한다")
    void 로그인하지_않은_목록_조회는_권한과_admin_usecase보다_먼저_거부한다() {
        SecurityContextHolder.clearContext();

        graphQlTester.document("""
                query {
                  userFeedbackTemplates {
                    templateId
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertError(errors, "userFeedbackTemplates", ErrorType.FORBIDDEN,
                "COMMON-403", 403));

        then(checkPermissionUseCase).shouldHaveNoInteractions();
        then(getUserFeedbackTemplateAdminUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("로그인하지 않은 상세 조회는 권한과 admin usecase보다 먼저 거부한다")
    void 로그인하지_않은_상세_조회는_권한과_admin_usecase보다_먼저_거부한다() {
        SecurityContextHolder.clearContext();

        graphQlTester.document("""
                query {
                  userFeedbackTemplate(id: 42) {
                    templateId
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertError(errors, "userFeedbackTemplate", ErrorType.FORBIDDEN,
                "COMMON-403", 403));

        then(checkPermissionUseCase).shouldHaveNoInteractions();
        then(getUserFeedbackTemplateAdminUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("MemberPrincipal이 아닌 인증 주체의 목록 조회는 거부한다")
    void MemberPrincipal이_아닌_인증_주체의_목록_조회는_거부한다() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("invalid-principal", null, List.of())
        );

        graphQlTester.document("""
                query {
                  userFeedbackTemplates {
                    templateId
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertError(errors, "userFeedbackTemplates", ErrorType.FORBIDDEN,
                "COMMON-403", 403));

        then(checkPermissionUseCase).shouldHaveNoInteractions();
        then(getUserFeedbackTemplateAdminUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("권한이 없는 로그인 사용자의 목록 조회는 admin usecase를 호출하지 않는다")
    void 권한이_없는_로그인_사용자의_목록_조회는_admin_usecase를_호출하지_않는다() {
        willThrow(new AccessDeniedException("권한이 없습니다."))
            .given(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, feedbackReadPermission());

        graphQlTester.document("""
                query {
                  userFeedbackTemplates {
                    templateId
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertError(errors, "userFeedbackTemplates", ErrorType.FORBIDDEN,
                "COMMON-403", 403));

        then(getUserFeedbackTemplateAdminUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("권한이 없는 로그인 사용자의 상세 조회는 admin usecase를 호출하지 않는다")
    void 권한이_없는_로그인_사용자의_상세_조회는_admin_usecase를_호출하지_않는다() {
        willThrow(new AccessDeniedException("권한이 없습니다."))
            .given(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, feedbackReadPermission());

        graphQlTester.document("""
                query {
                  userFeedbackTemplate(id: 42) {
                    templateId
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertError(errors, "userFeedbackTemplate", ErrorType.FORBIDDEN,
                "COMMON-403", 403));

        then(getUserFeedbackTemplateAdminUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("없는 상세 템플릿은 null data와 FEEDBACK-0001 NOT_FOUND 오류를 반환한다")
    void 없는_상세_템플릿은_null_data와_FEEDBACK_0001_NOT_FOUND_오류를_반환한다() {
        given(getUserFeedbackTemplateAdminUseCase.getTemplate(TEMPLATE_ID))
            .willThrow(new FeedbackDomainException(FeedbackErrorCode.USER_FEEDBACK_TEMPLATE_NOT_FOUND));

        GraphQlTester.Response response = graphQlTester.document("""
                query {
                  userFeedbackTemplate(id: 42) {
                    templateId
                  }
                }
                """)
            .execute();
        response
            .errors()
            .satisfy(errors -> assertError(errors, "userFeedbackTemplate", ErrorType.NOT_FOUND,
                "FEEDBACK-0001", 404));
        response.path("userFeedbackTemplate").valueIsNull();

        var order = inOrder(checkPermissionUseCase, getUserFeedbackTemplateAdminUseCase);
        order.verify(checkPermissionUseCase).checkOrThrow(REQUESTER_ID, feedbackReadPermission());
        order.verify(getUserFeedbackTemplateAdminUseCase).getTemplate(TEMPLATE_ID);
    }

    private void assertError(
        List<ResponseError> errors,
        String path,
        ErrorType errorType,
        String code,
        int httpStatus
    ) {
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getPath()).isEqualTo(path);
        assertThat(errors.get(0).getErrorType()).isEqualTo(errorType);
        assertThat(errors.get(0).getExtensions())
            .containsEntry("code", code)
            .containsEntry("httpStatus", httpStatus);
    }

    private ResourcePermission feedbackReadPermission() {
        return ResourcePermission.ofType(ResourceType.FEEDBACK, PermissionType.READ);
    }

    private UserFeedbackTemplateSummaryInfo summaryInfo() {
        return UserFeedbackTemplateSummaryInfo.builder()
            .templateId(TEMPLATE_ID)
            .context(UserFeedbackContext.APPLICATION_SUBMITTED)
            .targetType(UserFeedbackTargetType.ADMIN)
            .isActive(true)
            .formId(100L)
            .title("피드백 폼")
            .createdAt(CREATED_AT)
            .updatedAt(UPDATED_AT)
            .build();
    }

    private UserFeedbackTemplateDetailInfo detailInfo(boolean active) {
        return detailInfo(TEMPLATE_ID, active);
    }

    private UserFeedbackTemplateDetailInfo detailInfo(Long templateId, boolean active) {
        return UserFeedbackTemplateDetailInfo.builder()
            .templateId(templateId)
            .context(UserFeedbackContext.APPLICATION_SUBMITTED)
            .targetType(UserFeedbackTargetType.ADMIN)
            .isActive(active)
            .form(formWithStructure())
            .createdAt(CREATED_AT)
            .updatedAt(UPDATED_AT)
            .build();
    }

    private FormWithStructureInfo formWithStructure() {
        return FormWithStructureInfo.builder()
            .formId(100L)
            .title("피드백 폼")
            .description("폼 설명")
            .status(FormStatus.PUBLISHED)
            .isAnonymous(true)
            .allowDuplicateResponses(false)
            .sections(List.of(
                FormWithStructureInfo.SectionWithQuestions.builder()
                    .sectionId(200L)
                    .title("공통 질문")
                    .description("섹션 설명")
                    .orderNo(1L)
                    .questions(List.of(
                        FormWithStructureInfo.QuestionWithOptions.builder()
                            .questionId(300L)
                            .type(QuestionType.RADIO)
                            .title("선호 색상")
                            .description("하나를 선택하세요")
                            .isRequired(true)
                            .orderNo(1L)
                            .options(List.of(
                                FormWithStructureInfo.Option.builder()
                                    .optionId(400L)
                                    .content("파랑")
                                    .orderNo(1L)
                                    .isOther(false)
                                    .build()
                            ))
                            .build()
                    ))
                    .build()
            ))
            .build();
    }
}
