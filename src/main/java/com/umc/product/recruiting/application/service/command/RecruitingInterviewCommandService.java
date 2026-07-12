package com.umc.product.recruiting.application.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.FindRecruitingInterviewScheduleCandidatesUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.FindRecruitingInterviewScheduleCandidatesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;
import com.umc.product.recruiting.domain.RecruitingApplication;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingInterviewCommandService implements
    SkipRecruitingInterviewUseCase,
    FindRecruitingInterviewScheduleCandidatesUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final SaveRecruitingApplicationPort saveApplicationPort;
    private final FindRecruitingScheduleOverlapPort findScheduleOverlapPort;

    @Override
    public void skip(SkipRecruitingInterviewCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(command.applicationId());
        application.skipInterview(command.skippedByMemberId(), command.reason());
        saveApplicationPort.save(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitingInterviewScheduleCandidate> findScheduleCandidates(
        FindRecruitingInterviewScheduleCandidatesCommand command
    ) {
        return findScheduleOverlapPort.findOverlaps(command.formId(), command.formResponseIds());
    }

}
