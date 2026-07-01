package com.umc.product.recruiting.application.service.query;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        "formId",
        "track",
        "applicationNo",
        "maskedEmail",
        "applicationStatus",
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
            value(row.formId()),
            value(row.track()),
            value(row.applicationNo()),
            value(row.maskedEmail()),
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
