package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundConfigurationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingRoundUpdateCommandServiceTest {

    @Mock
    LoadRecruitingSeasonPort loadSeasonPort;
    @Mock
    LoadRecruitingRoundPort loadRoundPort;
    @Mock
    SaveRecruitingRoundPort saveRoundPort;
    @Mock
    LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;
    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;
    @Mock
    LoadRecruitingApplicationFormPort loadApplicationFormPort;
    @Mock
    PublishRecruitingApplicationFormUseCase publishApplicationFormUseCase;
    @Mock
    CloseRecruitingApplicationFormUseCase closeApplicationFormUseCase;
    @Mock
    ManageFormUseCase manageFormUseCase;
    @Mock
    GetFormUseCase getFormUseCase;
    @InjectMocks
    RecruitingRoundCommandService sut;

    @Test
    @DisplayName("양수 쿼터 트랙으로 차수 설정을 변경한다")
    void updateRoundConfiguration() {
        RecruitingSeason season = season(10L);
        RecruitingRound round = round(20L, season);
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadQuotaPort.listBySeasonId(10L)).willReturn(List.of(
            RecruitingSeasonTrackQuota.create(season, ChallengerTrack.DESIGN, 4)
        ));

        sut.updateRound(UpdateRecruitingRoundCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .title("본모집")
            .configuration(configuration(ChallengerTrack.DESIGN))
            .build());

        assertThat(round.getRecruitableTracks()).containsExactly(ChallengerTrack.DESIGN);
        then(saveRoundPort).should().save(round);
    }

    @Test
    @DisplayName("지원서가 있으면 DRAFT 차수의 모집 트랙을 변경할 수 없다")
    void rejectRecruitableTrackChangeWhenApplicationExists() {
        RecruitingSeason season = season(10L);
        RecruitingRound round = configuredRound(20L, season, ChallengerTrack.PLAN, false);
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadApplicationPort.existsByRoundId(20L)).willReturn(true);
        given(loadQuotaPort.listBySeasonId(10L)).willReturn(List.of(
            RecruitingSeasonTrackQuota.create(season, ChallengerTrack.DESIGN, 4)
        ));

        assertThatThrownBy(() -> sut.updateRound(UpdateRecruitingRoundCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .title("본모집")
            .configuration(configuration(ChallengerTrack.DESIGN))
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_RECRUITMENT_POLICY_LOCKED);

        then(saveRoundPort).should(never()).save(any());
    }

    @Test
    @DisplayName("지원서가 있으면 DRAFT 차수의 2지망 정책을 변경할 수 없다")
    void rejectSecondChoicePolicyChangeWhenApplicationExists() {
        RecruitingSeason season = season(10L);
        RecruitingRound round = configuredRound(20L, season, ChallengerTrack.PLAN, false);
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadApplicationPort.existsByRoundId(20L)).willReturn(true);
        given(loadQuotaPort.listBySeasonId(10L)).willReturn(List.of(
            RecruitingSeasonTrackQuota.create(season, ChallengerTrack.PLAN, 4)
        ));

        assertThatThrownBy(() -> sut.updateRound(UpdateRecruitingRoundCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .title("본모집")
            .configuration(configuration(ChallengerTrack.PLAN, true, null, null))
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_RECRUITMENT_POLICY_LOCKED);

        then(saveRoundPort).should(never()).save(any());
    }

    @Test
    @DisplayName("OPEN 차수의 모집 트랙은 지원서가 없어도 변경할 수 없다")
    void rejectRecruitableTrackChangeWhenRoundIsOpen() {
        RecruitingSeason season = season(10L);
        RecruitingRound round = configuredRound(20L, season, ChallengerTrack.PLAN, false);
        round.open();
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadQuotaPort.listBySeasonId(10L)).willReturn(List.of(
            RecruitingSeasonTrackQuota.create(season, ChallengerTrack.DESIGN, 4)
        ));

        assertThatThrownBy(() -> sut.updateRound(UpdateRecruitingRoundCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .title("본모집")
            .configuration(configuration(ChallengerTrack.DESIGN))
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_RECRUITMENT_POLICY_LOCKED);

        then(saveRoundPort).should(never()).save(any());
    }

    @Test
    @DisplayName("OPEN 차수에서도 모집 정책을 유지하면 일정과 공지를 변경할 수 있다")
    void updateScheduleAndAnnouncementWhileOpen() {
        RecruitingSeason season = season(10L);
        RecruitingRound round = configuredRound(20L, season, ChallengerTrack.PLAN, false);
        round.open();
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadApplicationPort.existsByRoundId(20L)).willReturn(true);
        given(loadQuotaPort.listBySeasonId(10L)).willReturn(List.of(
            RecruitingSeasonTrackQuota.create(season, ChallengerTrack.PLAN, 4)
        ));

        sut.updateRound(UpdateRecruitingRoundCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .title("본모집")
            .configuration(configuration(ChallengerTrack.PLAN, "변경 공지", "010-0000-0000"))
            .build());

        assertThat(round.getAnnouncement()).isEqualTo("변경 공지");
        assertThat(round.getContactText()).isEqualTo("010-0000-0000");
        then(saveRoundPort).should().save(round);
    }

    @Test
    @DisplayName("CLOSED 차수에서도 모집 정책을 유지하면 일정과 연락처를 변경할 수 있다")
    void updateScheduleAndContactWhileClosed() {
        RecruitingSeason season = season(10L);
        RecruitingRound round = configuredRound(20L, season, ChallengerTrack.PLAN, false);
        round.open();
        round.close();
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadApplicationPort.existsByRoundId(20L)).willReturn(true);
        given(loadQuotaPort.listBySeasonId(10L)).willReturn(List.of(
            RecruitingSeasonTrackQuota.create(season, ChallengerTrack.PLAN, 4)
        ));

        sut.updateRound(UpdateRecruitingRoundCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .title("본모집")
            .configuration(configuration(ChallengerTrack.PLAN, "마감 공지", "문의 채널"))
            .build());

        assertThat(round.getAnnouncement()).isEqualTo("마감 공지");
        assertThat(round.getContactText()).isEqualTo("문의 채널");
        then(saveRoundPort).should().save(round);
    }

    @Test
    @DisplayName("다른 시즌의 차수 설정을 변경할 수 없다")
    void updateRoundRejectsDifferentSeason() {
        RecruitingRound round = round(20L, season(10L));
        given(loadRoundPort.getById(20L)).willReturn(round);

        assertThatThrownBy(() -> sut.updateRound(UpdateRecruitingRoundCommand.builder()
            .seasonId(999L)
            .roundId(20L)
            .title("본모집")
            .configuration(configuration(ChallengerTrack.PLAN))
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND);
    }

    @Test
    @DisplayName("같은 시즌의 차수 상태를 OPEN으로 변경한다")
    void updateRoundStatus() {
        RecruitingRound round = round(20L, season(10L));
        RecruitingApplicationForm applicationForm = RecruitingApplicationForm.create(round, 100L);
        ReflectionTestUtils.setField(applicationForm, "id", 30L);
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(applicationForm));

        sut.updateRoundStatus(UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .status(RecruitingRoundStatus.OPEN)
            .build());

        assertThat(round.getStatus()).isEqualTo(RecruitingRoundStatus.OPEN);
        then(publishApplicationFormUseCase).should().publish(any());
        then(saveRoundPort).should().save(round);
    }

    @Test
    @DisplayName("다른 시즌의 차수 상태를 변경할 수 없다")
    void updateRoundStatusRejectsDifferentSeason() {
        RecruitingRound round = round(20L, season(10L));
        given(loadRoundPort.getById(20L)).willReturn(round);

        assertThatThrownBy(() -> sut.updateRoundStatus(UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(999L)
            .roundId(20L)
            .status(RecruitingRoundStatus.OPEN)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND);
        then(saveRoundPort).should(never()).save(any());
    }

    private RecruitingRoundConfigurationCommand configuration(ChallengerTrack track) {
        return configuration(track, null, null);
    }

    private RecruitingRoundConfigurationCommand configuration(
        ChallengerTrack track,
        String announcement,
        String contactText
    ) {
        return configuration(track, false, announcement, contactText);
    }

    private RecruitingRoundConfigurationCommand configuration(
        ChallengerTrack track,
        boolean secondChoiceEnabled,
        String announcement,
        String contactText
    ) {
        return RecruitingRoundConfigurationCommand.of(
            List.of(track),
            secondChoiceEnabled,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            announcement,
            contactText
        );
    }

    private RecruitingSeason season(Long id) {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", id);
        return season;
    }

    private RecruitingRound round(Long id, RecruitingSeason season) {
        RecruitingRound round = RecruitingRound.createRegular(season);
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }

    private RecruitingRound configuredRound(
        Long id,
        RecruitingSeason season,
        ChallengerTrack track,
        boolean secondChoiceEnabled
    ) {
        RecruitingRoundConfigurationCommand command = configuration(track);
        RecruitingRound round = RecruitingRound.createRegular(
            season,
            RecruitingRoundConfigurationCommand.of(
                command.recruitableTracks(),
                secondChoiceEnabled,
                command.documentStartAt(),
                command.documentEndAt(),
                command.documentResultPublishedAt(),
                command.interviewRequired(),
                command.interviewStartAt(),
                command.interviewEndAt(),
                command.finalResultPublishedAt(),
                command.availabilityFormId(),
                command.announcement(),
                command.contactText()
            ).toDomain()
        );
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }
}
