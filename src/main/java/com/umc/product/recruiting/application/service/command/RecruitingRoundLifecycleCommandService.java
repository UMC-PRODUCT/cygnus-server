package com.umc.product.recruiting.application.service.command;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.CloneRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.DeleteRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.RestoreRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpsertRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CloneRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundConfigurationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RestoreRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand.OptionEntry;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand.QuestionEntry;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand.SectionEntry;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingRoundLifecycleCommandService implements
    DeleteRecruitingRoundUseCase,
    RestoreRecruitingRoundUseCase,
    CloneRecruitingRoundUseCase {

    private final LoadRecruitingRoundPort loadRoundPort;
    private final SaveRecruitingRoundPort saveRoundPort;
    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final LoadRecruitingFormSectionPolicyPort loadPolicyPort;
    private final LoadRecruitingRoundInterviewQuestionPort loadQuestionPort;
    private final SaveRecruitingRoundInterviewQuestionPort saveQuestionPort;
    private final GetFormUseCase getFormUseCase;
    private final GetFormResponseUseCase getFormResponseUseCase;
    private final CreateRecruitingRoundUseCase createRoundUseCase;
    private final UpsertRecruitingApplicationFormUseCase upsertFormUseCase;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;
    private final Clock clock;

    @Override
    public void deleteRound(DeleteRecruitingRoundCommand command) {
        authorizeManagementUseCase.authorizeSeasonManagement(command.requesterMemberId(), command.seasonId());
        RecruitingRound round = loadRoundPort.getByIdForUpdate(command.roundId());
        validateRoundInSeason(round, command.seasonId());
        if (round.getStatus() != RecruitingRoundStatus.DRAFT
            || loadApplicationPort.existsByRoundId(round.getId())) {
            throw deleteConflict();
        }

        RecruitingApplicationForm applicationForm = loadApplicationFormPort.findByRoundId(round.getId())
            .orElse(null);
        if (applicationForm != null
            && !getFormResponseUseCase.listByFormId(applicationForm.getFormId()).isEmpty()) {
            throw deleteConflict();
        }

        round.delete(clock.instant());
        saveRoundPort.save(round);
    }

    /**
     * 삭제 기간에 제한이 없어, 삭제된 사이에 같은 (유형, 차수 번호)나 제목이 다시 사용될 수 있다.
     * 활성 차수만 대상으로 하는 부분 유니크 인덱스가 슬롯을 풀어 주기 때문이며,
     * 그대로 복구하면 인덱스 위반이 나므로 선점 여부를 먼저 확인한다.
     */
    @Override
    public void restoreRound(RestoreRecruitingRoundCommand command) {
        authorizeManagementUseCase.authorizeSeasonManagement(command.requesterMemberId(), command.seasonId());
        RecruitingRound round = loadRoundPort.getByIdForUpdateIncludingDeleted(command.roundId());
        validateRoundInSeason(round, command.seasonId());
        if (!round.isDeleted()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_NOT_DELETED);
        }
        boolean slotTaken = loadRoundPort.existsBySeasonIdAndTypeAndRoundNo(
            command.seasonId(),
            round.getType(),
            round.getRoundNo()
        );
        if (slotTaken || loadRoundPort.existsBySeasonIdAndTitleIgnoreCase(command.seasonId(), round.getTitle())) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_RESTORE_CONFLICT);
        }

        round.restore();
        saveRoundPort.save(round);
    }

    @Override
    public Long cloneRound(CloneRecruitingRoundCommand command) {
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            command.sourceSeasonId()
        );
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            command.targetSeasonId()
        );
        RecruitingRound source = loadRoundPort.getById(command.sourceRoundId());
        validateRoundInSeason(source, command.sourceSeasonId());
        Long clonedRoundId = createRoundUseCase.createRound(CreateRecruitingRoundCommand.builder()
            .seasonId(command.targetSeasonId())
            .type(command.type())
            .roundNo(command.roundNo())
            .title(command.title())
            .configuration(copyConfiguration(source))
            .requesterMemberId(command.requesterMemberId())
            .build());
        RecruitingRound clonedRound = loadRoundPort.getById(clonedRoundId);

        loadApplicationFormPort.findByRoundId(source.getId()).ifPresent(sourceForm ->
            cloneForm(sourceForm, clonedRound, command.requesterMemberId())
        );
        for (RecruitingRoundInterviewQuestion question : loadQuestionPort.listActiveByRoundId(source.getId())) {
            saveQuestionPort.save(RecruitingRoundInterviewQuestion.create(
                clonedRound,
                question.getContent(),
                question.getOrderNo(),
                command.requesterMemberId()
            ));
        }
        return clonedRoundId;
    }

    private void cloneForm(
        RecruitingApplicationForm sourceForm,
        RecruitingRound clonedRound,
        Long requesterMemberId
    ) {
        FormWithStructureInfo structure = getFormUseCase.getFormWithStructure(sourceForm.getFormId());
        Map<Long, RecruitingFormSectionPolicy> policyBySectionId = loadPolicyPort
            .listByApplicationFormId(sourceForm.getId()).stream()
            .collect(Collectors.toMap(RecruitingFormSectionPolicy::getFormSectionId, Function.identity()));
        Map<Long, String> keyBySectionId = structure.sections().stream()
            .collect(Collectors.toMap(
                FormWithStructureInfo.SectionWithQuestions::sectionId,
                section -> "section-" + section.sectionId()
            ));
        List<SectionEntry> sections = structure.sections().stream()
            .map(section -> toSectionEntry(section, policyBySectionId.get(section.sectionId()), keyBySectionId))
            .toList();
        upsertFormUseCase.upsert(UpsertRecruitingApplicationFormCommand.builder()
            .seasonId(clonedRound.getSeason().getId())
            .roundId(clonedRound.getId())
            .requesterMemberId(requesterMemberId)
            .description(structure.description())
            .sections(sections)
            .build());
    }

    private SectionEntry toSectionEntry(
        FormWithStructureInfo.SectionWithQuestions section,
        RecruitingFormSectionPolicy policy,
        Map<Long, String> keyBySectionId
    ) {
        if (policy == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
        }
        return SectionEntry.builder()
            .clientKey(keyBySectionId.get(section.sectionId()))
            .title(section.title())
            .description(section.description())
            .type(policy.getType())
            .track(policy.getTrack())
            .questions(section.questions().stream()
                .map(question -> QuestionEntry.builder()
                    .type(question.type())
                    .title(question.title())
                    .description(question.description())
                    .required(question.isRequired())
                    .options(question.options().stream()
                        .map(option -> OptionEntry.builder()
                            .content(option.content())
                            .other(option.isOther())
                            .nextSectionKey(keyBySectionId.get(option.nextSectionId()))
                            .build())
                        .toList())
                    .build())
                .toList())
            .build();
    }

    private RecruitingRoundConfigurationCommand copyConfiguration(RecruitingRound source) {
        return RecruitingRoundConfigurationCommand.of(
            source.getRecruitableTracks(),
            source.isSecondChoiceEnabled(),
            source.getDocumentStartAt(),
            source.getDocumentEndAt(),
            source.getDocumentResultPublishedAt(),
            source.isInterviewRequired(),
            source.getInterviewStartAt(),
            source.getInterviewEndAt(),
            source.getFinalResultPublishedAt(),
            null,
            null,
            source.getAnnouncement(),
            source.getContactText()
        );
    }

    private void validateRoundInSeason(RecruitingRound round, Long seasonId) {
        if (!Objects.equals(round.getSeason().getId(), seasonId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND);
        }
    }

    private RecruitingDomainException deleteConflict() {
        return new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_DELETE_CONFLICT);
    }
}
