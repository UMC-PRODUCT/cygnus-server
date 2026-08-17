package com.umc.product.test.adapter.in.web;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authentication.adapter.out.external.AppleOAuthProperties;
import com.umc.product.authentication.adapter.out.external.AppleTokenVerifier;
import com.umc.product.authentication.application.port.in.command.ManageAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.IssueAuthenticationTokensCommand;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.response.ApiResponse;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.global.security.annotation.Public;
import com.umc.product.notification.application.port.in.SendEmailUseCase;
import com.umc.product.notification.application.port.in.SendNotificationToAudienceUseCase;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.application.port.in.annotation.WebhookAlarm;
import com.umc.product.notification.application.port.in.dto.SendVerificationEmailCommand;
import com.umc.product.notification.application.port.in.dto.SendWebhookAlarmCommand;
import com.umc.product.notification.domain.WebhookPlatform;
import com.umc.product.storage.adapter.in.web.dto.FileResponse;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import com.umc.product.test.dto.FcmTestSendRequest;
import com.umc.product.test.dto.TestAopAlarmResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Profile("local | alpha")
@RestController
@RequestMapping("/test")
@Tag(name = "Test | 일반 테스트", description = "개발과 테스트 환경에서만 사용하는 점검 API입니다.")
@Slf4j
@Public
public class TestController {

    private final JwtTokenProvider jwtTokenProvider;
    private final ManageAuthenticationUseCase manageAuthenticationUseCase;
    private final AppleTokenVerifier appleTokenVerifier;
    private final AppleOAuthProperties appleOAuthProperties;
    private final SendWebhookAlarmUseCase sendWebhookAlarmUseCase;
    private final GetFileUseCase getFileUseCase;
    private final SendEmailUseCase sendEmailUseCase;
    private final SendNotificationToAudienceUseCase sendNotificationToAudienceUseCase;

    /**
     * 파일 정보 및 접근 URL을 조회합니다.
     */
    @Operation(operationId = "TEST-001", summary = "파일 접근 URL 조회", description = """
        local과 development 환경에서만 사용할 수 있습니다.

        파일 업로드 상태 확인 용도로만 사용합니다.
        """)
    @GetMapping("/file/{fileId}")
    @Public
    public ApiResponse<FileResponse> getFile(@PathVariable String fileId) {
        FileInfo fileInfo = getFileUseCase.getById(fileId);
        return ApiResponse.onSuccess(FileResponse.from(fileInfo));
    }

    @Operation(operationId = "TEST-002", summary = "FCM 푸시 알림 테스트 전송")
    @PostMapping("/fcm/test-send")
    public void sendTestNotification(@RequestBody FcmTestSendRequest request) {
        sendNotificationToAudienceUseCase.sendToMember(request.toCommand());
    }

    @PostMapping("/email/send-test")
    @Public
    void sendTestEmail(
        @RequestBody SendVerificationEmailCommand command
    ) {
        sendEmailUseCase.sendVerificationEmail(command);
    }

    @WebhookAlarm(
        title = "'알람 테스트 : ' + #title",
        content = "'내용 : ' + #result.content",
        platforms = {WebhookPlatform.TELEGRAM}
    )
    @Audited(
        domain = Domain.AUDIT_LOG,
        action = AuditAction.CREATE,
        targetType = "Test",
        targetId = "'TEST'",
        description = "'내용 : ' + #result.content"
    )
    @GetMapping("webhook/aop-test")
    @Operation(operationId = "TEST-003", summary = "AOP로 전송하는 알람 테스트")
    public TestAopAlarmResponse sendAopWebhookAlarm(
        @RequestParam String title,
        @RequestParam String content
    ) {
        log.debug("웹훅 알람 AOP 테스트를 호출했습니다.");

        return TestAopAlarmResponse.builder()
            .content(content)
            .build();
    }

    @PostMapping("webhook/alarm")
    @Operation(operationId = "TEST-004", summary = "웹훅 알람 전송 테스트")
    public void sendWebhookAlarm(
        @RequestParam String title,
        @RequestParam String content
    ) {
        sendWebhookAlarmUseCase.send(
            SendWebhookAlarmCommand.builder()
                .title(title == null ? "알람 테스트" : title)
                .content(content == null ? "알람 테스트 내용입니다." : content)
                .platforms(List.of(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD))
                .build()
        );
    }

    // buffer alarm test
    @PostMapping("webhook/alarm/buffer")
    @Operation(operationId = "TEST-005", summary = "웹훅 알람 버퍼 전송 테스트")
    public void sendBufferedWebhookAlarm(
        @RequestParam String title,
        @RequestParam String content,
        @RequestParam int repeatCount
    ) {
        for (int i = 0; i < repeatCount; i++) {
            String bTitle = title == null ? "버퍼 알람 테스트" : title + " #" + (i + 1);
            String bContent = content == null ? "버퍼 알람 테스트 내용입니다." : content + " #" + (i + 1);

            sendWebhookAlarmUseCase.sendBuffered(
                SendWebhookAlarmCommand.builder()
                    .title(bTitle)
                    .content(bContent)
                    .platforms(List.of(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD))
                    .build()
            );
        }
    }

    @GetMapping("apple-client-secret")
    @Operation(operationId = "TEST-006", summary = "Apple Client Secret 생성")
    String getAppleClientSecret(@RequestParam ClientType clientType) {
        return appleTokenVerifier.generateClientSecret(appleOAuthProperties.resolveClientId(clientType));
    }

    @Public
    @Operation(operationId = "TEST-007", summary = "AccessToken 발급")
    @GetMapping("/token/access")
    public String getAccessToken(
        @RequestParam Long memberId,
        @RequestParam(required = false) Long expirationInMinutes
    ) {
        return expirationInMinutes == null ?
            // null이면 기본값
            jwtTokenProvider.createAccessToken(memberId, null) :
            // 받았으면 해당 시간으로 만료하기
            jwtTokenProvider.createAccessToken(memberId, null, expirationInMinutes * 60);
    }

    @Operation(operationId = "TEST-008", summary = "RefreshToken 발급")
    @Public
    @GetMapping("/token/refresh")
    public String getRefreshToken(@RequestParam Long memberId) {
        return manageAuthenticationUseCase.issueTokens(IssueAuthenticationTokensCommand.of(memberId)).refreshToken();
    }

    @Operation(operationId = "TEST-009", summary = "EmailVerificationToken 발급")
    @Public
    @GetMapping("/token/email")
    public String getEmailVerification(
        @RequestParam String email,
        @RequestParam(required = false, defaultValue = "REGISTER") EmailVerificationPurpose purpose
    ) {
        return jwtTokenProvider.createEmailVerificationToken(email, purpose);
    }

    @Operation(operationId = "TEST-010", summary = "oAuthVerificationToken 발급")
    @Public
    @GetMapping("/token/oauth")
    public String getOAuthVerificationToken(OAuthProvider provider, String providerId, String email) {
        return jwtTokenProvider.createOAuthVerificationToken(email, provider, providerId);
    }


    @Operation(operationId = "TEST-011", summary = "헬스 체크")
    @Public
    @GetMapping("/health-check")
    public String healthCheck() {
        return "OK";
    }

    @Operation(operationId = "TEST-012", summary = "인증 상태 확인", description = "인증되지 않은 사용자는 401을 반환합니다.")
    @GetMapping("/check-authenticated")
    public ApiResponse<String> checkAuthenticated(@CurrentMember MemberPrincipal currentUser) {
        return ApiResponse.onSuccess(currentUser.toString());
    }

    @GetMapping("log-test")
    public void logTest() {
        log.trace("TRACE");
        log.debug("DEBUG");
        log.info("INFO");
        log.warn("WARN");
        log.error("ERROR");
    }
}
