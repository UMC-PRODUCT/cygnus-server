package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeployChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.SaveChallengerWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("챌린저 워크북 Command Service")
class ChallengerWorkbookCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final Long ORIGINAL_WORKBOOK_ID = 10L;
    private static final Long CHALLENGER_WORKBOOK_ID = 100L;

    @Mock
    LoadChallengerWorkbookPort loadChallengerWorkbookPort;

    @Mock
    LoadOriginalWorkbookPort loadOriginalWorkbookPort;

    @Mock
    SaveChallengerWorkbookPort saveChallengerWorkbookPort;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @InjectMocks
    ChallengerWorkbookCommandService sut;

    @Nested
    @DisplayName("챌린저 워크북 배포")
    class BatchDeploy {

        @Test
        @DisplayName("배포된 원본 워크북을 활성 챌린저에게 배포할 수 있다")
        void 배포된_원본_워크북을_활성_챌린저에게_배포할_수_있다() {
            // given
            OriginalWorkbook workbook = releasedWorkbook(ORIGINAL_WORKBOOK_ID);
            given(loadOriginalWorkbookPort.batchGetByIds(List.of(ORIGINAL_WORKBOOK_ID))).willReturn(List.of(workbook));
            given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(activeChallenger(MEMBER_ID)));
            given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(
                MEMBER_ID,
                List.of(ORIGINAL_WORKBOOK_ID)
            ))
                .willReturn(List.of());
            given(saveChallengerWorkbookPort.save(any(ChallengerWorkbook.class)))
                .willAnswer(invocation -> {
                    ChallengerWorkbook saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", CHALLENGER_WORKBOOK_ID);
                    return saved;
                });

            var command = DeployChallengerWorkbookCommand.builder()
                .originalWorkbookIds(List.of(ORIGINAL_WORKBOOK_ID))
                .requestedMemberId(MEMBER_ID)
                .build();

            // when
            var result = sut.batchDeploy(command);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).challengerWorkbookId()).isEqualTo(CHALLENGER_WORKBOOK_ID);
            assertThat(result.get(0).challengerId()).isEqualTo(MEMBER_ID);

            ArgumentCaptor<ChallengerWorkbook> captor = ArgumentCaptor.forClass(ChallengerWorkbook.class);
            then(saveChallengerWorkbookPort).should().save(captor.capture());
            assertThat(captor.getValue().getOriginalWorkbook()).isSameAs(workbook);
        }

        @Test
        @DisplayName("이미 배포된 워크북은 다시 생성하지 않고 기존 워크북을 반환한다")
        void 이미_배포된_워크북은_다시_생성하지_않고_기존_워크북을_반환한다() {
            // given
            OriginalWorkbook workbook = releasedWorkbook(ORIGINAL_WORKBOOK_ID);
            ChallengerWorkbook existing = challengerWorkbook(workbook, MEMBER_ID);
            given(loadOriginalWorkbookPort.batchGetByIds(List.of(ORIGINAL_WORKBOOK_ID))).willReturn(List.of(workbook));
            given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(activeChallenger(MEMBER_ID)));
            given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(
                MEMBER_ID,
                List.of(ORIGINAL_WORKBOOK_ID)
            ))
                .willReturn(List.of(existing));

            var command = DeployChallengerWorkbookCommand.builder()
                .originalWorkbookIds(List.of(ORIGINAL_WORKBOOK_ID))
                .requestedMemberId(MEMBER_ID)
                .build();

            // when
            var result = sut.batchDeploy(command);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).challengerWorkbookId()).isEqualTo(CHALLENGER_WORKBOOK_ID);
            then(saveChallengerWorkbookPort).should(never()).save(any());
        }

        @Test
        @DisplayName("배포되지 않은 원본 워크북은 배포할 수 없다")
        void 배포되지_않은_원본_워크북은_배포할_수_없다() {
            // given
            OriginalWorkbook workbook = readyWorkbook(ORIGINAL_WORKBOOK_ID);
            given(loadOriginalWorkbookPort.batchGetByIds(List.of(ORIGINAL_WORKBOOK_ID))).willReturn(List.of(workbook));

            var command = DeployChallengerWorkbookCommand.builder()
                .originalWorkbookIds(List.of(ORIGINAL_WORKBOOK_ID))
                .requestedMemberId(MEMBER_ID)
                .build();

            // when & then
            assertThatThrownBy(() -> sut.batchDeploy(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.INVALID_WORKBOOK_STATUS);

            then(saveChallengerWorkbookPort).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("챌린저 워크북 수정")
    class Edit {

        @Test
        @DisplayName("본인 워크북의 내용을 수정할 수 있다")
        void 본인_워크북의_내용을_수정할_수_있다() {
            // given
            ChallengerWorkbook workbook = challengerWorkbook(releasedWorkbook(ORIGINAL_WORKBOOK_ID), MEMBER_ID);
            given(loadChallengerWorkbookPort.getById(CHALLENGER_WORKBOOK_ID)).willReturn(workbook);

            var command = EditChallengerWorkbookCommand.builder()
                .challengerWorkbookId(CHALLENGER_WORKBOOK_ID)
                .requestedMemberId(MEMBER_ID)
                .content("수정된 워크북 내용")
                .build();

            // when
            sut.edit(command);

            // then
            assertThat(workbook.getContent()).isEqualTo("수정된 워크북 내용");
            then(saveChallengerWorkbookPort).should().save(workbook);
        }

        @Test
        @DisplayName("다른 사람의 워크북은 수정할 수 없다")
        void 다른_사람의_워크북은_수정할_수_없다() {
            // given
            ChallengerWorkbook workbook = challengerWorkbook(releasedWorkbook(ORIGINAL_WORKBOOK_ID), OTHER_MEMBER_ID);
            given(loadChallengerWorkbookPort.getById(CHALLENGER_WORKBOOK_ID)).willReturn(workbook);

            var command = EditChallengerWorkbookCommand.builder()
                .challengerWorkbookId(CHALLENGER_WORKBOOK_ID)
                .requestedMemberId(MEMBER_ID)
                .content("수정된 워크북 내용")
                .build();

            // when & then
            assertThatThrownBy(() -> sut.edit(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);

            then(saveChallengerWorkbookPort).should(never()).save(any());
        }
    }

    private ChallengerInfo activeChallenger(Long memberId) {
        return ChallengerInfo.builder()
            .challengerId(1L)
            .memberId(memberId)
            .gisuId(9L)
            .part(ChallengerPart.SPRINGBOOT)
            .challengerStatus(ChallengerStatus.ACTIVE)
            .build();
    }

    private ChallengerWorkbook challengerWorkbook(OriginalWorkbook originalWorkbook, Long memberId) {
        ChallengerWorkbook workbook = ChallengerWorkbook.create(originalWorkbook, memberId, null);
        ReflectionTestUtils.setField(workbook, "id", CHALLENGER_WORKBOOK_ID);
        return workbook;
    }

    private OriginalWorkbook readyWorkbook(Long id) {
        OriginalWorkbook workbook = OriginalWorkbook.createAsReady(
            weeklyCurriculum(),
            "원본 워크북",
            "설명",
            null,
            null,
            OriginalWorkbookType.MAIN
        );
        ReflectionTestUtils.setField(workbook, "id", id);
        return workbook;
    }

    private OriginalWorkbook releasedWorkbook(Long id) {
        OriginalWorkbook workbook = readyWorkbook(id);
        workbook.changeStatus(OriginalWorkbookStatus.RELEASED, 99L);
        return workbook;
    }

    private WeeklyCurriculum weeklyCurriculum() {
        WeeklyCurriculum weeklyCurriculum = WeeklyCurriculum.create(
            Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "9기 스프링부트"),
            1L,
            false,
            "1주차",
            Instant.parse("2027-03-01T00:00:00Z"),
            Instant.parse("2027-03-07T23:59:59Z")
        );
        ReflectionTestUtils.setField(weeklyCurriculum, "id", 20L);
        return weeklyCurriculum;
    }
}
