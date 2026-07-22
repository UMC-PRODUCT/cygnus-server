package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.ChangeOriginalWorkbookStatusCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateOriginalWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditOriginalWorkbookCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.application.port.out.SaveOriginalWorkbookPort;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.global.exception.NotImplementedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OriginalWorkbookCommandService")
class OriginalWorkbookCommandServiceTest {

    @Mock LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    @Mock SaveOriginalWorkbookPort saveOriginalWorkbookPort;
    @Mock LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    @Mock LoadChallengerWorkbookPort loadChallengerWorkbookPort;

    OriginalWorkbookCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new OriginalWorkbookCommandService(
            loadOriginalWorkbookPort, saveOriginalWorkbookPort,
            loadWeeklyCurriculumPort, loadChallengerWorkbookPort
        );
    }

    @Test
    @DisplayName("빈 bulk는 port 호출 없이 빈 ID를 반환한다")
    void empty_bulk_short_circuit() {
        assertThat(sut.createBulk(List.of())).isEmpty();
        then(loadWeeklyCurriculumPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DRAFT와 READY 원본 워크북을 입력 순서대로 생성한다")
    void creates_draft_and_ready_in_order() {
        WeeklyCurriculum weekly = weekly();
        given(loadWeeklyCurriculumPort.getById(1L)).willReturn(weekly);
        given(saveOriginalWorkbookPort.save(any(OriginalWorkbook.class))).willAnswer(invocation -> {
            OriginalWorkbook workbook = invocation.getArgument(0);
            long id = workbook.getOriginalWorkbookStatus() == OriginalWorkbookStatus.DRAFT ? 10L : 11L;
            ReflectionTestUtils.setField(workbook, "id", id);
            return workbook;
        });

        List<Long> ids = sut.createBulk(List.of(
            command(OriginalWorkbookStatus.DRAFT), command(OriginalWorkbookStatus.READY)
        ));

        assertThat(ids).containsExactly(10L, 11L);
        ArgumentCaptor<OriginalWorkbook> captor = ArgumentCaptor.forClass(OriginalWorkbook.class);
        then(saveOriginalWorkbookPort).should(org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(OriginalWorkbook::getOriginalWorkbookStatus)
            .containsExactly(OriginalWorkbookStatus.DRAFT, OriginalWorkbookStatus.READY);
    }

    @Test
    @DisplayName("RELEASED 상태로 직접 생성할 수 없다")
    void released_initial_status_is_rejected() {
        given(loadWeeklyCurriculumPort.getById(1L)).willReturn(weekly());

        assertError(() -> sut.create(command(OriginalWorkbookStatus.RELEASED)),
            CurriculumErrorCode.INVALID_WORKBOOK_STATUS);
    }

    @Test
    @DisplayName("원본 워크북의 제공된 필드를 수정하고 저장한다")
    void edits_workbook() {
        OriginalWorkbook workbook = workbook(10L, OriginalWorkbookStatus.DRAFT);
        given(loadOriginalWorkbookPort.getById(10L)).willReturn(workbook);
        EditOriginalWorkbookCommand command = EditOriginalWorkbookCommand.builder()
            .originalWorkbookId(10L).title("변경").description("설명2").url("url2").content("내용2").build();

        sut.edit(command);

        assertThat(workbook.getTitle()).isEqualTo("변경");
        then(saveOriginalWorkbookPort).should().save(workbook);
    }

    @Test
    @DisplayName("제출물이 없을 때만 원본 워크북을 삭제한다")
    void delete_protects_submissions() {
        OriginalWorkbook workbook = workbook(10L, OriginalWorkbookStatus.DRAFT);
        given(loadOriginalWorkbookPort.getById(10L)).willReturn(workbook);
        given(loadChallengerWorkbookPort.existsByOriginalWorkbookId(10L)).willReturn(true);
        assertError(() -> sut.delete(10L), CurriculumErrorCode.WORKBOOK_HAS_SUBMISSIONS);

        given(loadChallengerWorkbookPort.existsByOriginalWorkbookId(10L)).willReturn(false);
        sut.delete(10L);
        then(saveOriginalWorkbookPort).should().delete(workbook);
    }

    @Test
    @DisplayName("batch 상태 변경은 ID로 매핑해 전체를 한 번에 저장한다")
    void changes_status_in_batch() {
        OriginalWorkbook first = workbook(10L, OriginalWorkbookStatus.DRAFT);
        OriginalWorkbook second = workbook(11L, OriginalWorkbookStatus.READY);
        given(loadOriginalWorkbookPort.batchGetByIds(List.of(10L, 11L))).willReturn(List.of(first, second));

        sut.changeStatusForRelease(List.of(
            ChangeOriginalWorkbookStatusCommand.builder()
                .originalWorkbookId(10L).status(OriginalWorkbookStatus.READY).requestedMemberId(1L).build(),
            ChangeOriginalWorkbookStatusCommand.builder()
                .originalWorkbookId(11L).status(OriginalWorkbookStatus.RELEASED).requestedMemberId(1L).build()
        ));

        assertThat(first.getOriginalWorkbookStatus()).isEqualTo(OriginalWorkbookStatus.READY);
        assertThat(second.getOriginalWorkbookStatus()).isEqualTo(OriginalWorkbookStatus.RELEASED);
        assertThat(second.getReleasedMemberId()).isEqualTo(1L);
        then(saveOriginalWorkbookPort).should().saveAll(org.mockito.ArgumentMatchers.argThat(
            values -> values.size() == 2 && values.containsAll(List.of(first, second))
        ));
    }

    @Test
    @DisplayName("자동 배포 미구현 경로는 명시적인 NotImplementedException을 반환한다")
    void auto_release_is_explicitly_not_implemented() {
        assertThatThrownBy(sut::releaseAllDue).isInstanceOf(NotImplementedException.class);
    }

    private CreateOriginalWorkbookCommand command(OriginalWorkbookStatus status) {
        return CreateOriginalWorkbookCommand.builder()
            .weeklyCurriculumId(1L).title("워크북").description("설명")
            .url("url").content("내용").type(OriginalWorkbookType.MAIN).initialStatus(status).build();
    }

    private OriginalWorkbook workbook(Long id, OriginalWorkbookStatus status) {
        OriginalWorkbook workbook = status == OriginalWorkbookStatus.DRAFT
            ? OriginalWorkbook.createAsDraft(weekly(), "제목", null, null, null, OriginalWorkbookType.MAIN)
            : OriginalWorkbook.createAsReady(weekly(), "제목", null, null, null, OriginalWorkbookType.MAIN);
        ReflectionTestUtils.setField(workbook, "id", id);
        return workbook;
    }

    private WeeklyCurriculum weekly() {
        Curriculum curriculum = Curriculum.create(1L, ChallengerPart.SPRINGBOOT, "커리큘럼");
        return WeeklyCurriculum.create(
            curriculum, 1L, false, "1주차",
            Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2030-01-07T00:00:00Z")
        );
    }

    private void assertError(Runnable action, CurriculumErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(CurriculumDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code)
            );
    }
}
