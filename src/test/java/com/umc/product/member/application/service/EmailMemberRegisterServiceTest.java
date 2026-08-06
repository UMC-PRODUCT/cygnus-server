package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.port.in.command.CredentialAuthenticationUseCase;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.member.application.port.in.command.dto.EmailRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.TermConsents;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.term.application.port.in.command.ManageTermAgreementUseCase;
import com.umc.product.term.domain.exception.TermDomainException;
import com.umc.product.term.domain.exception.TermErrorCode;

@ExtendWith(MockitoExtension.class)
class EmailMemberRegisterServiceTest {

    @Mock
    SaveMemberPort saveMemberPort;

    @Mock
    MemberRegistrationValidator registrationValidator;

    @Mock
    CredentialAuthenticationUseCase credentialAuthenticationUseCase;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    @Mock
    DomainEventPublisher eventPublisher;

    @Mock
    ManageTermAgreementUseCase manageTermAgreementUseCase;

    @InjectMocks
    EmailMemberRegisterService sut;

    @Test
    @DisplayName("이메일 회원가입 성공 시 약관 동의 정보를 저장한다")
    void 이메일_회원가입_성공시_약관_동의_정보를_저장한다() {
        // given
        EmailRegisterMemberCommand command = EmailRegisterMemberCommand.builder()
            .rawPassword("Password123!")
            .name("홍길동")
            .nickname("길동")
            .email("gildong@example.com")
            .schoolId(1L)
            .termConsents(List.of(
                TermConsents.builder().termId(1L).isAgreed(true).build(),
                TermConsents.builder().termId(2L).isAgreed(true).build()
            ))
            .build();

        given(saveMemberPort.save(any(Member.class))).willAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", 100L);
            return member;
        });
        given(getSchoolUseCase.getSchoolDetail(1L)).willReturn(new SchoolDetailInfo(
            10L,
            "중앙",
            "테스트대학교",
            null,
            1L,
            null,
            null,
            List.of(),
            true,
            null,
            null
        ));

        // when
        Long memberId = sut.register(command);

        // then
        assertThat(memberId).isEqualTo(100L);
        then(manageTermAgreementUseCase).should(times(2)).createTermConsent(any());
    }

    @Test
    @DisplayName("필수 약관 검증 실패 시 회원과 자격증명을 저장하지 않는다")
    void 필수_약관_검증_실패시_회원과_자격증명을_저장하지_않는다() {
        // given
        EmailRegisterMemberCommand command = EmailRegisterMemberCommand.builder()
            .rawPassword("Password123!")
            .name("홍길동")
            .nickname("길동")
            .email("gildong@example.com")
            .schoolId(1L)
            .termConsents(List.of(
                TermConsents.builder().termId(1L).isAgreed(false).build()
            ))
            .build();

        willThrow(new TermDomainException(TermErrorCode.MANDATORY_TERMS_NOT_AGREED))
            .given(registrationValidator).validateMandatoryTermsAgreed(command.termConsents());

        // when & then
        assertThatThrownBy(() -> sut.register(command))
            .isInstanceOf(TermDomainException.class)
            .extracting("baseCode")
            .isEqualTo(TermErrorCode.MANDATORY_TERMS_NOT_AGREED);

        then(saveMemberPort).should(never()).save(any());
        then(credentialAuthenticationUseCase).should(never()).registerCredentialByEmail(any());
        then(manageTermAgreementUseCase).should(never()).createTermConsent(any());
        then(eventPublisher).should(never()).publish(any());
    }
}
