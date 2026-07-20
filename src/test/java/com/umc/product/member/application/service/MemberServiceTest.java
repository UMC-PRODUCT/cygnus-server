package com.umc.product.member.application.service;

import static com.umc.product.support.fixture.MemberUnitFixture.OAuth_회원가입_명령;
import static com.umc.product.support.fixture.MemberUnitFixture.필수_약관_동의;
import static com.umc.product.support.fixture.MemberUnitFixture.학교_상세;
import static com.umc.product.support.fixture.MemberUnitFixture.회원;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.LinkOAuthCommand;
import com.umc.product.authentication.application.port.in.command.dto.UnlinkOAuthCommand;
import com.umc.product.authentication.application.port.in.query.GetMemberOAuthUseCase;
import com.umc.product.authentication.application.port.in.query.dto.MemberOAuthInfo;
import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.member.application.port.in.command.dto.DeleteMemberCommand;
import com.umc.product.member.application.port.in.command.dto.OAuthRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.UpdateMemberCommand;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.term.application.port.in.command.ManageTermAgreementUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService")
class MemberServiceTest {

    private static final Long MEMBER_ID = 100L;
    private static final Long SCHOOL_ID = 10L;

    @Mock
    LoadMemberPort loadMemberPort;
    @Mock
    SaveMemberPort saveMemberPort;
    @Mock
    MemberRegistrationValidator registrationValidator;
    @Mock
    OAuthAuthenticationUseCase oAuthAuthenticationUseCase;
    @Mock
    GetMemberOAuthUseCase getMemberOAuthUseCase;
    @Mock
    ManageTermAgreementUseCase manageTermAgreementUseCase;
    @Mock
    GetSchoolUseCase getSchoolUseCase;
    @Mock
    DomainEventPublisher eventPublisher;
    @Mock
    SendWebhookAlarmUseCase sendWebhookAlarmUseCase;
    @Mock
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    MemberService sut;

    @BeforeEach
    void setUp() {
        sut = new MemberService(
            loadMemberPort,
            saveMemberPort,
            registrationValidator,
            oAuthAuthenticationUseCase,
            getMemberOAuthUseCase,
            manageTermAgreementUseCase,
            getSchoolUseCase,
            eventPublisher,
            sendWebhookAlarmUseCase,
            evictAuthoritySnapshotCacheUseCase
        );
    }

    @Nested
    @DisplayName("OAuth 회원가입")
    class Register {

        @Test
        @DisplayName("검증 후 회원·OAuth·약관을 저장하고 알림과 감사 이벤트를 발행한다")
        void 회원가입_전체_흐름을_순서대로_수행한다() {
            OAuthRegisterMemberCommand command = OAuth_회원가입_명령(
                "apple-provider-id",
                SCHOOL_ID,
                필수_약관_동의()
            );
            given(saveMemberPort.save(any(Member.class))).willReturn(회원(MEMBER_ID, SCHOOL_ID));
            given(getSchoolUseCase.getSchoolDetail(SCHOOL_ID)).willReturn(학교_상세(SCHOOL_ID, "테스트대학교"));

            Long result = sut.register(command);

            assertThat(result).isEqualTo(MEMBER_ID);
            then(registrationValidator).should().validateSchoolExists(SCHOOL_ID);
            then(registrationValidator).should().validateProfileImageExists("profile-image-id");
            then(registrationValidator).should().validateMandatoryTermsAgreed(command.termConsents());
            ArgumentCaptor<LinkOAuthCommand> oauthCaptor = ArgumentCaptor.forClass(LinkOAuthCommand.class);
            then(oAuthAuthenticationUseCase).should().linkOAuth(oauthCaptor.capture());
            assertThat(oauthCaptor.getValue().memberId()).isEqualTo(MEMBER_ID);
            assertThat(oauthCaptor.getValue().appleRefreshToken()).isEqualTo("apple-refresh-token");
            assertThat(oauthCaptor.getValue().appleClientId()).isEqualTo("apple-client-id");
            then(manageTermAgreementUseCase).should(times(2)).createTermConsent(any());
            then(sendWebhookAlarmUseCase).should().sendBuffered(any());
            ArgumentCaptor<AuditLogEvent> eventCaptor = ArgumentCaptor.forClass(AuditLogEvent.class);
            then(eventPublisher).should().publish(eventCaptor.capture());
            assertThat(eventCaptor.getValue().action()).isEqualTo(AuditAction.REGISTER);
            assertThat(eventCaptor.getValue().description()).contains("테스트대학교", "길동", "홍길동");
        }

        @Test
        @DisplayName("첫 검증이 실패하면 회원이나 외부 계정을 저장하지 않는다")
        void 검증_실패는_부수효과를_막는다() {
            OAuthRegisterMemberCommand command = OAuth_회원가입_명령("provider-id", SCHOOL_ID, 필수_약관_동의());
            RuntimeException validationFailure = new RuntimeException("학교 없음");
            org.mockito.BDDMockito.willThrow(validationFailure)
                .given(registrationValidator)
                .validateSchoolExists(SCHOOL_ID);

            assertThatThrownBy(() -> sut.register(command)).isSameAs(validationFailure);
            then(saveMemberPort).shouldHaveNoInteractions();
            then(oAuthAuthenticationUseCase).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("OAuth 회원 일괄 가입")
    class BatchRegister {

        @Test
        @DisplayName("입력 순서대로 회원과 OAuth와 약관을 연결한다")
        void 입력_순서대로_일괄_가입한다() {
            OAuthRegisterMemberCommand first = OAuth_회원가입_명령("first", SCHOOL_ID, 필수_약관_동의());
            OAuthRegisterMemberCommand second = OAuth_회원가입_명령("second", SCHOOL_ID + 1, 필수_약관_동의());
            given(saveMemberPort.saveAll(any())).willReturn(List.of(
                회원(MEMBER_ID, SCHOOL_ID),
                회원(MEMBER_ID + 1, SCHOOL_ID + 1)
            ));

            List<Long> result = sut.batchRegister(List.of(first, second));

            assertThat(result).containsExactly(MEMBER_ID, MEMBER_ID + 1);
            then(registrationValidator).should(times(2)).validateMandatoryTermsAgreed(any());
            then(manageTermAgreementUseCase).should(times(4)).createTermConsent(any());
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<LinkOAuthCommand>> captor = ArgumentCaptor.forClass(List.class);
            then(oAuthAuthenticationUseCase).should().linkOAuthBulk(captor.capture());
            assertThat(captor.getValue())
                .extracting(LinkOAuthCommand::memberId, LinkOAuthCommand::providerId)
                .containsExactly(
                    org.assertj.core.groups.Tuple.tuple(MEMBER_ID, "first"),
                    org.assertj.core.groups.Tuple.tuple(MEMBER_ID + 1, "second")
                );
        }

        @Test
        @DisplayName("빈 입력은 빈 저장과 빈 OAuth 연결을 수행하고 빈 ID 목록을 반환한다")
        void 빈_입력은_빈_결과를_반환한다() {
            given(saveMemberPort.saveAll(List.of())).willReturn(List.of());

            List<Long> result = sut.batchRegister(List.of());

            assertThat(result).isEmpty();
            then(oAuthAuthenticationUseCase).should().linkOAuthBulk(List.of());
            then(manageTermAgreementUseCase).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("회원 수정")
    class Update {

        @Test
        @DisplayName("회원 조회와 이미지 검증 후 프로필 이미지를 변경한다")
        void 프로필_이미지를_변경한다() {
            Member member = 회원(MEMBER_ID, SCHOOL_ID);
            given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.of(member));

            sut.updateMember(UpdateMemberCommand.forProfileUpdate(MEMBER_ID, "new-profile-id"));

            assertThat(member.getProfileImageId()).isEqualTo("new-profile-id");
            then(registrationValidator).should().validateProfileImageExists("new-profile-id");
        }

        @Test
        @DisplayName("존재하지 않는 회원은 이미지 검증 전에 거부한다")
        void 존재하지_않는_회원은_거부한다() {
            given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.updateMember(
                UpdateMemberCommand.forProfileUpdate(MEMBER_ID, "new-profile-id")
            )).isInstanceOf(MemberDomainException.class)
                .extracting("baseCode")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
            then(registrationValidator).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class Delete {

        @Test
        @DisplayName("연결된 모든 OAuth를 탈퇴 모드로 해제한 뒤 회원과 권한 캐시를 삭제한다")
        void 모든_OAuth를_해제하고_회원을_삭제한다() {
            Member member = 회원(MEMBER_ID, SCHOOL_ID);
            given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(getMemberOAuthUseCase.getOAuthList(MEMBER_ID)).willReturn(List.of(
                new MemberOAuthInfo(1L, MEMBER_ID, OAuthProvider.GOOGLE),
                new MemberOAuthInfo(2L, MEMBER_ID, OAuthProvider.KAKAO)
            ));
            given(getSchoolUseCase.getSchoolDetail(SCHOOL_ID)).willReturn(학교_상세(SCHOOL_ID, "테스트대학교"));
            DeleteMemberCommand command = DeleteMemberCommand.builder()
                .memberId(MEMBER_ID)
                .googleAccessToken("google-token")
                .kakaoAccessToken("kakao-token")
                .build();

            sut.deleteMember(command);

            ArgumentCaptor<UnlinkOAuthCommand> unlinkCaptor = ArgumentCaptor.forClass(UnlinkOAuthCommand.class);
            then(oAuthAuthenticationUseCase).should(times(2)).unlinkOAuth(unlinkCaptor.capture());
            assertThat(unlinkCaptor.getAllValues())
                .extracting(UnlinkOAuthCommand::memberOAuthId)
                .containsExactly(1L, 2L);
            assertThat(unlinkCaptor.getAllValues()).allSatisfy(unlink -> {
                assertThat(unlink.isWithdrawal()).isTrue();
                assertThat(unlink.googleAccessToken()).isEqualTo("google-token");
                assertThat(unlink.kakaoAccessToken()).isEqualTo("kakao-token");
            });
            InOrder order = inOrder(saveMemberPort, evictAuthoritySnapshotCacheUseCase, eventPublisher);
            order.verify(saveMemberPort).delete(member);
            order.verify(evictAuthoritySnapshotCacheUseCase).evictByMemberId(MEMBER_ID);
            order.verify(eventPublisher).publish(any(AuditLogEvent.class));
        }

        @Test
        @DisplayName("연결 OAuth가 없어도 회원과 권한 캐시는 삭제한다")
        void OAuth가_없어도_회원과_캐시를_삭제한다() {
            Member member = 회원(MEMBER_ID, SCHOOL_ID);
            given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(getMemberOAuthUseCase.getOAuthList(MEMBER_ID)).willReturn(List.of());
            given(getSchoolUseCase.getSchoolDetail(SCHOOL_ID)).willReturn(학교_상세(SCHOOL_ID, "테스트대학교"));

            sut.deleteMember(DeleteMemberCommand.builder().memberId(MEMBER_ID).build());

            then(oAuthAuthenticationUseCase).should(never()).unlinkOAuth(any());
            then(saveMemberPort).should().delete(member);
            then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(MEMBER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 회원은 OAuth 조회 전에 거부한다")
        void 존재하지_않는_회원은_탈퇴를_거부한다() {
            given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.deleteMember(
                DeleteMemberCommand.builder().memberId(MEMBER_ID).build()
            )).isInstanceOf(MemberDomainException.class)
                .extracting("baseCode")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
            then(getMemberOAuthUseCase).shouldHaveNoInteractions();
            then(saveMemberPort).shouldHaveNoInteractions();
        }
    }
}
