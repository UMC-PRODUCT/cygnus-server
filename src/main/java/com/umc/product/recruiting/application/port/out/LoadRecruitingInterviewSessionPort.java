package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.recruiting.domain.RecruitingInterviewSession;

public interface LoadRecruitingInterviewSessionPort {

    RecruitingInterviewSession getById(Long id);

    RecruitingInterviewSession getByIdForUpdate(Long id);

    List<RecruitingInterviewSession> getAllByIdsForUpdate(List<Long> ids);

    List<RecruitingInterviewSession> listByRoundId(Long roundId);
}
