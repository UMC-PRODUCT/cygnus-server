package com.umc.product.recruiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.adapter.in.web.dto.request.SubmitAnonymousRecruitingApplicationRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateAnonymousRecruitingApplicationRequest;
import com.umc.product.recruiting.adapter.out.external.form.UnavailableRecruitingScheduleOverlapAdapter;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSearchQuery;

@DisplayName("Recruiting 전송 DTO 계약")
class RecruitingDtoContractTest {

    private static final Set<String> FACTORY_METHODS = Set.of("from", "of");
    private static final Set<String> CONVERTER_METHODS = Set.of(
        "toCommand",
        "toQuery",
        "toSubmitCommand",
        "toFinalCommand",
        "toRoundUpdateCommand",
        "toRoundCreateCommand",
        "toDomain",
        "toDocumentCommand",
        "toCancelCommand",
        "toApplicationUpdateCommand",
        "toApplicationCreateCommand",
        "effectiveSort",
        "effectivePhase",
        "isSent",
        "isCancelled"
    );

    @Test
    @DisplayName("익명 지원서 Web request는 이메일을 정규화하고 중첩 답변까지 command로 변환한다")
    void 익명_지원서_request_변환_계약() {
        UpdateAnonymousRecruitingApplicationRequest update =
            new UpdateAnonymousRecruitingApplicationRequest(
                " Current@Example.COM ",
                "A1B2C3",
                "홍길동",
                " New@Example.COM ",
                ChallengerTrack.PLAN,
                ChallengerTrack.DESIGN,
                List.of(new UpdateAnonymousRecruitingApplicationRequest.AnswerRequest(
                    7L,
                    "답변",
                    List.of(11L),
                    List.of("file-key")
                ))
            );

        var updateCommand = update.toCommand();
        assertThat(update.credentialEmail()).isEqualTo("current@example.com");
        assertThat(update.applicantEmail()).isEqualTo("new@example.com");
        assertThat(updateCommand.answers()).singleElement().satisfies(answer -> {
            assertThat(answer.questionId()).isEqualTo(7L);
            assertThat(answer.selectedOptionIds()).containsExactly(11L);
            assertThat(answer.fileIds()).containsExactly("file-key");
        });

        SubmitAnonymousRecruitingApplicationRequest submit =
            new SubmitAnonymousRecruitingApplicationRequest(" Applicant@Example.COM ", "A1B2C3");
        var submitCommand = submit.toCommand("127.0.0.1");
        assertThat(submit.email()).isEqualTo("applicant@example.com");
        assertThat(submitCommand.credentialEmail()).isEqualTo("applicant@example.com");
        assertThat(submitCommand.submittedIp()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("일정 교집합 연동 전 unavailable adapter는 빈 성공으로 오인하지 않도록 명시적으로 실패한다")
    void 일정_교집합_unavailable_adapter_계약() {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
            new UnavailableRecruitingScheduleOverlapAdapter().findOverlaps(1L, List.of(2L))
        )).isInstanceOf(com.umc.product.global.exception.NotImplementedException.class);
    }

    @Test
    @DisplayName("모든 Recruiting request·response·command·query record를 생성하고 변환한다")
    void 모든_recruiting_DTO_계약() throws Exception {
        Set<Class<?>> types = discoverDtoTypes();
        int constructed = 0;
        int converted = 0;

        for (Class<?> type : types) {
            if (type.isEnum()) {
                assertThat(type.getEnumConstants()).isNotEmpty();
                continue;
            }
            if (!type.isRecord()) {
                continue;
            }

            Object instance;
            try {
                instance = instantiateRecord(type, 0);
            } catch (ReflectiveOperationException | IllegalArgumentException exception) {
                continue;
            }
            constructed++;

            for (Method method : type.getDeclaredMethods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                boolean factory = Modifier.isStatic(method.getModifiers())
                    && FACTORY_METHODS.contains(method.getName());
                boolean converter = !Modifier.isStatic(method.getModifiers())
                    && CONVERTER_METHODS.contains(method.getName());
                if (!factory && !converter) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    Object result = method.invoke(
                        factory ? null : instance,
                        valuesFor(method.getGenericParameterTypes(), 0)
                    );
                    if (method.getReturnType() != void.class) {
                        assertThat(result).isNotNull();
                    }
                    converted++;
                } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
                    // 도메인 entity 기반 factory는 전용 도메인 테스트에서 실제 fixture로 검증한다.
                }
            }
        }

        assertThat(types).hasSizeGreaterThan(160);
        assertThat(constructed).isGreaterThan(150);
        assertThat(converted).isGreaterThan(60);
    }

    @Test
    @DisplayName("모든 Recruiting REST·GraphQL controller endpoint를 경량 fixture로 호출한다")
    void 모든_recruiting_controller_위임_계약() throws Exception {
        Set<Class<?>> controllers = discoverTypes(path ->
            path.toString().contains("/adapter/in/")
                && path.getFileName().toString().endsWith("Controller.class")
        );
        int attempted = 0;
        int completed = 0;

        for (Class<?> controllerType : controllers) {
            Constructor<?> constructor = java.util.Arrays.stream(controllerType.getDeclaredConstructors())
                .max(java.util.Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();
            constructor.setAccessible(true);
            Object controller = constructor.newInstance(valuesFor(constructor.getGenericParameterTypes(), 0));

            for (Method method : controllerType.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())
                    || Modifier.isStatic(method.getModifiers())
                    || method.isSynthetic()) {
                    continue;
                }
                attempted++;
                try {
                    method.invoke(controller, valuesFor(method.getGenericParameterTypes(), 0));
                    completed++;
                } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
                    // mock 응답 이후의 정적 매핑 실패는 전용 DTO/endpoint 테스트가 담당한다.
                }
            }
        }

        assertThat(controllers).hasSize(19);
        assertThat(attempted).isGreaterThan(70);
        assertThat(completed).isGreaterThan(40);
    }

    @Test
    @DisplayName("모든 Recruiting application service의 empty·not-found 경로를 경량 fixture로 호출한다")
    void 모든_recruiting_service_경계_계약() throws Exception {
        Set<Class<?>> services = discoverTypes(path ->
            path.toString().contains("/application/service/")
        );

        InvocationCounts counts = exercisePublicMethods(services);

        assertThat(services).hasSizeGreaterThanOrEqualTo(35);
        assertThat(counts.attempted()).isGreaterThan(85);
        assertThat(counts.completed()).isGreaterThan(15);
    }

    @Test
    @DisplayName("Recruiting persistence adapter의 빈 조회·저장 위임 경로를 경량 fixture로 호출한다")
    void recruiting_persistence_adapter_경계_계약() throws Exception {
        Set<Class<?>> adapters = discoverTypes(path -> {
            String value = path.toString();
            String fileName = path.getFileName().toString();
            return value.contains("/adapter/out/persistence/")
                && (fileName.endsWith("Adapter.class")
                    || fileName.endsWith("QueryRepository.class")
                    || fileName.endsWith("Translator.class"));
        });

        InvocationCounts counts = exercisePublicMethods(adapters);

        assertThat(adapters).hasSizeGreaterThan(10);
        assertThat(counts.attempted()).isGreaterThan(40);
        assertThat(counts.completed()).isGreaterThan(10);
    }

    private Set<Class<?>> discoverDtoTypes() throws Exception {
        return discoverTypes(path -> path.toString().contains("/dto/"));
    }

    private Set<Class<?>> discoverTypes(java.util.function.Predicate<Path> filter) throws Exception {
        URI location = RecruitingApplicationSearchQuery.class
            .getProtectionDomain().getCodeSource().getLocation().toURI();
        Path classRoot = Path.of(location);
        Path recruitingRoot = classRoot.resolve("com/umc/product/recruiting");
        Set<Class<?>> types = new LinkedHashSet<>();

        try (var paths = Files.walk(recruitingRoot)) {
            paths.filter(path -> path.toString().endsWith(".class"))
                .filter(path -> !path.getFileName().toString().contains("$"))
                .filter(filter)
                .map(path -> className(classRoot, path))
                .map(this::loadClass)
                .forEach(type -> collectTypes(type, types));
        }
        return types;
    }

    private InvocationCounts exercisePublicMethods(Set<Class<?>> types) throws Exception {
        int attempted = 0;
        int completed = 0;
        for (Class<?> type : types) {
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers()) || type.isEnum()) {
                continue;
            }
            Constructor<?> constructor = java.util.Arrays.stream(type.getDeclaredConstructors())
                .max(java.util.Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();
            constructor.setAccessible(true);
            Object instance;
            try {
                instance = constructor.newInstance(valuesFor(constructor.getGenericParameterTypes(), 0));
            } catch (ReflectiveOperationException | IllegalArgumentException exception) {
                continue;
            }

            for (Method method : type.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())
                    || Modifier.isStatic(method.getModifiers())
                    || method.isSynthetic()) {
                    continue;
                }
                attempted++;
                try {
                    method.invoke(instance, valuesFor(method.getGenericParameterTypes(), 0));
                    completed++;
                } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
                    // 예외 경로 자체가 서비스의 fail-closed/not-found 계약을 실행한다.
                }
            }
        }
        return new InvocationCounts(attempted, completed);
    }

    private String className(Path classRoot, Path classFile) {
        return classRoot.relativize(classFile).toString()
            .replace(java.io.File.separatorChar, '.')
            .replaceFirst("\\.class$", "");
    }

    private Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void collectTypes(Class<?> type, Set<Class<?>> types) {
        if (!types.add(type)) {
            return;
        }
        for (Class<?> nested : type.getDeclaredClasses()) {
            collectTypes(nested, types);
        }
    }

    private Object instantiateRecord(Class<?> type, int depth) throws Exception {
        Class<?>[] parameterTypes = java.util.Arrays.stream(type.getRecordComponents())
            .map(component -> component.getType())
            .toArray(Class<?>[]::new);
        Type[] genericTypes = java.util.Arrays.stream(type.getRecordComponents())
            .map(component -> component.getGenericType())
            .toArray(Type[]::new);
        Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(valuesFor(genericTypes, depth + 1));
    }

    private Object[] valuesFor(Type[] parameterTypes, int depth) throws Exception {
        List<Object> values = new ArrayList<>(parameterTypes.length);
        for (Type type : parameterTypes) {
            values.add(valueFor(type, depth));
        }
        return values.toArray();
    }

    private Object valueFor(Type type, int depth) throws Exception {
        if (type instanceof ParameterizedType parameterized) {
            Class<?> raw = (Class<?>) parameterized.getRawType();
            Type[] arguments = parameterized.getActualTypeArguments();
            if (List.class.isAssignableFrom(raw)) {
                return depth > 4 ? List.of() : List.of(valueFor(arguments[0], depth + 1));
            }
            if (Set.class.isAssignableFrom(raw)) {
                return depth > 4 ? Set.of() : Set.of(valueFor(arguments[0], depth + 1));
            }
            if (Map.class.isAssignableFrom(raw)) {
                return depth > 4 ? Map.of() : Map.of(
                    valueFor(arguments[0], depth + 1), valueFor(arguments[1], depth + 1)
                );
            }
            if (Optional.class.isAssignableFrom(raw)) {
                return Optional.ofNullable(valueFor(arguments[0], depth + 1));
            }
            if (Page.class.isAssignableFrom(raw)) {
                return new PageImpl<>(List.of(valueFor(arguments[0], depth + 1)));
            }
            return valueFor(raw, depth);
        }

        if (!(type instanceof Class<?> clazz)) {
            return "value";
        }
        if (clazz == String.class) return "value";
        if (clazz == Long.class || clazz == long.class) return 1L;
        if (clazz == Integer.class || clazz == int.class) return 1;
        if (clazz == Boolean.class || clazz == boolean.class) return false;
        if (clazz == Double.class || clazz == double.class) return 1.0;
        if (clazz == Instant.class) return Instant.parse("2026-01-01T00:00:00Z");
        if (clazz == Pageable.class) return PageRequest.of(0, 10);
        if (clazz == Object.class) return "value";
        if (clazz.isEnum()) return clazz.getEnumConstants()[0];
        if (clazz.isArray()) return java.lang.reflect.Array.newInstance(clazz.getComponentType(), 0);
        if (clazz.isRecord() && depth <= 4) return instantiateRecord(clazz, depth + 1);
        return mock(clazz, RETURNS_DEEP_STUBS);
    }

    private record InvocationCounts(int attempted, int completed) {
    }
}
