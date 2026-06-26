package com.umc.product.inquiry.application.port.out;

import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;

/**
 * 운영진 판정을 격리하는 아웃바운드 포트.
 * <p>
 * 판정 로직(gisuId 결정, InquiryTarget↔ChallengerRoleType 매핑, PRODUCT_TEAM 예외 등)은 미정 영역이므로 인터페이스 뒤로 숨긴다. 서비스는 맥락 객체만 넘기고
 * boolean만 돌려받는다.
 */
public interface LoadOperatorStatusPort {

    boolean isOperator(LoadOperatorStatusContext context);
}
