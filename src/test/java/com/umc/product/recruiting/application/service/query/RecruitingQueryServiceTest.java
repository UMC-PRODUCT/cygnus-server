package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

@ExtendWith(MockitoExtension.class)
class RecruitingQueryServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @InjectMocks
    RecruitingQueryService sut;

    @Test
    @DisplayName("상태_요약은_summary_row의_지원서_상태를_집계한다")
    void summarizeApplicationStatuses() {
        // Given
        given(getChallengerRoleUseCase.isCentralCoreInGisu(99L, 1L)).willReturn(true);
        given(loadApplicationPort.searchSummaryRows(1L, 10L, null)).willReturn(List.of(
            row("지원자1", RecruitingApplicationStatus.SUBMITTED),
            row("지원자2", RecruitingApplicationStatus.SUBMITTED),
            row("지원자3", RecruitingApplicationStatus.FINAL_PASSED)
        ));

        // When
        RecruitingStatusSummaryInfo result = sut.getStatusSummary(1L, 10L, 99L);

        // Then
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.countByStatus().get(RecruitingApplicationStatus.SUBMITTED)).isEqualTo(2L);
        assertThat(result.countByStatus().get(RecruitingApplicationStatus.FINAL_PASSED)).isEqualTo(1L);
    }

    @Test
    @DisplayName("다른 기수의 중앙 총괄단은 상태 요약을 조회할 수 없다")
    void rejectStatusSummaryForCentralCoreFromDifferentGisu() {
        given(getChallengerRoleUseCase.isCentralCoreInGisu(99L, 1L)).willReturn(false);
        given(getChallengerRoleUseCase.isSuperAdmin(99L)).willReturn(false);

        assertThatThrownBy(() -> sut.getStatusSummary(1L, 10L, 99L))
            .isInstanceOf(com.umc.product.recruiting.domain.exception.RecruitingDomainException.class);
        then(loadApplicationPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 기수와 무관하게 상태 요약을 조회할 수 있다")
    void superAdminReadsStatusSummaryAcrossGisu() {
        given(getChallengerRoleUseCase.isCentralCoreInGisu(99L, 1L)).willReturn(false);
        given(getChallengerRoleUseCase.isSuperAdmin(99L)).willReturn(true);
        given(loadApplicationPort.searchSummaryRows(1L, 10L, null)).willReturn(List.of());

        RecruitingStatusSummaryInfo result = sut.getStatusSummary(1L, 10L, 99L);

        assertThat(result.totalCount()).isZero();
    }

    private RecruitingApplicationSummaryRow row(String applicantName, RecruitingApplicationStatus status) {
        return new RecruitingApplicationSummaryRow(
            1L,
            1L,
            10L,
            20L,
            RecruitingRoundType.REGULAR,
            1,
            100L,
            500L,
            900L,
            applicantName,
            "masked-source@umc.test",
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            null,
            null,
            status,
            RecruitingApplicationRegistrationStatus.NOT_READY,
            Instant.parse("2026-07-02T01:00:00Z")
        );
    }
}
