package com.umc.product.curriculum.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.SaveMissionFeedbackPort;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionFeedbackPersistenceAdapter implements LoadMissionFeedbackPort, SaveMissionFeedbackPort {

    private final MissionFeedbackJpaRepository missionFeedbackJpaRepository;

    @Override
    public MissionFeedback getById(Long missionFeedbackId) {
        return missionFeedbackJpaRepository.findById(missionFeedbackId)
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.MISSION_FEEDBACK_NOT_FOUND));
    }

    @Override
    public List<MissionFeedback> listByMissionSubmissionIdIn(List<Long> submissionIds) {
        if (submissionIds.isEmpty()) {
            return List.of();
        }
        return missionFeedbackJpaRepository.findByMissionSubmission_IdIn(submissionIds);
    }

    @Override
    public MissionFeedback save(MissionFeedback missionFeedback) {
        return missionFeedbackJpaRepository.save(missionFeedback);
    }

    @Override
    public void delete(MissionFeedback missionFeedback) {
        missionFeedbackJpaRepository.delete(missionFeedback);
    }

    @Override
    public void deleteByMissionSubmissionId(Long missionSubmissionId) {
        missionFeedbackJpaRepository.deleteByMissionSubmission_Id(missionSubmissionId);
    }
}
