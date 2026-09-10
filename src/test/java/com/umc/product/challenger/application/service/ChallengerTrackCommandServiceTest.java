package com.umc.product.challenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.challenger.application.port.in.command.dto.AddChallengerTrackCommand;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerCommand;
import com.umc.product.challenger.application.port.out.LoadChallengerPointPort;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPointPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("Challenger track command")
class ChallengerTrackCommandServiceTest {

    @Mock
    Environment environment;

    @Mock
    LoadChallengerPort loadChallengerPort;

    @Mock
    SaveChallengerPort saveChallengerPort;

    @Mock
    LoadChallengerPointPort loadChallengerPointPort;

    @Mock
    SaveChallengerPointPort saveChallengerPointPort;

    @Mock
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @BeforeEach
    void 기본_기수는_파트_학습을_사용한다() {
        GisuInfo gisu = mock(GisuInfo.class);
        lenient().when(gisu.learningType()).thenReturn(GisuLearningType.PART);
        lenient().when(getGisuUseCase.getById(any())).thenReturn(gisu);
    }

    @InjectMocks
    ChallengerCommandService sut;

    @Nested
    @DisplayName("createChallenger")
    class CreateChallenger {

        @Test
        @DisplayName("트랙 기수에서도 여러 기본 트랙을 직접 등록할 수 있다")
        void 트랙_기수에서도_여러_기본_트랙을_직접_등록할_수_있다() {
            givenTrackGisu();
            given(saveChallengerPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
            CreateChallengerCommand command = CreateChallengerCommand.builder()
                .memberId(1L).gisuId(9L)
                .tracks(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.MOBILE_PRODUCT_ENGINEER))
                .build();

            sut.createChallenger(command);

            ArgumentCaptor<Challenger> captor = ArgumentCaptor.forClass(Challenger.class);
            then(saveChallengerPort).should().save(captor.capture());
            assertThat(captor.getValue().getTracks()).containsExactly(
                ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.MOBILE_PRODUCT_ENGINEER);
        }

        @Test
        @DisplayName("트랙 기수에서는 파트를 혼합한 직접 생성을 거부한다")
        void 트랙_기수에서는_파트를_혼합한_직접_생성을_거부한다() {
            givenTrackGisu();
            CreateChallengerCommand command = CreateChallengerCommand.builder()
                .memberId(1L).gisuId(9L).part(ChallengerPart.WEB)
                .tracks(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER)).build();

            assertThatThrownBy(() -> sut.createChallenger(command))
                .isInstanceOf(ChallengerDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ChallengerErrorCode.INVALID_CHALLENGER_LEARNING_TYPE);
            then(saveChallengerPort).should(never()).save(any());
        }

        @Test
        @DisplayName("트랙 기수에서는 PLUS를 포함한 직접 생성을 거부한다")
        void 트랙_기수에서는_PLUS를_포함한_직접_생성을_거부한다() {
            givenTrackGisu();
            CreateChallengerCommand command = CreateChallengerCommand.builder()
                .memberId(1L).gisuId(9L)
                .tracks(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.INFRA_PLUS)).build();

            assertThatThrownBy(() -> sut.createChallenger(command))
                .isInstanceOf(ChallengerDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ChallengerErrorCode.INVALID_CHALLENGER_LEARNING_TYPE);
            then(saveChallengerPort).should(never()).save(any());
        }

        @Test
        @DisplayName("동일 기수 챌린저가 없으면 기존 part로 생성한다")
        void 동일_기수_챌린저가_없으면_기존_part로_생성한다() {
            CreateChallengerCommand command = CreateChallengerCommand.builder()
                .memberId(1L)
                .part(ChallengerPart.SPRINGBOOT)
                .gisuId(9L)
                .build();
            given(loadChallengerPort.findByMemberIdAndGisuId(1L, 9L)).willReturn(Optional.empty());
            given(saveChallengerPort.save(any(Challenger.class))).willAnswer(invocation -> {
                Challenger challenger = invocation.getArgument(0);
                ReflectionTestUtils.setField(challenger, "id", 100L);
                return challenger;
            });

            Long result = sut.createChallenger(command);

            ArgumentCaptor<Challenger> captor = ArgumentCaptor.forClass(Challenger.class);
            assertThat(result).isEqualTo(100L);
            then(saveChallengerPort).should().save(captor.capture());
            then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(1L);
            assertThat(captor.getValue().getPart()).isEqualTo(ChallengerPart.SPRINGBOOT);
            assertThat(captor.getValue().getTracks()).isEmpty();
            assertThat(captor.getValue().getEffectiveTracks())
                .containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        }

        @Test
        @DisplayName("트랙 기반 챌린저는 파트 없이 여러 트랙으로 생성한다")
        void 트랙_기반_챌린저는_파트_없이_여러_트랙으로_생성한다() {
            CreateChallengerCommand command = CreateChallengerCommand.builder()
                .memberId(1L)
                .tracks(List.of(
                    ChallengerTrack.WEB_PRODUCT_ENGINEER,
                    ChallengerTrack.MOBILE_PRODUCT_ENGINEER
                ))
                .gisuId(9L)
                .build();
            given(loadChallengerPort.findByMemberIdAndGisuId(1L, 9L)).willReturn(Optional.empty());
            given(saveChallengerPort.save(any(Challenger.class))).willAnswer(invocation -> {
                Challenger challenger = invocation.getArgument(0);
                ReflectionTestUtils.setField(challenger, "id", 101L);
                return challenger;
            });

            Long result = sut.createChallenger(command);

            ArgumentCaptor<Challenger> captor = ArgumentCaptor.forClass(Challenger.class);
            assertThat(result).isEqualTo(101L);
            then(saveChallengerPort).should().save(captor.capture());
            assertThat(captor.getValue().getPart()).isNull();
            assertThat(captor.getValue().getTracks()).containsExactly(
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                ChallengerTrack.MOBILE_PRODUCT_ENGINEER
            );
        }

        @Test
        @DisplayName("동일 기수 챌린저가 있으면 생성하지 않는다")
        void 동일_기수_챌린저가_있으면_생성하지_않는다() {
            CreateChallengerCommand command = CreateChallengerCommand.builder()
                .memberId(1L)
                .part(ChallengerPart.SPRINGBOOT)
                .gisuId(9L)
                .build();
            given(loadChallengerPort.findByMemberIdAndGisuId(1L, 9L))
                .willReturn(Optional.of(challenger(1L)));

            assertThatThrownBy(() -> sut.createChallenger(command))
                .isInstanceOf(ChallengerDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS);

            then(saveChallengerPort).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("addTrack")
    class AddTrack {

        @Test
        @DisplayName("동일 기수 챌린저가 없으면 track 기반 신규 챌린저를 생성한다")
        void 동일_기수_챌린저가_없으면_track_기반_신규_챌린저를_생성한다() {
            given(loadChallengerPort.findByMemberIdAndGisuId(1L, 9L)).willReturn(Optional.empty());

            sut.addTrack(AddChallengerTrackCommand.of(1L, 9L, ChallengerTrack.DESIGN));

            ArgumentCaptor<Challenger> captor = ArgumentCaptor.forClass(Challenger.class);
            then(saveChallengerPort).should().save(captor.capture());
            then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(1L);
            assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
            assertThat(captor.getValue().getGisuId()).isEqualTo(9L);
            assertThat(captor.getValue().getPart()).isNull();
            assertThat(captor.getValue().getTracks()).containsExactly(ChallengerTrack.DESIGN);
        }

        @Test
        @DisplayName("같은 트랙을 반복 추가해도 한 번만 저장한다")
        void 같은_트랙을_반복_추가해도_한_번만_저장한다() {
            Challenger challenger = challenger(1L);
            AddChallengerTrackCommand command = AddChallengerTrackCommand.of(
                1L,
                9L,
                ChallengerTrack.WEB_PRODUCT_ENGINEER
            );
            given(loadChallengerPort.findByMemberIdAndGisuId(1L, 9L)).willReturn(Optional.of(challenger));

            sut.addTrack(command);
            sut.addTrack(command);

            assertThat(challenger.getTracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
            then(saveChallengerPort).should().save(challenger);
            then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(1L);
        }

        @Test
        @DisplayName("기존 챌린저에 INFRA_PLUS 트랙을 추가한다")
        void 기존_챌린저에_INFRA_PLUS_트랙을_추가한다() {
            Challenger challenger = challenger(1L);
            given(loadChallengerPort.findByMemberIdAndGisuId(1L, 9L)).willReturn(Optional.of(challenger));

            sut.addTrack(AddChallengerTrackCommand.of(1L, 9L, ChallengerTrack.INFRA_PLUS));

            assertThat(challenger.getTracks()).containsExactly(ChallengerTrack.INFRA_PLUS);
            then(saveChallengerPort).should().save(challenger);
        }
    }

    private Challenger challenger(Long id) {
        Challenger challenger = Challenger.builder()
            .memberId(1L)
            .part(ChallengerPart.SPRINGBOOT)
            .gisuId(9L)
            .build();
        ReflectionTestUtils.setField(challenger, "id", id);
        ReflectionTestUtils.setField(challenger, "status", ChallengerStatus.ACTIVE);
        return challenger;
    }

    private void givenTrackGisu() {
        GisuInfo gisu = mock(GisuInfo.class);
        given(gisu.learningType()).willReturn(GisuLearningType.TRACK);
        given(getGisuUseCase.getById(9L)).willReturn(gisu);
    }
}
