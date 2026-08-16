package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditOriginalWorkbookMissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateOriginalWorkbookMissionCommand;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.SaveOriginalWorkbookMissionPort;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("원본 워크북 미션 Command Service")
class OriginalWorkbookMissionCommandServiceTest {

    private static final Long ORIGINAL_WORKBOOK_ID = 10L;
    private static final Long ORIGINAL_WORKBOOK_MISSION_ID = 20L;

    @Mock
    LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;

    @Mock
    SaveOriginalWorkbookMissionPort saveOriginalWorkbookMissionPort;

    @Mock
    LoadOriginalWorkbookPort loadOriginalWorkbookPort;

    @Mock
    LoadMissionSubmissionPort loadMissionSubmissionPort;

    @InjectMocks
    OriginalWorkbookMissionCommandService sut;

    @Nested
    @DisplayName("원본 워크북 미션 생성")
    class Create {

        @Test
        @DisplayName("배포되지 않은 워크북에는 필수 미션을 생성할 수 있다")
        void 배포되지_않은_워크북에는_필수_미션을_생성할_수_있다() {
            // given
            OriginalWorkbook workbook = readyWorkbook();
            given(loadOriginalWorkbookPort.getById(ORIGINAL_WORKBOOK_ID)).willReturn(workbook);

            OriginalWorkbookMission saved = mission(workbook, true);
            given(saveOriginalWorkbookMissionPort.save(any(OriginalWorkbookMission.class))).willReturn(saved);

            var command = CreateOriginalWorkbookMissionCommand.builder()
                .originalWorkbookId(ORIGINAL_WORKBOOK_ID)
                .title("미션 제목")
                .description("미션 설명")
                .missionType(MissionType.LINK)
                .isNecessary(true)
                .build();

            // when
            Long result = sut.create(command);

            // then
            assertThat(result).isEqualTo(ORIGINAL_WORKBOOK_MISSION_ID);

            ArgumentCaptor<OriginalWorkbookMission> captor = ArgumentCaptor.forClass(OriginalWorkbookMission.class);
            then(saveOriginalWorkbookMissionPort).should().save(captor.capture());
            assertThat(captor.getValue().getOriginalWorkbook()).isSameAs(workbook);
            assertThat(captor.getValue().getTitle()).isEqualTo("미션 제목");
            assertThat(captor.getValue().isNecessary()).isTrue();
        }

        @Test
        @DisplayName("배포된 워크북에는 필수 미션을 생성할 수 없다")
        void 배포된_워크북에는_필수_미션을_생성할_수_없다() {
            // given
            given(loadOriginalWorkbookPort.getById(ORIGINAL_WORKBOOK_ID)).willReturn(releasedWorkbook());

            var command = CreateOriginalWorkbookMissionCommand.builder()
                .originalWorkbookId(ORIGINAL_WORKBOOK_ID)
                .title("필수 미션")
                .missionType(MissionType.PLAIN)
                .isNecessary(true)
                .build();

            // when & then
            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.RELEASED_WORKBOOK_NECESSARY_MISSION_FORBIDDEN);

            then(saveOriginalWorkbookMissionPort).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("원본 워크북 미션 수정")
    class Edit {

        @Test
        @DisplayName("배포된 워크북의 선택 미션은 필수로 승격할 수 없다")
        void 배포된_워크북의_선택_미션은_필수로_승격할_수_없다() {
            // given
            OriginalWorkbookMission mission = mission(releasedWorkbook(), false);
            given(loadOriginalWorkbookMissionPort.getById(ORIGINAL_WORKBOOK_MISSION_ID)).willReturn(mission);

            var command = EditOriginalWorkbookMissionCommand.builder()
                .originalWorkbookMissionId(ORIGINAL_WORKBOOK_MISSION_ID)
                .isNecessary(true)
                .build();

            // when & then
            assertThatThrownBy(() -> sut.edit(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.RELEASED_WORKBOOK_MISSION_UPGRADE_FORBIDDEN);

            then(loadOriginalWorkbookPort).should(never()).getById(anyLong());
            then(saveOriginalWorkbookMissionPort).should(never()).save(any());
        }

        @Test
        @DisplayName("배포된 워크북의 필수 미션은 선택 미션으로 변경할 수 있다")
        void 배포된_워크북의_필수_미션은_선택_미션으로_변경할_수_있다() {
            // given
            OriginalWorkbookMission mission = mission(releasedWorkbook(), true);
            given(loadOriginalWorkbookMissionPort.getById(ORIGINAL_WORKBOOK_MISSION_ID)).willReturn(mission);

            var command = EditOriginalWorkbookMissionCommand.builder()
                .originalWorkbookMissionId(ORIGINAL_WORKBOOK_MISSION_ID)
                .title("수정된 제목")
                .isNecessary(false)
                .build();

            // when
            sut.edit(command);

            // then
            assertThat(mission.getTitle()).isEqualTo("수정된 제목");
            assertThat(mission.isNecessary()).isFalse();
            then(loadOriginalWorkbookPort).should(never()).getById(anyLong());
            then(saveOriginalWorkbookMissionPort).should().save(mission);
        }
    }

    @Nested
    @DisplayName("원본 워크북 미션 삭제")
    class Delete {

        @Test
        @DisplayName("제출물이 없으면 미션을 삭제할 수 있다")
        void 제출물이_없으면_미션을_삭제할_수_있다() {
            // given
            OriginalWorkbookMission mission = mission(readyWorkbook(), false);
            given(loadOriginalWorkbookMissionPort.getById(ORIGINAL_WORKBOOK_MISSION_ID)).willReturn(mission);
            given(loadMissionSubmissionPort.existsByOriginalWorkbookMissionId(ORIGINAL_WORKBOOK_MISSION_ID))
                .willReturn(false);

            // when
            sut.delete(ORIGINAL_WORKBOOK_MISSION_ID);

            // then
            then(saveOriginalWorkbookMissionPort).should().delete(mission);
        }

        @Test
        @DisplayName("제출물이 있으면 미션을 삭제할 수 없다")
        void 제출물이_있으면_미션을_삭제할_수_없다() {
            // given
            OriginalWorkbookMission mission = mission(readyWorkbook(), false);
            given(loadOriginalWorkbookMissionPort.getById(ORIGINAL_WORKBOOK_MISSION_ID)).willReturn(mission);
            given(loadMissionSubmissionPort.existsByOriginalWorkbookMissionId(ORIGINAL_WORKBOOK_MISSION_ID))
                .willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.delete(ORIGINAL_WORKBOOK_MISSION_ID))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.MISSION_HAS_SUBMISSIONS);

            then(saveOriginalWorkbookMissionPort).should(never()).delete(any());
        }
    }

    private OriginalWorkbook readyWorkbook() {
        OriginalWorkbook workbook = OriginalWorkbook.createAsReady(
            weeklyCurriculum(),
            "원본 워크북",
            "설명",
            null,
            null,
            OriginalWorkbookType.MAIN
        );
        ReflectionTestUtils.setField(workbook, "id", ORIGINAL_WORKBOOK_ID);
        return workbook;
    }

    private OriginalWorkbook releasedWorkbook() {
        OriginalWorkbook workbook = readyWorkbook();
        workbook.changeStatus(OriginalWorkbookStatus.RELEASED, 1L);
        return workbook;
    }

    private OriginalWorkbookMission mission(OriginalWorkbook workbook, boolean isNecessary) {
        OriginalWorkbookMission mission = OriginalWorkbookMission.create(
            workbook,
            "기존 미션",
            "기존 설명",
            MissionType.LINK,
            isNecessary
        );
        ReflectionTestUtils.setField(mission, "id", ORIGINAL_WORKBOOK_MISSION_ID);
        return mission;
    }

    private WeeklyCurriculum weeklyCurriculum() {
        return WeeklyCurriculum.create(
            Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "9기 스프링부트"),
            1L,
            false,
            "1주차",
            Instant.parse("2027-03-01T00:00:00Z"),
            Instant.parse("2027-03-07T23:59:59Z")
        );
    }
}
