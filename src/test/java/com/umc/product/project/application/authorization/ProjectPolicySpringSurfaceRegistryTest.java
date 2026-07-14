package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurfaceBinding;

class ProjectPolicySpringSurfaceRegistryTest {

    private static final String MUTATION_ENV = "PROJECT_CATALOG_MUTATION";
    private static final String MUTATION_PROFILE = "project-policy-catalog-mutation";

    @Test
    @DisplayName("Spring MVC registry의 Project 호출면과 AOP seam 분류가 카탈로그와 일치한다")
    void springRegistryAndCheckAccessSeamsMatchCatalog() {
        try (GenericWebApplicationContext context = new GenericWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            if (Boolean.parseBoolean(System.getenv(MUTATION_ENV))) {
                context.getEnvironment().setActiveProfiles(MUTATION_PROFILE);
            }
            new AnnotatedBeanDefinitionReader(context).register(WebConfiguration.class);
            ProjectPolicyRuntimeSurfaceDiscovery.restControllers(context.getEnvironment())
                .forEach(controller -> registerController(context, controller));
            context.refresh();

            RequestMappingHandlerMapping handlerMapping = context.getBean(RequestMappingHandlerMapping.class);
            Map<ProjectPolicySurfaceIdentity, HandlerMethod> registered = projectMappings(handlerMapping);
            Set<ProjectPolicySurfaceIdentity> catalogRest = ProjectPolicySurfaceCatalog.surfaces().stream()
                .filter(surface -> surface.type() == ProjectPolicySurfaceType.REST)
                .map(ProjectPolicySurfaceDescriptor::identity)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

            assertThat(registered).hasSize(37);
            assertThat(registered.keySet()).containsExactlyInAnyOrderElementsOf(catalogRest);
            assertThat(registered.values().stream().filter(this::hasCheckAccess)).hasSize(21);
            assertThat(registered.values().stream().filter(handler -> !hasCheckAccess(handler))).hasSize(16);
            assertSurfaceBindingsMatchSpringMappings(registered);
            assertGatesMatchAopSeams(registered);
        }
    }

    private Map<ProjectPolicySurfaceIdentity, HandlerMethod> projectMappings(
        RequestMappingHandlerMapping handlerMapping
    ) {
        Map<ProjectPolicySurfaceIdentity, HandlerMethod> mappings = new LinkedHashMap<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (!ProjectPolicyRuntimeSurfaceDiscovery.isProjectRestController(handler.getBeanType())) {
                return;
            }
            for (RequestMethod requestMethod : mapping.getMethodsCondition().getMethods()) {
                for (String path : mapping.getPatternValues()) {
                    ProjectPolicySurfaceIdentity identity = new ProjectPolicySurfaceIdentity(
                        "rest:" + requestMethod.name() + " " + path,
                        handler.getBeanType().getName() + "#" + handler.getMethod().getName()
                    );
                    mappings.put(identity, handler);
                }
            }
        });
        return Map.copyOf(mappings);
    }

    private void assertSurfaceBindingsMatchSpringMappings(
        Map<ProjectPolicySurfaceIdentity, HandlerMethod> registered
    ) {
        registered.forEach((identity, handler) -> {
            ProjectAuthorizationSurfaceBinding binding =
                handler.getMethodAnnotation(ProjectAuthorizationSurfaceBinding.class);
            assertThat(binding).as(identity.id()).isNotNull();
            assertThat(binding.value().identity()).as(identity.id()).isEqualTo(identity);
        });
    }

    private void assertGatesMatchAopSeams(Map<ProjectPolicySurfaceIdentity, HandlerMethod> registered) {
        Map<ProjectPolicySurfaceIdentity, ProjectPolicySurfaceDescriptor> catalog = new LinkedHashMap<>();
        ProjectPolicySurfaceCatalog.surfaces().stream()
            .filter(surface -> surface.type() == ProjectPolicySurfaceType.REST)
            .forEach(surface -> catalog.put(surface.identity(), surface));

        registered.forEach((identity, handler) -> {
            CheckAccess checkAccess = handler.getMethodAnnotation(CheckAccess.class);
            ProjectPolicyGate expectedGate;
            if (checkAccess == null) {
                expectedGate = identity.id().equals("rest:GET /api/v1/projects/statistics/matchings")
                    ? ProjectPolicyGate.PUBLIC
                    : ProjectPolicyGate.DIRECT;
            } else {
                expectedGate = checkAccess.resourceId().isBlank()
                    ? ProjectPolicyGate.ACTOR
                    : ProjectPolicyGate.RESOURCE;
            }
            assertThat(catalog.get(identity).gate())
                .as(identity.id())
                .isEqualTo(expectedGate);
        });
    }

    private boolean hasCheckAccess(HandlerMethod handler) {
        return handler.hasMethodAnnotation(CheckAccess.class);
    }

    private <T> void registerController(GenericWebApplicationContext context, Class<T> controller) {
        context.registerBean(controller.getName(), controller, () -> instantiate(controller));
    }

    private <T> T instantiate(Class<T> controller) {
        try {
            Constructor<?> constructor = controller.getDeclaredConstructors()[0];
            Object[] arguments = Arrays.stream(constructor.getParameterTypes())
                .map(Mockito::mock)
                .toArray();
            return controller.cast(constructor.newInstance(arguments));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Project controller를 생성할 수 없습니다: " + controller.getName(), exception);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class WebConfiguration {
    }
}
