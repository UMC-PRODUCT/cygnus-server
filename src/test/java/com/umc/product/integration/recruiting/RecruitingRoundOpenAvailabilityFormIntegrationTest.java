package com.umc.product.integration.recruiting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpsertRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingSeasonPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.support.IntegrationTestSupport;

/**
 * 면접 차수 OPEN 전이에서 프로비저너가 만든 Form이 곧바로 뒤따르는 검증을 통과하는지 확인한다.
 * <p>
 * 서비스 단위 테스트는 provisioner와 GetFormUseCase를 각각 stub 하므로 두 축의 결합을 증명하지 못한다.
 * 실제 Form 모듈과 DB를 함께 태워야 게시 누락, 익명 설정, 질문 구조, 같은 트랜잭션 내 가시성을 잡을 수 있다.
 */
class RecruitingRoundOpenAvailabilityFormIntegrationTest extends IntegrationTestSupport {

    private static final Long REQUESTER_MEMBER_ID = 9001L;
    private static final ChallengerTrack RECRUITABLE_TRACK = ChallengerTrack.WEB_PRODUCT_ENGINEER;

    @Autowired
    UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;

    @Autowired
    UpsertRecruitingApplicationFormUseCase upsertApplicationFormUseCase;

    @Autowired
    SaveRecruitingSeasonPort saveSeasonPort;

    @Autowired
    SaveRecruitingRoundPort saveRoundPort;

    @Autowired
    LoadRecruitingRoundPort loadRoundPort;

    @Autowired
    GetFormUseCase getFormUseCase;

    @Test
    @DisplayName("면접 차수를 OPEN하면 자동 생성된 조율 Form이 OPEN 검증을 그대로 통과한다")
    void openInterviewRoundProvisionsAvailabilityFormThatPassesValidation() {
        RecruitingSeason season = saveSeasonPort.save(RecruitingSeason.create(9L, 101L));
        RecruitingRound round = saveRoundPort.save(RecruitingRound.createRegular(
            season,
            "본모집",
            interviewConfigurationWithoutMapping()
        ));
        upsertApplicationForm(season.getId(), round.getId());

        updateRoundStatusUseCase.updateRoundStatus(UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(season.getId())
            .roundId(round.getId())
            .status(RecruitingRoundStatus.OPEN)
            .requesterMemberId(REQUESTER_MEMBER_ID)
            .build());

        RecruitingRound opened = loadRoundPort.getById(round.getId());
        assertThat(opened.getStatus()).isEqualTo(RecruitingRoundStatus.OPEN);
        assertThat(opened.getAvailabilityFormId()).isNotNull();
        assertThat(opened.getAvailabilityScheduleQuestionId()).isNotNull();

        FormWithStructureInfo availabilityForm =
            getFormUseCase.getFormWithStructure(opened.getAvailabilityFormId());
        assertThat(availabilityForm.status()).isEqualTo(FormStatus.PUBLISHED);
        assertThat(availabilityForm.isAnonymous()).isFalse();

        List<FormWithStructureInfo.QuestionWithOptions> questions = availabilityForm.sections().stream()
            .flatMap(section -> section.questions().stream())
            .toList();
        assertThat(questions.stream().filter(FormWithStructureInfo.QuestionWithOptions::isRequired))
            .singleElement()
            .satisfies(question -> {
                assertThat(question.questionId()).isEqualTo(opened.getAvailabilityScheduleQuestionId());
                assertThat(question.type()).isEqualTo(QuestionType.SCHEDULE);
            });
    }

    private void upsertApplicationForm(Long seasonId, Long roundId) {
        upsertApplicationFormUseCase.upsert(UpsertRecruitingApplicationFormCommand.builder()
            .seasonId(seasonId)
            .roundId(roundId)
            .requesterMemberId(REQUESTER_MEMBER_ID)
            .description("지원서")
            .sections(List.of(
                UpsertRecruitingApplicationFormCommand.SectionEntry.builder()
                    .clientKey("common")
                    .title("공통")
                    .type(RecruitingFormSectionType.COMMON)
                    .questions(List.of(UpsertRecruitingApplicationFormCommand.QuestionEntry.builder()
                        .type(QuestionType.SHORT_TEXT)
                        .title("지원 동기")
                        .required(true)
                        .build()))
                    .build(),
                UpsertRecruitingApplicationFormCommand.SectionEntry.builder()
                    .clientKey("track")
                    .title("파트 문항")
                    .type(RecruitingFormSectionType.TRACK)
                    .track(RECRUITABLE_TRACK)
                    .questions(List.of(UpsertRecruitingApplicationFormCommand.QuestionEntry.builder()
                        .type(QuestionType.SHORT_TEXT)
                        .title("사용 기술")
                        .required(true)
                        .build()))
                    .build()
            ))
            .build());
    }

    private RecruitingRoundConfiguration interviewConfigurationWithoutMapping() {
        return RecruitingRoundConfiguration.of(
            List.of(RECRUITABLE_TRACK),
            false,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            true,
            Instant.parse("2026-08-11T00:00:00Z"),
            Instant.parse("2026-08-15T00:00:00Z"),
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            null,
            null,
            "문의 채널"
        );
    }
}
