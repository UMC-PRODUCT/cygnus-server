package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;

public interface ListMyRecruitingApplicationsUseCase {

    List<RecruitingPublicApplicationInfo> listMyApplications(Long requesterMemberId);
}
