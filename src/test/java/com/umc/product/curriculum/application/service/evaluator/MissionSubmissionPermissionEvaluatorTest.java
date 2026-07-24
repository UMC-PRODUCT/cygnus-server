package com.umc.product.curriculum.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;

@ExtendWith(MockitoExtension.class)
class MissionSubmissionPermissionEvaluatorTest {

    @Mock private LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    @Mock private LoadMissionSubmissionPort loadMissionSubmissionPort;
    @InjectMocks private MissionSubmissionPermissionEvaluator evaluator;

    @Test
    @DisplayName("철회된 제출도 소유자 권한은 통과하고 상태 충돌은 서비스에서 처리한다")
    void withdrawnSubmissionOwnerPermissionIsPreserved() {
        MissionSubmission submission = submission();
        submission.withdraw(Instant.parse("2026-07-23T00:00:00Z"));
        given(loadMissionSubmissionPort.getById(400L)).willReturn(submission);

        boolean allowed = evaluator.evaluate(
            SubjectAttributes.builder().memberId(30L).build(),
            ResourcePermission.of(ResourceType.MISSION_SUBMISSION, 400L, PermissionType.EDIT)
        );

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("리소스 ID 없는 제출 타입 단위 권한은 예외 없이 false를 반환한다")
    void typePermissionWithoutResourceIdFailsClosed() {
        boolean allowed = evaluator.evaluate(
            SubjectAttributes.builder().memberId(30L).build(),
            ResourcePermission.ofType(ResourceType.MISSION_SUBMISSION, PermissionType.EDIT)
        );

        assertThat(allowed).isFalse();
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
