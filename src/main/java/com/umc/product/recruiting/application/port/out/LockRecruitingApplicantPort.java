package com.umc.product.recruiting.application.port.out;

import java.util.Collection;

public interface LockRecruitingApplicantPort {

    /**
     * 기수 단위로 지원자 slot에 advisory lock을 건다.
     *
     * @param gisuId 필수
     * @param applicantMemberId 회원 지원 시 필수, 익명 지원이면 {@code null}
     * @param normalizedEmails 잠글 이메일 목록. memberId가 {@code null}이면 최소 1건 필요.
     */
    void lockByGisuAndApplicant(Long gisuId, Long applicantMemberId, Collection<String> normalizedEmails);
}
