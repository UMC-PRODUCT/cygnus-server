package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleBoardRow;

public interface LoadRecruitingInterviewScheduleBoardPort {

    List<RecruitingInterviewScheduleBoardRow> listByRoundId(Long roundId);
}
