package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.project.application.authorization.ProjectPolicySurfaceCatalog;

class ProjectAuthorizationSurfaceBindingTest {

    @Test
    @DisplayName("Project의 46개 외부 진입점은 closed enum surface와 양방향으로 정확히 연결된다")
    void bindsExactExternalSurfaceSet() {
        List<BoundMethod> bindings = handlerClasses().stream()
            .flatMap(handler -> Arrays.stream(handler.getDeclaredMethods()))
            .flatMap(method -> binding(method).stream())
            .toList();
        Map<ProjectAuthorizationSurface, BoundMethod> bySurface = bindings.stream()
            .collect(Collectors.toUnmodifiableMap(BoundMethod::surface, Function.identity()));

        assertThat(bindings).hasSize(46);
        assertThat(bySurface.keySet()).containsExactlyInAnyOrder(ProjectAuthorizationSurface.values());
        bySurface.forEach((surface, binding) ->
            assertThat(binding.handler()).as(surface.id()).isEqualTo(surface.identity().handler()));
        assertThat(ProjectPolicySurfaceCatalog.identities())
            .containsExactlyInAnyOrderElementsOf(
                bySurface.keySet().stream()
                    .map(ProjectAuthorizationSurface::identity)
                    .collect(Collectors.toUnmodifiableSet()));
    }

    private Set<Class<?>> handlerClasses() {
        return ProjectPolicySurfaceCatalog.identities().stream()
            .map(identity -> identity.handler().substring(0, identity.handler().indexOf('#')))
            .map(this::loadClass)
            .collect(Collectors.toUnmodifiableSet());
    }

    private java.util.Optional<BoundMethod> binding(Method method) {
        return java.util.Optional.ofNullable(method.getDeclaredAnnotation(ProjectAuthorizationSurfaceBinding.class))
            .map(annotation -> new BoundMethod(annotation.value(), handler(method)));
    }

    private Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Project handler를 읽을 수 없습니다: " + name, exception);
        }
    }

    private String handler(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }

    private record BoundMethod(ProjectAuthorizationSurface surface, String handler) {
    }
}
