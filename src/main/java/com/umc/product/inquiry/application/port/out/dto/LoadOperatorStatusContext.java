package com.umc.product.inquiry.application.port.out.dto;

import com.umc.product.inquiry.domain.Inquiry;

/**
 * 운영진 판정에 필요한 맥락. 현재는 memberId+inquiry(target/chatRoomId 보유). 향후 gisuId 등이 필요하면 이 record에 필드를 추가하거나 구현체가 inquiry에서
 * 도출한다. 서비스는 이 그릇만 넘기므로 서비스 시그니처는 불변.
 */
public record LoadOperatorStatusContext(
    Long memberId,
    Inquiry inquiry
) {
    public static LoadOperatorStatusContext of(Long memberId, Inquiry inquiry) {
        return new LoadOperatorStatusContext(memberId, inquiry);
    }
}
