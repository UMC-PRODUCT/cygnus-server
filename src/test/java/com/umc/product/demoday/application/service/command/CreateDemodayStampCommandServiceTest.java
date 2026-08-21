package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.demoday.application.port.in.command.dto.CreateStampCredentialCommand;
import com.umc.product.demoday.application.port.out.EncryptDemodayStampCredentialPort;
import com.umc.product.demoday.application.port.out.GenerateDemodayStampCredentialPort;
import com.umc.product.demoday.application.port.out.HashDemodayStampCredentialPort;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.config.DemodayQrProperties;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;

@ExtendWith(MockitoExtension.class)
class CreateDemodayStampCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long POLL_ID = 2L;
    private static final Long BOOTH_ID = 3L;
    private static final Long GISU_ID = 4L;
    private static final String CREDENTIAL = "stamp-credential";
    private static final String CREDENTIAL_HASH = "credential-hash";
    private static final String ENCRYPTED_CREDENTIAL = "encrypted-credential";
    private static final String QR_BASE_URL = "https://vote.example.com";
    private static final Instant GENERATED_AT = Instant.parse("2026-08-17T00:00:00Z");

    @Mock
    private DemodayAdminAccessChecker adminAccessChecker;

    @Mock
    private LoadDemodayBoothPort loadDemodayBoothPort;

    @Mock
    private LoadDemodayPollPort loadDemodayPollPort;

    @Mock
    private EncryptDemodayStampCredentialPort encryptDemodayStampCredentialPort;

    @Mock
    private GenerateDemodayStampCredentialPort generateDemodayStampCredentialPort;

    @Mock
    private HashDemodayStampCredentialPort hashDemodayStampCredentialPort;

    @Mock
    private DemodayQrProperties demodayQrProperties;

    @Mock
    private Clock clock;

    @InjectMocks
    private CreateDemodayStampCommandService createDemodayStampCommandService;

    @Test
    @DisplayName("관리자가 부스의 스탬프 QR을 발급하면 QR 값과 발급 정보를 일관되게 저장하고 반환한다")
    void createStampCredential() {
        // given
        DemodayBooth demodayBooth = DemodayBooth.forExternal(POLL_ID, 11, "UMC PRODUCT");
        DemodayPoll demodayPoll = DemodayPoll.create(
            GISU_ID,
            "데모데이",
            GENERATED_AT,
            GENERATED_AT.plusSeconds(3600)
        );
        CreateStampCredentialCommand command = new CreateStampCredentialCommand(POLL_ID, BOOTH_ID);

        given(loadDemodayBoothPort.findById(BOOTH_ID)).willReturn(Optional.of(demodayBooth));
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(demodayPoll));
        given(generateDemodayStampCredentialPort.generate()).willReturn(CREDENTIAL);
        given(hashDemodayStampCredentialPort.hash(CREDENTIAL)).willReturn(CREDENTIAL_HASH);
        given(encryptDemodayStampCredentialPort.encrypt(CREDENTIAL)).willReturn(ENCRYPTED_CREDENTIAL);
        given(clock.instant()).willReturn(GENERATED_AT);
        given(demodayQrProperties.baseUrl()).willReturn(QR_BASE_URL);

        // when
        var result = createDemodayStampCommandService.create(MEMBER_ID, command);

        // then
        assertThat(result.qrValue()).isEqualTo(
            "%s/demoday/polls/%d/stamp#credential=%s".formatted(QR_BASE_URL, POLL_ID, CREDENTIAL)
        );
        assertThat(result.generatedAt()).isEqualTo(GENERATED_AT);
        assertThat(demodayBooth.getStampCredentialHash()).isEqualTo(CREDENTIAL_HASH);
        assertThat(demodayBooth.getStampCredentialCipher()).isEqualTo(ENCRYPTED_CREDENTIAL);
        assertThat(demodayBooth.getStampCredentialGeneratedAt()).isEqualTo(GENERATED_AT);

        then(adminAccessChecker).should().validateAdminAccess(MEMBER_ID, GISU_ID);
        then(generateDemodayStampCredentialPort).should().generate();
        then(hashDemodayStampCredentialPort).should().hash(CREDENTIAL);
        then(encryptDemodayStampCredentialPort).should().encrypt(CREDENTIAL);
        then(clock).should().instant();
        then(demodayQrProperties).should().baseUrl();
    }

    @Test
    @DisplayName("존재하지 않는 부스의 스탬프 QR 발급 요청은 실패한다")
    void throwExceptionWhenBoothDoesNotExist() {
        // given
        CreateStampCredentialCommand command = new CreateStampCredentialCommand(POLL_ID, BOOTH_ID);
        given(loadDemodayBoothPort.findById(BOOTH_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> createDemodayStampCommandService.create(MEMBER_ID, command))
                .isInstanceOf(DemodayDomainException.class);

        then(adminAccessChecker).shouldHaveNoInteractions();
        then(loadDemodayPollPort).shouldHaveNoInteractions();
        then(generateDemodayStampCredentialPort).shouldHaveNoInteractions();
        then(hashDemodayStampCredentialPort).shouldHaveNoInteractions();
        then(encryptDemodayStampCredentialPort).shouldHaveNoInteractions();
        then(demodayQrProperties).shouldHaveNoInteractions();
        then(clock).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다른 투표 행사에 속한 부스의 스탬프 QR 발급 요청은 실패한다")
    void throwExceptionWhenBoothDoesNotBelongToPoll() {
        // given
        Long anotherPollId = 4L;
        DemodayBooth demodayBooth = DemodayBooth.forExternal(anotherPollId, 11, "UMC PRODUCT");
        CreateStampCredentialCommand command = new CreateStampCredentialCommand(POLL_ID, BOOTH_ID);
        given(loadDemodayBoothPort.findById(BOOTH_ID)).willReturn(Optional.of(demodayBooth));

        // when & then
        assertThatThrownBy(() -> createDemodayStampCommandService.create(MEMBER_ID, command))
                .isInstanceOf(DemodayDomainException.class);

        then(adminAccessChecker).shouldHaveNoInteractions();
        then(loadDemodayPollPort).shouldHaveNoInteractions();
        then(generateDemodayStampCredentialPort).shouldHaveNoInteractions();
        then(hashDemodayStampCredentialPort).shouldHaveNoInteractions();
        then(encryptDemodayStampCredentialPort).shouldHaveNoInteractions();
        then(demodayQrProperties).shouldHaveNoInteractions();
        then(clock).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 데모데이 투표의 스탬프 QR 발급 요청은 실패한다")
    void throwExceptionWhenPollDoesNotExist() {
        // given
        DemodayBooth demodayBooth = DemodayBooth.forExternal(POLL_ID, 11, "팀 리버");
        CreateStampCredentialCommand command = new CreateStampCredentialCommand(POLL_ID, BOOTH_ID);
        given(loadDemodayBoothPort.findById(BOOTH_ID)).willReturn(Optional.of(demodayBooth));
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> createDemodayStampCommandService.create(MEMBER_ID, command))
                .isInstanceOf(DemodayDomainException.class);

        then(adminAccessChecker).shouldHaveNoInteractions();
        then(generateDemodayStampCredentialPort).shouldHaveNoInteractions();
        then(hashDemodayStampCredentialPort).shouldHaveNoInteractions();
        then(encryptDemodayStampCredentialPort).shouldHaveNoInteractions();
        then(demodayQrProperties).shouldHaveNoInteractions();
        then(clock).shouldHaveNoInteractions();
    }
}
