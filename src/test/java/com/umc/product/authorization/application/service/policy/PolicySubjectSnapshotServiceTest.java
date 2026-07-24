package com.umc.product.authorization.application.service.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;

@ExtendWith(MockitoExtension.class)
class PolicySubjectSnapshotServiceTest {

    @Mock
    CheckPermissionUseCase checkPermissionUseCase;
    @Mock
    SubjectAttributes subjectAttributes;
    @Mock
    SubjectPolicyFacts policyFacts;
    @Mock
    AuthorizationSubjectSnapshot snapshot;

    @InjectMocks
    PolicySubjectSnapshotService service;

    @Test
    @DisplayName("권한 subject를 독립 policy snapshot으로 변환한다")
    void loadMemberPolicySubject() {
        given(checkPermissionUseCase.loadSubject(1L)).willReturn(subjectAttributes);
        given(subjectAttributes.policyFacts()).willReturn(policyFacts);
        given(subjectAttributes.schoolId()).willReturn(2L);
        given(subjectAttributes.systemRoles()).willReturn(Set.of());
        given(policyFacts.toAuthorizationSubjectSnapshot(1L, 2L, Set.of()))
            .willReturn(snapshot);

        AuthorizationSubjectSnapshot result = service.loadMemberPolicySubject(1L);

        assertThat(result).isSameAs(snapshot);
    }

    @Test
    @DisplayName("policy fact가 없는 subject는 fail closed한다")
    void rejectSubjectWithoutPolicyFacts() {
        given(checkPermissionUseCase.loadSubject(1L)).willReturn(subjectAttributes);
        given(subjectAttributes.policyFacts()).willReturn(null);

        assertThatThrownBy(() -> service.loadMemberPolicySubject(1L))
            .isInstanceOf(AuthorizationDomainException.class)
            .satisfies(exception -> assertThat(
                ((AuthorizationDomainException) exception).getBaseCode())
                .isEqualTo(AuthorizationErrorCode.POLICY_EVALUATION_FAILED));
    }
}
