package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

@ExtendWith(MockitoExtension.class)
class RecruitingCsvExportServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @InjectMocks
    RecruitingCsvExportService sut;

    @Test
    @DisplayName("CSV는_지원서_본문과_raw_email_없이_최소_현황_필드만_포함한다")
    void exportSummaryCsvExcludesApplicationBodyAndRawEmail() {
        // Given
        given(loadApplicationPort.searchSummaryRows(1L, 10L, null)).willReturn(List.of(row()));

        // When
        String csv = new String(sut.exportSummaryCsv(1L, 10L), StandardCharsets.UTF_8);

        // Then
        assertThat(csv).contains("gisuId,schoolId,roundType,roundNo,formId,track,applicationNo,maskedEmail");
        assertThat(csv).contains("APP-001");
        assertThat(csv).contains("a***@umc.test");
        assertThat(csv).doesNotContain("applicant@umc.test");
        assertThat(csv).doesNotContain("answer");
        assertThat(csv).doesNotContain("formResponseId");
    }

    private RecruitingApplicationSummaryRow row() {
        return new RecruitingApplicationSummaryRow(
            1L,
            1L,
            10L,
            20L,
            RecruitingRoundType.ADDITIONAL,
            2,
            100L,
            500L,
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            900L,
            "APP-001",
            "a***@umc.test",
            RecruitingApplicationStatus.SUBMITTED,
            RecruitingApplicationRegistrationStatus.NOT_READY,
            Instant.parse("2026-07-02T01:00:00Z")
        );
    }
}
