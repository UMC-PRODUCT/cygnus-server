package com.umc.product.challenger.application.service;

import static com.umc.product.support.fixture.ChallengerUnitFixture.챌린저;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.authorization.application.port.in.command.ManageChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.DeleteChallengerPointCommand;
import com.umc.product.challenger.application.port.in.command.dto.GrantChallengerPointCommand;
import com.umc.product.challenger.application.port.in.command.dto.UpdateChallengerCommand;
import com.umc.product.challenger.application.port.in.query.GetChallengerPointUseCase;
import com.umc.product.challenger.application.port.out.LoadChallengerPointPort;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.application.port.out.LoadChallengerRecordPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPointPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPort;
import com.umc.product.challenger.application.port.out.SaveChallengerRecordPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerPoint;
import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;

@DisplayName("Challenger application 잔여 계약")
class ChallengerApplicationResidualTest {

    @Test
    @DisplayName("상태 변경·상벌점 부여·삭제 command를 도메인과 port에 위임한다")
    void 단건_command를_처리한다() {
        Fixture fixture = new Fixture("local");
        Challenger challenger = 챌린저(1L, 10L, 9L);
        ChallengerPoint point = ChallengerPoint.create(challenger, PointType.WARNING, "warning");
        ReflectionTestUtils.setField(point, "id", 100L);
        given(fixture.loadChallengerPort.getById(1L)).willReturn(challenger);
        given(fixture.loadPointPort.getById(100L)).willReturn(point);

        fixture.sut.updateChallenger(UpdateChallengerCommand.forStatusChange(
            1L, ChallengerStatus.GRADUATED, "수료", 99L));

        assertThat(challenger.getStatus()).isEqualTo(ChallengerStatus.GRADUATED);

        Challenger active = 챌린저(2L, 20L, 9L);
        given(fixture.loadChallengerPort.getById(2L)).willReturn(active);
        fixture.sut.grantChallengerPoint(GrantChallengerPointCommand.builder()
            .challengerId(2L)
            .pointType(PointType.CUSTOM)
            .pointValue(7)
            .description("조정")
            .build());
        then(fixture.savePointPort).should().save(argThat(saved ->
            saved.getChallengerId().equals(2L) && saved.getPointValue() == 7.0));

        fixture.sut.deleteChallengerPoint(new DeleteChallengerPointCommand(100L));
        then(fixture.savePointPort).should().delete(point);
    }

    @Test
    @DisplayName("상벌점 bulk는 중복 challenger ID를 한 번 조회하고 각 command를 추가한다")
    void 상벌점_bulk를_처리한다() {
        Fixture fixture = new Fixture("local");
        Challenger challenger = 챌린저(1L, 10L, 9L);
        given(fixture.loadChallengerPort.getAllByIds(Set.of(1L))).willReturn(List.of(challenger));
        List<GrantChallengerPointCommand> commands = List.of(
            GrantChallengerPointCommand.builder()
                .challengerId(1L).pointType(PointType.WARNING).description("경고").build(),
            GrantChallengerPointCommand.builder()
                .challengerId(1L).pointType(PointType.BLOG_CHALLENGE).description("블로그").build()
        );

        fixture.sut.grantChallengerPointBulk(commands);

        then(fixture.savePointPort).should().saveAll(argThat(points ->
            points.size() == 2 && points.stream().allMatch(point -> point.getChallengerId().equals(1L))));
    }

    @Test
    @DisplayName("production에서는 검증 없는 bulk command를 차단한다")
    void production_bulk를_차단한다() {
        Fixture fixture = new Fixture("prod");

        assertThatThrownBy(() -> fixture.sut.grantChallengerPointBulk(List.of()))
            .isInstanceOf(CommonException.class);
    }

    @Test
    @DisplayName("ChallengerRecord bulk 생성과 삭제를 port에 위임한다")
    void record_bulk와_삭제를_처리한다() {
        SaveChallengerRecordPort saveRecordPort = mock(SaveChallengerRecordPort.class);
        LoadChallengerRecordPort loadRecordPort = mock(LoadChallengerRecordPort.class);
        ChallengerRecordCommandService sut = new ChallengerRecordCommandService(
            saveRecordPort,
            loadRecordPort,
            mock(SaveChallengerPort.class),
            mock(LoadChallengerPort.class),
            mock(GetChapterUseCase.class),
            mock(GetMemberUseCase.class),
            mock(ManageChallengerRoleUseCase.class),
            mock(EvictAuthoritySnapshotCacheUseCase.class),
            mock(SendWebhookAlarmUseCase.class)
        );
        CreateChallengerRecordCommand command = recordCommand();
        ChallengerRecord record = command.toEntity();
        ReflectionTestUtils.setField(record, "id", 1L);
        given(saveRecordPort.saveAll(any())).willReturn(List.of(record));
        given(loadRecordPort.getById(1L)).willReturn(record);

        assertThat(sut.createBulk(List.of(command))).containsExactly(1L);
        sut.delete(1L);

        then(saveRecordPort).should().delete(record);
    }

    @Test
    @DisplayName("record와 point query service는 entity를 DTO로 일괄 변환한다")
    void query_service를_변환한다() {
        Challenger challenger = 챌린저(1L, 10L, 9L);
        ChallengerPoint point = ChallengerPoint.create(challenger, PointType.BLOG_CHALLENGE, "blog");
        ReflectionTestUtils.setField(point, "id", 100L);
        ChallengerRecord record = recordCommand().toEntity();
        ReflectionTestUtils.setField(record, "id", 200L);
        LoadChallengerRecordPort recordPort = mock(LoadChallengerRecordPort.class);
        given(recordPort.getById(200L)).willReturn(record);
        given(recordPort.getByCode(record.getCode())).willReturn(record);
        given(recordPort.findBySchoolId(3L)).willReturn(List.of(record));
        given(recordPort.findByChapterId(2L)).willReturn(List.of(record));
        ChallengerRecordQueryService recordService = new ChallengerRecordQueryService(recordPort);

        assertThat(recordService.getById(200L).id()).isEqualTo(200L);
        assertThat(recordService.getByCode(record.getCode()).code()).isEqualTo(record.getCode());
        assertThat(recordService.getBySchoolId(3L)).hasSize(1);
        assertThat(recordService.getByChapterId(2L)).hasSize(1);

        LoadChallengerPointPort pointPort = mock(LoadChallengerPointPort.class);
        given(pointPort.getById(100L)).willReturn(point);
        given(pointPort.findByChallengerIdIn(Set.of(1L))).willReturn(List.of(point));
        ChallengerPointQueryService pointService = new ChallengerPointQueryService(pointPort);
        assertThat(pointService.getById(100L).challengerId()).isEqualTo(1L);
        assertThat(pointService.getMapByChallengerIds(null)).isEmpty();
        assertThat(pointService.getMapByChallengerIds(Set.of())).isEmpty();
        assertThat(pointService.getMapByChallengerIds(Set.of(1L))).containsKey(1L);

        assertThat(ChallengerPoint.create(challenger, PointType.CUSTOM, "default").getPointValue())
            .isEqualTo(0.0);
    }

    @Test
    @DisplayName("member와 gisu로 조회한 challenger를 point 정보와 함께 변환한다")
    void member_gisu_challenger를_변환한다() {
        LoadChallengerPort loadPort = mock(LoadChallengerPort.class);
        GetChallengerPointUseCase pointUseCase = mock(GetChallengerPointUseCase.class);
        Challenger challenger = 챌린저(1L, 10L, 9L);
        given(loadPort.findByMemberIdAndGisuId(10L, 9L)).willReturn(Optional.of(challenger));
        given(pointUseCase.getListByChallengerId(1L)).willReturn(List.of());
        ChallengerQueryService sut = new ChallengerQueryService(loadPort, pointUseCase);

        assertThat(sut.getByMemberIdAndGisuId(10L, 9L).challengerId()).isEqualTo(1L);
    }

    private CreateChallengerRecordCommand recordCommand() {
        return CreateChallengerRecordCommand.builder()
            .creatorMemberId(99L)
            .gisuId(9L)
            .chapterId(2L)
            .schoolId(3L)
            .part(ChallengerPart.SPRINGBOOT)
            .memberName("홍길동")
            .build();
    }

    private static class Fixture {
        final Environment environment = mock(Environment.class);
        final LoadChallengerPort loadChallengerPort = mock(LoadChallengerPort.class);
        final SaveChallengerPort saveChallengerPort = mock(SaveChallengerPort.class);
        final LoadChallengerPointPort loadPointPort = mock(LoadChallengerPointPort.class);
        final SaveChallengerPointPort savePointPort = mock(SaveChallengerPointPort.class);
        final ChallengerCommandService sut;

        Fixture(String profile) {
            given(environment.getActiveProfiles()).willReturn(new String[] {profile});
            sut = new ChallengerCommandService(
                environment,
                loadChallengerPort,
                saveChallengerPort,
                loadPointPort,
                savePointPort,
                mock(EvictAuthoritySnapshotCacheUseCase.class)
            );
        }
    }
}
