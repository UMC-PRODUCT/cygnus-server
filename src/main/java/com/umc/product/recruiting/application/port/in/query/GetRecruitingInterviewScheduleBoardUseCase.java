package com.umc.product.recruiting.application.port.in.query;

import java.time.LocalDate;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleBoardInfo;

public interface GetRecruitingInterviewScheduleBoardUseCase {

    RecruitingInterviewScheduleBoardInfo getBoard(Long roundId, LocalDate date, Long requesterMemberId);
}
