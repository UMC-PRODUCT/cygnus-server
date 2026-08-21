package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayEntryCodeCommand;
import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayEntryCodesInfo;
import com.umc.product.demoday.application.port.out.GenerateDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.HashDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayEntryCodePort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;

@ExtendWith(MockitoExtension.class)
class CreateDemodayEntryCodeCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long POLL_ID = 1L;
    private static final Long GISU_ID = 8L;
    private static final String POLL_NAME = "8기 데모데이 투표";
    private static final Instant FUTURE_OPENS_AT = Instant.parse("2100-08-15T09:00:00Z");
    private static final Instant FUTURE_CLOSES_AT = Instant.parse("2100-08-15T12:00:00Z");
    private static final Instant PAST_OPENS_AT = Instant.parse("2000-08-15T09:00:00Z");
    private static final Instant PAST_CLOSES_AT = Instant.parse("2000-08-15T12:00:00Z");

    @Mock
    private LoadDemodayPollPort loadDemodayPollPort;

    @Mock
    private SaveDemodayEntryCodePort saveDemodayEntryCodePort;

    @Mock
    private DemodayAdminAccessChecker demodayAdminAccessChecker;

    @Mock
    private GenerateDemodayEntryCodePort generateDemodayEntryCodePort;

    @Mock
    private HashDemodayEntryCodePort hashDemodayEntryCodePort;

    @InjectMocks
    private CreateDemodayEntryCodeCommandService createDemodayEntryCodeCommandService;

    @Test
    @DisplayName("준비 상태의 투표 행사에서 여러 인증 코드를 발급한다")
    void createEntryCodesWhenPollIsReady() {
        // given
        DemodayPoll demodayPoll = createPoll(FUTURE_OPENS_AT, FUTURE_CLOSES_AT);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(demodayPoll));
        given(generateDemodayEntryCodePort.generate()).willReturn("CODE-A", "CODE-B");
        given(hashDemodayEntryCodePort.hash("CODE-A")).willReturn("hash-A");
        given(hashDemodayEntryCodePort.hash("CODE-B")).willReturn("hash-B");

        // when
        CreateDemodayEntryCodesInfo result = createDemodayEntryCodeCommandService.create(
            MEMBER_ID,
            new CreateDemodayEntryCodeCommand(POLL_ID, 2)
        );

        // then
        assertThat(result.codes()).containsExactly("CODE-A", "CODE-B");
        then(demodayAdminAccessChecker).should().validateAdminAccess(MEMBER_ID, GISU_ID);
        then(saveDemodayEntryCodePort).should()
            .saveAll(argThat(entryCodes -> hasEntryCodes(entryCodes, List.of("hash-A", "hash-B"))));
    }

    @Test
    @DisplayName("진행 중인 투표 행사에서 인증 코드를 추가로 발급한다")
    void createEntryCodesWhenPollIsOpen() {
        // given
        DemodayPoll demodayPoll = createPoll(FUTURE_OPENS_AT, FUTURE_CLOSES_AT);
        demodayPoll.open();
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(demodayPoll));
        given(generateDemodayEntryCodePort.generate()).willReturn("CODE-C");
        given(hashDemodayEntryCodePort.hash("CODE-C")).willReturn("hash-C");

        // when
        CreateDemodayEntryCodesInfo result = createDemodayEntryCodeCommandService.create(
            MEMBER_ID,
            new CreateDemodayEntryCodeCommand(POLL_ID, 1)
        );

        // then
        assertThat(result.codes()).containsExactly("CODE-C");
        then(demodayAdminAccessChecker).should().validateAdminAccess(MEMBER_ID, GISU_ID);
        then(saveDemodayEntryCodePort).should()
            .saveAll(argThat(entryCodes -> hasEntryCodes(entryCodes, List.of("hash-C"))));
    }

    @Test
    @DisplayName("종료 상태의 투표 행사에서는 인증 코드 발급을 거부한다")
    void rejectEntryCodeCreationWhenPollIsClosed() {
        // given
        DemodayPoll demodayPoll = createPoll(FUTURE_OPENS_AT, FUTURE_CLOSES_AT);
        demodayPoll.open();
        demodayPoll.close();
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(demodayPoll));

        // when & then
        assertThatThrownBy(() -> createDemodayEntryCodeCommandService.create(
            MEMBER_ID, new CreateDemodayEntryCodeCommand(POLL_ID, 1)))
            .isInstanceOf(DemodayDomainException.class);

        then(demodayAdminAccessChecker).shouldHaveNoInteractions();
        then(generateDemodayEntryCodePort).shouldHaveNoInteractions();
        then(hashDemodayEntryCodePort).shouldHaveNoInteractions();
        then(saveDemodayEntryCodePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("마감 시각이 지난 투표 행사에서는 인증 코드 발급을 거부한다")
    void rejectEntryCodeCreationWhenPollIsExpired() {
        // given
        DemodayPoll demodayPoll = createPoll(PAST_OPENS_AT, PAST_CLOSES_AT);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(demodayPoll));

        // when & then
        assertThatThrownBy(() -> createDemodayEntryCodeCommandService.create(
            MEMBER_ID, new CreateDemodayEntryCodeCommand(POLL_ID, 1)))
            .isInstanceOf(DemodayDomainException.class);

        then(demodayAdminAccessChecker).shouldHaveNoInteractions();
        then(generateDemodayEntryCodePort).shouldHaveNoInteractions();
        then(hashDemodayEntryCodePort).shouldHaveNoInteractions();
        then(saveDemodayEntryCodePort).shouldHaveNoInteractions();
    }

    private static DemodayPoll createPoll(Instant opensAt, Instant closesAt) {
        return DemodayPoll.create(GISU_ID, POLL_NAME, opensAt, closesAt);
    }

    private static boolean hasEntryCodes(List<DemodayEntryCode> entryCodes, List<String> expectedCodeHashes) {
        return entryCodes.size() == expectedCodeHashes.size()
            && entryCodes.stream().allMatch(entryCode -> entryCode.getPollId().equals(POLL_ID))
            && entryCodes.stream()
                .map(DemodayEntryCode::getCodeHash)
                .toList()
                .equals(expectedCodeHashes);
    }
}
