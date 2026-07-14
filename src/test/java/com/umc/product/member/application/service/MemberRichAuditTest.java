package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.authentication.application.port.in.command.CredentialAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.query.GetMemberOAuthUseCase;
import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.member.application.port.in.command.dto.DeleteMemberCommand;
import com.umc.product.member.application.port.in.command.dto.EmailRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.OAuthRegisterMemberCommand;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.term.application.port.in.command.ManageTermAgreementUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 rich audit")
class MemberRichAuditTest {

    private static final long MEMBER_ID = 101L;
    private static final long SCHOOL_ID = 11L;
    private static final String SCHOOL_NAME = "테스트대학교";
    private static final String SENSITIVE_EMAIL = "sensitive-member@example.com";
    private static final String SENSITIVE_PASSWORD = "Password123!";
    private static final String SENSITIVE_PROVIDER_ID = "oauth-provider-subject";
    private static final String SENSITIVE_TOKEN = "oauth-access-token";

    @Mock
    LoadMemberPort loadMemberPort;
    @Mock
    SaveMemberPort saveMemberPort;
    @Mock
    MemberRegistrationValidator registrationValidator;
    @Mock
    OAuthAuthenticationUseCase oAuthAuthenticationUseCase;
    @Mock
    CredentialAuthenticationUseCase credentialAuthenticationUseCase;
    @Mock
    GetMemberOAuthUseCase getMemberOAuthUseCase;
    @Mock
    ManageTermAgreementUseCase manageTermAgreementUseCase;
    @Mock
    GetSchoolUseCase getSchoolUseCase;
    @Mock
    RecordAuditLogUseCase recordAuditLogUseCase;
    @Mock
    SendWebhookAlarmUseCase sendWebhookAlarmUseCase;
    @Mock
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @InjectMocks
    MemberService memberService;
    @InjectMocks
    EmailMemberRegisterService emailMemberRegisterService;

    @Test
    @DisplayName("기존 OAuth 가입은 저장된 회원 ID로 REGISTER 이벤트를 발행한다")
    void publishesRegisterEventForExistingOauthRegistration() {
        givenOAuthRegistration();

        Long memberId = memberService.register(oauthCommand());

        RecordAuditLogCommand event = capturedEvent();
        assertThat(memberId).isEqualTo(MEMBER_ID);
        assertThat(event.action()).isEqualTo(AuditAction.REGISTER);
        assertThat(event.targetType()).isEqualTo("Member");
        assertThat(event.targetId()).isEqualTo(String.valueOf(MEMBER_ID));
    }

    @Test
    @DisplayName("OAuth 가입 감사 이벤트는 schemaVersion 1 회원 snapshot을 담고 인증 비밀을 제외한다")
    void publishesMemberSnapshotForOauthRegistration() {
        givenOAuthRegistration();

        memberService.register(oauthCommand());

        RecordAuditLogCommand event = capturedEvent();
        assertMemberSnapshot(event, "after");
        assertThat(event.source()).isEqualTo(AuditSource.EXPLICIT_RECORDER);
        assertSensitiveValuesAbsent(event);
    }

    @Test
    @DisplayName("이메일 가입 감사 이벤트는 schemaVersion 1 회원 snapshot을 담고 비밀번호와 이메일을 제외한다")
    void publishesMemberSnapshotForEmailRegistration() {
        given(saveMemberPort.save(any(Member.class))).willReturn(member());
        given(getSchoolUseCase.getSchoolDetail(SCHOOL_ID)).willReturn(school());

        emailMemberRegisterService.register(emailCommand());

        RecordAuditLogCommand event = capturedEvent();
        assertMemberSnapshot(event, "after");
        assertThat(event.source()).isEqualTo(AuditSource.EXPLICIT_RECORDER);
        assertSensitiveValuesAbsent(event);
    }

    @Test
    @DisplayName("기존 탈퇴는 회원을 삭제하고 WITHDRAW 이벤트를 발행한다")
    void preservesExistingWithdrawalBehavior() {
        givenWithdrawal();

        memberService.deleteMember(deleteCommand());

        RecordAuditLogCommand event = capturedEvent();
        then(saveMemberPort).should().delete(any(Member.class));
        assertThat(event.action()).isEqualTo(AuditAction.WITHDRAW);
        assertThat(event.targetId()).isEqualTo(String.valueOf(MEMBER_ID));
    }

    @Test
    @DisplayName("탈퇴는 삭제 전에 회원 snapshot을 확보해 WITHDRAW 감사 이벤트에 보존한다")
    void preservesMemberSnapshotBeforeWithdrawal() {
        givenWithdrawal();

        memberService.deleteMember(deleteCommand());

        RecordAuditLogCommand event = capturedEvent();
        assertMemberSnapshot(event, "before");
        assertThat(section(event, "target")).containsAllEntriesOf(section(event, "before"));
        assertThat(section(event, "after")).isEmpty();
        assertSensitiveValuesAbsent(event);

        InOrder order = inOrder(getSchoolUseCase, saveMemberPort);
        order.verify(getSchoolUseCase).getSchoolDetail(SCHOOL_ID);
        order.verify(saveMemberPort).delete(any(Member.class));
    }

    private void givenOAuthRegistration() {
        given(saveMemberPort.save(any(Member.class))).willReturn(member());
        given(getSchoolUseCase.getSchoolDetail(SCHOOL_ID)).willReturn(school());
    }

    private void givenWithdrawal() {
        given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.of(member()));
        given(getMemberOAuthUseCase.getOAuthList(MEMBER_ID)).willReturn(List.of());
        given(getSchoolUseCase.getSchoolDetail(SCHOOL_ID)).willReturn(school());
    }

    private OAuthRegisterMemberCommand oauthCommand() {
        return OAuthRegisterMemberCommand.builder()
            .provider(OAuthProvider.GOOGLE)
            .providerId(SENSITIVE_PROVIDER_ID)
            .name("회원이름")
            .nickname("회원닉네임")
            .email(SENSITIVE_EMAIL)
            .schoolId(SCHOOL_ID)
            .termConsents(List.of())
            .appleRefreshToken(SENSITIVE_TOKEN)
            .appleClientId("sensitive-client-id")
            .build();
    }

    private EmailRegisterMemberCommand emailCommand() {
        return EmailRegisterMemberCommand.builder()
            .rawPassword(SENSITIVE_PASSWORD)
            .name("회원이름")
            .nickname("회원닉네임")
            .email(SENSITIVE_EMAIL)
            .schoolId(SCHOOL_ID)
            .termConsents(List.of())
            .build();
    }

    private DeleteMemberCommand deleteCommand() {
        return DeleteMemberCommand.builder()
            .memberId(MEMBER_ID)
            .googleAccessToken(SENSITIVE_TOKEN)
            .kakaoAccessToken("kakao-access-token")
            .build();
    }

    private static Member member() {
        Member member = Member.create("회원이름", "회원닉네임", SENSITIVE_EMAIL, SCHOOL_ID, null);
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    private static SchoolDetailInfo school() {
        return new SchoolDetailInfo(
            null, null, SCHOOL_NAME, SCHOOL_ID, null, null, List.of(), true, null, null
        );
    }

    private RecordAuditLogCommand capturedEvent() {
        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(RecordAuditLogCommand.class);
        then(recordAuditLogUseCase).should().record(captor.capture());
        return captor.getValue();
    }

    private static void assertMemberSnapshot(RecordAuditLogCommand event, String stateName) {
        assertThat(event.details()).containsEntry("schemaVersion", 1);
        assertThat(section(event, "target"))
            .containsEntry("type", "Member")
            .containsEntry("id", MEMBER_ID)
            .containsEntry("memberId", MEMBER_ID)
            .containsEntry("name", "회원이름")
            .containsEntry("nickname", "회원닉네임")
            .containsEntry("schoolName", SCHOOL_NAME)
            .containsEntry("status", "ACTIVE");
        assertThat(section(event, stateName)).containsAllEntriesOf(section(event, "target"));
    }

    private static void assertSensitiveValuesAbsent(RecordAuditLogCommand event) {
        assertThat(event.description() + event.details())
            .doesNotContain(
                "email", SENSITIVE_EMAIL,
                "password", SENSITIVE_PASSWORD,
                "providerId", SENSITIVE_PROVIDER_ID,
                "subject", SENSITIVE_PROVIDER_ID,
                "token", SENSITIVE_TOKEN,
                "clientId", "sensitive-client-id"
            );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(RecordAuditLogCommand event, String name) {
        return (Map<String, Object>) event.details().get(name);
    }
}
