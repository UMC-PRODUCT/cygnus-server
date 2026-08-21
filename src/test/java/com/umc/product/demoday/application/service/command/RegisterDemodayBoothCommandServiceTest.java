package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothBatchCommand;
import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothBatchCommand.BoothRegistration;
import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothCommand;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayBoothPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterDemodayBoothCommandService")
class RegisterDemodayBoothCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long POLL_ID = 10L;
    private static final Long GISU_ID = 8L;
    private static final Long PROJECT_ID = 101L;
    private static final Long BOOTH_ID = 20L;
    private static final Integer BOOTH_CODE = 11;
    private static final Integer SECOND_BOOTH_CODE = 12;
    private static final String DISPLAY_NAME = "외부 참가팀 A";
    private static final String POLL_NAME = "8기 데모데이 투표";
    private static final Instant OPENS_AT = Instant.parse("2026-08-15T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-08-15T12:00:00Z");

    @Mock
    private DemodayAdminAccessChecker adminAccessChecker;

    @Mock
    private LoadDemodayPollPort loadDemodayPollPort;

    @Mock
    private SaveDemodayBoothPort saveDemodayBoothPort;

    @Captor
    private ArgumentCaptor<DemodayBooth> boothCaptor;

    @Captor
    private ArgumentCaptor<List<DemodayBooth>> boothListCaptor;

    @InjectMocks
    private RegisterDemodayBoothCommandService service;

    @Test
    @DisplayName("projectId만 담긴 요청은 프로젝트 부스로 저장하고 부스 ID를 반환한다")
    void registerProjectBooth() {
        // given
        givenClosedPoll();
        givenSaveAssignsBoothId();

        // when
        Long boothId = service.register(commandOf(PROJECT_ID, null));

        // then
        assertThat(boothId).isEqualTo(BOOTH_ID);

        then(adminAccessChecker).should().validateAdminAccess(MEMBER_ID, GISU_ID);
        then(saveDemodayBoothPort).should().save(boothCaptor.capture());

        assertThat(boothCaptor.getValue().getPollId()).isEqualTo(POLL_ID);
        assertThat(boothCaptor.getValue().getBoothCode()).isEqualTo(BOOTH_CODE);
        assertThat(boothCaptor.getValue().getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(boothCaptor.getValue().getDisplayName()).isNull();
    }

    @Test
    @DisplayName("displayName만 담긴 요청은 외부 부스로 저장한다")
    void registerExternalBooth() {
        // given
        givenClosedPoll();
        givenSaveAssignsBoothId();

        // when
        Long boothId = service.register(commandOf(null, DISPLAY_NAME));

        // then
        assertThat(boothId).isEqualTo(BOOTH_ID);
        then(saveDemodayBoothPort).should().save(boothCaptor.capture());
        assertThat(boothCaptor.getValue().getBoothCode()).isEqualTo(BOOTH_CODE);
        assertThat(boothCaptor.getValue().getProjectId()).isNull();
        assertThat(boothCaptor.getValue().getDisplayName()).isEqualTo(DISPLAY_NAME);
    }

    @Test
    @DisplayName("projectId와 displayName이 모두 담기면 등록 경로를 정할 수 없어 저장하지 않는다")
    void rejectCommandWithBothIdentifiers() {
        // given
        givenClosedPoll();

        // when & then
        assertThatThrownBy(() -> service.register(commandOf(PROJECT_ID, DISPLAY_NAME)))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_IDENTIFIER));

        then(saveDemodayBoothPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("projectId와 displayName이 모두 비면 저장하지 않는다")
    void rejectCommandWithoutIdentifier() {
        // given
        givenClosedPoll();

        // when & then
        assertThatThrownBy(() -> service.register(commandOf(null, null)))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_IDENTIFIER));

        then(saveDemodayBoothPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("공백만 담긴 displayName은 값이 없는 것으로 보고 저장하지 않는다")
    void rejectCommandWithBlankDisplayName() {
        // given
        givenClosedPoll();

        // when & then
        assertThatThrownBy(() -> service.register(commandOf(null, "   ")))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_IDENTIFIER));

        then(saveDemodayBoothPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("부스 코드가 없으면 저장하지 않는다")
    void rejectCommandWithoutBoothCode() {
        // given
        givenClosedPoll();

        // when & then
        assertThatThrownBy(() -> service.register(commandOf(null, PROJECT_ID, null)))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_CODE));

        then(saveDemodayBoothPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("투표가 OPEN이면 부스를 추가하지 않고 잠금 오류로 거부한다")
    void rejectRegistrationWhenPollIsOpen() {
        // given
        DemodayPoll poll = persistedPoll();
        poll.open();
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));

        // when & then
        assertThatThrownBy(() -> service.register(commandOf(PROJECT_ID, null)))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_POLL_BOOTH_LOCKED));

        then(saveDemodayBoothPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("투표가 존재하지 않으면 권한 검사와 저장을 수행하지 않는다")
    void rejectRegistrationWhenPollDoesNotExist() {
        // given
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.register(commandOf(PROJECT_ID, null)))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        then(adminAccessChecker).shouldHaveNoInteractions();
        then(saveDemodayBoothPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("관리자 권한이 없으면 부스를 저장하지 않는다")
    void rejectRegistrationWhenAdminAccessIsDenied() {
        // given
        givenClosedPoll();

        DemodayDomainException expectedException =
            new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED);

        willThrow(expectedException)
            .given(adminAccessChecker)
            .validateAdminAccess(MEMBER_ID, GISU_ID);

        // when & then
        assertThatThrownBy(() -> service.register(commandOf(PROJECT_ID, null)))
            .isSameAs(expectedException);

        then(saveDemodayBoothPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일괄 등록은 입력 순서와 대응하는 부스 ID 목록을 반환한다")
    void registerBoothsInBatch() {
        // given
        givenClosedPoll();
        givenSaveAllAssignsSequentialIds();

        // when
        List<Long> boothIds = service.registerAll(batchCommandOf(
            new BoothRegistration(BOOTH_CODE, PROJECT_ID, null),
            new BoothRegistration(SECOND_BOOTH_CODE, null, DISPLAY_NAME)));

        // then
        assertThat(boothIds).containsExactly(BOOTH_ID, BOOTH_ID + 1);

        then(adminAccessChecker).should().validateSystemAdminAccess(MEMBER_ID);
        then(saveDemodayBoothPort).should().saveAll(boothListCaptor.capture());

        assertThat(boothListCaptor.getValue())
            .extracting(DemodayBooth::getBoothCode, DemodayBooth::getProjectId, DemodayBooth::getDisplayName)
            .containsExactly(
                tuple(BOOTH_CODE, PROJECT_ID, null),
                tuple(SECOND_BOOTH_CODE, null, DISPLAY_NAME));
    }

    @Test
    @DisplayName("일괄 등록은 시스템 관리자가 아니면 투표를 조회하지도 않는다")
    void rejectBatchWhenNotSystemAdmin() {
        // given
        DemodayDomainException expectedException =
            new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED);

        willThrow(expectedException)
            .given(adminAccessChecker)
            .validateSystemAdminAccess(MEMBER_ID);

        // when & then
        assertThatThrownBy(() -> service.registerAll(
            batchCommandOf(new BoothRegistration(BOOTH_CODE, PROJECT_ID, null))))
            .isSameAs(expectedException);

        then(loadDemodayPollPort).shouldHaveNoInteractions();
        then(saveDemodayBoothPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일괄 등록은 한 항목이라도 등록 경로가 모호하면 전체를 저장하지 않는다")
    void rejectWholeBatchWhenAnyItemIsAmbiguous() {
        // given
        givenClosedPoll();

        // when & then
        assertThatThrownBy(() -> service.registerAll(
            batchCommandOf(
                new BoothRegistration(BOOTH_CODE, PROJECT_ID, null),
                new BoothRegistration(SECOND_BOOTH_CODE, PROJECT_ID, DISPLAY_NAME))))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_IDENTIFIER));

        then(saveDemodayBoothPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일괄 등록은 한 항목이라도 부스 코드가 양수가 아니면 전체를 저장하지 않는다")
    void rejectWholeBatchWhenAnyBoothCodeIsInvalid() {
        // given
        givenClosedPoll();

        // when & then
        assertThatThrownBy(() -> service.registerAll(
            batchCommandOf(
                new BoothRegistration(BOOTH_CODE, PROJECT_ID, null),
                new BoothRegistration(0, null, DISPLAY_NAME))))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_CODE));

        then(saveDemodayBoothPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일괄 등록도 투표가 OPEN이면 잠금 오류로 거부한다")
    void rejectBatchWhenPollIsOpen() {
        // given
        DemodayPoll poll = persistedPoll();
        poll.open();
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));

        // when & then
        assertThatThrownBy(() -> service.registerAll(
            batchCommandOf(new BoothRegistration(BOOTH_CODE, PROJECT_ID, null))))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_POLL_BOOTH_LOCKED));
        then(saveDemodayBoothPort).shouldHaveNoInteractions();
    }

    private void givenClosedPoll() {
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(persistedPoll()));
    }

    private void givenSaveAssignsBoothId() {
        given(saveDemodayBoothPort.save(any(DemodayBooth.class))).willAnswer(invocation -> {
            DemodayBooth booth = invocation.getArgument(0);
            ReflectionTestUtils.setField(booth, "id", BOOTH_ID);
            return booth;
        });
    }

    @SuppressWarnings("unchecked")
    private void givenSaveAllAssignsSequentialIds() {
        given(saveDemodayBoothPort.saveAll(any(List.class))).willAnswer(invocation -> {
            List<DemodayBooth> booths = invocation.getArgument(0);
            long nextId = BOOTH_ID;
            for (DemodayBooth booth : booths) {
                ReflectionTestUtils.setField(booth, "id", nextId++);
            }
            return booths;
        });
    }

    private RegisterDemodayBoothBatchCommand batchCommandOf(BoothRegistration... registrations) {
        return new RegisterDemodayBoothBatchCommand(MEMBER_ID, POLL_ID, List.of(registrations));
    }

    private DemodayPoll persistedPoll() {
        DemodayPoll poll = DemodayPoll.create(GISU_ID, POLL_NAME, OPENS_AT, CLOSES_AT);
        ReflectionTestUtils.setField(poll, "id", POLL_ID);
        return poll;
    }

    private RegisterDemodayBoothCommand commandOf(Long projectId, String displayName) {
        return commandOf(BOOTH_CODE, projectId, displayName);
    }

    private RegisterDemodayBoothCommand commandOf(
        Integer boothCode,
        Long projectId,
        String displayName
    ) {
        return new RegisterDemodayBoothCommand(MEMBER_ID, POLL_ID, boothCode, projectId, displayName);
    }
}
