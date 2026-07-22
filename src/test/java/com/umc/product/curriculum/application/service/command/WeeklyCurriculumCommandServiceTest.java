package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateWeeklyCurriculumCommand;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.EditWeeklyCurriculumCommand;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.application.port.out.SaveWeeklyCurriculumPort;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

@ExtendWith(MockitoExtension.class)
class WeeklyCurriculumCommandServiceTest {

    @Mock
    LoadCurriculumPort loadCurriculumPort;

    @Mock
    LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;

    @Mock
    SaveWeeklyCurriculumPort saveWeeklyCurriculumPort;

    @InjectMocks
    WeeklyCurriculumCommandService sut;

    // 테스트 픽스처 헬퍼
    private static final Instant START = Instant.parse("2027-03-01T00:00:00Z");
    private static final Instant END = Instant.parse("2027-03-07T23:59:59Z");

    private Curriculum 커리큘럼() {
        return Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "9기 스프링부트");
    }

    private WeeklyCurriculum 주차별_커리큘럼(Curriculum curriculum) {
        return WeeklyCurriculum.create(curriculum, 1L, false, "1주차", START, END);
    }

    // ===== create =====

    @Test
    @DisplayName("빈 일괄 생성은 조회와 저장 없이 빈 목록을 반환한다")
    void empty_bulk_short_circuits() {
        assertThat(sut.createBulk(List.of())).isEmpty();

        then(loadCurriculumPort).shouldHaveNoInteractions();
        then(saveWeeklyCurriculumPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일괄 생성은 입력 순서대로 생성 ID를 반환한다")
    void bulk_create_preserves_input_order() {
        Curriculum curriculum = 커리큘럼();
        given(loadCurriculumPort.findById(1L)).willReturn(Optional.of(curriculum));
        given(saveWeeklyCurriculumPort.save(any())).willAnswer(invocation -> {
            WeeklyCurriculum weekly = invocation.getArgument(0);
            ReflectionTestUtils.setField(weekly, "id", weekly.getWeekNo() + 100L);
            return weekly;
        });

        List<Long> result = sut.createBulk(List.of(
            createCommand(1L, false, END),
            createCommand(2L, false, END.plusSeconds(86400))
        ));

        assertThat(result).containsExactly(101L, 102L);
    }

    @Nested
    @DisplayName("주차별 커리큘럼 생성")
    class Create {

        @Test
        void 주차별_커리큘럼_생성에_성공한다() {
            // given
            Curriculum curriculum = 커리큘럼();
            given(loadCurriculumPort.findById(1L)).willReturn(Optional.of(curriculum));

            WeeklyCurriculum saved = 주차별_커리큘럼(curriculum);
            ReflectionTestUtils.setField(saved, "id", 10L);
            given(saveWeeklyCurriculumPort.save(any(WeeklyCurriculum.class))).willReturn(saved);

            var command = CreateWeeklyCurriculumCommand.builder()
                .curriculumId(1L)
                .weekNo(1L)
                .isExtra(false)
                .title("1주차")
                .startsAt(START)
                .endsAt(END)
                .build();

            // when
            Long result = sut.create(command);

            // then
            assertThat(result).isEqualTo(10L);
            then(saveWeeklyCurriculumPort).should().save(any(WeeklyCurriculum.class));
        }

        @Test
        void 부록_주차_생성에_성공한다() {
            // given
            Curriculum curriculum = 커리큘럼();
            given(loadCurriculumPort.findById(1L)).willReturn(Optional.of(curriculum));

            WeeklyCurriculum saved = WeeklyCurriculum.create(curriculum, 1L, true, "1주차 부록", START, END);
            ReflectionTestUtils.setField(saved, "id", 11L);
            given(saveWeeklyCurriculumPort.save(any())).willReturn(saved);

            var command = CreateWeeklyCurriculumCommand.builder()
                .curriculumId(1L)
                .weekNo(1L)
                .isExtra(true)
                .title("1주차 부록")
                .startsAt(START)
                .endsAt(END)
                .build();

            // when
            Long result = sut.create(command);

            // then
            assertThat(result).isEqualTo(11L);
        }

        @Test
        void 존재하지_않는_커리큘럼에_주차를_생성하면_예외가_발생한다() {
            // given
            given(loadCurriculumPort.findById(999L)).willReturn(Optional.empty());

            var command = CreateWeeklyCurriculumCommand.builder()
                .curriculumId(999L)
                .weekNo(1L)
                .isExtra(false)
                .title("1주차")
                .startsAt(START)
                .endsAt(END)
                .build();

            // when & then
            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.CURRICULUM_NOT_FOUND);

            then(saveWeeklyCurriculumPort).should(never()).save(any());
        }

        @Test
        void 시작일이_종료일_이후이면_생성_시_예외가_발생한다() {
            // given
            Curriculum curriculum = 커리큘럼();
            given(loadCurriculumPort.findById(1L)).willReturn(Optional.of(curriculum));

            Instant invalidStart = END.plusSeconds(1);
            var command = CreateWeeklyCurriculumCommand.builder()
                .curriculumId(1L)
                .weekNo(1L)
                .isExtra(false)
                .title("1주차")
                .startsAt(invalidStart)
                .endsAt(END)
                .build();

            // when & then
            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.INVALID_WEEKLY_CURRICULUM_PERIOD);
        }

        @Test
        @DisplayName("이미 종료된 주차는 생성할 수 없다")
        void rejects_already_ended_period() {
            given(loadCurriculumPort.findById(1L)).willReturn(Optional.of(커리큘럼()));

            assertThatThrownBy(() -> sut.create(
                createCommand(1L, false, Instant.now().minusSeconds(1))))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WEEKLY_CURRICULUM_PERIOD_ALREADY_ENDED);
        }

        @Test
        @DisplayName("같은 커리큘럼·주차·부록 조합은 중복 생성할 수 없다")
        void rejects_duplicate_week_and_type() {
            given(loadCurriculumPort.findById(1L)).willReturn(Optional.of(커리큘럼()));
            given(loadWeeklyCurriculumPort.existsByCurriculumIdAndWeekNoAndIsExtra(1L, 1L, false))
                .willReturn(true);

            assertThatThrownBy(() -> sut.create(createCommand(1L, false, END)))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WEEKLY_CURRICULUM_ALREADY_EXISTS);

            then(saveWeeklyCurriculumPort).should(never()).save(any());
        }
    }

    // ===== edit =====

    @Nested
    @DisplayName("주차별 커리큘럼 수정")
    class Edit {

        @Test
        void 제목만_수정하면_성공한다() {
            // given
            Curriculum curriculum = 커리큘럼();
            WeeklyCurriculum weeklyCurriculum = 주차별_커리큘럼(curriculum);
            given(loadWeeklyCurriculumPort.getById(10L)).willReturn(weeklyCurriculum);
            given(saveWeeklyCurriculumPort.save(weeklyCurriculum)).willReturn(weeklyCurriculum);

            var command = EditWeeklyCurriculumCommand.builder()
                .weeklyCurriculumId(10L)
                .title("수정된 제목")
                .build();

            // when
            sut.edit(command);

            // then
            assertThat(weeklyCurriculum.getTitle()).isEqualTo("수정된 제목");
            then(loadWeeklyCurriculumPort).should(never())
                .existsReleasedOriginalWorkbookByWeeklyCurriculumId(any());
        }

        @Test
        void 배포된_워크북이_없으면_날짜_수정에_성공한다() {
            // given
            Curriculum curriculum = 커리큘럼();
            WeeklyCurriculum weeklyCurriculum = 주차별_커리큘럼(curriculum);
            given(loadWeeklyCurriculumPort.getById(10L)).willReturn(weeklyCurriculum);
            given(loadWeeklyCurriculumPort.existsReleasedOriginalWorkbookByWeeklyCurriculumId(10L))
                .willReturn(false);
            given(saveWeeklyCurriculumPort.save(weeklyCurriculum)).willReturn(weeklyCurriculum);

            Instant newEnd = END.plusSeconds(86400);
            var command = EditWeeklyCurriculumCommand.builder()
                .weeklyCurriculumId(10L)
                .endsAt(newEnd)
                .build();

            // when
            sut.edit(command);

            // then
            assertThat(weeklyCurriculum.getEndsAt()).isEqualTo(newEnd);
        }

        @Test
        void 배포된_워크북이_있으면_날짜_수정_시_예외가_발생한다() {
            // given
            Curriculum curriculum = 커리큘럼();
            WeeklyCurriculum weeklyCurriculum = 주차별_커리큘럼(curriculum);
            given(loadWeeklyCurriculumPort.getById(10L)).willReturn(weeklyCurriculum);
            given(loadWeeklyCurriculumPort.existsReleasedOriginalWorkbookByWeeklyCurriculumId(10L))
                .willReturn(true);

            var command = EditWeeklyCurriculumCommand.builder()
                .weeklyCurriculumId(10L)
                .startsAt(START.plusSeconds(3600))
                .build();

            // when & then
            assertThatThrownBy(() -> sut.edit(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WEEKLY_CURRICULUM_DATE_LOCKED);

            then(saveWeeklyCurriculumPort).should(never()).save(any());
        }

        @Test
        void 날짜_수정_후_시작일이_종료일_이후면_예외가_발생한다() {
            // given
            Curriculum curriculum = 커리큘럼();
            WeeklyCurriculum weeklyCurriculum = 주차별_커리큘럼(curriculum);
            given(loadWeeklyCurriculumPort.getById(10L)).willReturn(weeklyCurriculum);
            given(loadWeeklyCurriculumPort.existsReleasedOriginalWorkbookByWeeklyCurriculumId(10L))
                .willReturn(false);

            // 기존 END 이후로 시작일을 변경하면 start > end 조건 위반
            var command = EditWeeklyCurriculumCommand.builder()
                .weeklyCurriculumId(10L)
                .startsAt(END.plusSeconds(1))
                .build();

            // when & then
            assertThatThrownBy(() -> sut.edit(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.INVALID_WEEKLY_CURRICULUM_PERIOD);
        }

        @Test
        void null_필드는_기존_값을_유지한다() {
            // given
            Curriculum curriculum = 커리큘럼();
            WeeklyCurriculum weeklyCurriculum = 주차별_커리큘럼(curriculum);
            given(loadWeeklyCurriculumPort.getById(10L)).willReturn(weeklyCurriculum);
            given(saveWeeklyCurriculumPort.save(weeklyCurriculum)).willReturn(weeklyCurriculum);

            // 모든 필드가 null인 커맨드
            var command = EditWeeklyCurriculumCommand.builder()
                .weeklyCurriculumId(10L)
                .build();

            // when
            sut.edit(command);

            // then - 기존 값 유지
            assertThat(weeklyCurriculum.getWeekNo()).isEqualTo(1L);
            assertThat(weeklyCurriculum.isExtra()).isFalse();
            assertThat(weeklyCurriculum.getTitle()).isEqualTo("1주차");
            assertThat(weeklyCurriculum.getStartsAt()).isEqualTo(START);
            assertThat(weeklyCurriculum.getEndsAt()).isEqualTo(END);
        }

        @Test
        void 존재하지_않는_주차별_커리큘럼을_수정하면_예외가_발생한다() {
            // given
            given(loadWeeklyCurriculumPort.getById(999L))
                .willThrow(new CurriculumDomainException(CurriculumErrorCode.WEEKLY_CURRICULUM_NOT_FOUND));

            var command = EditWeeklyCurriculumCommand.builder()
                .weeklyCurriculumId(999L)
                .title("수정")
                .build();

            // when & then
            assertThatThrownBy(() -> sut.edit(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WEEKLY_CURRICULUM_NOT_FOUND);
        }

        @Test
        @DisplayName("종료 시각을 과거로 변경할 수 없다")
        void rejects_edit_to_already_ended_period() {
            given(loadWeeklyCurriculumPort.getById(10L)).willReturn(주차별_커리큘럼(커리큘럼()));

            var command = EditWeeklyCurriculumCommand.builder()
                .weeklyCurriculumId(10L)
                .endsAt(Instant.now().minusSeconds(1))
                .build();

            assertThatThrownBy(() -> sut.edit(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WEEKLY_CURRICULUM_PERIOD_ALREADY_ENDED);
        }

        @Test
        @DisplayName("주차만 변경해 기존 부록 여부와 중복되면 수정할 수 없다")
        void rejects_duplicate_when_only_week_changes() {
            Curriculum curriculum = 커리큘럼();
            WeeklyCurriculum weekly = 주차별_커리큘럼(curriculum);
            given(loadWeeklyCurriculumPort.getById(10L)).willReturn(weekly);
            given(loadWeeklyCurriculumPort.existsByCurriculumIdAndWeekNoAndIsExtraAndIdNot(
                curriculum.getId(), 2L, false, 10L)).willReturn(true);

            var command = EditWeeklyCurriculumCommand.builder()
                .weeklyCurriculumId(10L).weekNo(2L).build();

            assertThatThrownBy(() -> sut.edit(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WEEKLY_CURRICULUM_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("부록 여부만 변경하면 기존 주차 번호로 중복을 검사하고 저장한다")
        void edits_only_extra_flag_with_effective_week() {
            Curriculum curriculum = 커리큘럼();
            WeeklyCurriculum weekly = 주차별_커리큘럼(curriculum);
            given(loadWeeklyCurriculumPort.getById(10L)).willReturn(weekly);
            given(loadWeeklyCurriculumPort.existsByCurriculumIdAndWeekNoAndIsExtraAndIdNot(
                curriculum.getId(), 1L, true, 10L)).willReturn(false);

            sut.edit(EditWeeklyCurriculumCommand.builder()
                .weeklyCurriculumId(10L).isExtra(true).build());

            assertThat(weekly.isExtra()).isTrue();
            then(saveWeeklyCurriculumPort).should().save(weekly);
        }
    }

    // ===== delete =====

    @Nested
    @DisplayName("주차별 커리큘럼 삭제")
    class Delete {

        @Test
        void 주차별_커리큘럼_삭제에_성공한다() {
            // given
            Curriculum curriculum = 커리큘럼();
            WeeklyCurriculum weeklyCurriculum = 주차별_커리큘럼(curriculum);
            given(loadWeeklyCurriculumPort.getById(10L)).willReturn(weeklyCurriculum);
            given(loadWeeklyCurriculumPort.existsOriginalWorkbookByWeeklyCurriculumId(10L))
                .willReturn(false);

            // when
            sut.delete(10L);

            // then
            then(saveWeeklyCurriculumPort).should().delete(weeklyCurriculum);
        }

        @Test
        void 존재하지_않는_주차별_커리큘럼을_삭제하면_예외가_발생한다() {
            // given
            given(loadWeeklyCurriculumPort.getById(999L))
                .willThrow(new CurriculumDomainException(CurriculumErrorCode.WEEKLY_CURRICULUM_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> sut.delete(999L))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WEEKLY_CURRICULUM_NOT_FOUND);
        }

        @Test
        void 원본_워크북이_존재하면_삭제할_수_없다() {
            // given
            Curriculum curriculum = 커리큘럼();
            WeeklyCurriculum weeklyCurriculum = 주차별_커리큘럼(curriculum);
            given(loadWeeklyCurriculumPort.getById(10L)).willReturn(weeklyCurriculum);
            given(loadWeeklyCurriculumPort.existsOriginalWorkbookByWeeklyCurriculumId(10L))
                .willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.delete(10L))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WEEKLY_CURRICULUM_HAS_WORKBOOKS);

            then(saveWeeklyCurriculumPort).should(never()).delete(any());
        }
    }

    private CreateWeeklyCurriculumCommand createCommand(long weekNo, boolean isExtra, Instant endsAt) {
        return CreateWeeklyCurriculumCommand.builder()
            .curriculumId(1L)
            .weekNo(weekNo)
            .isExtra(isExtra)
            .title(weekNo + "주차")
            .startsAt(START)
            .endsAt(endsAt)
            .build();
    }
}
