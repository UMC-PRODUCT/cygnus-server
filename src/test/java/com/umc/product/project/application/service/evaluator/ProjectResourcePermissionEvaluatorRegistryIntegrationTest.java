package com.umc.product.project.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("프로젝트 ResourcePermissionEvaluator 레지스트리")
class ProjectResourcePermissionEvaluatorRegistryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    ApplicationContext applicationContext;

    @Test
    @DisplayName("PROJECT와 PROJECT_APPLICATION evaluator 빈은 각각 정확히 하나다")
    void 프로젝트_리소스별_evaluator_빈은_정확히_하나다() {
        List<ResourcePermissionEvaluator> evaluators = applicationContext
            .getBeansOfType(ResourcePermissionEvaluator.class)
            .values()
            .stream()
            .toList();

        assertThat(evaluators)
            .filteredOn(evaluator -> evaluator.supportedResourceType() == ResourceType.PROJECT)
            .singleElement()
            .isInstanceOf(ProjectPermissionEvaluator.class);
        assertThat(evaluators)
            .filteredOn(evaluator -> evaluator.supportedResourceType() == ResourceType.PROJECT_APPLICATION)
            .singleElement()
            .isInstanceOf(ProjectApplicationPermissionEvaluator.class);
    }
}
