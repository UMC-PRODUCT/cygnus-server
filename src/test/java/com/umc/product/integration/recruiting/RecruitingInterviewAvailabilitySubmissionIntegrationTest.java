package com.umc.product.integration.recruiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.command.ManageFormSectionUseCase;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.CreateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingSeasonPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.support.IntegrationTestSupport;

class RecruitingInterviewAvailabilitySubmissionIntegrationTest extends IntegrationTestSupport {

    private static final Long APPLICANT_MEMBER_ID = 1001L;
    private static final Instant INTERVIEW_START_AT = Instant.parse("2026-08-11T00:00:00Z");

    @Autowired
    ManageFormUseCase manageFormUseCase;

    @Autowired
    ManageFormSectionUseCase manageFormSectionUseCase;

    @Autowired
    ManageQuestionUseCase manageQuestionUseCase;

    @Autowired
    GetFormResponseUseCase getFormResponseUseCase;

    @Autowired
    ManageRecruitingInterviewScheduleUseCase manageScheduleUseCase;

    @Autowired
    SaveRecruitingSeasonPort saveSeasonPort;

    @Autowired
    SaveRecruitingRoundPort saveRoundPort;

    @Autowired
    SaveRecruitingApplicationFormPort saveApplicationFormPort;

    @Autowired
    SaveRecruitingApplicationPort saveApplicationPort;

    @Autowired
    SaveRecruitingInterviewSchedulePort saveSchedulePort;

    @Autowired
    LoadRecruitingInterviewSchedulePort loadSchedulePort;

    @Test
    @DisplayName("면접 가능 시간을 실제 Form SCHEDULE 응답으로 최종 제출하고 일정을 전이한다")
    void 면접_가능_시간을_실제_Form_SCHEDULE_응답으로_최종_제출하고_일정을_전이한다() {
        Fixture fixture = fixture();
        List<Instant> times = List.of(
            INTERVIEW_START_AT,
            Instant.parse("2026-08-12T01:15:00Z")
        );

        manageScheduleUseCase.submitAvailability(command(fixture.applicationId(), times));

        RecruitingInterviewSchedule schedule = loadSchedulePort.getByApplicationId(fixture.applicationId());
        FormResponseWithAnswersInfo response = getFormResponseUseCase.getResponseWithAnswers(
            schedule.getAvailabilityFormResponseId()
        );
        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.AVAILABILITY_SUBMITTED);
        assertThat(response.formId()).isEqualTo(fixture.formId());
        assertThat(response.respondentMemberId()).isEqualTo(APPLICANT_MEMBER_ID);
        assertThat(response.status()).isEqualTo(FormResponseStatus.SUBMITTED);
        assertThat(response.answers()).singleElement().satisfies(answer -> {
            assertThat(answer.questionId()).isEqualTo(fixture.questionId());
            assertThat(answer.answeredAsType()).isEqualTo(QuestionType.SCHEDULE);
            assertThat(answer.times()).containsExactlyInAnyOrderElementsOf(times);
        });
    }

    @Test
    @DisplayName("Form이 빈 SCHEDULE 응답을 거부하면 응답과 일정 상태를 남기지 않는다")
    void Form이_빈_SCHEDULE_응답을_거부하면_응답과_일정_상태를_남기지_않는다() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> manageScheduleUseCase.submitAvailability(
            command(fixture.applicationId(), List.of())
        )).isInstanceOf(FormDomainException.class);

        RecruitingInterviewSchedule schedule = loadSchedulePort.getByApplicationId(fixture.applicationId());
        assertThat(schedule.getStatus()).isEqualTo(RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED);
        assertThat(schedule.getAvailabilityFormResponseId()).isNull();
        assertThat(getFormResponseUseCase.listByFormId(fixture.formId())).isEmpty();
    }

    @Test
    @DisplayName("동시 두 번 제출하면 application 잠금이 직렬화해 한 번만 성공한다")
    void 동시_두_번_제출하면_application_잠금이_직렬화해_한_번만_성공한다() throws Exception {
        Fixture fixture = fixture();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<RuntimeException> submission = () -> {
            ready.countDown();
            start.await();
            try {
                manageScheduleUseCase.submitAvailability(command(
                    fixture.applicationId(),
                    List.of(INTERVIEW_START_AT)
                ));
                return null;
            } catch (RuntimeException exception) {
                return exception;
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(submission);
            var second = executor.submit(submission);
            ready.await();
            start.countDown();
            List<RuntimeException> results = new ArrayList<>();
            results.add(first.get());
            results.add(second.get());

            assertThat(results).filteredOn(result -> result == null).hasSize(1);
            assertThat(results).filteredOn(result -> result != null).singleElement().satisfies(failure -> {
                assertThat(failure).isInstanceOf(RecruitingDomainException.class);
                assertThat(((RecruitingDomainException) failure).getBaseCode())
                    .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION);
            });
        }

        assertThat(getFormResponseUseCase.listByFormId(fixture.formId())).hasSize(1);
        assertThat(loadSchedulePort.getByApplicationId(fixture.applicationId()).getStatus())
            .isEqualTo(RecruitingInterviewScheduleStatus.AVAILABILITY_SUBMITTED);
    }

    private Fixture fixture() {
        Long formId = manageFormUseCase.createDraft(CreateDraftFormCommand.builder()
            .createdMemberId(9001L)
            .title("면접 가능 일정")
            .isAnonymous(false)
            .allowDuplicateResponses(false)
            .build());
        Long sectionId = manageFormSectionUseCase.createSection(CreateFormSectionCommand.builder()
            .formId(formId)
            .requesterMemberId(9001L)
            .title("가능 시간")
            .build());
        Long questionId = manageQuestionUseCase.createQuestion(CreateQuestionCommand.builder()
            .sectionId(sectionId)
            .requesterMemberId(9001L)
            .type(QuestionType.SCHEDULE)
            .title("면접 가능한 시간을 선택해주세요")
            .isRequired(true)
            .build());
        manageFormUseCase.publishForm(PublishFormCommand.builder()
            .formId(formId)
            .requesterMemberId(9001L)
            .build());

        RecruitingSeason season = saveSeasonPort.save(RecruitingSeason.create(9L, 101L));
        RecruitingRound round = saveRoundPort.save(RecruitingRound.createRegular(
            season,
            RecruitingRoundConfiguration.of(
                List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
                false,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-08T00:00:00Z"),
                Instant.parse("2026-08-10T00:00:00Z"),
                true,
                INTERVIEW_START_AT,
                Instant.parse("2026-08-15T00:00:00Z"),
                Instant.parse("2026-08-16T00:00:00Z"),
                formId,
                questionId,
                null,
                "문의 채널"
            )
        ));
        RecruitingApplicationForm applicationForm = saveApplicationFormPort.save(
            RecruitingApplicationForm.create(round, 8001L)
        );
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            applicationForm,
            8002L,
            APPLICANT_MEMBER_ID,
            RecruitingApplicantProfile.create(
                round,
                "지원자",
                RecruitingApplicantEmail.from("availability-integration@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            "A1B2C3"
        );
        application.submit(APPLICANT_MEMBER_ID);
        application.assignInterview(9001L, null);
        application = saveApplicationPort.save(application);
        saveSchedulePort.saveSchedule(RecruitingInterviewSchedule.requestAvailability(
            application,
            "카카오톡 @umc"
        ));
        return new Fixture(formId, questionId, application.getId());
    }

    private SubmitRecruitingInterviewAvailabilityCommand command(Long applicationId, List<Instant> times) {
        return SubmitRecruitingInterviewAvailabilityCommand.of(applicationId, APPLICANT_MEMBER_ID, times);
    }

    private record Fixture(Long formId, Long questionId, Long applicationId) {
    }
}
