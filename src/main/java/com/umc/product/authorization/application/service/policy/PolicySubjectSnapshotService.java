package com.umc.product.authorization.application.service.policy;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.application.port.in.policy.LoadPolicySubjectSnapshotUseCase;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicySubjectSnapshotService implements LoadPolicySubjectSnapshotUseCase {

    private final CheckPermissionUseCase checkPermissionUseCase;

    @Override
    public AuthorizationSubjectSnapshot loadMemberPolicySubject(long memberId) {
        SubjectAttributes subject = checkPermissionUseCase.loadSubject(memberId);
        if (subject == null || subject.policyFacts() == null) {
            throw new AuthorizationDomainException(
                AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
        }
        return subject.policyFacts().toAuthorizationSubjectSnapshot(
            memberId,
            subject.schoolId(),
            subject.systemRoles());
    }
}
