package com.umc.product.recruiting.application.service.command;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.dto.UpdateFormCommand;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.UnpublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UnpublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingRoundCommandService implements
    CreateRecruitingRoundUseCase,
    UpdateRecruitingRoundStatusUseCase,
    UpdateRecruitingRoundUseCase {

    private final LoadRecruitingSeasonPort loadSeasonPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final SaveRecruitingRoundPort saveRoundPort;
    private final LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;
    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final PublishRecruitingApplicationFormUseCase publishApplicationFormUseCase;
    private final CloseRecruitingApplicationFormUseCase closeApplicationFormUseCase;
    private final UnpublishRecruitingApplicationFormUseCase unpublishApplicationFormUseCase;
    private final ManageFormUseCase manageFormUseCase;
    private final GetFormUseCase getFormUseCase;
    private final GetFormResponseUseCase getFormResponseUseCase;

    @Override
    public Long createRound(CreateRecruitingRoundCommand command) {
        RecruitingSeason season = loadSeasonPort.getByIdForUpdate(command.seasonId());
        Integer roundNo = resolveRoundNo(command);
        RecruitingRoundType type = command.type();
        if (loadRoundPort.existsBySeasonIdAndTypeAndRoundNo(command.seasonId(), type, roundNo)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_ALREADY_EXISTS);
        }
        validateTitleAvailable(command.seasonId(), command.title(), null);

        RecruitingRoundConfiguration configuration = command.configuration().toDomain();
        validateRecruitableTrackSubset(command.seasonId(), configuration.recruitableTracks());
        RecruitingRound round = type == RecruitingRoundType.REGULAR
            ? RecruitingRound.createRegular(season, command.title(), configuration)
            : RecruitingRound.createAdditional(season, roundNo, command.title(), configuration);
        return saveRoundPort.save(round).getId();
    }

    @Override
    public void updateRound(UpdateRecruitingRoundCommand command) {
        RecruitingRound round = getRoundInSeason(command.roundId(), command.seasonId());
        validateTitleAvailable(command.seasonId(), command.title(), command.roundId());
        RecruitingRoundConfiguration configuration = command.configuration().toDomain();
        validateRecruitableTrackSubset(command.seasonId(), configuration.recruitableTracks());
        if (round.getStatus() == RecruitingRoundStatus.OPEN && configuration.interviewRequired()) {
            validateAvailabilityFormForOpen(
                configuration.availabilityFormId(),
                configuration.availabilityScheduleQuestionId()
            );
        }
        String previousTitle = round.getTitle();
        round.update(command.title(), configuration, loadApplicationPort.existsByRoundId(round.getId()));
        if (!Objects.equals(previousTitle, round.getTitle())) {
            syncFormTitle(round, command.requesterMemberId());
        }
        // TODO: Form 기간 설정 공개 UseCase가 제공되면 연결된 Form에도 변경된 서류 기간을 재동기화한다.
        saveRoundPort.save(round);
    }

    @Override
    public void updateRoundStatus(UpdateRecruitingRoundStatusCommand command) {
        RecruitingRound round = getRoundInSeasonForUpdate(command.roundId(), command.seasonId());
        RecruitingRoundStatus status = command.status();
        if (status == RecruitingRoundStatus.OPEN) {
            validateAvailabilityFormForOpen(round);
            var applicationForm = loadApplicationFormPort.findByRoundId(round.getId())
                .orElseThrow(() -> new RecruitingDomainException(
                    RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND
                ));
            round.open();
            publishApplicationFormUseCase.publish(PublishRecruitingApplicationFormCommand.builder()
                .seasonId(command.seasonId())
                .applicationFormId(applicationForm.getId())
                .requesterMemberId(command.requesterMemberId())
                .build());
        } else if (status == RecruitingRoundStatus.CLOSED) {
            var applicationForm = loadApplicationFormPort.findByRoundId(round.getId())
                .orElseThrow(() -> new RecruitingDomainException(
                    RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND
                ));
            round.close();
            closeApplicationFormUseCase.close(CloseRecruitingApplicationFormCommand.builder()
                .seasonId(command.seasonId())
                .applicationFormId(applicationForm.getId())
                .build());
        } else if (status == RecruitingRoundStatus.DRAFT) {
            var applicationForm = loadApplicationFormPort.findByRoundId(round.getId())
                .orElseThrow(() -> new RecruitingDomainException(
                    RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND
                ));
            if (loadApplicationPort.existsByRoundId(round.getId())
                || getFormResponseUseCase.existsByFormId(applicationForm.getFormId())) {
                throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_UNPUBLISH_CONFLICT);
            }
            round.unpublish();
            unpublishApplicationFormUseCase.unpublish(UnpublishRecruitingApplicationFormCommand.builder()
                .seasonId(command.seasonId())
                .applicationFormId(applicationForm.getId())
                .requesterMemberId(command.requesterMemberId())
                .build());
        } else {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_TRANSITION);
        }
        saveRoundPort.save(round);
    }

    private void validateAvailabilityFormForOpen(RecruitingRound round) {
        if (!round.isInterviewRequired()) {
            return;
        }
        validateAvailabilityFormForOpen(
            round.getAvailabilityFormId(),
            round.getAvailabilityScheduleQuestionId()
        );
    }

    private void validateAvailabilityFormForOpen(
        Long availabilityFormId,
        Long availabilityScheduleQuestionId
    ) {
        if (availabilityFormId == null || availabilityScheduleQuestionId == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_SCHEDULE);
        }
        FormWithStructureInfo form;
        try {
            form = getFormUseCase.getFormWithStructure(availabilityFormId);
        } catch (BusinessException ignored) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_SCHEDULE);
        }
        List<FormWithStructureInfo.QuestionWithOptions> questions = form.sections().stream()
            .flatMap(section -> section.questions().stream())
            .toList();
        boolean designatedQuestionValid = questions.stream()
            .anyMatch(question -> Objects.equals(question.questionId(), availabilityScheduleQuestionId)
                && question.type() == QuestionType.SCHEDULE
                && question.isRequired());
        long requiredQuestionCount = questions.stream()
            .filter(FormWithStructureInfo.QuestionWithOptions::isRequired)
            .count();
        if (form.status() != FormStatus.PUBLISHED
            || form.isAnonymous()
            || !designatedQuestionValid
            || requiredQuestionCount != 1) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_SCHEDULE);
        }
    }

    private Integer resolveRoundNo(CreateRecruitingRoundCommand command) {
        if (command.type() == RecruitingRoundType.REGULAR) {
            if (command.roundNo() != null && command.roundNo() != 1) {
                throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_ROUND_NO);
            }
            return 1;
        }
        int expectedRoundNo = loadRoundPort.getMaxAdditionalRoundNo(command.seasonId()) + 1;
        if (command.roundNo() != null && command.roundNo() != expectedRoundNo) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_NO_SEQUENCE_CONFLICT);
        }
        return expectedRoundNo;
    }

    private void validateTitleAvailable(Long seasonId, String title, Long excludedRoundId) {
        String normalizedTitle = RecruitingRound.normalizeTitle(title);
        boolean exists = excludedRoundId == null
            ? loadRoundPort.existsBySeasonIdAndTitleIgnoreCase(seasonId, normalizedTitle)
            : loadRoundPort.existsBySeasonIdAndTitleIgnoreCaseAndIdNot(
                seasonId,
                normalizedTitle,
                excludedRoundId
            );
        if (exists) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_TITLE_ALREADY_EXISTS);
        }
    }

    private RecruitingRound getRoundInSeason(Long roundId, Long seasonId) {
        RecruitingRound round = loadRoundPort.getById(roundId);
        validateRoundInSeason(round, seasonId);
        return round;
    }

    private RecruitingRound getRoundInSeasonForUpdate(Long roundId, Long seasonId) {
        RecruitingRound round = loadRoundPort.getByIdForUpdate(roundId);
        validateRoundInSeason(round, seasonId);
        return round;
    }

    private void validateRoundInSeason(RecruitingRound round, Long seasonId) {
        if (!Objects.equals(round.getSeason().getId(), seasonId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND);
        }
    }

    private void validateRecruitableTrackSubset(Long seasonId, List<ChallengerTrack> recruitableTracks) {
        Set<ChallengerTrack> seasonTracks = loadQuotaPort.listBySeasonId(seasonId).stream()
            .filter(quota -> quota.getTargetCount() > 0)
            .map(RecruitingSeasonTrackQuota::getTrack)
            .collect(Collectors.toSet());
        if (!seasonTracks.containsAll(recruitableTracks)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_TRACK_NOT_IN_SEASON);
        }
    }

    private void syncFormTitle(RecruitingRound round, Long requesterMemberId) {
        loadApplicationFormPort.findByRoundId(round.getId()).ifPresent(applicationForm -> {
            var form = getFormUseCase.getFormWithStructure(applicationForm.getFormId());
            manageFormUseCase.updateForm(UpdateFormCommand.builder()
                .formId(applicationForm.getFormId())
                .requesterMemberId(requesterMemberId)
                .title(round.getTitle())
                .description(form.description())
                .clearDescription(form.description() == null)
                .isAnonymous(form.isAnonymous())
                .allowDuplicateResponses(form.allowDuplicateResponses())
                .build());
        });
    }
}
