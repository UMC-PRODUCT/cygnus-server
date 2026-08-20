package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.demoday.application.port.in.command.dto.CollectDemodayStampCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayStampCollectInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.GuestDemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.MemberDemodayParticipant;
import com.umc.product.demoday.application.port.out.HashDemodayStampCredentialPort;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.port.out.SaveDemodayStampPort;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
class CollectDemodayStampCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long ENTRY_CODE_ID = 2L;
    private static final Long POLL_ID = 10L;
    private static final Long BOOTH_ID = 20L;
    private static final String QR_CREDENTIAL = "sq_opaque_credential";
    private static final String CREDENTIAL_HASH = "credential-hash";
    private static final Instant NOW = Instant.parse("2026-08-19T10:00:00Z");

    @Mock
    private LoadDemodayBoothPort loadDemodayBoothPort;

    @Mock
    private LoadDemodayEntryCodePort loadDemodayEntryCodePort;

    @Mock
    private LoadDemodayPollPort loadDemodayPollPort;

    @Mock
    private LoadDemodayStampPort loadDemodayStampPort;

    @Mock
    private LoadDemodayVotePort loadDemodayVotePort;

    @Mock
    private SaveDemodayStampPort saveDemodayStampPort;

    @Mock
    private HashDemodayStampCredentialPort hashDemodayStampCredentialPort;

    @Mock
    private Clock clock;

    @InjectMocks
    private CollectDemodayStampCommandService collectDemodayStampCommandService;

    @Test
    @DisplayName("처음 스캔한 부스는 새 스탬프를 적립한다")
    void collectNewStamp() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        DemodayBooth booth = boothOf(POLL_ID);
        CollectDemodayStampCommand command = new CollectDemodayStampCommand(POLL_ID, QR_CREDENTIAL, participant);

        given(hashDemodayStampCredentialPort.hash(QR_CREDENTIAL)).willReturn(CREDENTIAL_HASH);
        given(loadDemodayBoothPort.findByStampCredentialHash(CREDENTIAL_HASH)).willReturn(Optional.of(booth));
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(loadDemodayStampPort.findMemberStamp(MEMBER_ID, BOOTH_ID)).willReturn(Optional.empty());
        given(loadDemodayStampPort.countActiveMemberStamps(POLL_ID, MEMBER_ID)).willReturn(0, 1);
        given(loadDemodayStampPort.findLatestActiveMemberStamp(MEMBER_ID)).willReturn(Optional.empty());
        given(clock.instant()).willReturn(NOW);
        given(saveDemodayStampPort.save(any(DemodayStamp.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        DemodayStampCollectInfo result = collectDemodayStampCommandService.collect(command);

        // then
        assertThat(result.stamp().boothId()).isEqualTo(BOOTH_ID);
        assertThat(result.stampCount()).isEqualTo(1);
        assertThat(result.requiredStampCount()).isEqualTo(6);
        assertThat(result.nextStampAvailableAt()).isNull();
        assertThat(result.canRequestVoteAuthorization()).isFalse();

        then(saveDemodayStampPort).should().save(any(DemodayStamp.class));
    }

    @Test
    @DisplayName("같은 부스를 재스캔하면 새 스탬프를 만들지 않고 현재 상태를 그대로 응답한다")
    void rescanReturnsCurrentStateWithoutCreatingNewStamp() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        DemodayBooth booth = boothOf(POLL_ID);
        DemodayStamp existingStamp = stampWithCreatedAt(
            DemodayStamp.forMember(MEMBER_ID, booth), NOW.minusSeconds(600));
        CollectDemodayStampCommand command = new CollectDemodayStampCommand(POLL_ID, QR_CREDENTIAL, participant);

        given(hashDemodayStampCredentialPort.hash(QR_CREDENTIAL)).willReturn(CREDENTIAL_HASH);
        given(loadDemodayBoothPort.findByStampCredentialHash(CREDENTIAL_HASH)).willReturn(Optional.of(booth));
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(loadDemodayStampPort.findMemberStamp(MEMBER_ID, BOOTH_ID)).willReturn(Optional.of(existingStamp));
        given(loadDemodayStampPort.countActiveMemberStamps(POLL_ID, MEMBER_ID)).willReturn(3);
        given(loadDemodayStampPort.findLatestActiveMemberStamp(MEMBER_ID)).willReturn(Optional.of(existingStamp));
        given(clock.instant()).willReturn(NOW);

        // when
        DemodayStampCollectInfo result = collectDemodayStampCommandService.collect(command);

        // then
        assertThat(result.stampCount()).isEqualTo(3);
        then(saveDemodayStampPort).should(never()).save(any(DemodayStamp.class));
    }

    @Test
    @DisplayName("유효하지 않은 QR credential은 실패한다")
    void throwExceptionWhenCredentialInvalid() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        CollectDemodayStampCommand command = new CollectDemodayStampCommand(POLL_ID, QR_CREDENTIAL, participant);

        given(hashDemodayStampCredentialPort.hash(QR_CREDENTIAL)).willReturn(CREDENTIAL_HASH);
        given(loadDemodayBoothPort.findByStampCredentialHash(CREDENTIAL_HASH)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> collectDemodayStampCommandService.collect(command))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_STAMP_CREDENTIAL_INVALID));

        then(loadDemodayPollPort).shouldHaveNoInteractions();
        then(saveDemodayStampPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다른 투표 행사의 부스 QR이면 실패한다")
    void throwExceptionWhenPollMismatch() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        DemodayBooth booth = boothOf(99L);
        CollectDemodayStampCommand command = new CollectDemodayStampCommand(POLL_ID, QR_CREDENTIAL, participant);

        given(hashDemodayStampCredentialPort.hash(QR_CREDENTIAL)).willReturn(CREDENTIAL_HASH);
        given(loadDemodayBoothPort.findByStampCredentialHash(CREDENTIAL_HASH)).willReturn(Optional.of(booth));

        // when & then
        assertThatThrownBy(() -> collectDemodayStampCommandService.collect(command))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_STAMP_POLL_MISMATCH));

        then(loadDemodayPollPort).shouldHaveNoInteractions();
        then(loadDemodayStampPort).shouldHaveNoInteractions();
        then(saveDemodayStampPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("아직 열리지 않은 데모데이의 부스 QR이면 실패한다")
    void throwExceptionWhenPollNotOpenYet() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        DemodayBooth booth = boothOf(POLL_ID);
        DemodayPoll readyPoll = DemodayPoll.create(9L, "테스트 데모데이", NOW.minusSeconds(3600), NOW.plusSeconds(3600));
        CollectDemodayStampCommand command = new CollectDemodayStampCommand(POLL_ID, QR_CREDENTIAL, participant);

        given(hashDemodayStampCredentialPort.hash(QR_CREDENTIAL)).willReturn(CREDENTIAL_HASH);
        given(loadDemodayBoothPort.findByStampCredentialHash(CREDENTIAL_HASH)).willReturn(Optional.of(booth));
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(readyPoll));
        given(clock.instant()).willReturn(NOW);

        // when & then
        assertThatThrownBy(() -> collectDemodayStampCommandService.collect(command))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        then(loadDemodayStampPort).shouldHaveNoInteractions();
        then(saveDemodayStampPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이미 6개를 적립했으면 새 스탬프 적립이 거절된다")
    void throwExceptionWhenMaxCountReached() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        DemodayBooth booth = boothOf(POLL_ID);
        CollectDemodayStampCommand command = new CollectDemodayStampCommand(POLL_ID, QR_CREDENTIAL, participant);

        given(hashDemodayStampCredentialPort.hash(QR_CREDENTIAL)).willReturn(CREDENTIAL_HASH);
        given(loadDemodayBoothPort.findByStampCredentialHash(CREDENTIAL_HASH)).willReturn(Optional.of(booth));
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(loadDemodayStampPort.findMemberStamp(MEMBER_ID, BOOTH_ID)).willReturn(Optional.empty());
        given(loadDemodayStampPort.countActiveMemberStamps(POLL_ID, MEMBER_ID)).willReturn(6);
        given(clock.instant()).willReturn(NOW);

        // when & then
        assertThatThrownBy(() -> collectDemodayStampCommandService.collect(command))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_STAMP_MAX_COUNT_REACHED));

        then(saveDemodayStampPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("직전 스탬프 이후 5분이 지나지 않았으면 다음 스탬프 적립이 거절된다")
    void throwExceptionWhenCooldownNotElapsed() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        DemodayBooth booth = boothOf(POLL_ID);
        DemodayBooth previousBooth = boothOf(POLL_ID);
        DemodayStamp latestStamp = stampWithCreatedAt(
            DemodayStamp.forMember(MEMBER_ID, previousBooth), NOW.minusSeconds(60));
        CollectDemodayStampCommand command = new CollectDemodayStampCommand(POLL_ID, QR_CREDENTIAL, participant);

        given(hashDemodayStampCredentialPort.hash(QR_CREDENTIAL)).willReturn(CREDENTIAL_HASH);
        given(loadDemodayBoothPort.findByStampCredentialHash(CREDENTIAL_HASH)).willReturn(Optional.of(booth));
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(loadDemodayStampPort.findMemberStamp(MEMBER_ID, BOOTH_ID)).willReturn(Optional.empty());
        given(loadDemodayStampPort.countActiveMemberStamps(POLL_ID, MEMBER_ID)).willReturn(1);
        given(loadDemodayStampPort.findLatestActiveMemberStamp(MEMBER_ID)).willReturn(Optional.of(latestStamp));
        given(clock.instant()).willReturn(NOW);

        // when & then
        assertThatThrownBy(() -> collectDemodayStampCommandService.collect(command))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_STAMP_COOLDOWN_ACTIVE));

        then(saveDemodayStampPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("경합으로 저장이 중복 제약에 걸리면 예외 대신 현재 상태를 반환한다")
    void fallsBackToCurrentStateWhenSaveLosesRace() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        DemodayBooth booth = boothOf(POLL_ID);
        DemodayStamp winnerStamp = stampWithCreatedAt(DemodayStamp.forMember(MEMBER_ID, booth), NOW);
        CollectDemodayStampCommand command = new CollectDemodayStampCommand(POLL_ID, QR_CREDENTIAL, participant);

        given(hashDemodayStampCredentialPort.hash(QR_CREDENTIAL)).willReturn(CREDENTIAL_HASH);
        given(loadDemodayBoothPort.findByStampCredentialHash(CREDENTIAL_HASH)).willReturn(Optional.of(booth));
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(loadDemodayStampPort.findMemberStamp(MEMBER_ID, BOOTH_ID))
            .willReturn(Optional.empty(), Optional.of(winnerStamp));
        given(loadDemodayStampPort.countActiveMemberStamps(POLL_ID, MEMBER_ID)).willReturn(0, 1);
        given(loadDemodayStampPort.findLatestActiveMemberStamp(MEMBER_ID)).willReturn(Optional.empty());
        given(clock.instant()).willReturn(NOW);
        given(saveDemodayStampPort.save(any(DemodayStamp.class)))
            .willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_STAMP_ALREADY_COLLECTED));

        // when
        DemodayStampCollectInfo result = collectDemodayStampCommandService.collect(command);

        // then
        assertThat(result.stamp().boothId()).isEqualTo(BOOTH_ID);
        assertThat(result.stampCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("외부 방문자는 입장 코드 엔티티를 로드해 스탬프를 적립한다")
    void collectStampForGuestParticipant() {
        // given
        DemodayParticipant participant = new GuestDemodayParticipant(ENTRY_CODE_ID);
        DemodayBooth booth = boothOf(POLL_ID);
        DemodayEntryCode entryCode = entryCodeOf(POLL_ID);
        CollectDemodayStampCommand command = new CollectDemodayStampCommand(POLL_ID, QR_CREDENTIAL, participant);

        given(hashDemodayStampCredentialPort.hash(QR_CREDENTIAL)).willReturn(CREDENTIAL_HASH);
        given(loadDemodayBoothPort.findByStampCredentialHash(CREDENTIAL_HASH)).willReturn(Optional.of(booth));
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(loadDemodayStampPort.findVisitorStamp(ENTRY_CODE_ID, BOOTH_ID)).willReturn(Optional.empty());
        given(loadDemodayStampPort.countActiveVisitorStamps(POLL_ID, ENTRY_CODE_ID)).willReturn(0, 1);
        given(loadDemodayStampPort.findLatestActiveVisitorStamp(ENTRY_CODE_ID)).willReturn(Optional.empty());
        given(loadDemodayEntryCodePort.findById(ENTRY_CODE_ID)).willReturn(Optional.of(entryCode));
        given(clock.instant()).willReturn(NOW);
        given(saveDemodayStampPort.save(any(DemodayStamp.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        DemodayStampCollectInfo result = collectDemodayStampCommandService.collect(command);

        // then
        assertThat(result.stamp().boothId()).isEqualTo(BOOTH_ID);
        then(loadDemodayEntryCodePort).should().findById(ENTRY_CODE_ID);
    }

    private static DemodayPoll openPoll() {
        DemodayPoll poll = DemodayPoll.create(9L, "테스트 데모데이", NOW.minusSeconds(3600), NOW.plusSeconds(3600));
        poll.open();
        return poll;
    }

    private static DemodayBooth boothOf(Long pollId) {
        DemodayBooth booth = DemodayBooth.forExternal(pollId, 11, "테스트 부스");
        ReflectionTestUtils.setField(booth, "id", BOOTH_ID);
        return booth;
    }

    private static DemodayEntryCode entryCodeOf(Long pollId) {
        DemodayEntryCode entryCode = DemodayEntryCode.create(pollId, "code-hash");
        ReflectionTestUtils.setField(entryCode, "id", ENTRY_CODE_ID);
        return entryCode;
    }

    private static DemodayStamp stampWithCreatedAt(DemodayStamp stamp, Instant createdAt) {
        ReflectionTestUtils.setField(stamp, "createdAt", createdAt);
        return stamp;
    }
}
