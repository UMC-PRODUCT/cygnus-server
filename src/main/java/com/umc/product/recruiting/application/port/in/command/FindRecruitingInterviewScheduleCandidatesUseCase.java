package com.umc.product.recruiting.application.port.in.command;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.FindRecruitingInterviewScheduleCandidatesCommand;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;

public interface FindRecruitingInterviewScheduleCandidatesUseCase {

    List<RecruitingInterviewScheduleCandidate> findScheduleCandidates(
        FindRecruitingInterviewScheduleCandidatesCommand command
    );
}
