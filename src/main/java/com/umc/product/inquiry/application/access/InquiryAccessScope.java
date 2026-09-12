package com.umc.product.inquiry.application.access;

import java.util.List;

import com.umc.product.inquiry.domain.enums.InquiryTarget;

/**
 * 문의 목록 조회 시 적용되는 가시 범위.
 * <p>
 * application 계층 순수 객체 — QueryDSL 타입 포함 금지.
 */
public sealed interface InquiryAccessScope {

    /**
     * 일반 사용자: 본인 작성 문의만.
     */
    record OwnedOnly(Long memberId) implements InquiryAccessScope {}

    /**
     * 운영진: 권한 범위(여러 역할 OR) + 본인 작성.
     */
    record OperatorScoped(List<TargetCondition> conditions, Long memberId) implements InquiryAccessScope {}

    /**
     * 단일 권한 조건.
     * CENTRAL/PRODUCT_TEAM은 organizationId=null, SCHOOL/CHAPTER는 organizationId=해당 id.
     */
    record TargetCondition(InquiryTarget target, Long organizationId, Long gisuId) {}
}
