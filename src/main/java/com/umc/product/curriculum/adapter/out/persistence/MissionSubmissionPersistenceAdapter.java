package com.umc.product.curriculum.adapter.out.persistence;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.SaveMissionSubmissionPort;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.global.persistence.ConstraintViolationInspector;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionSubmissionPersistenceAdapter implements LoadMissionSubmissionPort, SaveMissionSubmissionPort {

    private static final String UNIQUE_SUBMISSION_CONSTRAINT =
        "uk_mission_submission_original_wb_mission_challenger_wb_id";

    private final MissionSubmissionJpaRepository missionSubmissionJpaRepository;

    @Override
    public MissionSubmission getById(Long missionSubmissionId) {
        return missionSubmissionJpaRepository.findById(missionSubmissionId)
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.MISSION_SUBMISSION_NOT_FOUND));
    }

    @Override
    public MissionSubmission getByIdForUpdate(Long missionSubmissionId) {
        return missionSubmissionJpaRepository.findByIdForUpdate(missionSubmissionId)
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.MISSION_SUBMISSION_NOT_FOUND));
    }

    @Override
    public List<MissionSubmission> listActiveByChallengerWorkbookId(Long challengerWorkbookId) {
        return missionSubmissionJpaRepository.findByChallengerWorkbook_IdAndWithdrawnAtIsNull(challengerWorkbookId);
    }

    @Override
    public List<MissionSubmission> listActiveByChallengerWorkbookIdIn(List<Long> challengerWorkbookIds) {
        if (challengerWorkbookIds.isEmpty()) {
            return List.of();
        }
        return missionSubmissionJpaRepository.findByChallengerWorkbook_IdInAndWithdrawnAtIsNull(challengerWorkbookIds);
    }

    @Override
    public boolean existsByOriginalWorkbookMissionId(Long originalWorkbookMissionId) {
        return missionSubmissionJpaRepository.existsByOriginalWorkbookMission_Id(originalWorkbookMissionId);
    }

    @Override
    public boolean existsByOriginalWorkbookMissionIdAndChallengerWorkbookId(
        Long originalWorkbookMissionId,
        Long challengerWorkbookId
    ) {
        return missionSubmissionJpaRepository.existsByOriginalWorkbookMission_IdAndChallengerWorkbook_Id(
            originalWorkbookMissionId,
            challengerWorkbookId
        );
    }

    @Override
    public boolean existsByChallengerWorkbookId(Long challengerWorkbookId) {
        return missionSubmissionJpaRepository.existsByChallengerWorkbook_Id(challengerWorkbookId);
    }

    @Override
    public MissionSubmission save(MissionSubmission missionSubmission) {
        try {
            MissionSubmission saved = missionSubmissionJpaRepository.save(missionSubmission);
            missionSubmissionJpaRepository.flush();
            return saved;
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolationInspector.matches(e, UNIQUE_SUBMISSION_CONSTRAINT)) {
                throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_SUBMISSION_ALREADY_EXISTS, e);
            }
            throw e;
        }
    }
}
