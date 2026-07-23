package com.umc.product.term.application.port.out;

import com.umc.product.term.domain.TermConsent;

public interface SaveTermConsentPort {
    /**
     * 약관 동의 정보를 저장합니다.
     */
    TermConsent save(TermConsent termConsent);

    /**
     * 동일 회원과 약관의 동의가 없을 때만 저장합니다.
     *
     * @return 새 동의가 저장되었으면 {@code true}, 이미 존재하면 {@code false}
     */
    boolean saveIfAbsent(TermConsent termConsent);

    /**
     * 약관 동의 정보를 삭제합니다.
     */
    void delete(TermConsent termConsent);
}
