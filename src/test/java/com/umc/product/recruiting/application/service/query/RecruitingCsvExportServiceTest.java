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
    @DisplayName("CSV는 정확한 헤더와 마스킹 이메일만 포함한다")
    void exportSummaryCsvUsesExactHeaderAndMaskedEmail() {
        // Given
        given(loadApplicationPort.searchSummaryRows(1L, 10L, null)).willReturn(List.of(row()));

        // When
        String csv = new String(sut.exportSummaryCsv(1L, 10L), StandardCharsets.UTF_8);

        // Then
        assertThat(csv.lines().findFirst()).contains(
            "gisuId,schoolId,roundType,roundNo,applicationId,maskedEmail,firstChoiceTrack,"
                + "secondChoiceTrack,acceptedTrack,status,registrationStatus,submittedAt"
        );
        assertThat(csv).contains("900,app******@umc.test,WEB_PRODUCT_ENGINEER");
        assertThat(csv).doesNotContain("지원자", "applicant@umc.test", "A1B2C3", "answer", "formResponseId");
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
            900L,
            "지원자",
            "applicant@umc.test",
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            null,
            null,
            RecruitingApplicationStatus.SUBMITTED,
            RecruitingApplicationRegistrationStatus.NOT_READY,
            Instant.parse("2026-07-02T01:00:00Z")
        );
    }
}
