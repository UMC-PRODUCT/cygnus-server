package com.umc.product.recruiting.application.service.query;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.global.util.EmailMasker;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingCsvUseCase;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingCsvExportService implements ExportRecruitingCsvUseCase {

    private static final String HEADER = String.join(",",
        "gisuId",
        "schoolId",
        "roundType",
        "roundNo",
        "applicationId",
        "maskedEmail",
        "firstChoiceTrack",
        "secondChoiceTrack",
        "acceptedTrack",
        "status",
        "registrationStatus",
        "submittedAt"
    );

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Override
    public byte[] exportSummaryCsv(Long gisuId, Long schoolId, Long requesterMemberId) {
        validateCentralGisuAccess(requesterMemberId, gisuId);
        StringBuilder builder = new StringBuilder(HEADER).append('\n');
        for (RecruitingApplicationSummaryRow row : loadApplicationPort.searchSummaryRows(gisuId, schoolId, null)) {
            builder.append(toCsvLine(row)).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String toCsvLine(RecruitingApplicationSummaryRow row) {
        return String.join(",",
            RecruitingCsvCellEncoder.encode(row.gisuId()),
            RecruitingCsvCellEncoder.encode(row.schoolId()),
            RecruitingCsvCellEncoder.encode(row.roundType()),
            RecruitingCsvCellEncoder.encode(row.roundNo()),
            RecruitingCsvCellEncoder.encode(row.applicationId()),
            RecruitingCsvCellEncoder.encode(EmailMasker.mask(row.applicantEmail())),
            RecruitingCsvCellEncoder.encode(row.firstChoice()),
            RecruitingCsvCellEncoder.encode(row.secondChoice()),
            RecruitingCsvCellEncoder.encode(row.acceptedTrack()),
            RecruitingCsvCellEncoder.encode(row.applicationStatus()),
            RecruitingCsvCellEncoder.encode(row.registrationStatus()),
            RecruitingCsvCellEncoder.encode(row.submittedAt())
        );
    }

    private void validateCentralGisuAccess(Long requesterMemberId, Long gisuId) {
        if (getChallengerRoleUseCase.isCentralCoreInGisu(requesterMemberId, gisuId)
            || getChallengerRoleUseCase.isSuperAdmin(requesterMemberId)) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_SUMMARY_ACCESS_DENIED);
    }
}
