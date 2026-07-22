package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.member.application.port.in.command.RegisterEmailMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.EmailRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.TermConsents;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.test.application.port.in.command.dto.CreateSeedMemberCommand;
import com.umc.product.test.application.port.in.command.dto.SeedMembersCommand;
import com.umc.product.test.application.port.in.command.dto.SeedMembersResult;

@ExtendWith(MockitoExtension.class)
class MemberSeedServiceTest {

    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    RegisterEmailMemberUseCase registerEmailMemberUseCase;
    @Mock
    DummyMemberFactory dummyMemberFactory;

    MemberSeedService sut;
    SeedProperties properties;
    List<TermConsents> consents;

    @BeforeEach
    void setUp() {
        consents = List.of();
        properties = new SeedProperties(
            true,
            0L,
            "alpha.umc.test",
            "Alpha!Pass2026"
        );
        sut = new MemberSeedService(
            properties,
            dummyMemberFactory,
            getMemberUseCase,
            registerEmailMemberUseCase
        );
        lenient().when(dummyMemberFactory.snapshotMandatoryConsents()).thenReturn(consents);
    }

    @Test
    @DisplayName("force=false 이고 회원 수가 임계값 초과면 시딩을 스킵한다")
    void 멱등성_초과시_스킵() {
        // Given
        given(getMemberUseCase.countAll()).willReturn(10L);

        // When
        SeedMembersResult result = sut.seed(new SeedMembersCommand(5, false));

        // Then
        assertThat(result.skipped()).isTrue();
        assertThat(result.reason()).contains("threshold");
        verify(registerEmailMemberUseCase, never()).batchRegister(any());
    }

    @Test
    @DisplayName("force=true 면 임계값 체크를 무시하고 시딩한다")
    void force_true_면_임계값_무시() {
        // Given
        given(getMemberUseCase.countAll()).willReturn(100L);
        given(dummyMemberFactory.nextEmailCommand(anyLong(), any())).willReturn(mock(EmailRegisterMemberCommand.class));
        given(registerEmailMemberUseCase.batchRegister(any())).willReturn(List.of(1L, 2L));

        // When
        SeedMembersResult result = sut.seed(new SeedMembersCommand(2, true));

        // Then
        assertThat(result.skipped()).isFalse();
        assertThat(result.registered()).isEqualTo(2);
    }

    @Test
    @DisplayName("정상 시딩 시 email batchRegister 가 1번 호출된다")
    void 정상_시딩_호출_횟수() {
        // Given
        given(getMemberUseCase.countAll()).willReturn(0L);
        given(dummyMemberFactory.nextEmailCommand(anyLong(), any())).willReturn(mock(EmailRegisterMemberCommand.class));
        given(registerEmailMemberUseCase.batchRegister(any())).willReturn(List.of(1L, 2L, 3L, 4L, 5L));

        // When
        sut.seed(new SeedMembersCommand(5, false));

        // Then
        verify(registerEmailMemberUseCase, times(1)).batchRegister(any());
    }

    @Test
    @DisplayName("email batchRegister 실패 시 등록 수는 0으로 반환된다")
    void email_batch_실패_시_0_반환() {
        // Given
        given(getMemberUseCase.countAll()).willReturn(0L);
        given(dummyMemberFactory.nextEmailCommand(anyLong(), any())).willReturn(mock(EmailRegisterMemberCommand.class));
        given(registerEmailMemberUseCase.batchRegister(any()))
            .willThrow(new RuntimeException("boom"));

        // When
        SeedMembersResult result = sut.seed(new SeedMembersCommand(3, false));

        // Then
        verify(registerEmailMemberUseCase, times(1)).batchRegister(any());
        assertThat(result.registered()).isZero();
    }

    @Test
    @DisplayName("count=0 이면 register 가 호출되지 않는다")
    void count_0_미호출() {
        // Given
        given(getMemberUseCase.countAll()).willReturn(0L);

        // When
        SeedMembersResult result = sut.seed(new SeedMembersCommand(0, false));

        // Then
        assertThat(result.skipped()).isFalse();
        assertThat(result.registered()).isZero();
        verify(registerEmailMemberUseCase, never()).batchRegister(any());
    }

    @Test
    @DisplayName("email 시퀀스는 현재 회원 수 + 1 부터 시작한다")
    void 시퀀스_오프셋_확인() {
        // Given
        given(getMemberUseCase.countAll()).willReturn(42L);
        given(dummyMemberFactory.nextEmailCommand(anyLong(), any())).willReturn(mock(EmailRegisterMemberCommand.class));
        given(registerEmailMemberUseCase.batchRegister(any())).willReturn(List.of(1L, 2L));

        // When
        sut.seed(new SeedMembersCommand(2, true));

        // Then
        verify(dummyMemberFactory).nextEmailCommand(43L, consents);
        verify(dummyMemberFactory).nextEmailCommand(44L, consents);
    }

    @Test
    @DisplayName("단건 멤버 생성은 공백 비밀번호를 기본값으로 치환하고 명시값은 보존한다")
    void 단건_멤버_비밀번호_기본값() {
        given(registerEmailMemberUseCase.register(any()))
            .willReturn(1L, 2L, 3L);
        ArgumentCaptor<EmailRegisterMemberCommand> captor =
            ArgumentCaptor.forClass(EmailRegisterMemberCommand.class);

        var nullPassword = sut.create(CreateSeedMemberCommand.of(
            "이름", "닉네임", 10L, "one@example.com", null
        ));
        sut.create(CreateSeedMemberCommand.of(
            "이름", "닉네임", 10L, "two@example.com", "  "
        ));
        sut.create(CreateSeedMemberCommand.of(
            "이름", "닉네임", 10L, "three@example.com", "custom"
        ));

        verify(registerEmailMemberUseCase, times(3)).register(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(EmailRegisterMemberCommand::rawPassword)
            .containsExactly("Alpha!Pass2026", "Alpha!Pass2026", "custom");
        assertThat(nullPassword.memberId()).isEqualTo(1L);
        assertThat(nullPassword.email()).isEqualTo("one@example.com");
    }
}
