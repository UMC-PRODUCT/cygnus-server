package com.umc.product.recruiting.application.service.query;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResultInfo;
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
public class RecruitingQueryService implements GetRecruitingApplicationQueryUseCase, GetRecruitingFormQueryUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingSeasonPort loadSeasonPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;

    @Override
    public List<RecruitingApplicationFormInfo> listPublicForms(Long gisuId, Long schoolId) {
        return loadSeasonPort.findByGisuIdAndSchoolId(gisuId, schoolId)
            .map(this::listPublishedForms)
            .orElseGet(List::of);
    }

    @Override
    public RecruitingApplicationResultInfo getAnonymousResult(String applicationNo, String applicantIdentityKey) {
        RecruitingApplication application = loadApplicationPort.findByApplicationNo(applicationNo)
            .filter(found -> Objects.equals(found.getApplicantIdentityKey(), applicantIdentityKey))
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND));
        return RecruitingApplicationResultInfo.from(application);
    }

    @Override
    public RecruitingStatusSummaryInfo getStatusSummary(Long gisuId, Long schoolId) {
        List<RecruitingApplicationSummaryRow> rows = loadApplicationPort.searchSummaryRows(gisuId, schoolId, null);
        Map<RecruitingApplicationStatus, Long> countByStatus = new EnumMap<>(RecruitingApplicationStatus.class);
        for (RecruitingApplicationSummaryRow row : rows) {
            countByStatus.merge(row.applicationStatus(), 1L, Long::sum);
        }
        return new RecruitingStatusSummaryInfo((long) rows.size(), countByStatus);
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
