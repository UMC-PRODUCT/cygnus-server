package com.umc.product.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;
import com.umc.product.curriculum.domain.enums.WorkbookStatus;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

@DisplayName("Curriculum 도메인 잔여 경계")
class CurriculumDomainResidualTest {

    @Test
    @DisplayName("커리큘럼 제목은 유효한 값만 갱신한다")
    void curriculum_title_update() {
        Curriculum curriculum = Curriculum.create(1L, ChallengerPart.SPRINGBOOT, "기존 제목");

        curriculum.updateTitle(null);
        curriculum.updateTitle(" ");
        assertThat(curriculum.getTitle()).isEqualTo("기존 제목");
        curriculum.updateTitle("변경 제목");
        assertThat(curriculum.getTitle()).isEqualTo("변경 제목");
        assertThat(curriculum.getGisuId()).isEqualTo(1L);
        assertThat(curriculum.getPart()).isEqualTo(ChallengerPart.SPRINGBOOT);
    }

    @Test
    @DisplayName("원본 워크북 edit는 제공된 필드만 변경한다")
    void original_workbook_partial_edit() {
        OriginalWorkbook workbook = OriginalWorkbook.createAsDraft(
            weekly(), "제목", "설명", "url", "내용", OriginalWorkbookType.MAIN
        );

        workbook.edit(null, null, null, null);
        workbook.edit(" ", "", "", "");
        assertThat(workbook.getTitle()).isEqualTo("제목");
        assertThat(workbook.getDescription()).isEmpty();
        assertThat(workbook.getUrl()).isEmpty();
        assertThat(workbook.getContent()).isEmpty();
        workbook.edit("변경", "설명2", "url2", "내용2");
        assertThat(workbook.getTitle()).isEqualTo("변경");
    }

    @Test
    @DisplayName("원본 워크북 미션 edit는 null을 유지하고 제공된 값을 반영한다")
    void original_workbook_mission_partial_edit() {
        OriginalWorkbook workbook = OriginalWorkbook.createAsDraft(
            weekly(), "제목", null, null, null, OriginalWorkbookType.MAIN
        );
        OriginalWorkbookMission mission = OriginalWorkbookMission.create(
            workbook, "미션", "설명", MissionType.PLAIN, true
        );

        mission.edit(null, null, null, null);
        mission.edit(" ", "", MissionType.MEMO, false);

        assertThat(mission.getTitle()).isEqualTo("미션");
        assertThat(mission.getDescription()).isEmpty();
        assertThat(mission.getMissionType()).isEqualTo(MissionType.MEMO);
        assertThat(mission.isNecessary()).isFalse();
        mission.edit("변경 미션", "설명", MissionType.LINK, true);
        assertThat(mission.getTitle()).isEqualTo("변경 미션");
    }

    @Test
    @DisplayName("legacy 미션과 challenger 미션의 builder 및 제출 수정 계약을 보존한다")
    void legacy_mission_contract() {
        ChallengerWorkbook workbook = new ChallengerWorkbook() {
        };
        ChallengerMission challengerMission = ChallengerMission.builder()
            .workbookMissionId(1L)
            .challengerWorkbook(workbook)
            .submission("기존")
            .build();
        WorkbookMission workbookMission = WorkbookMission.builder()
            .originalWorkbookId(2L)
            .title("미션")
            .missionType(MissionType.PLAIN)
            .content("내용")
            .build();

        challengerMission.updateSubmission("변경");

        assertThat(challengerMission.getSubmission()).isEqualTo("변경");
        assertThat(challengerMission.getChallengerWorkbook()).isSameAs(workbook);
        assertThat(workbookMission.getOriginalWorkbookId()).isEqualTo(2L);
        assertThat(ChallengerWorkbook.create()).isNull();
    }

    @Test
    @DisplayName("생성 로직이 없는 persistence 전용 entity도 기본 상태로 안전하게 생성된다")
    void persistence_only_entities_have_safe_defaults() throws Exception {
        assertThat(new MissionFeedback() {
        }.getId()).isNull();
        assertThat(new MissionSubmission() {
        }.getId()).isNull();
        assertThat(new WeeklyBestWorkbook() {
        }.getId()).isNull();
        var constructor = ChallengerWorkbook.class.getDeclaredConstructor(OriginalWorkbook.class);
        constructor.setAccessible(true);
        assertThat(constructor.newInstance((OriginalWorkbook) null).getOriginalWorkbook()).isNull();
    }

    @Test
    @DisplayName("enum과 도메인 오류 코드는 외부 계약을 보존한다")
    void enum_and_error_contract() {
        assertThat(FeedbackResult.values()).isNotEmpty();
        assertThat(MissionType.values()).isNotEmpty();
        assertThat(OriginalWorkbookStatus.values()).isNotEmpty();
        assertThat(OriginalWorkbookStatus.RELEASED.isReleased()).isTrue();
        assertThat(OriginalWorkbookStatus.DRAFT.isReleased()).isFalse();
        assertThat(OriginalWorkbookType.values()).isNotEmpty();
        assertThat(SubmissionStatus.values()).isNotEmpty();
        assertThat(WorkbookStatus.values()).isNotEmpty();
        assertThat(CurriculumErrorCode.values()).allSatisfy(code -> {
            assertThat(code.getHttpStatus()).isNotNull();
            assertThat(code.getCode()).isNotBlank();
            assertThat(code.getMessage()).isNotBlank();
            assertThat(new CurriculumDomainException(code).getBaseCode()).isEqualTo(code);
            assertThat(new CurriculumDomainException(code, "상세").getMessage()).contains("상세");
        });
    }

    private WeeklyCurriculum weekly() {
        Curriculum curriculum = Curriculum.create(1L, ChallengerPart.SPRINGBOOT, "커리큘럼");
        return WeeklyCurriculum.create(
            curriculum, 1L, false, "1주차",
            Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2030-01-07T00:00:00Z")
        );
    }
}
