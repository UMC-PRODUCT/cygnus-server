package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResultInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingQueryServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @InjectMocks
    RecruitingQueryService sut;

    @Test
    @DisplayName("익명_결과_조회는_지원서번호와_identity_key가_모두_맞아야_한다")
    void getAnonymousResultRequiresApplicationNoAndIdentityKey() {
        // Given
        RecruitingApplication application = submittedApplication();
        given(loadApplicationPort.findByApplicationNo("APP-001")).willReturn(Optional.of(application));

        // When
        RecruitingApplicationResultInfo result = sut.getAnonymousResult("APP-001", "identity:1");

        // Then
        assertThat(result.applicationNo()).isEqualTo("APP-001");
        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.SUBMITTED);

        assertThatThrownBy(() -> sut.getAnonymousResult("APP-001", "wrong"))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("상태_요약은_summary_row의_지원서_상태를_집계한다")
    void summarizeApplicationStatuses() {
        // Given
        given(loadApplicationPort.searchSummaryRows(1L, 10L, null)).willReturn(List.of(
            row("APP-1", RecruitingApplicationStatus.SUBMITTED),
            row("APP-2", RecruitingApplicationStatus.SUBMITTED),
            row("APP-3", RecruitingApplicationStatus.FINAL_PASSED)
        ));

        // When
        RecruitingStatusSummaryInfo result = sut.getStatusSummary(1L, 10L);

        // Then
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.countByStatus().get(RecruitingApplicationStatus.SUBMITTED)).isEqualTo(2L);
        assertThat(result.countByStatus().get(RecruitingApplicationStatus.FINAL_PASSED)).isEqualTo(1L);
    }

    private RecruitingApplication submittedApplication() {
        RecruitingApplication application = RecruitingApplication.createDraft(
            applicationForm(),
            700L,
            200L,
            "identity:1",
            "APP-001",
            "a***@umc.test"
        );
        ReflectionTestUtils.setField(application, "id", 900L);
        application.submit(200L);
        return application;
    }

    private RecruitingApplicationForm applicationForm() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(
            RecruitingRound.createRegular(RecruitingSeason.create(1L, 10L)),
            500L,
            ChallengerTrack.WEB_PRODUCT_ENGINEER
        );
        form.publish();
        return form;
    }

    private RecruitingApplicationSummaryRow row(String applicationNo, RecruitingApplicationStatus status) {
        return new RecruitingApplicationSummaryRow(
            1L,
            1L,
            10L,
            20L,
            RecruitingRoundType.REGULAR,
            1,
            100L,
            500L,
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            900L,
            applicationNo,
            "a***@umc.test",
            status,
            RecruitingApplicationRegistrationStatus.NOT_READY,
            Instant.parse("2026-07-02T01:00:00Z")
        );
    }
}
