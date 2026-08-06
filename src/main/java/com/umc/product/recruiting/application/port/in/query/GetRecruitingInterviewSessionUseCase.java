package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewSessionInfo;

public interface GetRecruitingInterviewSessionUseCase {

    RecruitingInterviewSessionInfo getSession(Long roundId, Long sessionId, Long requesterMemberId);

    List<RecruitingInterviewSessionInfo> listSessions(Long roundId, Long requesterMemberId);
}
