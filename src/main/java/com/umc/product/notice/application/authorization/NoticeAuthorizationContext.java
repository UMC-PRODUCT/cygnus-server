package com.umc.product.notice.application.authorization;

import java.time.Instant;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTargetPattern;

record NoticeAuthorizationContext(
    NoticePolicyAction action,
    long memberId,
    Optional<SubjectAttributes> legacySubject,
    Optional<AuthorizationSubjectSnapshot> subject,
    NoticeTargetInfo target,
    NoticeTargetPattern targetPattern,
    boolean author,
    Instant evaluatedAt
) {
}
