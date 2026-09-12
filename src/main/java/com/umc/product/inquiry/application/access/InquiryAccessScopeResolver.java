package com.umc.product.inquiry.application.access;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.inquiry.application.access.InquiryAccessScope.OperatorScoped;
import com.umc.product.inquiry.application.access.InquiryAccessScope.OwnedOnly;
import com.umc.product.inquiry.application.access.InquiryAccessScope.TargetCondition;
import com.umc.product.inquiry.domain.enums.InquiryTarget;

import lombok.RequiredArgsConstructor;

/**
 * 요청 멤버의 역할 목록을 기반으로 문의 목록 조회 가시 범위를 결정한다.
 * <p>
 * 운영진 역할이 하나라도 있으면 {@link OperatorScoped}, 없으면 {@link OwnedOnly}.
 */
@Component
@RequiredArgsConstructor
public class InquiryAccessScopeResolver {

    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    public InquiryAccessScope resolve(Long memberId) {
        List<ChallengerRoleInfo> roles = getChallengerRoleUseCase.findAllByMemberId(memberId);

        List<TargetCondition> conditions = new ArrayList<>();
        for (ChallengerRoleInfo role : roles) {
            if (role.roleType().isAtLeastCentralMember()) {
                // CENTRAL과 PRODUCT_TEAM 모두 중앙 멤버가 담당(ADR-011 임시 매핑)
                conditions.add(new TargetCondition(InquiryTarget.CENTRAL, null, role.gisuId()));
                conditions.add(new TargetCondition(InquiryTarget.PRODUCT_TEAM, null, role.gisuId()));
            } else if (role.organizationType() == OrganizationType.CHAPTER) {
                conditions.add(new TargetCondition(InquiryTarget.CHAPTER, role.organizationId(), role.gisuId()));
            } else if (role.organizationType() == OrganizationType.SCHOOL) {
                conditions.add(new TargetCondition(InquiryTarget.SCHOOL, role.organizationId(), role.gisuId()));
            }
        }

        if (conditions.isEmpty()) {
            return new OwnedOnly(memberId);
        }
        return new OperatorScoped(conditions, memberId);
    }
}
