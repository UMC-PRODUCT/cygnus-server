package com.umc.product.curriculum.adapter.in.web.v2;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateMissionSubmissionRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.response.OriginalWorkbookMissionResponse;
import com.umc.product.global.response.PageResponse;
import com.umc.product.global.security.MemberPrincipal;

class CurriculumV2ApiCompatibilityTest {

    @Test
    void 기존_문자열_수정_요청_body_계약을_유지한다() throws NoSuchMethodException {
        assertThat(ChallengerWorkbookCommandV2Controller.class.getDeclaredMethod(
            "edit",
            Long.class,
            String.class,
            MemberPrincipal.class
        )).isNotNull();
        assertThat(ChallengerWorkbookCommandV2Controller.class.getDeclaredMethod(
            "excuse",
            Long.class,
            String.class,
            MemberPrincipal.class
        )).isNotNull();
        assertThat(ChallengerWorkbookCommandV2Controller.class.getDeclaredMethod(
            "editBestReason",
            Long.class,
            String.class,
            MemberPrincipal.class
        )).isNotNull();
        assertThat(ChallengerWorkbookMissionCommandV2Controller.class.getDeclaredMethod(
            "editMissionSubmission",
            MemberPrincipal.class,
            Long.class,
            String.class
        )).isNotNull();
        assertThat(ChallengerWorkbookMissionCommandV2Controller.class.getDeclaredMethod(
            "editMissionFeedback",
            MemberPrincipal.class,
            Long.class,
            String.class
        )).isNotNull();
    }

    @Test
    void 제출과_피드백_생성의_기존_void_응답을_유지한다() throws NoSuchMethodException {
        assertThat(ChallengerWorkbookMissionCommandV2Controller.class.getDeclaredMethod(
            "createMissionSubmission",
            MemberPrincipal.class,
            CreateMissionSubmissionRequest.class
        ).getReturnType()).isEqualTo(void.class);
        assertThat(ChallengerWorkbookMissionCommandV2Controller.class.getDeclaredMethod(
            "createMissionFeedback",
            MemberPrincipal.class,
            com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateMissionFeedbackRequest.class
        ).getReturnType()).isEqualTo(void.class);
    }

    @Test
    void 제출_요청의_기존_challengerMissionId_필드명을_유지한다() throws Exception {
        CreateMissionSubmissionRequest request = new ObjectMapper().readValue(
            """
                {
                  "originalWorkbookMissionId": 1,
                  "challengerMissionId": 2,
                  "content": "제출"
                }
                """,
            CreateMissionSubmissionRequest.class
        );

        assertThat(request.challengerMissionId()).isEqualTo(2L);
    }

    @Test
    void 베스트_조회는_기존_PageResponse_계약을_유지한다() throws NoSuchMethodException {
        assertThat(WorkbookQueryV2Controller.class.getDeclaredMethod(
            "getBestWorkbooks",
            com.umc.product.curriculum.adapter.in.web.v2.dto.request.GetBestWorkbooksRequest.class
        ).getReturnType()).isEqualTo(PageResponse.class);
    }

    @Test
    void 원본_워크북의_기존_missions_단건_필드는_유지하고_전체_목록을_별도_제공한다() {
        RecordComponent[] components =
            com.umc.product.curriculum.adapter.in.web.v2.dto.response.OriginalWorkbookResponse.class
                .getRecordComponents();

        assertThat(Arrays.stream(components)
            .filter(component -> component.getName().equals("missions"))
            .findFirst()
            .map(RecordComponent::getType))
            .contains(OriginalWorkbookMissionResponse.class);
        assertThat(Arrays.stream(components).map(RecordComponent::getName))
            .contains("missionList");
    }
}
