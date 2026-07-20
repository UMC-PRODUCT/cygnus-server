package com.umc.product.member.application.service;

import static com.umc.product.support.fixture.MemberUnitFixture.회원;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.domain.Member;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 자격증명 조회 서비스")
class MemberCredentialQueryServiceTest {

    @Mock
    LoadMemberPort loadMemberPort;

    @InjectMocks
    MemberCredentialQueryService sut;

    @Test
    @DisplayName("null·blank email과 null member ID는 port 호출 없이 empty를 반환한다")
    void 빈_식별자를_즉시_반환한다() {
        assertThat(sut.findCredentialByEmail(null)).isEmpty();
        assertThat(sut.findCredentialByEmail("  ")).isEmpty();
        assertThat(sut.findCredentialByMemberId(null)).isEmpty();
        assertThat(sut.existsByEmail(null)).isFalse();
        assertThat(sut.existsByEmail("")).isFalse();
        then(loadMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("회원은 있지만 password hash가 없으면 자격증명을 숨긴다")
    void password_hash_없는_회원을_필터링한다() {
        Member member = 회원(1L, 10L);
        given(loadMemberPort.findByEmail("member@example.com")).willReturn(Optional.of(member));
        given(loadMemberPort.findById(1L)).willReturn(Optional.of(member));

        assertThat(sut.findCredentialByEmail("member@example.com")).isEmpty();
        assertThat(sut.findCredentialByMemberId(1L)).isEmpty();
    }

    @Test
    @DisplayName("등록된 password hash를 마스킹 DTO로 반환하고 email 존재 조회를 위임한다")
    void 자격증명을_반환한다() {
        Member member = 회원(1L, 10L);
        member.registerCredential("{bcrypt}encoded-password");
        given(loadMemberPort.findByEmail("member@example.com")).willReturn(Optional.of(member));
        given(loadMemberPort.findById(1L)).willReturn(Optional.of(member));
        given(loadMemberPort.existsByEmail("member@example.com")).willReturn(true);

        assertThat(sut.findCredentialByEmail("member@example.com")).get()
            .satisfies(info -> {
                assertThat(info.memberId()).isEqualTo(1L);
                assertThat(info.passwordHash()).isEqualTo("{bcrypt}encoded-password");
                assertThat(info.toString()).doesNotContain("encoded-password").contains("***");
            });
        assertThat(sut.findCredentialByMemberId(1L)).isPresent();
        assertThat(sut.existsByEmail("member@example.com")).isTrue();
    }
}
