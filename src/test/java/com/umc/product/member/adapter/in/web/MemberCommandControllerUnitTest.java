package com.umc.product.member.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.application.port.in.command.ManageAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.IssueAuthenticationTokensCommand;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.OAuthVerificationClaims;
import com.umc.product.member.adapter.in.web.assembler.MemberInfoResponseAssembler;
import com.umc.product.member.adapter.in.web.dto.request.DeleteMemberRequest;
import com.umc.product.member.adapter.in.web.dto.request.EditMemberInfoRequest;
import com.umc.product.member.adapter.in.web.dto.request.EditMemberProfileRequest;
import com.umc.product.member.adapter.in.web.dto.request.OAuthRegisterMemberRequest;
import com.umc.product.member.adapter.in.web.dto.request.TermConsentStatus;
import com.umc.product.member.adapter.in.web.dto.response.MemberInfoResponse;
import com.umc.product.member.adapter.in.web.dto.response.RegisterResponse;
import com.umc.product.member.application.port.in.command.ChangeMemberEmailUseCase;
import com.umc.product.member.application.port.in.command.ManageMemberProfileUseCase;
import com.umc.product.member.application.port.in.command.ManageMemberUseCase;
import com.umc.product.member.application.port.in.command.RegisterEmailMemberUseCase;
import com.umc.product.member.application.port.in.command.RegisterOAuthMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.DeleteMemberCommand;
import com.umc.product.member.application.port.in.command.dto.OAuthRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.UpdateMemberCommand;
import com.umc.product.member.domain.LinkTypeAndLink;
import com.umc.product.member.domain.MemberProfileLinkType;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberCommandController 단위 계약")
class MemberCommandControllerUnitTest {

    private static final Long MEMBER_ID = 10L;
    private static final MemberPrincipal PRINCIPAL = new MemberPrincipal(MEMBER_ID);

    @Mock
    MemberInfoResponseAssembler assembler;
    @Mock
    JwtTokenProvider jwtTokenProvider;
    @Mock
    ManageAuthenticationUseCase manageAuthenticationUseCase;
    @Mock
    ManageMemberUseCase manageMemberUseCase;
    @Mock
    ChangeMemberEmailUseCase changeMemberEmailUseCase;
    @Mock
    ManageMemberProfileUseCase manageMemberProfileUseCase;
    @Mock
    RegisterOAuthMemberUseCase registerOAuthMemberUseCase;
    @Mock
    RegisterEmailMemberUseCase registerEmailMemberUseCase;

    MemberCommandController sut;

    @BeforeEach
    void setUp() {
        sut = new MemberCommandController(
            assembler,
            jwtTokenProvider,
            manageAuthenticationUseCase,
            manageMemberUseCase,
            changeMemberEmailUseCase,
            manageMemberProfileUseCase,
            registerOAuthMemberUseCase,
            registerEmailMemberUseCase
        );
    }

    @Test
    @DisplayName("OAuth·email 검증 claim과 Apple 자격 정보를 회원가입 command에 모두 전달한다")
    void oauth_회원가입을_조립한다() {
        OAuthRegisterMemberRequest request = new OAuthRegisterMemberRequest(
            "oauth-verification-token",
            "홍길동",
            "길동",
            "email-verification-token",
            20L,
            "profile-image",
            List.of(new TermConsentStatus(1L, true), new TermConsentStatus(2L, false)),
            "apple-refresh-token",
            "apple-client"
        );
        given(jwtTokenProvider.parseOAuthVerificationToken("oauth-verification-token"))
            .willReturn(new OAuthVerificationClaims(
                "provider@example.com",
                OAuthProvider.APPLE,
                "apple-user"
            ));
        given(jwtTokenProvider.parseEmailVerificationToken(
            "email-verification-token",
            EmailVerificationPurpose.REGISTER
        )).willReturn("verified@example.com");
        given(registerOAuthMemberUseCase.register(any())).willReturn(MEMBER_ID);
        given(manageAuthenticationUseCase.issueTokens(IssueAuthenticationTokensCommand.of(MEMBER_ID)))
            .willReturn(NewTokens.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build());

        RegisterResponse result = sut.registerMemberByOAuth(request);

        assertThat(result.memberId()).isEqualTo(MEMBER_ID);
        assertThat(result.accessToken()).isEqualTo("access-token");
        ArgumentCaptor<OAuthRegisterMemberCommand> captor =
            ArgumentCaptor.forClass(OAuthRegisterMemberCommand.class);
        then(registerOAuthMemberUseCase).should().register(captor.capture());
        assertThat(captor.getValue()).satisfies(command -> {
            assertThat(command.provider()).isEqualTo(OAuthProvider.APPLE);
            assertThat(command.providerId()).isEqualTo("apple-user");
            assertThat(command.email()).isEqualTo("verified@example.com");
            assertThat(command.termConsents()).extracting(item -> item.isAgreed())
                .containsExactly(true, false);
            assertThat(command.appleRefreshToken()).isEqualTo("apple-refresh-token");
            assertThat(command.appleClientId()).isEqualTo("apple-client");
        });
    }

    @Nested
    @DisplayName("회원 정보 변경")
    class Update {

        @Test
        @DisplayName("profile image 수정은 로그인 회원 ID로 위임하고 최신 응답을 재조회한다")
        void profile_image를_수정한다() {
            MemberInfoResponse response = MemberInfoResponse.builder().id(MEMBER_ID).build();
            given(assembler.fromMemberId(MEMBER_ID)).willReturn(response);

            assertThat(sut.editMemberInfo(PRINCIPAL, new EditMemberInfoRequest("new-image")))
                .isSameAs(response);
            then(manageMemberUseCase).should().updateMember(
                UpdateMemberCommand.forProfileUpdate(MEMBER_ID, "new-image")
            );
        }

        @Test
        @DisplayName("프로필 링크 수정은 요청 링크와 로그인 회원 ID를 command로 전달한다")
        void profile_link를_수정한다() {
            List<LinkTypeAndLink> links = List.of(
                new LinkTypeAndLink(MemberProfileLinkType.GITHUB, "https://github.com/member")
            );
            MemberInfoResponse response = MemberInfoResponse.builder().id(MEMBER_ID).build();
            given(assembler.fromMemberId(MEMBER_ID)).willReturn(response);

            assertThat(sut.editMemberProfile(PRINCIPAL, new EditMemberProfileRequest(links)))
                .isSameAs(response);
            then(manageMemberProfileUseCase).should().upsert(
                org.mockito.ArgumentMatchers.argThat(command ->
                    command.memberId().equals(MEMBER_ID) && command.links().equals(links)
                )
            );
        }
    }

    @Nested
    @DisplayName("회원 삭제")
    class Delete {

        @Test
        @DisplayName("본인 삭제는 응답을 먼저 보존하고 provider access token을 command에 전달한다")
        void 본인을_삭제한다() {
            MemberInfoResponse beforeDelete = MemberInfoResponse.builder().id(MEMBER_ID).name("홍길동").build();
            given(assembler.fromMemberId(MEMBER_ID)).willReturn(beforeDelete);

            MemberInfoResponse result = sut.deleteMember(
                PRINCIPAL,
                new DeleteMemberRequest("google-token", "kakao-token")
            );

            assertThat(result).isSameAs(beforeDelete);
            then(manageMemberUseCase).should().deleteMember(
                DeleteMemberCommand.builder()
                    .memberId(MEMBER_ID)
                    .googleAccessToken("google-token")
                    .kakaoAccessToken("kakao-token")
                    .build()
            );
        }

        @Test
        @DisplayName("request body 없는 본인 삭제와 관리자 삭제는 provider token을 null로 전달한다")
        void token_없는_삭제를_처리한다() {
            given(assembler.fromMemberId(MEMBER_ID))
                .willReturn(MemberInfoResponse.builder().id(MEMBER_ID).build());
            given(assembler.fromMemberId(99L))
                .willReturn(MemberInfoResponse.builder().id(99L).build());

            sut.deleteMember(PRINCIPAL, null);
            sut.deleteMember(99L);

            then(manageMemberUseCase).should().deleteMember(
                DeleteMemberCommand.builder().memberId(MEMBER_ID).build()
            );
            then(manageMemberUseCase).should().deleteMember(
                DeleteMemberCommand.builder().memberId(99L).build()
            );
        }
    }
}
