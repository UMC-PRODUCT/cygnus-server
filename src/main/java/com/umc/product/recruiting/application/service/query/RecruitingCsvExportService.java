package com.umc.product.recruiting.application.service.query;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.global.util.EmailMasker;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingCsvUseCase;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;

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

    @Override
    public byte[] exportSummaryCsv(Long gisuId, Long schoolId) {
        StringBuilder builder = new StringBuilder(HEADER).append('\n');
        for (RecruitingApplicationSummaryRow row : loadApplicationPort.searchSummaryRows(gisuId, schoolId, null)) {
            builder.append(toCsvLine(row)).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String toCsvLine(RecruitingApplicationSummaryRow row) {
        return String.join(",",
            value(row.gisuId()),
            value(row.schoolId()),
            value(row.roundType()),
            value(row.roundNo()),
            value(row.applicationId()),
            value(EmailMasker.mask(row.applicantEmail())),
            value(row.firstChoice()),
            value(row.secondChoice()),
            value(row.acceptedTrack()),
            value(row.applicationStatus()),
            value(row.registrationStatus()),
            value(row.submittedAt())
        );
    }

    private String value(Object value) {
        if (value == null) {
            return "";
        }
        String raw = String.valueOf(value);
        if (!raw.contains(",") && !raw.contains("\"") && !raw.contains("\n")) {
            return raw;
        }
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }
}
