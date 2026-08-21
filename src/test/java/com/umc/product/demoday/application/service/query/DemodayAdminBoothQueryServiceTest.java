package com.umc.product.demoday.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminBoothListInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.enums.DemodayPollStatus;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemodayAdminBoothQueryService")
class DemodayAdminBoothQueryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long POLL_ID = 10L;
    private static final Long GISU_ID = 8L;
    private static final String POLL_NAME = "8기 데모데이 투표";
    private static final Instant OPENS_AT = Instant.parse("2026-08-15T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-08-15T12:00:00Z");

    @Mock
    private DemodayAdminAccessChecker adminAccessChecker;

    @Mock
    private LoadDemodayPollPort loadDemodayPollPort;

    @Mock
    private LoadDemodayBoothPort loadDemodayBoothPort;

    @InjectMocks
    private DemodayAdminBoothQueryService service;

    @Test
    @DisplayName("닫힌 투표의 부스 목록과 등록 개수를 조회하고 아직 추가할 수 있다고 알린다")
    void listBoothsOfClosedPoll() {
        // given
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(persistedClosedPoll()));
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(List.of(
            booth(20L, 11, 101L, null),
            booth(21L, 12, null, "외부 참가팀 A")
        ));

        // when
        DemodayAdminBoothListInfo info = service.listBooths(POLL_ID, MEMBER_ID);

        // then
        assertThat(info.pollId()).isEqualTo(POLL_ID);
        assertThat(info.pollStatus()).isEqualTo(DemodayPollStatus.CLOSED);
        assertThat(info.boothAddable()).isTrue();
        assertThat(info.boothCount()).isEqualTo(2);
        assertThat(info.booths())
            .extracting(
                DemodayBoothInfo::boothId,
                DemodayBoothInfo::boothCode,
                DemodayBoothInfo::projectId,
                DemodayBoothInfo::displayName)
            .containsExactly(
                tuple(20L, 11, 101L, null),
                tuple(21L, 12, null, "외부 참가팀 A"));
        then(adminAccessChecker).should().validateAdminAccess(MEMBER_ID, GISU_ID);
    }

    @Test
    @DisplayName("열린 투표는 부스를 더 추가할 수 없다고 알린다")
    void reportBoothLockedForOpenPoll() {
        // given
        DemodayPoll poll = persistedPoll();
        poll.open();
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(List.of());

        // when
        DemodayAdminBoothListInfo info = service.listBooths(POLL_ID, MEMBER_ID);

        // then
        assertThat(info.pollStatus()).isEqualTo(DemodayPollStatus.OPEN);
        assertThat(info.boothAddable()).isFalse();
        assertThat(info.boothCount()).isZero();
        assertThat(info.booths()).isEmpty();
    }

    @Test
    @DisplayName("투표가 존재하지 않으면 권한 검사와 부스 조회를 수행하지 않는다")
    void rejectListWhenPollDoesNotExist() {
        // given
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.listBooths(POLL_ID, MEMBER_ID))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));
        then(adminAccessChecker).shouldHaveNoInteractions();
        then(loadDemodayBoothPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("관리자 권한이 없으면 부스를 조회하지 않는다")
    void rejectListWhenAdminAccessIsDenied() {
        // given
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(persistedPoll()));
        DemodayDomainException expectedException =
            new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED);
        willThrow(expectedException)
            .given(adminAccessChecker)
            .validateAdminAccess(MEMBER_ID, GISU_ID);

        // when & then
        assertThatThrownBy(() -> service.listBooths(POLL_ID, MEMBER_ID)).isSameAs(expectedException);
        then(loadDemodayBoothPort).shouldHaveNoInteractions();
    }

    private DemodayPoll persistedPoll() {
        DemodayPoll poll = DemodayPoll.create(GISU_ID, POLL_NAME, OPENS_AT, CLOSES_AT);
        ReflectionTestUtils.setField(poll, "id", POLL_ID);
        return poll;
    }

    private DemodayPoll persistedClosedPoll() {
        DemodayPoll poll = persistedPoll();
        poll.open();
        poll.close();
        return poll;
    }

    private DemodayBooth booth(
        Long boothId,
        Integer boothCode,
        Long projectId,
        String displayName
    ) {
        DemodayBooth booth = projectId != null
            ? DemodayBooth.forProject(POLL_ID, boothCode, projectId)
            : DemodayBooth.forExternal(POLL_ID, boothCode, displayName);
        ReflectionTestUtils.setField(booth, "id", boothId);
        return booth;
    }
}
