package com.umc.product.recruiting.application.port.in.query;

import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationDetailInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;

public interface SearchRecruitingApplicationUseCase {

    Page<RecruitingApplicationSummaryInfo> search(RecruitingApplicationSearchQuery query);

    RecruitingApplicationDetailInfo getDetail(Long roundId, Long applicationId, Long requesterMemberId);

    Map<Long, RecruitingApplicationDetailInfo> getDetails(Set<Long> applicationIds, Long requesterMemberId);
}
