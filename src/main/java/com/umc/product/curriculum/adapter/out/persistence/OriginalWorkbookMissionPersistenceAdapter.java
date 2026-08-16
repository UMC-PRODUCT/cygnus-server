package com.umc.product.curriculum.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.SaveOriginalWorkbookMissionPort;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OriginalWorkbookMissionPersistenceAdapter
    implements LoadOriginalWorkbookMissionPort, SaveOriginalWorkbookMissionPort {

    private final OriginalWorkbookMissionJpaRepository originalWorkbookMissionJpaRepository;

    @Override
    public OriginalWorkbookMission getById(Long originalWorkbookMissionId) {
        return originalWorkbookMissionJpaRepository.findById(originalWorkbookMissionId)
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.MISSION_NOT_FOUND));
    }

    @Override
    public List<OriginalWorkbookMission> findByOriginalWorkbookId(Long originalWorkbookId) {
        return originalWorkbookMissionJpaRepository.findByOriginalWorkbookId(originalWorkbookId);
    }

    @Override
    public List<OriginalWorkbookMission> findByOriginalWorkbookIdIn(List<Long> originalWorkbookIds) {
        if (originalWorkbookIds.isEmpty()) {
            return List.of();
        }
        return originalWorkbookMissionJpaRepository.findByOriginalWorkbookIdIn(originalWorkbookIds);
    }

    @Override
    public OriginalWorkbookMission save(OriginalWorkbookMission mission) {
        return originalWorkbookMissionJpaRepository.save(mission);
    }

    @Override
    public void delete(OriginalWorkbookMission mission) {
        originalWorkbookMissionJpaRepository.delete(mission);
    }
}
