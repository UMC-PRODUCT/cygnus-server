package com.umc.product.curriculum.application.service.command;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.curriculum.application.port.in.command.ManageChallengerWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeleteChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeployChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.ExcuseChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.SaveChallengerWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengerWorkbookCommandService implements ManageChallengerWorkbookUseCase {

    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    private final SaveChallengerWorkbookPort saveChallengerWorkbookPort;
    private final GetChallengerUseCase getChallengerUseCase;

    @Override
    public List<ChallengerWorkbookInfo> batchDeploy(DeployChallengerWorkbookCommand command) {
        List<Long> originalWorkbookIds = command.originalWorkbookIds().stream()
            .distinct()
            .toList();

        List<OriginalWorkbook> originalWorkbooks = orderedOriginalWorkbooks(originalWorkbookIds);
        originalWorkbooks.forEach(this::validateReleased);

        List<ChallengerInfo> challengers = getChallengerUseCase.getAllByMemberId(command.requestedMemberId());
        originalWorkbooks.forEach(originalWorkbook -> validateDeployableMember(originalWorkbook, challengers));

        Map<Long, ChallengerWorkbook> existingWorkbookByOriginalWorkbookId =
            loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(
                    command.requestedMemberId(),
                    originalWorkbookIds
                )
                .stream()
                .collect(Collectors.toMap(
                    challengerWorkbook -> challengerWorkbook.getOriginalWorkbook().getId(),
                    Function.identity()
                ));

        return originalWorkbooks.stream()
            .map(originalWorkbook -> getOrDeploy(
                existingWorkbookByOriginalWorkbookId,
                originalWorkbook,
                command.requestedMemberId()
            ))
            .map(this::toInfo)
            .toList();
    }

    @Override
    public void edit(EditChallengerWorkbookCommand command) {
        ChallengerWorkbook challengerWorkbook = loadChallengerWorkbookPort.getById(command.challengerWorkbookId());
        validateOwner(challengerWorkbook, command.requestedMemberId());

        challengerWorkbook.edit(command.content());
        saveChallengerWorkbookPort.save(challengerWorkbook);
    }

    @Override
    public void delete(DeleteChallengerWorkbookCommand command) {
        ChallengerWorkbook challengerWorkbook = loadChallengerWorkbookPort.getById(command.challengerWorkbookId());
        saveChallengerWorkbookPort.delete(challengerWorkbook);
    }

    @Override
    public void excuse(ExcuseChallengerWorkbookCommand command) {
        ChallengerWorkbook challengerWorkbook = loadChallengerWorkbookPort.getById(command.challengerWorkbookId());
        challengerWorkbook.excuse(command.reason(), command.excuseApprovedMemberId());
        saveChallengerWorkbookPort.save(challengerWorkbook);
    }

    private List<OriginalWorkbook> orderedOriginalWorkbooks(List<Long> originalWorkbookIds) {
        Map<Long, OriginalWorkbook> workbookById = loadOriginalWorkbookPort.batchGetByIds(originalWorkbookIds)
            .stream()
            .collect(Collectors.toMap(OriginalWorkbook::getId, Function.identity()));

        return originalWorkbookIds.stream()
            .map(originalWorkbookId -> {
                OriginalWorkbook workbook = workbookById.get(originalWorkbookId);
                if (workbook == null) {
                    throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_NOT_FOUND);
                }
                return workbook;
            })
            .toList();
    }

    private void validateReleased(OriginalWorkbook originalWorkbook) {
        if (!originalWorkbook.getOriginalWorkbookStatus().isReleased()) {
            throw new CurriculumDomainException(CurriculumErrorCode.INVALID_WORKBOOK_STATUS);
        }
    }

    private void validateDeployableMember(OriginalWorkbook originalWorkbook, List<ChallengerInfo> challengers) {
        Curriculum curriculum = originalWorkbook.getWeeklyCurriculum().getCurriculum();

        boolean hasMatchedChallenger = challengers.stream()
            .anyMatch(challenger -> ChallengerStatus.ACTIVE == challenger.challengerStatus()
                && curriculum.getGisuId().equals(challenger.gisuId())
                && curriculum.getPart() == challenger.part());

        if (!hasMatchedChallenger) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }
    }

    private ChallengerWorkbook deploy(OriginalWorkbook originalWorkbook, Long requestedMemberId) {
        ChallengerWorkbook challengerWorkbook = ChallengerWorkbook.create(originalWorkbook, requestedMemberId, null);
        return saveChallengerWorkbookPort.save(challengerWorkbook);
    }

    private ChallengerWorkbook getOrDeploy(
        Map<Long, ChallengerWorkbook> existingWorkbookByOriginalWorkbookId,
        OriginalWorkbook originalWorkbook,
        Long requestedMemberId
    ) {
        ChallengerWorkbook existingWorkbook = existingWorkbookByOriginalWorkbookId.get(originalWorkbook.getId());
        if (existingWorkbook != null) {
            return existingWorkbook;
        }
        return deploy(originalWorkbook, requestedMemberId);
    }

    private void validateOwner(ChallengerWorkbook challengerWorkbook, Long requestedMemberId) {
        if (!challengerWorkbook.isOwnedBy(requestedMemberId)) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }
    }

    private ChallengerWorkbookInfo toInfo(ChallengerWorkbook challengerWorkbook) {
        return ChallengerWorkbookInfo.builder()
            .challengerWorkbookId(challengerWorkbook.getId())
            .originalWorkbookId(challengerWorkbook.getOriginalWorkbook().getId())
            .receivedStudyGroupId(challengerWorkbook.getStudyGroupId())
            .challengerId(challengerWorkbook.getMemberId())
            .isExcused(challengerWorkbook.isExcused())
            .excusedReason(challengerWorkbook.getExcusedReason())
            .content(challengerWorkbook.getContent())
            .isBestWorkbook(false)
            .submissions(List.of())
            .build();
    }

}
