package com.umc.product.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.test.adapter.in.web.dto.CertificatePdfPreviewRequest;

@DisplayName("테스트 seed 전송 DTO 계약")
class SeedDtoContractTest {

    private static final List<String> ROOT_TYPES = List.of(
        "com.umc.product.test.dto.TestAopAlarmResponse",
        "com.umc.product.test.dto.FcmTestSendRequest",
        "com.umc.product.test.adapter.in.web.dto.CertificatePdfPreviewRequest",
        "com.umc.product.test.adapter.in.web.dto.CreateSeedChallengerRequest",
        "com.umc.product.test.adapter.in.web.dto.CreateSeedChallengerResponse",
        "com.umc.product.test.adapter.in.web.dto.CreateSeedChallengerRoleRequest",
        "com.umc.product.test.adapter.in.web.dto.CreateSeedChallengerRoleResponse",
        "com.umc.product.test.adapter.in.web.dto.CreateSeedMemberRequest",
        "com.umc.product.test.adapter.in.web.dto.CreateSeedMemberResponse",
        "com.umc.product.test.adapter.in.web.dto.DeleteSeedProjectDataRequest",
        "com.umc.product.test.adapter.in.web.dto.DeleteSeedProjectDataResponse",
        "com.umc.product.test.adapter.in.web.dto.SeedChallengersRequest",
        "com.umc.product.test.adapter.in.web.dto.SeedChallengersResponse",
        "com.umc.product.test.adapter.in.web.dto.SeedCurriculumRequest",
        "com.umc.product.test.adapter.in.web.dto.SeedCurriculumResponse",
        "com.umc.product.test.adapter.in.web.dto.SeedMembersRequest",
        "com.umc.product.test.adapter.in.web.dto.SeedMembersResponse",
        "com.umc.product.test.adapter.in.web.dto.SeedNoticeRequest",
        "com.umc.product.test.adapter.in.web.dto.SeedNoticeResponse",
        "com.umc.product.test.adapter.in.web.dto.SeedProjectApplicationsRequest",
        "com.umc.product.test.adapter.in.web.dto.SeedProjectApplicationsResponse",
        "com.umc.product.test.adapter.in.web.dto.SeedProjectScenariosRequest",
        "com.umc.product.test.adapter.in.web.dto.SeedProjectScenariosResponse",
        "com.umc.product.test.adapter.in.web.dto.SeedProjectsRequest",
        "com.umc.product.test.adapter.in.web.dto.SeedProjectsResponse",
        "com.umc.product.test.application.port.out.dto.ProjectDataDeletionCounts",
        "com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerCommand",
        "com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerResult",
        "com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerRoleCommand",
        "com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerRoleResult",
        "com.umc.product.test.application.port.in.command.dto.CreateSeedMemberCommand",
        "com.umc.product.test.application.port.in.command.dto.CreateSeedMemberResult",
        "com.umc.product.test.application.port.in.command.dto.DeleteSeedProjectDataCommand",
        "com.umc.product.test.application.port.in.command.dto.DeleteSeedProjectDataResult",
        "com.umc.product.test.application.port.in.command.dto.SeedChallengersCommand",
        "com.umc.product.test.application.port.in.command.dto.SeedChallengersResult",
        "com.umc.product.test.application.port.in.command.dto.SeedCurriculumCommand",
        "com.umc.product.test.application.port.in.command.dto.SeedCurriculumResult",
        "com.umc.product.test.application.port.in.command.dto.SeedMembersCommand",
        "com.umc.product.test.application.port.in.command.dto.SeedMembersResult",
        "com.umc.product.test.application.port.in.command.dto.SeedNoticeCommand",
        "com.umc.product.test.application.port.in.command.dto.SeedNoticeResult",
        "com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsCommand",
        "com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsResult",
        "com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosCommand",
        "com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosResult",
        "com.umc.product.test.application.port.in.command.dto.SeedProjectsCommand",
        "com.umc.product.test.application.port.in.command.dto.SeedProjectsResult",
        "com.umc.product.test.application.port.in.command.dto.TargetProjectStatus"
    );

    @Test
    @DisplayName("모든 seed request·response·command·result record를 생성하고 변환한다")
    void all_seed_transport_records_are_constructible_and_mappable() throws Exception {
        Set<Class<?>> types = new LinkedHashSet<>();
        for (String typeName : ROOT_TYPES) {
            collectTypes(Class.forName(typeName), types);
        }

        int constructed = 0;
        int mapped = 0;
        for (Class<?> type : types) {
            if (type.isEnum()) {
                assertThat(type.getEnumConstants()).isNotEmpty();
                continue;
            }
            if (!type.isRecord()) {
                continue;
            }
            Object instance = instantiateRecord(type);
            constructed++;
            for (Method method : type.getDeclaredMethods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                boolean factory = Modifier.isStatic(method.getModifiers())
                    && Set.of("from", "of", "skipped").contains(method.getName());
                boolean converter = !Modifier.isStatic(method.getModifiers())
                    && Set.of("toCommand", "toQuery", "isOrganizationIdValid", "totalFailed")
                        .contains(method.getName());
                if (!factory && !converter) {
                    continue;
                }
                method.setAccessible(true);
                Object[] arguments = valuesFor(method.getParameterTypes());
                Object result = method.invoke(factory ? null : instance, arguments);
                if (method.getReturnType() != void.class) {
                    assertThat(result).isNotNull();
                }
                mapped++;
            }
        }

        assertThat(constructed).isGreaterThan(40);
        assertThat(mapped).isGreaterThan(25);
    }

    @Test
    @DisplayName("증서 미리보기는 공백 입력을 trim하고 안정적인 sample 기본값을 제공한다")
    void certificate_preview_defaults_blank_values() {
        CertificatePdfPreviewRequest request = new CertificatePdfPreviewRequest(
            CertificateTemplate.values()[0], " ", " ", null, null, " ", null, " ");

        var query = request.toQuery("https://example.test");

        assertThat(query.issuanceNumber()).contains("SAMPLE01");
        assertThat(query.recipientName()).isEqualTo("김유엠");
        assertThat(query.gisuGeneration()).isEqualTo(7L);
    }

    private void collectTypes(Class<?> type, Set<Class<?>> types) {
        if (!types.add(type)) {
            return;
        }
        for (Class<?> nested : type.getDeclaredClasses()) {
            collectTypes(nested, types);
        }
    }

    private Object instantiateRecord(Class<?> type) throws Exception {
        Class<?>[] parameterTypes = java.util.Arrays.stream(type.getRecordComponents())
            .map(component -> component.getType())
            .toArray(Class<?>[]::new);
        Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(valuesFor(parameterTypes));
    }

    private Object[] valuesFor(Class<?>[] parameterTypes) throws Exception {
        List<Object> values = new ArrayList<>(parameterTypes.length);
        for (Class<?> type : parameterTypes) {
            values.add(valueFor(type));
        }
        return values.toArray();
    }

    private Object valueFor(Class<?> type) throws Exception {
        if (type == String.class) return "value";
        if (type == Long.class || type == long.class) return 1L;
        if (type == Integer.class || type == int.class) return 1;
        if (type == Boolean.class || type == boolean.class) return false;
        if (type == Instant.class) return Instant.parse("2026-01-01T00:00:00Z");
        if (List.class.isAssignableFrom(type)) return List.of();
        if (Set.class.isAssignableFrom(type)) return Set.of();
        if (Map.class.isAssignableFrom(type)) return Map.of();
        if (type.isEnum()) return type.getEnumConstants()[0];
        if (type.isRecord()) return instantiateRecord(type);
        throw new IllegalArgumentException("지원하지 않는 fixture type: " + type.getName());
    }
}
