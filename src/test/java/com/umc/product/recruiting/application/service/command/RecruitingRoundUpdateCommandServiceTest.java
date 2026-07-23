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
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.UnpublishRecruitingApplicationFormUseCase;
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
    UnpublishRecruitingApplicationFormUseCase unpublishApplicationFormUseCase;
    @Mock
    ManageFormUseCase manageFormUseCase;
    @Mock
    GetFormUseCase getFormUseCase;
    @Mock
    GetFormResponseUseCase getFormResponseUseCase;
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
        given(loadRoundPort.getByIdForUpdate(20L)).willReturn(round);
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
    @DisplayName("면접 차수는 availability Form이 게시되어야 OPEN으로 전환한다")
    void openInterviewRoundWithPublishedAvailabilityForm() {
        RecruitingRound round = interviewRound(20L, season(10L), 500L);
        RecruitingApplicationForm applicationForm = RecruitingApplicationForm.create(round, 100L);
        ReflectionTestUtils.setField(applicationForm, "id", 30L);
        given(loadRoundPort.getByIdForUpdate(20L)).willReturn(round);
        given(getFormUseCase.getById(500L)).willReturn(FormInfo.builder()
            .id(500L)
            .status(FormStatus.PUBLISHED)
            .build());
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(applicationForm));

        sut.updateRoundStatus(UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .status(RecruitingRoundStatus.OPEN)
            .requesterMemberId(99L)
            .build());

        assertThat(round.getStatus()).isEqualTo(RecruitingRoundStatus.OPEN);
        then(publishApplicationFormUseCase).should().publish(any());
    }

    @Test
    @DisplayName("게시되지 않은 availability Form으로는 면접 차수를 OPEN할 수 없다")
    void rejectOpenInterviewRoundWithDraftAvailabilityForm() {
        RecruitingRound round = interviewRound(20L, season(10L), 500L);
        given(loadRoundPort.getByIdForUpdate(20L)).willReturn(round);
        given(getFormUseCase.getById(500L)).willReturn(FormInfo.builder()
            .id(500L)
            .status(FormStatus.DRAFT)
            .build());

        assertThatThrownBy(() -> sut.updateRoundStatus(UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .status(RecruitingRoundStatus.OPEN)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_INVALID_SCHEDULE);

        then(publishApplicationFormUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원서와 Form 응답이 없는 OPEN 차수는 DRAFT로 비공개할 수 있다")
    void unpublishOpenRoundWithoutResponses() {
        RecruitingRound round = round(20L, season(10L));
        round.open();
        RecruitingApplicationForm applicationForm = RecruitingApplicationForm.create(round, 100L);
        ReflectionTestUtils.setField(applicationForm, "id", 30L);
        applicationForm.publish(round.getRecruitableTracks());
        given(loadRoundPort.getByIdForUpdate(20L)).willReturn(round);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(applicationForm));

        sut.updateRoundStatus(UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .status(RecruitingRoundStatus.DRAFT)
            .requesterMemberId(99L)
            .build());

        assertThat(round.getStatus()).isEqualTo(RecruitingRoundStatus.DRAFT);
        then(unpublishApplicationFormUseCase).should().unpublish(any());
        then(saveRoundPort).should().save(round);
    }

    @Test
    @DisplayName("지원서가 있는 OPEN 차수는 DRAFT로 비공개할 수 없다")
    void rejectUnpublishWhenApplicationExists() {
        RecruitingRound round = round(20L, season(10L));
        round.open();
        RecruitingApplicationForm applicationForm = RecruitingApplicationForm.create(round, 100L);
        ReflectionTestUtils.setField(applicationForm, "id", 30L);
        given(loadRoundPort.getByIdForUpdate(20L)).willReturn(round);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(applicationForm));
        given(loadApplicationPort.existsByRoundId(20L)).willReturn(true);

        assertThatThrownBy(() -> sut.updateRoundStatus(UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .status(RecruitingRoundStatus.DRAFT)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_UNPUBLISH_CONFLICT);

        then(unpublishApplicationFormUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("실제 Form 응답이 있는 OPEN 차수는 DRAFT로 비공개할 수 없다")
    void rejectUnpublishWhenFormResponseExists() {
        RecruitingRound round = round(20L, season(10L));
        round.open();
        RecruitingApplicationForm applicationForm = RecruitingApplicationForm.create(round, 100L);
        ReflectionTestUtils.setField(applicationForm, "id", 30L);
        given(loadRoundPort.getByIdForUpdate(20L)).willReturn(round);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(applicationForm));
        given(getFormResponseUseCase.existsByFormId(100L)).willReturn(true);

        assertThatThrownBy(() -> sut.updateRoundStatus(UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .status(RecruitingRoundStatus.DRAFT)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_UNPUBLISH_CONFLICT);
    }

    @Test
    @DisplayName("다른 시즌의 차수 상태를 변경할 수 없다")
    void updateRoundStatusRejectsDifferentSeason() {
        RecruitingRound round = round(20L, season(10L));
        given(loadRoundPort.getByIdForUpdate(20L)).willReturn(round);

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

    @Test
    @DisplayName("CLOSED 차수는 DRAFT로 되돌릴 수 없고 Form 게시 취소를 호출하지 않는다")
    void rejectUnpublishClosedRoundBeforeFormMutation() {
        RecruitingRound round = round(20L, season(10L));
        round.open();
        round.close();
        RecruitingApplicationForm applicationForm = RecruitingApplicationForm.create(round, 100L);
        ReflectionTestUtils.setField(applicationForm, "id", 30L);
        given(loadRoundPort.getByIdForUpdate(20L)).willReturn(round);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(applicationForm));

        assertThatThrownBy(() -> sut.updateRoundStatus(UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .status(RecruitingRoundStatus.DRAFT)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_INVALID_TRANSITION);

        then(unpublishApplicationFormUseCase).shouldHaveNoInteractions();
        then(saveRoundPort).shouldHaveNoInteractions();
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

    private RecruitingRound interviewRound(Long id, RecruitingSeason season, Long availabilityFormId) {
        RecruitingRound round = RecruitingRound.createRegular(season, RecruitingRoundConfigurationCommand.of(
            List.of(ChallengerTrack.PLAN),
            false,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            true,
            Instant.parse("2026-08-11T00:00:00Z"),
            Instant.parse("2026-08-14T00:00:00Z"),
            Instant.parse("2026-08-16T00:00:00Z"),
            availabilityFormId,
            null,
            null
        ).toDomain());
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }
}
