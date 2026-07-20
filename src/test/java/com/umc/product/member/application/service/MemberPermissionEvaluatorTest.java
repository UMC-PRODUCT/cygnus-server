package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.common.domain.exception.CommonException;

@DisplayName("MemberPermissionEvaluator")
class MemberPermissionEvaluatorTest {

    private final MemberPermissionEvaluator sut = new MemberPermissionEvaluator();

    @Test
    @DisplayName("MEMBER 리소스를 지원하고 챌린저 이력이 있는 회원의 조회를 허용한다")
    void 챌린저의_회원_조회를_허용한다() {
        SubjectAttributes subject = SubjectAttributes.builder()
            .memberId(1L)
            .gisuChallengerInfos(List.of(SubjectAttributes.GisuChallengerInfo.builder()
                .gisuId(20L).challengerId(100L).build()))
            .build();

        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.MEMBER);
        assertThat(sut.evaluate(subject, permission(PermissionType.READ))).isTrue();
        assertThat(sut.evaluate(SubjectAttributes.builder().memberId(2L).build(), permission(PermissionType.READ)))
            .isFalse();
    }

    @Test
    @DisplayName("중앙 총괄단만 회원 삭제를 허용한다")
    void 중앙_총괄단의_삭제를_허용한다() {
        SubjectAttributes central = SubjectAttributes.builder()
            .memberId(1L)
            .roleAttributes(List.of(new RoleAttribute(
                ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL,
                null, null, 20L)))
            .build();

        assertThat(sut.evaluate(central, permission(PermissionType.DELETE))).isTrue();
        assertThat(sut.evaluate(
            SubjectAttributes.builder().memberId(2L).build(), permission(PermissionType.DELETE))).isFalse();
    }

    @Test
    @DisplayName("정의되지 않은 회원 권한 종류는 내부 오류로 거부한다")
    void 지원하지_않는_권한을_거부한다() {
        ResourcePermission unsupported = mock(ResourcePermission.class);
        given(unsupported.permission()).willReturn(PermissionType.WRITE);

        assertThatThrownBy(() -> sut.evaluate(
            SubjectAttributes.builder().memberId(1L).build(), unsupported))
            .isInstanceOf(CommonException.class);
    }

    private ResourcePermission permission(PermissionType type) {
        return ResourcePermission.ofType(ResourceType.MEMBER, type);
    }
}
