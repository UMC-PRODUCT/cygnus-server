package com.umc.product.curriculum.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;

@ExtendWith(MockitoExtension.class)
class MissionFeedbackPermissionEvaluatorTest {

    @Mock private LoadMissionSubmissionPort loadMissionSubmissionPort;
    @Mock private LoadMissionFeedbackPort loadMissionFeedbackPort;
    @Mock private CurriculumStudyGroupStaffPolicy staffPolicy;
    @InjectMocks private MissionFeedbackPermissionEvaluator evaluator;

    @Test
    @DisplayName("피드백 작성자는 현재 staff가 아니어도 자신의 피드백을 수정할 수 있다")
    void authorCanEditWithoutCurrentStaffRole() {
        MissionFeedback feedback = MissionFeedback.create(submission(), 50L, "피드백", FeedbackResult.PASS);
        ReflectionTestUtils.setField(feedback, "id", 500L);
        given(loadMissionFeedbackPort.getById(500L)).willReturn(feedback);

        boolean allowed = evaluator.evaluate(
            subject(50L),
            ResourcePermission.of(ResourceType.MISSION_FEEDBACK, 500L, PermissionType.EDIT)
        );

        assertThat(allowed).isTrue();
        verify(staffPolicy, never()).canManage(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("철회된 제출의 피드백 생성 권한은 staff 여부만 평가하고 상태 충돌은 서비스에 위임한다")
    void withdrawnSubmissionStillEvaluatesStaffPermission() {
        MissionSubmission submission = submission();
        submission.withdraw(Instant.parse("2026-07-23T00:00:00Z"));
        given(loadMissionSubmissionPort.getById(400L)).willReturn(submission);
        given(staffPolicy.canManage(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(30L), org.mockito.ArgumentMatchers.eq(9L)
        )).willReturn(true);

        boolean allowed = evaluator.evaluate(
            subject(50L),
            ResourcePermission.of(ResourceType.MISSION_FEEDBACK, 400L, PermissionType.WRITE)
        );

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("리소스 ID 없는 피드백 타입 단위 권한은 예외 없이 false를 반환한다")
    void typePermissionWithoutResourceIdFailsClosed() {
        boolean allowed = evaluator.evaluate(
            subject(50L),
            ResourcePermission.ofType(ResourceType.MISSION_FEEDBACK, PermissionType.EDIT)
        );

        assertThat(allowed).isFalse();
    }

    private SubjectAttributes subject(Long memberId) {
        return SubjectAttributes.builder().memberId(memberId).build();
    }

    private MissionSubmission submission() {
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼"), 1L, false, "1주차",
            Instant.EPOCH, Instant.parse("2026-08-01T00:00:00Z")
        );
        OriginalWorkbook original = OriginalWorkbook.createAsDraft(
            weekly, "워크북", null, null, null, OriginalWorkbookType.MAIN
        );
        OriginalWorkbookMission mission = OriginalWorkbookMission.create(
            original, "미션", null, MissionType.MEMO, true
        );
        MissionSubmission submission = MissionSubmission.create(
            mission, ChallengerWorkbook.create(original, 30L, 10L), "제출"
        );
        ReflectionTestUtils.setField(submission, "id", 400L);
        return submission;
    }
}
