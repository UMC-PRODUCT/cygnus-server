package com.umc.product.recruiting.application.service.query;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.ValidateRecruitingApplicationScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.ValidateRecruitingFormScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingQueryService implements
    GetRecruitingApplicationQueryUseCase,
    GetRecruitingFormQueryUseCase,
    ValidateRecruitingApplicationScopeUseCase,
    ValidateRecruitingFormScopeUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingSeasonPort loadSeasonPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Override
    public RecruitingApplicationInfo getById(Long applicationId, Long requesterMemberId) {
        RecruitingApplication application = loadApplicationPort.getById(applicationId);
        application.validateApplicant(requesterMemberId);
        return RecruitingApplicationInfo.from(application);
    }

    @Override
    public List<RecruitingApplicationFormInfo> listPublicForms(Long gisuId, Long schoolId) {
        return loadSeasonPort.findByGisuIdAndSchoolId(gisuId, schoolId)
            .map(this::listPublishedForms)
            .orElseGet(List::of);
    }

    @Override
    public RecruitingStatusSummaryInfo getStatusSummary(Long gisuId, Long schoolId, Long requesterMemberId) {
        validateCentralGisuAccess(requesterMemberId, gisuId);
        List<RecruitingApplicationSummaryRow> rows = loadApplicationPort.searchSummaryRows(gisuId, schoolId, null);
        Map<RecruitingApplicationStatus, Long> countByStatus = new EnumMap<>(RecruitingApplicationStatus.class);
        for (RecruitingApplicationSummaryRow row : rows) {
            countByStatus.merge(row.applicationStatus(), 1L, Long::sum);
        }
        return new RecruitingStatusSummaryInfo((long) rows.size(), countByStatus);
    }

    private void validateCentralGisuAccess(Long requesterMemberId, Long gisuId) {
        if (getChallengerRoleUseCase.isCentralCoreInGisu(requesterMemberId, gisuId)
            || getChallengerRoleUseCase.isSuperAdmin(requesterMemberId)) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_SUMMARY_ACCESS_DENIED);
    }

    @Override
    public boolean isRoundBelongsToSeason(Long roundId, Long seasonId) {
        if (roundId == null || seasonId == null) {
            return false;
        }
        return Objects.equals(loadRoundPort.getById(roundId).getSeason().getId(), seasonId);
    }

    @Override
    public boolean isApplicationBelongsToSeason(Long applicationId, Long seasonId) {
        if (applicationId == null || seasonId == null) {
            return false;
        }
        RecruitingApplication application = loadApplicationPort.getById(applicationId);
        return Objects.equals(application.getRound().getSeason().getId(), seasonId);
    }

    @Override
    public void validateRoundScope(Long applicationId, Long roundId) {
        RecruitingApplication application = loadApplicationPort.getById(applicationId);
        if (!Objects.equals(application.getRound().getId(), roundId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND);
        }
    }

    @Override
    public boolean isApplicationFormBelongsToSeason(Long applicationFormId, Long seasonId) {
        if (applicationFormId == null || seasonId == null) {
            return false;
        }
        return Objects.equals(
            loadApplicationFormPort.getById(applicationFormId).getRound().getSeason().getId(),
            seasonId
        );
    }

    @Override
    public void validateSeasonScope(Long applicationFormId, Long seasonId) {
        if (!isApplicationFormBelongsToSeason(applicationFormId, seasonId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND);
        }
    }

    @Override
    public boolean isFormBelongsToSeason(Long formId, Long seasonId) {
        if (formId == null || seasonId == null) {
            return false;
        }
        return loadApplicationFormPort.existsByFormIdAndSeasonId(formId, seasonId);
    }

    private List<RecruitingApplicationFormInfo> listPublishedForms(RecruitingSeason season) {
        List<Long> roundIds = loadRoundPort.listBySeasonId(season.getId())
            .stream()
            .map(RecruitingRound::getId)
            .toList();
        return loadApplicationFormPort.listByRoundIdsAndStatus(roundIds, RecruitingApplicationFormStatus.PUBLISHED)
            .stream()
            .map(RecruitingApplicationFormInfo::from)
            .toList();
    }
}
