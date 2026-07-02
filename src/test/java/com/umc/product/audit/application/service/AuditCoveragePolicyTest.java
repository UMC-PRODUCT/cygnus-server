package com.umc.product.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.audit.application.port.in.annotation.Audited;

class AuditCoveragePolicyTest {

    @Test
    @DisplayName("감사 로그 액션 enum은 전 도메인 상태 변경에 필요한 값을 제공한다")
    void required_audit_actions_exist() {
        assertThat(Stream.of("LOGIN", "LINK", "UNLINK", "ACCESS_DENIED", "PUBLISH", "CANCEL", "REMIND", "REORDER", "FINALIZE")
            .map(this::auditAction)
            .toList()).hasSize(9);
    }

    @Test
    @DisplayName("주요 CommandService 상태 변경 메서드는 감사 로그 대상으로 선언한다")
    void command_services_are_audited() {
        List<String> violations = auditedSpecs().stream()
            .filter(spec -> !matchesAuditPolicy(spec))
            .map(AuditSpec::describe)
            .toList();

        assertThat(violations).isEmpty();
    }

    private boolean matchesAuditPolicy(AuditSpec spec) {
        Audited audited = getMethod(spec).getAnnotation(Audited.class);
        return audited != null
            && audited.domain().name().equals(spec.domain())
            && audited.action().name().equals(spec.action())
            && audited.targetType().equals(spec.targetType());
    }

    private Method getMethod(AuditSpec spec) {
        try {
            return type(spec.serviceClassName()).getMethod(spec.methodName(), spec.parameterTypes());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("감사 로그 정책 테스트 메서드 조회 실패: " + spec.describe(), e);
        }
    }

    private Object auditAction(String name) {
        try {
            Class<?> auditActionClass = type("audit.domain.AuditAction");
            return Enum.valueOf((Class<Enum>) auditActionClass.asSubclass(Enum.class), name);
        } catch (IllegalArgumentException e) {
            throw new AssertionError("필수 AuditAction 누락: " + name, e);
        }
    }

    private static Class<?> type(String shortName) {
        try {
            return Class.forName("com.umc.product." + shortName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("감사 로그 정책 테스트 타입 조회 실패: " + shortName, e);
        }
    }

    private static AuditSpec spec(
        String serviceClassName,
        String methodName,
        String domain,
        String action,
        String targetType,
        Class<?>... parameterTypes
    ) {
        return new AuditSpec(serviceClassName, methodName, domain, action, targetType, parameterTypes);
    }

    private static List<AuditSpec> auditedSpecs() {
        Class<?> longType = Long.class;
        Class<?> listType = List.class;
        return List.of(
            spec("authentication.application.service.CredentialAuthenticationService", "loginByEmail", "AUTHENTICATION", "LOGIN", "MemberCredential", type("authentication.application.port.in.command.dto.LoginByEmailCommand")),
            spec("authentication.application.service.OAuthAuthenticationService", "loginWithOAuthAttributes", "AUTHENTICATION", "LOGIN", "OAuthAuthentication", type("authentication.domain.OAuthAttributes")),
            spec("authentication.application.service.OAuthAuthenticationService", "accessTokenLogin", "AUTHENTICATION", "LOGIN", "OAuthAuthentication", type("authentication.application.port.in.command.dto.AccessTokenLoginCommand")),
            spec("authentication.application.service.OAuthAuthenticationService", "authorizationCodeLogin", "AUTHENTICATION", "LOGIN", "OAuthAuthentication", type("authentication.application.port.in.command.dto.AuthorizationCodeLoginCommand")),
            spec("authentication.application.service.OAuthAuthenticationService", "linkOAuth", "AUTHENTICATION", "LINK", "MemberOAuth", type("authentication.application.port.in.command.dto.LinkOAuthCommand")),
            spec("authentication.application.service.OAuthAuthenticationService", "linkOAuthBulk", "AUTHENTICATION", "LINK", "MemberOAuth", listType),
            spec("authentication.application.service.OAuthAuthenticationService", "unlinkOAuth", "AUTHENTICATION", "UNLINK", "MemberOAuth", type("authentication.application.port.in.command.dto.UnlinkOAuthCommand")),

            spec("authorization.application.service.command.ChallengerRoleCommandService", "createChallengerRole", "AUTHORIZATION", "CREATE", "ChallengerRole", type("authorization.application.port.in.command.dto.CreateChallengerRoleCommand")),
            spec("authorization.application.service.command.ChallengerRoleCommandService", "updateChallengerRole", "AUTHORIZATION", "UPDATE", "ChallengerRole", type("authorization.application.port.in.command.dto.UpdateChallengerRoleCommand")),
            spec("authorization.application.service.command.ChallengerRoleCommandService", "deleteChallengerRole", "AUTHORIZATION", "DELETE", "ChallengerRole", type("authorization.application.port.in.command.dto.DeleteChallengerRoleCommand")),

            spec("member.application.service.MemberEmailCommandService", "changeEmail", "MEMBER", "UPDATE", "MemberEmail", type("member.application.port.in.command.dto.ChangeMemberEmailCommand")),
            spec("member.application.service.MemberCredentialCommandService", "registerCredentialByEmail", "MEMBER", "CREATE", "MemberCredential", type("member.application.port.in.command.dto.RegisterMemberCredentialByEmailCommand")),
            spec("member.application.service.MemberCredentialCommandService", "changePassword", "MEMBER", "UPDATE", "MemberCredential", type("member.application.port.in.command.dto.ChangeMemberPasswordCommand")),
            spec("member.application.service.MemberProfileCommandService", "upsert", "MEMBER", "UPDATE", "MemberProfile", type("member.application.port.in.command.dto.UpsertMemberProfileCommand")),
            spec("member.application.service.MemberProfileCommandService", "delete", "MEMBER", "DELETE", "MemberProfile", longType),

            spec("term.application.service.command.TermCommandService", "createTerms", "TERMS", "CREATE", "Term", type("term.application.port.in.command.dto.CreateTermCommand")),
            spec("term.application.service.command.TermAgreementCommandService", "createTermConsent", "TERMS", "SUBMIT", "TermConsent", type("term.application.port.in.command.dto.CreateTermConsentCommand")),

            spec("organization.application.service.UmcProductGenerationCommandService", "create", "ORGANIZATION", "CREATE", "UmcProductGeneration", type("organization.application.port.in.command.dto.CreateUmcProductGenerationCommand")),
            spec("organization.application.service.UmcProductGenerationCommandService", "update", "ORGANIZATION", "UPDATE", "UmcProductGeneration", type("organization.application.port.in.command.dto.UpdateUmcProductGenerationCommand")),
            spec("organization.application.service.UmcProductGenerationCommandService", "delete", "ORGANIZATION", "DELETE", "UmcProductGeneration", longType, longType),
            spec("organization.application.service.UmcProductMemberCommandService", "create", "ORGANIZATION", "CREATE", "UmcProductMember", type("organization.application.port.in.command.dto.CreateUmcProductMemberCommand")),
            spec("organization.application.service.UmcProductMemberCommandService", "updateProfile", "ORGANIZATION", "UPDATE", "UmcProductMember", type("organization.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand")),
            spec("organization.application.service.UmcProductSquadCommandService", "replaceParticipants", "ORGANIZATION", "UPDATE", "UmcProductSquad", type("organization.application.port.in.command.dto.ReplaceUmcProductSquadParticipantsCommand")),

            spec("challenger.application.service.ChallengerRecordCommandService", "consumeCode", "CHALLENGER", "CHECK", "ChallengerRecord", type("challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand")),

            spec("curriculum.application.service.command.CurriculumCommandService", "create", "CURRICULUM", "CREATE", "Curriculum", type("curriculum.application.port.in.command.dto.curriculum.CreateCurriculumCommand")),
            spec("curriculum.application.service.command.CurriculumCommandService", "edit", "CURRICULUM", "UPDATE", "Curriculum", type("curriculum.application.port.in.command.dto.curriculum.EditCurriculumCommand")),
            spec("curriculum.application.service.command.CurriculumCommandService", "delete", "CURRICULUM", "DELETE", "Curriculum", longType),
            spec("curriculum.application.service.command.OriginalWorkbookCommandService", "releaseAllDue", "CURRICULUM", "PUBLISH", "OriginalWorkbook"),
            spec("curriculum.application.service.command.MissionSubmissionCommandService", "create", "CURRICULUM", "SUBMIT", "MissionSubmission", type("curriculum.application.port.in.command.dto.workbook.mission.CreateMissionSubmissionCommand")),
            spec("curriculum.application.service.command.WeeklyBestWorkbookCommandService", "selectBest", "CURRICULUM", "APPROVE", "WeeklyBestWorkbook", type("curriculum.application.port.in.command.dto.workbook.CreateWeeklyBestWorkbookCommand")),

            spec("schedule.application.service.command.ScheduleParticipantCommandService", "decideAttendances", "SCHEDULE", "CHECK", "ScheduleAttendance", listType),

            spec("community.application.service.command.PostCommandService", "createPost", "COMMUNITY", "CREATE", "Post", type("community.application.port.in.command.post.dto.CreatePostCommand")),
            spec("community.application.service.command.PostCommandService", "updatePost", "COMMUNITY", "UPDATE", "Post", type("community.application.port.in.command.post.dto.UpdatePostCommand")),
            spec("community.application.service.command.PostCommandService", "deletePost", "COMMUNITY", "DELETE", "Post", longType),
            spec("community.application.service.command.ReportCommandService", "report", "COMMUNITY", "SUBMIT", "PostReport", type("community.application.port.in.command.report.dto.ReportPostCommand")),

            spec("blog.application.service.BlogContentCommandService", "create", "BLOG", "CREATE", "BlogContent", type("blog.application.port.in.command.dto.CreateBlogContentCommand")),
            spec("blog.application.service.BlogContentCommandService", "update", "BLOG", "UPDATE", "BlogContent", type("blog.application.port.in.command.dto.UpdateBlogContentCommand")),
            spec("blog.application.service.BlogContentCommandService", "delete", "BLOG", "DELETE", "BlogContent", type("blog.application.port.in.command.dto.DeleteBlogContentCommand")),
            spec("blog.application.service.BlogSeriesCommandService", "replaceContents", "BLOG", "REORDER", "BlogSeries", type("blog.application.port.in.command.dto.ReplaceBlogSeriesContentsCommand")),

            spec("notice.application.service.command.NoticeService", "createNotice", "NOTICE", "CREATE", "Notice", type("notice.application.port.in.command.dto.CreateNoticeCommand")),
            spec("notice.application.service.command.NoticeService", "updateNoticeTitleOrContent", "NOTICE", "UPDATE", "Notice", type("notice.application.port.in.command.dto.UpdateNoticeCommand")),
            spec("notice.application.service.command.NoticeService", "deleteNotice", "NOTICE", "DELETE", "Notice", type("notice.application.port.in.command.dto.DeleteNoticeCommand")),
            spec("notice.application.service.command.NoticeService", "remindNotice", "NOTICE", "REMIND", "Notice", type("notice.application.port.in.command.dto.SendNoticeReminderCommand")),
            spec("notice.application.service.command.NoticeVoteResponseCommandService", "submit", "NOTICE", "SUBMIT", "NoticeVoteResponse", type("notice.application.port.in.command.dto.SubmitNoticeVoteResponseCommand")),

            spec("survey.application.service.command.FormCommandService", "createDraft", "SURVEY", "CREATE", "Form", type("survey.application.port.in.command.dto.CreateDraftFormCommand")),
            spec("survey.application.service.command.FormCommandService", "publishForm", "SURVEY", "PUBLISH", "Form", type("survey.application.port.in.command.dto.PublishFormCommand")),
            spec("survey.application.service.command.QuestionCommandService", "reorderQuestions", "SURVEY", "REORDER", "Question", type("survey.application.port.in.command.dto.ReorderQuestionsCommand")),
            spec("survey.application.service.command.FormResponseCommandService", "submitImmediately", "SURVEY", "SUBMIT", "FormResponse", type("survey.application.port.in.command.dto.SubmitFormResponseCommand")),
            spec("survey.application.service.VoteService", "createVote", "SURVEY", "CREATE", "Vote", type("survey.application.port.in.command.dto.CreateVoteCommand")),

            spec("feedback.application.service.command.UserFeedbackResponseCommandService", "submit", "FEEDBACK", "SUBMIT", "UserFeedbackResponse", type("feedback.application.port.in.command.dto.SubmitUserFeedbackResponseCommand")),

            spec("project.application.service.command.ProjectCommandService", "create", "PROJECT", "CREATE", "Project", type("project.application.port.in.command.dto.CreateDraftProjectCommand")),
            spec("project.application.service.command.ProjectCommandService", "publish", "PROJECT", "PUBLISH", "Project", type("project.application.port.in.command.dto.PublishProjectCommand")),
            spec("project.application.service.command.ProjectCommandService", "delete", "PROJECT", "DELETE", "Project", type("project.application.port.in.command.dto.DeleteProjectCommand")),
            spec("project.application.service.command.ProjectApplicationCommandService", "submit", "PROJECT", "SUBMIT", "ProjectApplication", type("project.application.port.in.command.dto.SubmitProjectApplicationCommand")),
            spec("project.application.service.command.ProjectMatchingRoundFinalizationCommandService", "autoDecide", "PROJECT", "FINALIZE", "ProjectMatchingRound", longType, longType),
            spec("project.application.service.command.ProjectPartQuotaCommandService", "update", "PROJECT", "UPDATE", "ProjectPartQuota", type("project.application.port.in.command.dto.UpdatePartQuotasCommand")),

            spec("storage.application.service.FileCommandService", "getFileUploadUrl", "STORAGE", "CREATE", "FileMetadata", type("storage.application.port.in.command.dto.PrepareFileUploadCommand")),
            spec("storage.application.service.FileCommandService", "confirmUpload", "STORAGE", "CHECK", "FileMetadata", String.class),
            spec("storage.application.service.FileCommandService", "deleteFile", "STORAGE", "DELETE", "FileMetadata", type("storage.application.port.in.command.dto.DeleteFileCommand")),

            spec("certificate.application.service.CertificateCommandService", "issue", "CERTIFICATE", "CREATE", "Certificate", type("certificate.application.port.in.command.dto.IssueCertificateCommand")),
            spec("certificate.application.service.CertificateCommandService", "issueByAdmin", "CERTIFICATE", "CREATE", "Certificate", type("certificate.application.port.in.command.dto.AdminIssueCertificateCommand")),
            spec("certificate.application.service.CertificateCommandService", "revoke", "CERTIFICATE", "UPDATE", "Certificate", type("certificate.application.port.in.command.dto.RevokeCertificateCommand"))
        );
    }

    private record AuditSpec(
        String serviceClassName,
        String methodName,
        String domain,
        String action,
        String targetType,
        Class<?>[] parameterTypes
    ) {

        private String describe() {
            return "%s#%s expected domain=%s action=%s targetType=%s"
                .formatted(serviceClassName, methodName, domain, action, targetType);
        }
    }
}
