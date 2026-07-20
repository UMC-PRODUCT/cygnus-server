package com.umc.product.authorization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.service.evaluator.ChallengerRolePermissionEvaluator;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

@DisplayName("ResourceType·ResourcePermission 계약")
class ResourcePermissionContractTest {

    @Test
    @DisplayName("모든 ResourceType code를 손실 없이 역변환하고 지원 권한 집합을 불변으로 노출한다")
    void resource_type_code를_역변환한다() {
        for (ResourceType type : ResourceType.values()) {
            assertThat(ResourceType.fromCode(type.getCode())).isSameAs(type);
            assertThat(type.getDescription()).isNotBlank();
            assertThat(type.getSupportedPermissions()).isNotEmpty();
            assertThatThrownBy(() -> type.getSupportedPermissions().add(PermissionType.READ))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        assertThatThrownBy(() -> ResourceType.fromCode("unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unknown");
        assertThatThrownBy(() -> ResourceType.fromCode(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("지원하지 않는 권한은 resource type 전용 오류로 거부한다")
    void 지원하지_않는_권한을_거부한다() {
        assertThat(ResourceType.AUDIT.supports(PermissionType.READ)).isTrue();
        assertThat(ResourceType.AUDIT.supports(PermissionType.DELETE)).isFalse();

        assertThatThrownBy(() -> ResourceType.AUDIT.validatePermission(PermissionType.DELETE))
            .isInstanceOfSatisfying(AuthorizationDomainException.class, exception ->
                assertThat(exception.getBaseCode())
                    .isEqualTo(AuthorizationErrorCode.PERMISSION_TYPE_NOT_SUPPORTED_BY_RESOURCE_TYPE)
            );
    }

    @Test
    @DisplayName("resource ID의 null·Long·문자열 변환과 잘못된 숫자 형식을 구분한다")
    void resource_id를_변환한다() {
        assertThat(ResourcePermission.ofType(ResourceType.MEMBER, PermissionType.READ).getResourceIdAsLong())
            .isNull();
        assertThat(ResourcePermission.of(ResourceType.MEMBER, 17L, PermissionType.READ).getResourceIdAsLong())
            .isEqualTo(17L);
        assertThat(ResourcePermission.of(ResourceType.MEMBER, "18", PermissionType.READ).getResourceIdAsLong())
            .isEqualTo(18L);
        assertThatThrownBy(() ->
            ResourcePermission.of(ResourceType.MEMBER, "not-number", PermissionType.READ).getResourceIdAsLong()
        ).isInstanceOfSatisfying(AuthorizationDomainException.class, exception ->
            assertThat(exception.getBaseCode()).isEqualTo(AuthorizationErrorCode.INVALID_RESOURCE_ID_TYPE)
        );
    }

    @Test
    @DisplayName("resource type과 permission null은 생성 시점에 거부한다")
    void 필수_값_null을_거부한다() {
        assertThatThrownBy(() -> new ResourcePermission(null, null, PermissionType.READ))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ResourcePermission(ResourceType.MEMBER, null, null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ResourcePermission.of(ResourceType.MEMBER, (Long) null, PermissionType.READ))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ChallengerRole evaluator는 READ는 공개하고 쓰기는 중앙 운영진에게만 허용한다")
    void challenger_role_평가기_정책을_검증한다() {
        ChallengerRolePermissionEvaluator evaluator = new ChallengerRolePermissionEvaluator();
        SubjectAttributes regular = SubjectAttributes.builder().memberId(1L).build();
        SubjectAttributes central = SubjectAttributes.builder()
            .memberId(2L)
            .roleAttributes(List.of(new RoleAttribute(
                ChallengerRoleType.CENTRAL_PRESIDENT,
                OrganizationType.CENTRAL,
                null,
                null,
                3L
            )))
            .build();

        assertThat(evaluator.supportedResourceType()).isEqualTo(ResourceType.CHALLENGER_ROLE);
        assertThat(evaluator.evaluate(
            regular,
            ResourcePermission.ofType(ResourceType.CHALLENGER_ROLE, PermissionType.READ)
        )).isTrue();
        assertThat(evaluator.evaluate(
            regular,
            ResourcePermission.ofType(ResourceType.CHALLENGER_ROLE, PermissionType.WRITE)
        )).isFalse();
        assertThat(evaluator.evaluate(
            central,
            ResourcePermission.ofType(ResourceType.CHALLENGER_ROLE, PermissionType.DELETE)
        )).isTrue();
    }
}
