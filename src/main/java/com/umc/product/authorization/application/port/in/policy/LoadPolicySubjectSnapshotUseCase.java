package com.umc.product.authorization.application.port.in.policy;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;

public interface LoadPolicySubjectSnapshotUseCase {

    AuthorizationSubjectSnapshot loadMemberPolicySubject(long memberId);
}
