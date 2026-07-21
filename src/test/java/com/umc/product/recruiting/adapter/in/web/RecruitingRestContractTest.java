package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.recruiting.adapter.in.web.dto.request.ConfirmRecruitingInterviewScheduleRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.CreateRecruitingRoundRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingDocumentDecisionRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingInterviewQuestionRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.SkipRecruitingInterviewRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.SubmitRecruitingEvaluationRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateRecruitingRoundRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpsertRecruitingApplicationFormRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingEvaluationResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingInterviewScheduleResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingSeasonConfigurationResponse;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.media.Schema;

@DisplayName("Recruiting REST/OpenAPI 계약")
class RecruitingRestContractTest {

    @Test
    @DisplayName("모든 admin controller의 class path는 admin prefix를 사용한다")
    void allAdminControllersUseAdminPrefix() {
        assertThat(restControllers().stream().filter(type -> type.getSimpleName().contains("Admin")).toList())
            .allSatisfy(controller -> assertThat(classPath(controller))
                .startsWith("/api/v1/recruiting/admin"));
    }

    @Test
    @DisplayName("모든 REST handler는 설명이 있는 Operation을 제공한다")
    void allRestHandlersHaveDescribedOperations() {
        for (Class<?> controller : restControllers()) {
            mappedMethods(controller).forEach(method -> {
                Operation operation = method.getAnnotation(Operation.class);
                assertThat(operation).as("%s#%s", controller.getSimpleName(), method.getName()).isNotNull();
                assertThat(operation.description()).isNotBlank();
            });
        }
    }

    @Test
    @DisplayName("Operation ID는 suffix 없이 카테고리별 숫자 대역을 사용한다")
    void operationIdsUseNumericCategoryRanges() {
        List<String> operationIds = restControllers().stream()
            .flatMap(this::mappedMethods)
            .map(method -> method.getAnnotation(Operation.class).operationId())
            .toList();

        assertThat(operationIds)
            .doesNotHaveDuplicates()
            .allMatch(operationId -> operationId.matches(
                "RECRUITING-(PUBLIC|APPLICATION|SCHEDULE|EVALUATION|ADMIN)-\\d{3}"
            ));
        assertThat(operationIds)
            .filteredOn(operationId -> operationId.startsWith("RECRUITING-ADMIN-"))
            .containsExactlyInAnyOrder(
                "RECRUITING-ADMIN-001",
                "RECRUITING-ADMIN-002",
                "RECRUITING-ADMIN-003",
                "RECRUITING-ADMIN-004",
                "RECRUITING-ADMIN-011",
                "RECRUITING-ADMIN-012",
                "RECRUITING-ADMIN-013",
                "RECRUITING-ADMIN-014",
                "RECRUITING-ADMIN-015",
                "RECRUITING-ADMIN-016",
                "RECRUITING-ADMIN-017",
                "RECRUITING-ADMIN-021",
                "RECRUITING-ADMIN-031",
                "RECRUITING-ADMIN-032",
                "RECRUITING-ADMIN-033",
                "RECRUITING-ADMIN-041",
                "RECRUITING-ADMIN-042",
                "RECRUITING-ADMIN-043",
                "RECRUITING-ADMIN-044",
                "RECRUITING-ADMIN-045",
                "RECRUITING-ADMIN-046",
                "RECRUITING-ADMIN-047",
                "RECRUITING-ADMIN-048",
                "RECRUITING-ADMIN-051",
                "RECRUITING-ADMIN-052",
                "RECRUITING-ADMIN-061",
                "RECRUITING-ADMIN-062",
                "RECRUITING-ADMIN-063",
                "RECRUITING-ADMIN-071",
                "RECRUITING-ADMIN-072",
                "RECRUITING-ADMIN-073",
                "RECRUITING-ADMIN-081",
                "RECRUITING-ADMIN-082"
            );
    }

    @Test
    @DisplayName("REST path에는 금지된 legacy credential overlap email endpoint가 없다")
    void pathsExcludeDeferredAndLegacyEndpoints() {
        List<String> paths = restControllers().stream()
            .flatMap(controller -> mappedMethods(controller)
                .map(method -> classPath(controller) + methodPath(method)))
            .toList();

        assertThat(paths).noneMatch(path -> Stream.of(
            "assignment",
            "score",
            "credential",
            "claim",
            "schedule-candidates",
            "interview-guide",
            "applicationNo"
        ).anyMatch(path::contains));
        assertThat(classPath(RecruitingEvaluationController.class)).isEqualTo(
            "/api/v1/recruiting/rounds/{roundId}/applications/{applicationId}/evaluations/{stage}"
        );
    }

    @Test
    @DisplayName("Task10 request response OpenAPI schema는 설명과 actor 비노출 계약을 가진다")
    void task10SchemasAreDescribedAndDoNotExposeActor() {
        List<Class<?>> schemaTypes = List.of(
            SubmitRecruitingEvaluationRequest.class,
            CreateRecruitingRoundRequest.class,
            UpdateRecruitingRoundRequest.class,
            RecruitingInterviewQuestionRequest.class,
            RecruitingDocumentDecisionRequest.class,
            SkipRecruitingInterviewRequest.class,
            UpsertRecruitingApplicationFormRequest.class,
            ConfirmRecruitingInterviewScheduleRequest.class,
            RecruitingEvaluationResponse.class,
            RecruitingInterviewScheduleResponse.class,
            RecruitingSeasonConfigurationResponse.class
        );

        for (Class<?> schemaType : schemaTypes) {
            Map<String, Schema> schemas = ModelConverters.getInstance().read(schemaType);
            Schema<?> schema = schemas.get(schemaType.getSimpleName());
            assertThat(schema).as(schemaType.getSimpleName()).isNotNull();
            assertThat(schema.getDescription()).isNotBlank();
            assertThat(schema.getProperties()).doesNotContainKeys(
                "memberId",
                "requesterMemberId",
                "executorMemberId",
                "availabilityFormResponseId",
                "interviewConfigurationValid"
            );
        }
    }

    private Stream<Method> mappedMethods(Class<?> controller) {
        return Stream.of(controller.getDeclaredMethods())
            .filter(method -> AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) != null);
    }

    private List<? extends Class<?>> restControllers() {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        return scanner.findCandidateComponents("com.umc.product.recruiting.adapter.in.web")
            .stream()
            .map(definition -> (Class<?>) resolveClass(definition.getBeanClassName()))
            .toList();
    }

    private Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("REST controller class를 불러올 수 없습니다: " + className, exception);
        }
    }

    private String classPath(Class<?> controller) {
        return List.of(controller.getAnnotation(RequestMapping.class).value()).getFirst();
    }

    private String methodPath(Method method) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        return mapping.value().length == 0 ? "" : mapping.value()[0];
    }
}
