package com.umc.product.organization.application.service;

import static com.umc.product.support.fixture.OrganizationUnitFixture.스쿼드;
import static com.umc.product.support.fixture.OrganizationUnitFixture.스쿼드_참여;
import static com.umc.product.support.fixture.OrganizationUnitFixture.프로덕트_멤버;
import static com.umc.product.support.fixture.OrganizationUnitFixture.활동_기간;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductSquadCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductSquadParticipantCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductSquadCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductSquadParticipantCommand;
import com.umc.product.organization.application.port.out.command.SaveUmcProductSquadParticipantPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductSquadPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadParticipantPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadPort;
import com.umc.product.organization.domain.UmcProductMember;
import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;
import com.umc.product.organization.domain.UmcProductSquad;
import com.umc.product.organization.domain.UmcProductSquadParticipant;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.enums.UmcProductSquadRole;
import com.umc.product.organization.exception.OrganizationErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC PRODUCT 스쿼드 명령 서비스 엣지 케이스")
class UmcProductSquadCommandServiceEdgeCaseTest {

    private static final Long REQUESTER_ID = 999L;
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);

    @Mock
    LoadUmcProductSquadPort loadSquadPort;
    @Mock
    SaveUmcProductSquadPort saveSquadPort;
    @Mock
    LoadUmcProductMemberPort loadMemberPort;
    @Mock
    LoadUmcProductMemberActivityPeriodPort loadPeriodPort;
    @Mock
    LoadUmcProductSquadParticipantPort loadParticipantPort;
    @Mock
    SaveUmcProductSquadParticipantPort saveParticipantPort;
    @Mock
    UmcProductAccessPolicy accessPolicy;

    @InjectMocks
    UmcProductSquadCommandService sut;

    @BeforeEach
    void 관리_권한을_허용한다() {
        lenient().when(accessPolicy.canManageUmcProduct(REQUESTER_ID)).thenReturn(true);
    }

    @Test
    @DisplayName("중복되지 않은 코드로 스쿼드를 생성하고 ID를 반환한다")
    void 스쿼드를_생성한다() {
        given(saveSquadPort.save(any())).willReturn(squad(3L));

        Long result = sut.create(createSquad(" PLATFORM "));

        assertThat(result).isEqualTo(3L);
        then(loadSquadPort).should().existsByCode("PLATFORM", null);
    }

    @Test
    @DisplayName("코드가 null이면 중복 조회는 생략하지만 domain 생성 규칙에서 거부한다")
    void 코드_없는_스쿼드를_거부한다() {
        assertError(() -> sut.create(createSquad(null)),
            OrganizationErrorCode.UMC_PRODUCT_SQUAD_CODE_REQUIRED);
        then(loadSquadPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("관리 권한이 없으면 스쿼드를 생성할 수 없다")
    void 스쿼드_관리_권한을_검증한다() {
        given(accessPolicy.canManageUmcProduct(100L)).willReturn(false);

        assertError(() -> sut.create(CreateUmcProductSquadCommand.of(
            100L, "PLATFORM", "Platform", "설명", START, END, 1, true)),
            OrganizationErrorCode.UMC_PRODUCT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("수정 값에 코드와 시작일이 없으면 기존 값을 유지한다")
    void 기존_코드와_시작일을_유지하며_수정한다() {
        UmcProductSquad squad = squad(3L);
        given(loadSquadPort.getByIdWithLock(3L)).willReturn(squad);
        given(loadParticipantPort.listBySquadId(3L)).willReturn(List.of());

        sut.update(UpdateUmcProductSquadCommand.of(
            3L, REQUESTER_ID, null, "변경", null, null, END, null, null));

        assertThat(squad.getStartDate()).isEqualTo(START);
        then(saveSquadPort).should().save(squad);
        then(loadSquadPort).should(never()).existsByCode(any(), any());
    }

    @Test
    @DisplayName("수정 코드가 있으면 현재 스쿼드 ID를 제외하고 중복을 검사한다")
    void 코드와_시작일을_수정한다() {
        UmcProductSquad squad = squad(3L);
        LocalDate nextStart = START.plusDays(1);
        given(loadSquadPort.getByIdWithLock(3L)).willReturn(squad);
        given(loadParticipantPort.listBySquadId(3L)).willReturn(List.of());

        sut.update(UpdateUmcProductSquadCommand.of(
            3L, REQUESTER_ID, " NEXT ", null, null, nextStart, END, null, null));

        assertThat(squad.getCode()).isEqualTo("NEXT");
        assertThat(squad.getStartDate()).isEqualTo(nextStart);
        then(loadSquadPort).should().existsByCode("NEXT", 3L);
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠른 스쿼드 수정은 거부한다")
    void 잘못된_스쿼드_기간을_거부한다() {
        given(loadSquadPort.getByIdWithLock(3L)).willReturn(squad(3L));

        assertError(() -> sut.update(UpdateUmcProductSquadCommand.of(
            3L, REQUESTER_ID, null, null, null, END, START, null, null)),
            OrganizationErrorCode.UMC_PRODUCT_PERIOD_INVALID);
    }

    @Test
    @DisplayName("종료일이 없는 스쿼드 기간은 종료일 없는 참여 이력을 포함한다")
    void 무기한_스쿼드로_수정한다() {
        UmcProductSquad squad = squad(3L);
        UmcProductSquadParticipant participant = mock(UmcProductSquadParticipant.class);
        given(participant.getStartDate()).willReturn(START);
        given(participant.getEndDate()).willReturn(null);
        given(loadSquadPort.getByIdWithLock(3L)).willReturn(squad);
        given(loadParticipantPort.listBySquadId(3L)).willReturn(List.of(participant));

        sut.update(UpdateUmcProductSquadCommand.of(
            3L, REQUESTER_ID, null, null, null, null, null, null, null));

        assertThat(squad.getEndDate()).isNull();
    }

    @Test
    @DisplayName("종료일이 있는 스쿼드는 종료일 없는 참여 이력을 포함할 수 없다")
    void 종료일_없는_참여_이력을_거부한다() {
        UmcProductSquadParticipant participant = mock(UmcProductSquadParticipant.class);
        given(participant.getStartDate()).willReturn(START);
        given(participant.getEndDate()).willReturn(null);
        given(loadSquadPort.getByIdWithLock(3L)).willReturn(squad(3L));
        given(loadParticipantPort.listBySquadId(3L)).willReturn(List.of(participant));

        assertError(() -> sut.update(UpdateUmcProductSquadCommand.of(
            3L, REQUESTER_ID, null, null, null, START, END, null, null)),
            OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("참여 이력이 없는 스쿼드는 삭제한다")
    void 스쿼드를_삭제한다() {
        UmcProductSquad squad = squad(3L);
        given(loadSquadPort.getByIdWithLock(3L)).willReturn(squad);

        sut.delete(3L, REQUESTER_ID);

        then(saveSquadPort).should().delete(squad);
    }

    @Test
    @DisplayName("활동 기간 안에 겹치지 않는 일반 참여 이력을 생성한다")
    void 일반_참여_이력을_생성한다() {
        UmcProductSquadParticipant saved = participant(40L, UmcProductSquadRole.MEMBER);
        givenParticipantPrerequisites();
        given(saveParticipantPort.save(any())).willReturn(saved);

        Long result = sut.createParticipant(createParticipant(UmcProductSquadRole.MEMBER, START, END));

        assertThat(result).isEqualTo(40L);
        then(loadParticipantPort).should(never()).existsOverlappingSquadLead(any(), any(), any(), any());
    }

    @Test
    @DisplayName("다른 멤버 참여와 겹치지 않는 스쿼드 리더를 생성한다")
    void 스쿼드_리더를_생성한다() {
        givenParticipantPrerequisites();
        given(saveParticipantPort.save(any()))
            .willReturn(participant(40L, UmcProductSquadRole.SQUAD_LEAD));

        assertThat(sut.createParticipant(
            createParticipant(UmcProductSquadRole.SQUAD_LEAD, START, END))).isEqualTo(40L);
        then(loadParticipantPort).should().existsOverlappingSquadLead(3L, START, END, null);
    }

    @Test
    @DisplayName("시작일 누락과 역전된 참여 기간을 거부한다")
    void 참여_기간을_검증한다() {
        assertError(() -> sut.createParticipant(
            createParticipant(UmcProductSquadRole.MEMBER, null, END)),
            OrganizationErrorCode.UMC_PRODUCT_START_DATE_REQUIRED);
        assertError(() -> sut.createParticipant(
            createParticipant(UmcProductSquadRole.MEMBER, END, START)),
            OrganizationErrorCode.UMC_PRODUCT_PERIOD_INVALID);
    }

    @Test
    @DisplayName("참여 기간을 포함하는 멤버 활동 기간이 없으면 거부한다")
    void 참여_활동_기간의_범위를_검증한다() {
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadSquadPort.getByIdWithLock(3L)).willReturn(squad(3L));
        given(loadPeriodPort.findContaining(1L, START, END)).willReturn(Optional.empty());

        assertError(() -> sut.createParticipant(
            createParticipant(UmcProductSquadRole.MEMBER, START, END)),
            OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("동일 스쿼드의 같은 멤버 참여 기간 중복을 거부한다")
    void 멤버_참여_중복을_거부한다() {
        givenParticipantPrerequisites();
        given(loadParticipantPort.existsOverlappingMemberInSquad(3L, 1L, START, END, null))
            .willReturn(true);

        assertError(() -> sut.createParticipant(
            createParticipant(UmcProductSquadRole.MEMBER, START, END)),
            OrganizationErrorCode.UMC_PRODUCT_SQUAD_PARTICIPATION_OVERLAPPED);
    }

    @Test
    @DisplayName("같은 기간에 둘 이상의 스쿼드 리더를 둘 수 없다")
    void 스쿼드_리더_중복을_거부한다() {
        givenParticipantPrerequisites();
        given(loadParticipantPort.existsOverlappingSquadLead(3L, START, END, null))
            .willReturn(true);

        assertError(() -> sut.createParticipant(
            createParticipant(UmcProductSquadRole.SQUAD_LEAD, START, END)),
            OrganizationErrorCode.UMC_PRODUCT_SQUAD_LEAD_OVERLAPPED);
    }

    @Test
    @DisplayName("기존 참여 이력은 본인 ID를 제외한 뒤 수정한다")
    void 참여_이력을_수정한다() {
        UmcProductSquadParticipant participant = participant(40L, UmcProductSquadRole.MEMBER);
        given(loadParticipantPort.getById(40L)).willReturn(participant);
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadSquadPort.getByIdWithLock(3L)).willReturn(squad(3L));
        given(loadPeriodPort.findContaining(1L, START, END)).willReturn(Optional.of(period()));

        sut.updateParticipant(updateParticipant(3L, UmcProductSquadRole.SQUAD_LEAD));

        then(loadParticipantPort).should().existsOverlappingMemberInSquad(3L, 1L, START, END, 40L);
        then(loadParticipantPort).should().existsOverlappingSquadLead(3L, START, END, 40L);
        then(saveParticipantPort).should().save(participant);
    }

    @Test
    @DisplayName("다른 스쿼드의 참여 이력은 수정할 수 없다")
    void 다른_스쿼드_참여_수정을_거부한다() {
        given(loadParticipantPort.getById(40L))
            .willReturn(participant(40L, UmcProductSquadRole.MEMBER));
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadSquadPort.getByIdWithLock(4L)).willReturn(squad(4L));

        assertError(() -> sut.updateParticipant(updateParticipant(4L, UmcProductSquadRole.MEMBER)),
            OrganizationErrorCode.UMC_PRODUCT_SQUAD_PARTICIPANT_NOT_FOUND);
    }

    @Test
    @DisplayName("소속 스쿼드의 참여 이력을 삭제한다")
    void 참여_이력을_삭제한다() {
        UmcProductSquadParticipant participant = participant(40L, UmcProductSquadRole.MEMBER);
        given(loadParticipantPort.getById(40L)).willReturn(participant);
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadSquadPort.getByIdWithLock(3L)).willReturn(squad(3L));

        sut.deleteParticipant(3L, 40L, REQUESTER_ID);

        then(saveParticipantPort).should().delete(participant);
    }

    @Test
    @DisplayName("다른 스쿼드의 참여 이력은 삭제할 수 없다")
    void 다른_스쿼드_참여_삭제를_거부한다() {
        given(loadParticipantPort.getById(40L))
            .willReturn(participant(40L, UmcProductSquadRole.MEMBER));
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadSquadPort.getByIdWithLock(4L)).willReturn(squad(4L));

        assertError(() -> sut.deleteParticipant(4L, 40L, REQUESTER_ID),
            OrganizationErrorCode.UMC_PRODUCT_SQUAD_PARTICIPANT_NOT_FOUND);
    }

    private void givenParticipantPrerequisites() {
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadSquadPort.getByIdWithLock(3L)).willReturn(squad(3L));
        given(loadPeriodPort.findContaining(1L, START, END)).willReturn(Optional.of(period()));
    }

    private void assertError(Runnable action, OrganizationErrorCode errorCode) {
        assertThatThrownBy(action::run)
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(errorCode);
    }

    private CreateUmcProductSquadCommand createSquad(String code) {
        return CreateUmcProductSquadCommand.of(
            REQUESTER_ID, code, "Platform", "설명", START, END, 1, true);
    }

    private CreateUmcProductSquadParticipantCommand createParticipant(
        UmcProductSquadRole role,
        LocalDate start,
        LocalDate end
    ) {
        return CreateUmcProductSquadParticipantCommand.of(
            3L, REQUESTER_ID, 1L, role, UmcProductPosition.SERVER_DEVELOPER,
            "Backend", "API 개발", start, end);
    }

    private UpdateUmcProductSquadParticipantCommand updateParticipant(
        Long squadId,
        UmcProductSquadRole role
    ) {
        return UpdateUmcProductSquadParticipantCommand.of(
            squadId, 40L, REQUESTER_ID, role, UmcProductPosition.SERVER_DEVELOPER,
            "Backend", "API 개발", START, END);
    }

    private UmcProductMember member() {
        return 프로덕트_멤버(1L, 100L, null);
    }

    private UmcProductMemberActivityPeriod period() {
        return 활동_기간(10L, member(), START, END);
    }

    private UmcProductSquad squad(Long id) {
        return 스쿼드(id, START, END);
    }

    private UmcProductSquadParticipant participant(Long id, UmcProductSquadRole role) {
        UmcProductSquadParticipant participant = 스쿼드_참여(id, squad(3L), period(), START, END);
        participant.update(
            period(), role, UmcProductPosition.SERVER_DEVELOPER,
            "Backend", "API 개발", START, END);
        return participant;
    }
}
