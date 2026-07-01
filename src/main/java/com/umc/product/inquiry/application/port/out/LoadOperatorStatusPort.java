package com.umc.product.inquiry.application.port.out;

import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;

/**
 * 운영진 판정을 격리하는 아웃바운드 포트. 판정 로직(활성 기수 조회, InquiryTarget별 역할 판정, PRODUCT_TEAM 임시 라우팅)은 외부 도메인(authorization/gisu) 의존이 크므로
 * 어댑터 뒤로 숨긴다. 서비스는 맥락(context)만 넘기고 boolean만 돌려받는다.
 */
public interface LoadOperatorStatusPort {

    boolean isOperator(LoadOperatorStatusContext context);
}
