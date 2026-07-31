package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;

public interface GetAnonymousRecruitingApplicationUseCase {

    RecruitingPublicApplicationInfo getByCredential(String applicantEmail, String applicationKey);
}
