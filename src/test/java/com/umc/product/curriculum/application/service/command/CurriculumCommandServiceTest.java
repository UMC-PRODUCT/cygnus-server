package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateCurriculumCommand;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.EditCurriculumCommand;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.application.port.out.SaveCurriculumPort;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
class CurriculumCommandServiceTest {

    @Mock
    LoadCurriculumPort loadCurriculumPort;

    @Mock
    SaveCurriculumPort saveCurriculumPort;

    @Mock
    LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @InjectMocks
    CurriculumCommandService sut;

    @Test
    void 트랙_기수의_기본_트랙_커리큘럼을_생성한다() {
        // given
        given(getGisuUseCase.getById(10L)).willReturn(
            new GisuInfo(10L, 10L, Instant.EPOCH, Instant.MAX, false, GisuLearningType.TRACK));
        var command = CreateCurriculumCommand.builder().gisuId(10L)
            .track(ChallengerTrack.WEB_PRODUCT_ENGINEER).title("웹 프로덕트").build();
        given(saveCurriculumPort.save(any())).willAnswer(invocation -> {
            Curriculum curriculum = invocation.getArgument(0);
            assertThat(curriculum.getPart()).isNull();
            assertThat(curriculum.getTrack()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
            ReflectionTestUtils.setField(curriculum, "id", 11L);
            return curriculum;
        });

        // when / then
        assertThat(sut.create(command)).isEqualTo(11L);
    }

    @Test
    void 트랙_기수에_파트_커리큘럼을_생성하지_못한다() {
        // given
        given(getGisuUseCase.getById(10L)).willReturn(
            new GisuInfo(10L, 10L, Instant.EPOCH, Instant.MAX, false, GisuLearningType.TRACK));

        // when / then
        assertThatThrownBy(() -> sut.create(new CreateCurriculumCommand(10L, ChallengerPart.WEB, "웹")))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode").isEqualTo(CurriculumErrorCode.INVALID_CURRICULUM_LEARNING_TYPE);
        then(saveCurriculumPort).shouldHaveNoInteractions();
    }

    @Test
    void 플러스_트랙_커리큘럼은_생성하지_못한다() {
        // given
        given(getGisuUseCase.getById(10L)).willReturn(
            new GisuInfo(10L, 10L, Instant.EPOCH, Instant.MAX, false, GisuLearningType.TRACK));
        var command = CreateCurriculumCommand.builder().gisuId(10L)
            .track(ChallengerTrack.INFRA_PLUS).title("인프라").build();

        // when / then
        assertThatThrownBy(() -> sut.create(command)).isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode").isEqualTo(CurriculumErrorCode.UNSUPPORTED_CURRICULUM_TRACK);
        then(saveCurriculumPort).shouldHaveNoInteractions();
    }

    @Test
    void 같은_기수의_동일한_트랙은_중복_생성하지_못한다() {
        // given
        given(getGisuUseCase.getById(10L)).willReturn(
            new GisuInfo(10L, 10L, Instant.EPOCH, Instant.MAX, false, GisuLearningType.TRACK));
        given(loadCurriculumPort.existsByGisuIdAndTrack(10L, ChallengerTrack.DESIGN)).willReturn(true);
        var command = CreateCurriculumCommand.builder().gisuId(10L).track(ChallengerTrack.DESIGN)
            .title("디자인").build();

        // when / then
        assertThatThrownBy(() -> sut.create(command)).isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode").isEqualTo(CurriculumErrorCode.CURRICULUM_ALREADY_EXISTS);
        then(saveCurriculumPort).shouldHaveNoInteractions();
    }

    // ===== create =====

    @Nested
    @DisplayName("커리큘럼 생성")
    class Create {

        @BeforeEach
        void 기수를_준비한다() {
            given(getGisuUseCase.getById(9L)).willReturn(
                new GisuInfo(9L, 9L, Instant.EPOCH, Instant.MAX, true));
        }


        @Test
        void 커리큘럼_생성에_성공한다() {
            // given
            var command = CreateCurriculumCommand.builder()
                .gisuId(9L)
                .part(ChallengerPart.SPRINGBOOT)
                .title("9기 스프링부트 커리큘럼")
                .build();

            given(loadCurriculumPort.existsByGisuIdAndPart(9L, ChallengerPart.SPRINGBOOT))
                .willReturn(false);

            Curriculum saved = Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "9기 스프링부트 커리큘럼");
            ReflectionTestUtils.setField(saved, "id", 1L);
            given(saveCurriculumPort.save(any(Curriculum.class))).willReturn(saved);

            // when
            Long result = sut.create(command);

            // then
            assertThat(result).isEqualTo(1L);
            then(saveCurriculumPort).should().save(any(Curriculum.class));
        }

        @Test
        void 동일_기수와_파트의_커리큘럼이_이미_존재하면_예외가_발생한다() {
            // given
            var command = CreateCurriculumCommand.builder()
                .gisuId(9L)
                .part(ChallengerPart.SPRINGBOOT)
                .title("중복 커리큘럼")
                .build();

            given(loadCurriculumPort.existsByGisuIdAndPart(9L, ChallengerPart.SPRINGBOOT))
                .willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.CURRICULUM_ALREADY_EXISTS);

            then(saveCurriculumPort).should(never()).save(any());
        }
    }

    // ===== edit =====

    @Nested
    @DisplayName("커리큘럼 수정")
    class Edit {

        @Test
        void 커리큘럼_제목_수정에_성공한다() {
            // given
            Curriculum curriculum = Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "구 제목");
            given(loadCurriculumPort.findById(1L)).willReturn(Optional.of(curriculum));
            given(saveCurriculumPort.save(curriculum)).willReturn(curriculum);

            var command = EditCurriculumCommand.builder()
                .curriculumId(1L)
                .title("새 제목")
                .build();

            // when
            sut.edit(command);

            // then
            assertThat(curriculum.getTitle()).isEqualTo("새 제목");
            then(saveCurriculumPort).should().save(curriculum);
        }

        @Test
        void 제목이_빈_문자열이면_기존_제목이_유지된다() {
            // given
            Curriculum curriculum = Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "원래 제목");
            given(loadCurriculumPort.findById(1L)).willReturn(Optional.of(curriculum));
            given(saveCurriculumPort.save(curriculum)).willReturn(curriculum);

            var command = EditCurriculumCommand.builder()
                .curriculumId(1L)
                .title("   ")  // blank → StringUtils.hasText = false
                .build();

            // when
            sut.edit(command);

            // then
            assertThat(curriculum.getTitle()).isEqualTo("원래 제목");
        }

        @Test
        void 존재하지_않는_커리큘럼을_수정하면_예외가_발생한다() {
            // given
            given(loadCurriculumPort.findById(999L)).willReturn(Optional.empty());

            var command = EditCurriculumCommand.builder()
                .curriculumId(999L)
                .title("새 제목")
                .build();

            // when & then
            assertThatThrownBy(() -> sut.edit(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.CURRICULUM_NOT_FOUND);
        }
    }

    // ===== delete =====

    @Nested
    @DisplayName("커리큘럼 삭제")
    class Delete {

        @Test
        void 커리큘럼_삭제에_성공한다() {
            // given
            Curriculum curriculum = Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "삭제할 커리큘럼");
            given(loadCurriculumPort.findById(1L)).willReturn(Optional.of(curriculum));
            given(loadWeeklyCurriculumPort.existsByCurriculumId(1L)).willReturn(false);

            // when
            sut.delete(1L);

            // then
            then(saveCurriculumPort).should().delete(curriculum);
        }

        @Test
        void 존재하지_않는_커리큘럼을_삭제하면_예외가_발생한다() {
            // given
            given(loadCurriculumPort.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.delete(999L))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.CURRICULUM_NOT_FOUND);
        }

        @Test
        void 주차별_커리큘럼이_존재하면_삭제할_수_없다() {
            // given
            Curriculum curriculum = Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "삭제 불가");
            given(loadCurriculumPort.findById(1L)).willReturn(Optional.of(curriculum));
            given(loadWeeklyCurriculumPort.existsByCurriculumId(1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.delete(1L))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.CURRICULUM_HAS_WEEKLY_CURRICULUMS);

            then(saveCurriculumPort).should(never()).delete(any());
        }
    }
}
