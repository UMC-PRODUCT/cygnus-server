package com.umc.product.curriculum.application.service.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.command.ManageChallengerWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeleteChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeployChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.ExcuseChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.SaveChallengerWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengerWorkbookCommandService implements ManageChallengerWorkbookUseCase {

    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    private final LoadMissionSubmissionPort loadMissionSubmissionPort;
    private final SaveChallengerWorkbookPort saveChallengerWorkbookPort;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetStudyGroupUseCase getStudyGroupUseCase;

    @Override
    public List<ChallengerWorkbookInfo> batchDeploy(DeployChallengerWorkbookCommand command) {
        List<Long> workbookIds = command.originalWorkbookIds().stream().distinct().toList();
        List<OriginalWorkbook> originalWorkbooks = orderedOriginalWorkbooks(workbookIds);
        List<ChallengerInfo> challengers = getChallengerUseCase.getAllByMemberId(command.requestedMemberId());
        Map<GroupKey, StudyGroupInfo> groupCache = new HashMap<>();

        originalWorkbooks.forEach(workbook -> {
            validateReleased(workbook);
            validateDeployableMember(workbook, challengers);
            Curriculum curriculum = workbook.getWeeklyCurriculum().getCurriculum();
            groupCache.computeIfAbsent(
                new GroupKey(curriculum.getGisuId(), curriculum.getPart(), curriculum.getTrack()),
                key -> resolveStudyGroup(command.requestedMemberId(), key)
            );
        });

        Map<Long, ChallengerWorkbook> existingByOriginalId = loadChallengerWorkbookPort
            .listByMemberIdAndOriginalWorkbookIdIn(command.requestedMemberId(), workbookIds)
            .stream()
            .collect(Collectors.toMap(workbook -> workbook.getOriginalWorkbook().getId(), Function.identity()));

        return originalWorkbooks.stream()
            .map(original -> getOrDeploy(existingByOriginalId, original, command.requestedMemberId(), groupCache))
            .map(this::toInfo)
            .toList();
    }

    @Override
    public void edit(EditChallengerWorkbookCommand command) {
        ChallengerWorkbook workbook = loadChallengerWorkbookPort.getById(command.challengerWorkbookId());
        if (!workbook.isOwnedBy(command.requestedMemberId())) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }
        workbook.edit(command.content());
        saveChallengerWorkbookPort.save(workbook);
    }

    @Override
    public void delete(DeleteChallengerWorkbookCommand command) {
        ChallengerWorkbook workbook = loadChallengerWorkbookPort.getById(command.challengerWorkbookId());
        if (loadMissionSubmissionPort.existsByChallengerWorkbookId(workbook.getId())) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_HAS_SUBMISSIONS);
        }
        saveChallengerWorkbookPort.delete(workbook);
    }

    @Override
    public void excuse(ExcuseChallengerWorkbookCommand command) {
        ChallengerWorkbook workbook = loadChallengerWorkbookPort.getById(command.challengerWorkbookId());
        workbook.excuse(command.reason(), command.excuseApprovedMemberId());
        saveChallengerWorkbookPort.save(workbook);
    }

    private List<OriginalWorkbook> orderedOriginalWorkbooks(List<Long> ids) {
        Map<Long, OriginalWorkbook> byId = loadOriginalWorkbookPort.batchGetByIds(ids).stream()
            .collect(Collectors.toMap(OriginalWorkbook::getId, Function.identity()));
        return ids.stream().map(id -> {
            OriginalWorkbook workbook = byId.get(id);
            if (workbook == null) {
                throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_NOT_FOUND);
            }
            return workbook;
        }).toList();
    }

    private void validateReleased(OriginalWorkbook workbook) {
        if (!workbook.getOriginalWorkbookStatus().isReleased()) {
            throw new CurriculumDomainException(CurriculumErrorCode.INVALID_WORKBOOK_STATUS);
        }
    }

    private void validateDeployableMember(OriginalWorkbook workbook, List<ChallengerInfo> challengers) {
        Curriculum curriculum = workbook.getWeeklyCurriculum().getCurriculum();
        boolean matched = challengers.stream().anyMatch(challenger ->
            ChallengerStatus.ACTIVE == challenger.challengerStatus()
                && curriculum.getGisuId().equals(challenger.gisuId())
                && (curriculum.getTrack() == null ? curriculum.getPart() == challenger.part()
                : challenger.tracks() != null && challenger.tracks().contains(curriculum.getTrack())));
        if (!matched) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }
    }

    private StudyGroupInfo resolveStudyGroup(Long memberId, GroupKey key) {
        var group = key.track() == null
            ? getStudyGroupUseCase.findByMemberIdAndGisuIdAndPart(memberId, key.gisuId(), key.part())
            : getStudyGroupUseCase.findByMemberIdAndGisuIdAndTrack(memberId, key.gisuId(), key.track());
        return group
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.STUDY_GROUP_NOT_MATCHED));
    }

    private ChallengerWorkbook getOrDeploy(
        Map<Long, ChallengerWorkbook> existingByOriginalId,
        OriginalWorkbook original,
        Long memberId,
        Map<GroupKey, StudyGroupInfo> groupCache
    ) {
        Curriculum curriculum = original.getWeeklyCurriculum().getCurriculum();
        Long groupId = groupCache.get(new GroupKey(
            curriculum.getGisuId(), curriculum.getPart(), curriculum.getTrack())).groupId();
        ChallengerWorkbook existing = existingByOriginalId.get(original.getId());
        if (existing != null) {
            if (existing.getStudyGroupId() != null
                && !Objects.equals(existing.getStudyGroupId(), groupId)) {
                throw new CurriculumDomainException(CurriculumErrorCode.STUDY_GROUP_NOT_MATCHED);
            }
            existing.assignStudyGroupIfAbsent(groupId);
            return saveChallengerWorkbookPort.save(existing);
        }
        return saveChallengerWorkbookPort.save(ChallengerWorkbook.create(original, memberId, groupId));
    }

    private ChallengerWorkbookInfo toInfo(ChallengerWorkbook workbook) {
        return ChallengerWorkbookInfo.builder()
            .challengerWorkbookId(workbook.getId())
            .originalWorkbookId(workbook.getOriginalWorkbook().getId())
            .receivedStudyGroupId(workbook.getStudyGroupId())
            .challengerId(workbook.getMemberId())
            .isExcused(workbook.isExcused())
            .excusedReason(workbook.getExcusedReason())
            .content(workbook.getContent())
            .isBestWorkbook(false)
            .requiredMissionIds(Set.of())
            .submissions(List.of())
            .build();
    }

    private record GroupKey(Long gisuId, ChallengerPart part, ChallengerTrack track) {
    }
}
