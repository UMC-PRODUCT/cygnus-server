package com.umc.product.authorization.adapter.in.aspect;

import java.util.Objects;
import java.util.function.LongFunction;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;

@Component
@RequestScope
public class AuthorizationRequestSubjectContext {

    private SubjectAttributes subject;

    public synchronized SubjectAttributes getOrLoad(
        long expectedMemberId,
        LongFunction<SubjectAttributes> loader
    ) {
        Objects.requireNonNull(loader, "loader must not be null");
        if (subject == null) {
            SubjectAttributes loaded = Objects.requireNonNull(
                loader.apply(expectedMemberId), "loaded subject must not be null");
            validateMember(loaded, expectedMemberId);
            subject = loaded;
        } else {
            validateMember(subject, expectedMemberId);
        }
        return subject;
    }

    public synchronized SubjectAttributes require(long expectedMemberId) {
        if (subject == null) {
            throw policyFailure();
        }
        validateMember(subject, expectedMemberId);
        return subject;
    }

    private void validateMember(SubjectAttributes candidate, long expectedMemberId) {
        if (!Objects.equals(candidate.memberId(), expectedMemberId)) {
            throw policyFailure();
        }
    }

    private AuthorizationDomainException policyFailure() {
        return new AuthorizationDomainException(AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
    }
}
