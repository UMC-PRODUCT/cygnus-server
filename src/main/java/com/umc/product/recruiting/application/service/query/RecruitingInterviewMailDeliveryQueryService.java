package com.umc.product.recruiting.application.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewMailDeliveryUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewRequestMailInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingInterviewMailDeliveryQueryService implements GetRecruitingInterviewMailDeliveryUseCase {

    private final LoadRecruitingInterviewSchedulePort loadSchedulePort;

    @Override
    public RecruitingInterviewRequestMailInfo getRequestMail(Long applicationId) {
        return RecruitingInterviewRequestMailInfo.from(loadSchedulePort.getByApplicationId(applicationId));
    }
}
