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
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingDocumentDecisionRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingInterviewQuestionRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.SkipRecruitingInterviewRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.SubmitRecruitingEvaluationRequest;
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
            assertThat(schema.getProperties()).doesNotContainKeys("memberId", "requesterMemberId", "executorMemberId");
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
