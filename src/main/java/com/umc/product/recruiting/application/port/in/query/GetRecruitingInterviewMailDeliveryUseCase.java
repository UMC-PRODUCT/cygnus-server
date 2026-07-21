package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewRequestMailInfo;

public interface GetRecruitingInterviewMailDeliveryUseCase {

    RecruitingInterviewRequestMailInfo getRequestMail(Long applicationId);
}
