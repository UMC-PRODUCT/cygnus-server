package com.umc.product.term.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.term.domain.exception.TermDomainException;

@DisplayName("TermPermissionEvaluator")
class TermPermissionEvaluatorTest {

    @Test
    @DisplayName("약관 쓰기는 전역 관리자에게만 허용한다")
    void 전역_관리자만_약관을_작성한다() {
        TermPermissionEvaluator sut = new TermPermissionEvaluator();
        ResourcePermission write = ResourcePermission.ofType(ResourceType.TERM, PermissionType.WRITE);
        SubjectAttributes superAdmin = SubjectAttributes.builder()
            .memberId(1L)
            .systemRoles(Set.of(SystemRoleType.SUPER_ADMIN))
            .build();
        SubjectAttributes member = SubjectAttributes.builder().memberId(2L).build();

        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.TERM);
        assertThat(sut.evaluate(superAdmin, write)).isTrue();
        assertThat(sut.evaluate(member, write)).isFalse();
    }

    @Test
    @DisplayName("미지원 permission은 기본 허용하지 않고 fail-closed한다")
    void 미지원_permission을_거부한다() {
        TermPermissionEvaluator sut = new TermPermissionEvaluator();
        ResourcePermission unsupported = mock(ResourcePermission.class);
        given(unsupported.permission()).willReturn(PermissionType.READ);

        assertThatThrownBy(() -> sut.evaluate(SubjectAttributes.builder().build(), unsupported))
            .isInstanceOf(TermDomainException.class);
    }
}
