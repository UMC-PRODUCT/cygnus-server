package com.umc.product.curriculum.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.curriculum.application.port.in.query.GetOriginalWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.query.dto.OriginalWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.OriginalWorkbookInfo.OriginalWorkbookMissionInfo;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OriginalWorkbookQueryService implements GetOriginalWorkbookUseCase {

    private final LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    private final LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    private final GetStudyGroupUseCase getStudyGroupUseCase;
    private final GetChallengerUseCase getChallengerUseCase;

    @Override
    public OriginalWorkbookInfo getById(Long originalWorkbookId, Long requesterMemberId) {
        OriginalWorkbook workbook = loadOriginalWorkbookPort.getById(originalWorkbookId);
        var curriculum = workbook.getWeeklyCurriculum().getCurriculum();
        boolean belongsToMatchedStudyGroup;
        if (curriculum.getTrack() == null) {
            belongsToMatchedStudyGroup = getStudyGroupUseCase.findByMemberIdAndGisuIdAndPart(
                requesterMemberId, curriculum.getGisuId(), curriculum.getPart()).isPresent();
        } else {
            boolean enrolled = getChallengerUseCase.getAllByMemberId(requesterMemberId).stream()
                .anyMatch(challenger -> challenger.challengerStatus() == ChallengerStatus.ACTIVE
                    && curriculum.getGisuId().equals(challenger.gisuId())
                    && challenger.tracks() != null && challenger.tracks().contains(curriculum.getTrack()));
            belongsToMatchedStudyGroup = enrolled && getStudyGroupUseCase.findByMemberIdAndGisuIdAndTrack(
                requesterMemberId, curriculum.getGisuId(), curriculum.getTrack()).isPresent();
        }
        if (!belongsToMatchedStudyGroup) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }

        List<OriginalWorkbookMission> missions =
            loadOriginalWorkbookMissionPort.findByOriginalWorkbookId(originalWorkbookId);

        return OriginalWorkbookInfo.builder()
            .originalWorkbookId(workbook.getId())
            .title(workbook.getTitle())
            .description(workbook.getDescription())
            .url(workbook.getUrl())
            .content(workbook.getContent())
            .type(workbook.getType())
            .status(workbook.getOriginalWorkbookStatus())
            .releasedAt(workbook.getReleasedAt())
            .releasedMemberId(workbook.getReleasedMemberId())
            .missions(missions.stream().map(this::toMissionInfo).toList())
            .build();
    }

    private OriginalWorkbookMissionInfo toMissionInfo(OriginalWorkbookMission mission) {
        return OriginalWorkbookMissionInfo.builder()
            .originalWorkbookMissionId(mission.getId())
            .title(mission.getTitle())
            .description(mission.getDescription())
            .missionType(mission.getMissionType())
            .isNecessary(mission.isNecessary())
            .build();
    }
}
