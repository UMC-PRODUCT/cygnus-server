package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.member.application.port.in.command.dto.ChangeMemberPasswordCommand;
import com.umc.product.member.application.port.in.command.dto.MemberCredentialStatusInfo;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberCredentialCommandService")
class MemberCredentialCommandServiceTest {

    @Mock
    LoadMemberPort loadMemberPort;

    @InjectMocks
    MemberCredentialCommandService sut;

    @Test
    @DisplayName("credential 상태 조회는 member row를 lock으로 조회한다")
    void credential_상태_조회는_member_row를_lock으로_조회한다() {
        Member member = member(1L);
        member.registerCredential("{noop}password");
        given(loadMemberPort.findByIdForUpdate(1L)).willReturn(Optional.of(member));

        MemberCredentialStatusInfo result = sut.getCredentialStatusForUpdate(1L);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.hasCredential()).isTrue();
        then(loadMemberPort).should().findByIdForUpdate(1L);
    }

    @Test
    @DisplayName("passwordHash가 없으면 credential 미등록 상태를 반환한다")
    void passwordHash가_없으면_credential_미등록_상태를_반환한다() {
        Member member = member(1L);
        given(loadMemberPort.findByIdForUpdate(1L)).willReturn(Optional.of(member));

        MemberCredentialStatusInfo result = sut.getCredentialStatusForUpdate(1L);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.hasCredential()).isFalse();
        then(loadMemberPort).should().findByIdForUpdate(1L);
    }

    @Test
    @DisplayName("member가 없으면 MEMBER_NOT_FOUND를 던진다")
    void member가_없으면_MEMBER_NOT_FOUND를_던진다() {
        given(loadMemberPort.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getCredentialStatusForUpdate(1L))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호 변경은 회원을 조회해 encoding된 비밀번호로 교체한다")
    void 비밀번호를_변경한다() {
        Member member = member(1L);
        member.registerCredential("{noop}old");
        given(loadMemberPort.findById(1L)).willReturn(Optional.of(member));

        sut.changePassword(ChangeMemberPasswordCommand.of(1L, "{noop}changed"));

        assertThat(member.getPasswordHash()).isEqualTo("{noop}changed");
    }

    @Test
    @DisplayName("비밀번호를 변경할 회원이 없으면 MEMBER_NOT_FOUND를 던진다")
    void 비밀번호_변경_회원_누락을_거부한다() {
        given(loadMemberPort.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.changePassword(ChangeMemberPasswordCommand.of(1L, "{noop}changed")))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("lock 상태 조회의 회원 ID가 null이면 저장소 조회 전에 거부한다")
    void null_회원_ID의_lock_조회를_거부한다() {
        assertThatThrownBy(() -> sut.getCredentialStatusForUpdate(null))
            .isInstanceOf(MemberDomainException.class);
        then(loadMemberPort).shouldHaveNoInteractions();
    }

    private Member member(Long id) {
        Member member = Member.create("홍길동", "길동", "gildong@example.com", 1L, null);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
