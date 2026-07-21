package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingSeasonTrackQuotaCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingSeasonTrackQuotasCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingSeasonCommandServiceTest {

    @Mock
    LoadRecruitingSeasonPort loadSeasonPort;
    @Mock
    SaveRecruitingSeasonPort saveSeasonPort;
    @Mock
    LoadRecruitingRoundPort loadRoundPort;
    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;
    @Mock
    LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;
    @Mock
    SaveRecruitingSeasonTrackQuotaPort saveQuotaPort;
    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;
    @InjectMocks
    RecruitingSeasonCommandService sut;

    @Test
    @DisplayName("학교와 기수 조합이 없으면 모집 시즌과 쿼터를 생성한다")
    void createSeason() {
        CreateRecruitingSeasonCommand command = CreateRecruitingSeasonCommand.builder()
            .requesterMemberId(99L)
            .gisuId(1L)
            .schoolId(10L)
            .quotas(List.of(RecruitingSeasonTrackQuotaCommand.of(ChallengerTrack.PLAN, 3)))
            .build();
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(99L, 1L, 10L)).willReturn(true);
        given(loadSeasonPort.existsByGisuIdAndSchoolId(1L, 10L)).willReturn(false);
        given(saveSeasonPort.save(any())).willAnswer(invocation -> {
            RecruitingSeason season = invocation.getArgument(0);
            ReflectionTestUtils.setField(season, "id", 100L);
            return season;
        });

        assertThat(sut.createSeason(command)).isEqualTo(100L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RecruitingSeasonTrackQuota>> captor = ArgumentCaptor.forClass(List.class);
        then(saveQuotaPort).should().saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(quota -> {
            assertThat(quota.getTrack()).isEqualTo(ChallengerTrack.PLAN);
            assertThat(quota.getTargetCount()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("모집 시즌 생성 command는 기수가 필수이다")
    void createSeasonRequiresGisu() {
        assertThatThrownBy(() -> CreateRecruitingSeasonCommand.builder()
            .requesterMemberId(99L)
            .schoolId(10L)
            .build())
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_SEASON_REQUIRED_FIELD);
    }

    @Test
    @DisplayName("모집 시즌 생성 command는 학교가 필수이다")
    void createSeasonRequiresSchool() {
        assertThatThrownBy(() -> CreateRecruitingSeasonCommand.builder()
            .requesterMemberId(99L)
            .gisuId(1L)
            .build())
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_SEASON_REQUIRED_FIELD);
    }

    @Test
    @DisplayName("같은 학교와 기수의 모집 시즌을 중복 생성할 수 없다")
    void createSeasonRejectsDuplicateSeason() {
        CreateRecruitingSeasonCommand command = CreateRecruitingSeasonCommand.builder()
            .requesterMemberId(99L)
            .gisuId(1L)
            .schoolId(10L)
            .build();
        given(getChallengerRoleUseCase.isCentralCoreInGisu(99L, 1L)).willReturn(true);
        given(loadSeasonPort.existsByGisuIdAndSchoolId(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> sut.createSeason(command))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_SEASON_ALREADY_EXISTS);
        then(saveSeasonPort).should(never()).save(any());
    }

    @Test
    @DisplayName("시즌 쿼터는 같은 트랙을 중복 설정할 수 없다")
    void createSeasonRejectsDuplicateQuotaTracks() {
        CreateRecruitingSeasonCommand command = CreateRecruitingSeasonCommand.builder()
            .requesterMemberId(99L)
            .gisuId(1L)
            .schoolId(10L)
            .quotas(List.of(
                RecruitingSeasonTrackQuotaCommand.of(ChallengerTrack.PLAN, 3),
                RecruitingSeasonTrackQuotaCommand.of(ChallengerTrack.PLAN, 5)
            ))
            .build();
        given(getChallengerRoleUseCase.isSuperAdmin(99L)).willReturn(true);

        assertThatThrownBy(() -> sut.createSeason(command))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_QUOTA_DUPLICATE_TRACK);
        then(saveSeasonPort).should(never()).save(any());
    }

    @Test
    @DisplayName("다른 학교 회장단은 요청 학교의 시즌을 생성할 수 없다")
    void createSeasonRejectsDifferentSchoolPresident() {
        CreateRecruitingSeasonCommand command = CreateRecruitingSeasonCommand.builder()
            .requesterMemberId(99L)
            .gisuId(1L)
            .schoolId(10L)
            .build();
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(99L, 1L, 10L)).willReturn(false);

        assertThatThrownBy(() -> sut.createSeason(command))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_SEASON_CREATION_FORBIDDEN);

        then(getChallengerRoleUseCase).should().isSchoolCoreInGisu(99L, 1L, 10L);
        then(loadSeasonPort).shouldHaveNoInteractions();
        then(saveSeasonPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다른 기수 중앙 회장단은 요청 기수의 시즌을 생성할 수 없다")
    void createSeasonRejectsDifferentGisuCentralPresident() {
        CreateRecruitingSeasonCommand command = CreateRecruitingSeasonCommand.builder()
            .requesterMemberId(99L)
            .gisuId(1L)
            .schoolId(10L)
            .build();
        given(getChallengerRoleUseCase.isCentralCoreInGisu(99L, 1L)).willReturn(false);

        assertThatThrownBy(() -> sut.createSeason(command))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_SEASON_CREATION_FORBIDDEN);

        then(getChallengerRoleUseCase).should().isCentralCoreInGisu(99L, 1L);
        then(loadSeasonPort).shouldHaveNoInteractions();
        then(saveSeasonPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("시즌 쿼터는 0명을 포함해 전체 교체할 수 있다")
    void replaceSeasonQuotas() {
        RecruitingSeason season = season(10L);
        given(loadSeasonPort.getById(10L)).willReturn(season);
        given(loadRoundPort.listBySeasonId(10L)).willReturn(List.of());
        given(loadQuotaPort.listBySeasonIdForUpdate(10L)).willReturn(List.of());
        ReplaceRecruitingSeasonTrackQuotasCommand command = replaceCommand(
            RecruitingSeasonTrackQuotaCommand.of(ChallengerTrack.PLAN, 0),
            RecruitingSeasonTrackQuotaCommand.of(ChallengerTrack.DESIGN, 4)
        );

        sut.replaceQuotas(command);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RecruitingSeasonTrackQuota>> captor = ArgumentCaptor.forClass(List.class);
        then(saveQuotaPort).should().saveAll(captor.capture());
        assertThat(captor.getValue())
            .extracting(RecruitingSeasonTrackQuota::getTargetCount)
            .containsExactly(0, 4);
    }

    @Test
    @DisplayName("TO는 현재 READY와 REGISTERED 합계보다 작게 줄일 수 없다")
    void replaceQuotaCannotGoBelowReservedAndRegistered() {
        RecruitingSeason season = season(10L);
        RecruitingSeasonTrackQuota current = RecruitingSeasonTrackQuota.create(
            season,
            ChallengerTrack.DESIGN,
            5
        );
        given(loadSeasonPort.getById(10L)).willReturn(season);
        given(loadQuotaPort.listBySeasonIdForUpdate(10L)).willReturn(List.of(current));
        given(loadApplicationPort.countReservedOrRegisteredBySeasonIdAndTrack(10L, ChallengerTrack.DESIGN))
            .willReturn(3L);

        assertThatThrownBy(() -> sut.replaceQuotas(replaceCommand(
            RecruitingSeasonTrackQuotaCommand.of(ChallengerTrack.DESIGN, 2)
        )))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_QUOTA_BELOW_RESERVED);

        then(saveQuotaPort).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("기존 차수의 모집 트랙은 쿼터 교체 후에도 양수여야 한다")
    void replaceQuotasRequiresPositiveTargetForExistingRound() {
        RecruitingSeason season = season(10L);
        RecruitingRound round = RecruitingRound.createRegular(
            season,
            roundConfiguration(ChallengerTrack.PLAN)
        );
        given(loadSeasonPort.getById(10L)).willReturn(season);
        given(loadRoundPort.listBySeasonId(10L)).willReturn(List.of(round));
        given(loadQuotaPort.listBySeasonIdForUpdate(10L)).willReturn(List.of());

        assertThatThrownBy(() -> sut.replaceQuotas(replaceCommand(
            RecruitingSeasonTrackQuotaCommand.of(ChallengerTrack.PLAN, 0)
        )))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_TRACK_NOT_IN_SEASON);
        then(saveQuotaPort).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("모집 시즌 공유 메모를 변경한다")
    void updateSeason() {
        RecruitingSeason season = season(10L);
        given(loadSeasonPort.getById(10L)).willReturn(season);

        sut.updateSeason(UpdateRecruitingSeasonCommand.builder()
            .seasonId(10L)
            .memo("운영진 메모")
            .build());

        assertThat(season.getMemo()).isEqualTo("운영진 메모");
        then(saveSeasonPort).should().save(season);
    }

    private ReplaceRecruitingSeasonTrackQuotasCommand replaceCommand(
        RecruitingSeasonTrackQuotaCommand... quotas
    ) {
        return ReplaceRecruitingSeasonTrackQuotasCommand.builder()
            .seasonId(10L)
            .quotas(List.of(quotas))
            .build();
    }

    private RecruitingSeason season(Long id) {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", id);
        return season;
    }

    private RecruitingRoundConfiguration roundConfiguration(ChallengerTrack track) {
        return RecruitingRoundConfiguration.of(
            List.of(track),
            false,
            java.time.Instant.parse("2026-08-01T00:00:00Z"),
            java.time.Instant.parse("2026-08-08T00:00:00Z"),
            java.time.Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            java.time.Instant.parse("2026-08-16T00:00:00Z"),
            null,
            null,
            null
        );
    }
}
