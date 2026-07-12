package com.umc.product.recruiting.application.port.out;

import java.util.Collection;

public interface LockRecruitingApplicantPort {

    void lockByGisuAndApplicant(Long gisuId, Long applicantMemberId, Collection<String> normalizedEmails);
}
