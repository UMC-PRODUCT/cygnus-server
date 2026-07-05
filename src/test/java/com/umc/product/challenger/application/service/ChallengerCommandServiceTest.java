package com.umc.product.challenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ChallengerDeactivationType;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerCommand;
import com.umc.product.challenger.application.port.in.command.dto.DeactivateChallengerCommand;
import com.umc.product.challenger.application.port.in.command.dto.DeleteChallengerCommand;
import com.umc.product.challenger.application.port.in.command.dto.GrantChallengerPointCommand;
import com.umc.product.challenger.application.port.in.command.dto.UpdateChallengerCommand;
import com.umc.product.challenger.application.port.in.command.dto.UpdateChallengerPointCommand;
import com.umc.product.challenger.application.port.out.LoadChallengerPointPort;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPointPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerPoint;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerCommandService")
class ChallengerCommandServiceTest {

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

    @InjectMocks
    ChallengerCommandService sut;

    @Nested
    @DisplayName("createChallenger")
    class CreateChallenger {

        @Test
        @DisplayName("동일 기수 챌린저가 없으면 생성한다")
        void 동일_기수_챌린저가_없으면_생성한다() {
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

            assertThat(result).isEqualTo(100L);
            then(saveChallengerPort).should().save(any(Challenger.class));
        }

        @Test
        @DisplayName("동일 기수 챌린저가 없으면 생성 후 해당 회원의 권한 snapshot 캐시를 제거한다")
        void 동일_기수_챌린저가_없으면_생성_후_해당_회원의_권한_snapshot_캐시를_제거한다() {
            CreateChallengerCommand command = CreateChallengerCommand.builder()
                .memberId(1L)
                .part(ChallengerPart.SPRINGBOOT)
                .gisuId(9L)
                .build();
            given(loadChallengerPort.findByMemberIdAndGisuId(1L, 9L)).willReturn(Optional.empty());
            given(saveChallengerPort.save(any(Challenger.class))).willAnswer(invocation -> invocation.getArgument(0));

            sut.createChallenger(command);

            then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(1L);
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
                .willReturn(Optional.of(challenger(1L, ChallengerStatus.ACTIVE)));

            assertThatThrownBy(() -> sut.createChallenger(command))
                .isInstanceOf(ChallengerDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS);

            then(saveChallengerPort).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateChallenger")
    class UpdateChallenger {

        @Test
        @DisplayName("변경할 파트와 상태가 모두 없으면 실패한다")
        void 변경할_파트와_상태가_모두_없으면_실패한다() {
            given(loadChallengerPort.getById(1L)).willReturn(challenger(1L, ChallengerStatus.ACTIVE));

            assertThatThrownBy(() -> sut.updateChallenger(
                UpdateChallengerCommand.forPartChange(1L, null, 99L)
            ))
                .isInstanceOf(ChallengerDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ChallengerErrorCode.BAD_CHALLENGER_UPDATE_REQUEST);

            then(saveChallengerPort).should(never()).save(any());
        }

        @Test
        @DisplayName("비활성 챌린저는 파트를 변경할 수 없다")
        void 비활성_챌린저는_파트를_변경할_수_없다() {
            given(loadChallengerPort.getById(1L)).willReturn(challenger(1L, ChallengerStatus.WITHDRAWN));

            assertThatThrownBy(() -> sut.updateChallenger(
                UpdateChallengerCommand.forPartChange(1L, ChallengerPart.WEB, 99L)
            ))
                .isInstanceOf(ChallengerDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ChallengerErrorCode.CHALLENGER_NOT_ACTIVE);

            then(saveChallengerPort).should(never()).save(any());
        }

        @Test
        @DisplayName("파트를 변경하면 해당 회원의 권한 snapshot 캐시를 제거한다")
        void 파트를_변경하면_해당_회원의_권한_snapshot_캐시를_제거한다() {
            given(loadChallengerPort.getById(1L)).willReturn(challenger(1L, ChallengerStatus.ACTIVE));

            sut.updateChallenger(UpdateChallengerCommand.forPartChange(1L, ChallengerPart.WEB, 99L));

            then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(1L);
        }
    }

    @Test
    @DisplayName("챌린저를 대량 생성하면 생성된 회원들의 권한 snapshot 캐시를 제거한다")
    void 챌린저를_대량_생성하면_생성된_회원들의_권한_snapshot_캐시를_제거한다() {
        given(environment.getActiveProfiles()).willReturn(new String[] {"local"});
        given(saveChallengerPort.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        sut.createChallengerBulk(List.of(
            CreateChallengerCommand.builder()
                .memberId(1L)
                .part(ChallengerPart.SPRINGBOOT)
                .gisuId(9L)
                .build(),
            CreateChallengerCommand.builder()
                .memberId(2L)
                .part(ChallengerPart.WEB)
                .gisuId(9L)
                .build()
        ));

        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberIds(List.of(1L, 2L));
    }

    @Test
    @DisplayName("deactivateChallenger는 EXPEL 타입을 EXPELLED 상태로 매핑한다")
    void deactivateChallenger는_EXPEL_타입을_EXPELLED_상태로_매핑한다() {
        Challenger challenger = challenger(1L, ChallengerStatus.ACTIVE);
        given(loadChallengerPort.getById(1L)).willReturn(challenger);

        sut.deactivateChallenger(DeactivateChallengerCommand.of(
            1L,
            ChallengerDeactivationType.EXPEL,
            99L,
            "징계"
        ));

        assertThat(challenger.getStatus()).isEqualTo(ChallengerStatus.EXPELLED);
        assertThat(challenger.getModifiedBy()).isEqualTo(99L);
        assertThat(challenger.getModificationReason()).isEqualTo("징계");
    }

    @Test
    @DisplayName("챌린저를 비활성화하면 해당 회원의 권한 snapshot 캐시를 제거한다")
    void 챌린저를_비활성화하면_해당_회원의_권한_snapshot_캐시를_제거한다() {
        given(loadChallengerPort.getById(1L)).willReturn(challenger(1L, ChallengerStatus.ACTIVE));

        sut.deactivateChallenger(DeactivateChallengerCommand.of(
            1L,
            ChallengerDeactivationType.WITHDRAW,
            99L,
            "탈퇴"
        ));

        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(1L);
    }

    @Test
    @DisplayName("챌린저 삭제 후 해당 회원의 권한 snapshot 캐시를 제거한다")
    void 챌린저_삭제_후_해당_회원의_권한_snapshot_캐시를_제거한다() {
        given(loadChallengerPort.getById(1L)).willReturn(challenger(1L, ChallengerStatus.ACTIVE));

        sut.deleteChallenger(new DeleteChallengerCommand(1L, "잘못 생성된 기록"));

        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(1L);
    }

    @Test
    @DisplayName("비활성 챌린저에게 상벌점을 부여할 수 없다")
    void 비활성_챌린저에게_상벌점을_부여할_수_없다() {
        given(loadChallengerPort.getById(1L)).willReturn(challenger(1L, ChallengerStatus.WITHDRAWN));

        assertThatThrownBy(() -> sut.grantChallengerPoint(GrantChallengerPointCommand.builder()
            .challengerId(1L)
            .pointType(PointType.CUSTOM)
            .pointValue(1)
            .description("조정")
            .build()))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.CHALLENGER_NOT_ACTIVE);

        then(saveChallengerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("상벌점 설명을 수정한다")
    void 상벌점_설명을_수정한다() {
        Challenger challenger = challenger(1L, ChallengerStatus.ACTIVE);
        ChallengerPoint point = ChallengerPoint.create(challenger, PointType.CUSTOM, 1, "기존");
        given(loadChallengerPointPort.getById(10L)).willReturn(point);

        sut.updateChallengerPoint(UpdateChallengerPointCommand.of(10L, "수정"));

        assertThat(point.getDescription()).isEqualTo("수정");
        then(saveChallengerPointPort).should().save(point);
    }

    private Challenger challenger(Long id, ChallengerStatus status) {
        Challenger challenger = Challenger.builder()
            .memberId(1L)
            .part(ChallengerPart.SPRINGBOOT)
            .gisuId(9L)
            .build();
        ReflectionTestUtils.setField(challenger, "id", id);
        ReflectionTestUtils.setField(challenger, "status", status);
        return challenger;
    }
}
