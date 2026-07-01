package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewEvaluationInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewEvaluationPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingInterviewAssignment;
import com.umc.product.recruiting.domain.RecruitingInterviewEvaluation;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewQueryServiceTest {

    @Mock
    LoadRecruitingInterviewEvaluationPort loadEvaluationPort;

    @InjectMocks
    RecruitingInterviewQueryService sut;

    @Test
    @DisplayName("본인_평가_제출_전에는_타_면접관의_평가를_조회할_수_없다")
    void hidePeerEvaluationsBeforeOwnSubmit() {
        // Given
        RecruitingInterviewEvaluation ownDraft = RecruitingInterviewEvaluation.createDraft(assignment(901L), 901L);
        RecruitingInterviewEvaluation peerSubmitted = RecruitingInterviewEvaluation.createDraft(assignment(902L), 902L);
        peerSubmitted.submit(5, "좋음");
        given(loadEvaluationPort.findByApplicationIdAndEvaluatorMemberId(900L, 901L))
            .willReturn(Optional.of(ownDraft));
        given(loadEvaluationPort.listByApplicationId(900L)).willReturn(List.of(ownDraft, peerSubmitted));

        // When
        List<RecruitingInterviewEvaluationInfo> result = sut.listVisibleEvaluations(900L, 901L);

        // Then
        assertThat(result)
            .extracting(RecruitingInterviewEvaluationInfo::evaluatorMemberId)
            .containsExactly(901L);
    }

    @Test
    @DisplayName("본인_평가_제출_후에는_타_면접관의_평가를_조회할_수_있다")
    void showPeerEvaluationsAfterOwnSubmit() {
        // Given
        RecruitingInterviewEvaluation ownSubmitted = RecruitingInterviewEvaluation.createDraft(assignment(901L), 901L);
        ownSubmitted.submit(4, "보통");
        RecruitingInterviewEvaluation peerSubmitted = RecruitingInterviewEvaluation.createDraft(assignment(902L), 902L);
        peerSubmitted.submit(5, "좋음");
        given(loadEvaluationPort.findByApplicationIdAndEvaluatorMemberId(900L, 901L))
            .willReturn(Optional.of(ownSubmitted));
        given(loadEvaluationPort.listByApplicationId(900L)).willReturn(List.of(ownSubmitted, peerSubmitted));

        // When
        List<RecruitingInterviewEvaluationInfo> result = sut.listVisibleEvaluations(900L, 901L);

        // Then
        assertThat(result)
            .extracting(RecruitingInterviewEvaluationInfo::evaluatorMemberId)
            .containsExactly(901L, 902L);
    }

    private RecruitingInterviewAssignment assignment(Long interviewerMemberId) {
        RecruitingInterviewAssignment assignment = RecruitingInterviewAssignment.assign(
            application(),
            interviewerMemberId,
            Instant.parse("2026-07-02T01:00:00Z"),
            Instant.parse("2026-07-02T01:30:00Z"),
            "온라인"
        );
        ReflectionTestUtils.setField(assignment, "id", interviewerMemberId);
        return assignment;
    }

    private RecruitingApplication application() {
        RecruitingApplication application = RecruitingApplication.createDraft(
            applicationForm(),
            700L,
            200L,
            "identity:1",
            "APP-001",
            "a***@umc.test"
        );
        ReflectionTestUtils.setField(application, "id", 900L);
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
}
