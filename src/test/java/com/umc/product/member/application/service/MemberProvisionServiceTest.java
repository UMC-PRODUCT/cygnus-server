package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.port.in.command.CredentialAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.RegisterCredentialByEmailCommand;
import com.umc.product.member.application.port.in.command.dto.ProvisionMemberCommand;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;

@ExtendWith(MockitoExtension.class)
class MemberProvisionServiceTest {

    private static final String EMAIL = "jeong@university.neordinary.com";
    private static final String PASSWORD = "TempPass1!aaaaaa";

    @Mock
    SaveMemberPort saveMemberPort;
    @Mock
    LoadMemberPort loadMemberPort;
    @Mock
    CredentialAuthenticationUseCase credentialAuthenticationUseCase;

    @InjectMocks
    MemberProvisionService sut;

    @Test
    void 발급_이메일로_Member를_생성하고_자격증명을_등록한다() {
        Member saved = Member.create("정의찬", "제옹", EMAIL, 1L, null);
        ReflectionTestUtils.setField(saved, "id", 500L);
        given(loadMemberPort.existsByEmail(EMAIL)).willReturn(false);
        given(saveMemberPort.save(any(Member.class))).willReturn(saved);

        Long memberId = sut.provision(command());

        assertThat(memberId).isEqualTo(500L);
        verify(credentialAuthenticationUseCase).registerCredentialByEmail(
            RegisterCredentialByEmailCommand.of(500L, PASSWORD)
        );
    }

    @Test
    void 이미_존재하는_이메일이면_프로비저닝을_거부한다() {
        given(loadMemberPort.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> sut.provision(command()))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.EMAIL_ALREADY_EXISTS);

        verifyNoInteractions(saveMemberPort, credentialAuthenticationUseCase);
    }

    private ProvisionMemberCommand command() {
        return new ProvisionMemberCommand("정의찬", "제옹", EMAIL, 1L, PASSWORD);
    }
}
